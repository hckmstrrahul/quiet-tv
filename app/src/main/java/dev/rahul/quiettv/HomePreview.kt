package dev.rahul.quiettv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/** Paired clients see only our Home views, never other apps, dialogs or the QR code. */
object HomePreview {
    private fun image(view: View, width: Int): String {
        require(view.width > 0 && view.height > 0) { "Home is still loading. Try Refresh." }
        val scale = width.toFloat() / view.width
        val bitmap = Bitmap.createBitmap(width, (view.height * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        return try {
            view.draw(Canvas(bitmap).apply { scale(scale, scale) })
            val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
            "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } finally { bitmap.recycle() }
    }
    fun capture(body: ViewGroup, packages: List<String>): JSONObject {
        val tiles = JSONArray()
        var index = 0
        for (rowIndex in 1 until body.childCount) {
            if (index >= 18) break // At most three six-column rows; bound UI-thread encoding work.
            val row = body.getChildAt(rowIndex) as? ViewGroup ?: continue
            for (column in 0 until row.childCount) {
                if (index >= 18) break
                val cell = row.getChildAt(column) as? ViewGroup ?: continue
                val tile = cell.getChildAt(0) ?: continue
                if (index < packages.size) tiles.put(JSONObject().put("package", packages[index++]).put("image", image(tile, 192)))
            }
        }
        return JSONObject().put("header", image(body.getChildAt(0), 876)).put("tiles", tiles)
    }
}
