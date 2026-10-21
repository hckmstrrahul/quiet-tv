package dev.rahul.quiettv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.Inet4Address
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/** All preference operations run on the UI thread, shared with the TV editor. */
class CompanionEditor(
    private val context: Context,
    private val apps: () -> List<Pair<String, String>>,
    private val preview: () -> JSONObject,
    private val changed: () -> Unit
) {
    private val prefs = context.getSharedPreferences("home", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    @Volatile var server: CompanionServer? = null
        private set
    fun start(): CompanionServer {
        server?.let { return it }
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val address = cm.getLinkProperties(cm.activeNetwork)?.linkAddresses?.map { it.address }
            ?.filterIsInstance<Inet4Address>()?.firstOrNull { it.isSiteLocalAddress }?.hostAddress
            ?: throw IllegalStateException("Connect the TV to your home Wi-Fi or Ethernet first.")
        return CompanionServer(address, asset = ::asset, api = ::handle)
            .also { server = it }
    }
    // openRawResource reads file-backed resources; share the bundled TTF without a second APK copy.
    @android.annotation.SuppressLint("ResourceType")
    private fun asset(name: String): ByteArray =
        (if (name == "google-sans.ttf") context.resources.openRawResource(R.font.google_sans)
        else context.assets.open("companion/$name")).use { it.readBytes() }
    fun stop() { val old = server; server = null; old?.close() }
    private fun strings(key: String) = JSONArray(prefs.getString(key, "[]")).let { a -> List(a.length()) { a.getString(it) } }
    private fun response(value: JSONObject, status: Int = 200) = CompanionServer.Response.json(value.toString(), status)
    private fun error(message: String, status: Int = 400) = response(JSONObject().put("error", message), status)
    private fun onMain(block: () -> CompanionServer.Response): CompanionServer.Response {
        val active = server ?: return error("Open Connect device on the TV again.", 503)
        val task = FutureTask {
            if (server !== active) error("Session ended. Reconnect from the TV.", 401) else block()
        }
        main.post(task)
        return try { task.get(4, TimeUnit.SECONDS) }
        catch (_: Exception) { task.cancel(false); error("TV is busy. Try again.", 503) }
    }
    private fun handle(method: String, path: String, bytes: ByteArray): CompanionServer.Response {
        if (method == "POST" && path == "/api/wallpaper") return wallpaper(bytes)
        if(method=="GET" && path.startsWith("/api/backgrounds/")) {
            val id=path.removePrefix("/api/backgrounds/")
            if(!WallpaperCatalog.valid(id))return error("Unknown picture",404)
            return CompanionServer.Response(200,"image/jpeg",context.assets.open("wallpapers/thumbs/$id.jpg").use {it.readBytes()})
        }
        return onMain {
            try {
                when {
                    method == "GET" && path == "/api/state" -> response(state())
                    method == "GET" && path == "/api/preview" -> response(preview())
                    method == "GET" && path == "/api/backup" -> response(config())
                    method == "POST" && path == "/api/action" -> {
                        action(JSONObject(bytes.toString(Charsets.UTF_8))); changed(); response(state())
                    }
                    method == "POST" && path == "/api/restore" -> {
                        restore(JSONObject(bytes.toString(Charsets.UTF_8))); changed(); response(state())
                    }
                    else -> error("Unknown action.", 404)
                }
            } catch (e: IllegalArgumentException) { error(e.message ?: "Invalid settings.") }
            catch (_: org.json.JSONException) { error("Invalid settings file or request.") }
        }
    }
    private fun config(): JSONObject {
        return JSONObject().put("format", "quiet-tv-layout").put("version", 4)
            .put("order", JSONArray(strings("order")))
            .put("accent", prefs.getString("accent", "#AADBCB")).put("background", prefs.getString("background", "#0C1018"))
            .put("columns", prefs.getInt("columns", 6))
            .put("wallpaperId",prefs.getString("wallpaperId",WallpaperCatalog.defaultId)).put("wallpaperShuffle",prefs.getBoolean("wallpaperShuffle",false))
    }
    private fun state(): JSONObject {
        val known = apps()
        val saved = strings("order")
        val sorted = known.sortedBy { saved.indexOf(it.first).let { i -> if (i < 0) Int.MAX_VALUE else i } }
        val list = JSONArray()
        sorted.forEach { (pkg, label) -> list.put(JSONObject().put("package", pkg).put("label", label)) }
        return config().put("wallpapers",JSONArray(WallpaperCatalog.pictures.map {JSONObject().put("id",it.id).put("title",it.title)})).put("apps", list).put("photo", prefs.getBoolean("photo", false))
            .put("connectedDevices", server?.pairedCount ?: 0).put("appVersion", context.packageManager.getPackageInfo(context.packageName, 0).versionName)
    }
    private fun color(value: String): String {
        require(Regex("#[0-9a-fA-F]{6}").matches(value)) { "Use a color like #AADBCB." }; return value.uppercase()
    }
    private fun action(body: JSONObject) {
        val edit = prefs.edit()
        val known = apps().map { it.first }
        fun pkg(): String = body.getString("package").also { require(it in known) { "That TV app is no longer installed or enabled." } }
        when (body.getString("action")) {
            "move" -> {
                val p = pkg(); val direction = body.getInt("direction"); require(direction == -1 || direction == 1)
                val saved = strings("order")
                val order = known.sortedBy { saved.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } }.toMutableList()
                val at = order.indexOf(p); val to = at + direction
                if (to in order.indices) java.util.Collections.swap(order, at, to)
                edit.putString("order", JSONArray(order).toString())
            }
            "appearance" -> {
                if (body.has("accent")) edit.putString("accent", color(body.getString("accent")))
                if (body.has("background")) edit.putString("background", color(body.getString("background"))).putBoolean("photo", false)
                if (body.has("columns")) { val c = body.getInt("columns"); require(c in 4..6); edit.putInt("columns", c) }
            }
            "backdrop" -> {
                val id=body.getString("id");require(id=="shuffle" || WallpaperCatalog.valid(id)) {"Choose a picture from the gallery."}
                edit.putBoolean("photo",false).putBoolean("wallpaperShuffle",id=="shuffle")
                if(id!="shuffle")edit.putString("wallpaperId",id)
            }
            "clear-wallpaper" -> edit.putBoolean("photo", false)
            else -> throw IllegalArgumentException("Unknown action.")
        }
        edit.apply()
    }
    private fun restore(body: JSONObject) {
        require(body.getString("format") == "quiet-tv-layout" && body.getInt("version") in 1..4) { "Choose a Quiet TV layout backup." }
        fun array(key: String): List<String> {
            val a = body.getJSONArray(key); require(a.length() <= 200)
            return List(a.length()) { a.getString(it) }.also { require(it.distinct().size == it.size) }
        }
        val installed = apps().map { it.first }.toSet()
        val order = array("order").filter { it in installed }
        val accent = color(body.getString("accent")); val background = color(body.getString("background"))
        val columns = body.getInt("columns"); require(columns in 4..6)
        val picture=if(body.getInt("version")>=3)body.getString("wallpaperId") else WallpaperCatalog.defaultId
        require(WallpaperCatalog.valid(picture)) {"Unknown wallpaper."}
        val shuffle=if(body.getInt("version")>=3)body.getBoolean("wallpaperShuffle") else false
        prefs.edit().apply {
            putString("wallpaperId",picture);putBoolean("wallpaperShuffle",shuffle)
            prefs.all.keys.filter { it=="groups" || it.startsWith("group:") }.forEach { remove(it) }
            remove("favorites"); putString("order", JSONArray(order).toString())
            putString("accent", accent); putString("background", background); putInt("columns", columns)
            putBoolean("photo", false) // Photo binary is deliberately not included in layout backups.
        }.apply()
    }
    private fun wallpaper(bytes: ByteArray): CompanionServer.Response {
        val active = server ?: return error("Reconnect from the TV.", 401)
        var temp: File? = null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Choose a JPEG, PNG or WebP image." }
            var sample = 1
            while (bounds.outWidth / sample > 1920 || bounds.outHeight / sample > 1080) sample *= 2
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: throw IllegalArgumentException("Image could not be read.")
            temp = File.createTempFile("companion-", ".jpg", context.filesDir)
            try { temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) } } finally { bitmap.recycle() }
            val staged = temp
            onMain {
                if (server !== active) return@onMain error("Session ended.", 401)
                if (!staged.renameTo(File(context.filesDir, "wallpaper.jpg"))) return@onMain error("Couldn't save the image.", 500)
                prefs.edit().putBoolean("photo", true).apply(); changed(); response(state())
            }
        } catch (e: IllegalArgumentException) { error(e.message ?: "Invalid image.") }
        catch (_: Exception) { error("Couldn't save the image.", 500) }
        finally { temp?.delete() }
    }
}
