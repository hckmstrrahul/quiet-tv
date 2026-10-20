package dev.rahul.quiettv

import java.io.BufferedInputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Small, bounded HTTP/1.1 server for one foreground LAN editing session.
 * No arbitrary files, commands, CORS, cookies, persistent credentials or outbound traffic.
 */
class CompanionServer(
    val address: String,
    requestedPort: Int = 8765,
    private val asset: (String) -> ByteArray,
    private val api: (String, String, ByteArray) -> Response
) : AutoCloseable {
    data class Response(val status: Int, val mime: String, val bytes: ByteArray) {
        companion object {
            fun json(text: String, status: Int = 200) = Response(status, "application/json; charset=utf-8", text.toByteArray())
        }
    }
    private class HttpFailure(val status: Int) : Exception()
    private val random = SecureRandom()
    val code: String = "%06d".format(java.util.Locale.US, random.nextInt(1_000_000))
    private val socket = ServerSocket().apply {
        reuseAddress = true
        bind(java.net.InetSocketAddress(InetAddress.getByName(address), requestedPort), 8)
    }
    val port: Int get() = socket.localPort
    val url: String get() = "http://$address:$port"
    private val started = System.nanoTime()
    private val gate = Any()
    private val sessions = mutableMapOf<String, Long>()
    private var attempts = 0
    private var attemptWindow = started
    private val clients = ConcurrentHashMap.newKeySet<Socket>()
    private val pool = ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, ArrayBlockingQueue(6))
    @Volatile private var closed = false
    @Volatile var pairedCount = 0
        private set

    init {
        Thread({
            while (!closed) {
                try {
                    val client = socket.accept()
                    client.soTimeout = 5000
                    clients.add(client)
                    try { pool.execute { serve(client) } }
                    catch (_: java.util.concurrent.RejectedExecutionException) { clients.remove(client); client.close() }
                } catch (_: java.io.IOException) { if (!closed) close() }
            }
        }, "quiet-tv-listener").apply { isDaemon = true; start() }
    }
    private fun serve(client: Socket) {
        client.use {
            try {
                val input = BufferedInputStream(client.getInputStream())
                var headerSize = 0
                fun line(): String {
                    val bytes = java.io.ByteArrayOutputStream()
                    while (true) {
                        val c = input.read()
                        if (c < 0) throw HttpFailure(400)
                        headerSize++
                        if (headerSize > 8192) throw HttpFailure(431)
                        if (c == 10) break
                        bytes.write(c)
                    }
                    val raw = bytes.toByteArray().toString(Charsets.ISO_8859_1)
                    if (!raw.endsWith("\r")) throw HttpFailure(400)
                    return raw.dropLast(1)
                }
                val request = line().split(' ')
                if (request.size != 3 || request[2] != "HTTP/1.1") throw HttpFailure(400)
                val (method, path) = request
                if (method !in listOf("GET", "POST")) throw HttpFailure(405)
                val headers = mutableMapOf<String, String>()
                while (true) {
                    val text = line()
                    if (text.isEmpty()) break
                    val split = text.indexOf(':')
                    if (split <= 0) throw HttpFailure(400)
                    val name = text.substring(0, split).lowercase(java.util.Locale.US)
                    if (!name.matches(Regex("[a-z0-9-]+")) || headers.containsKey(name)) throw HttpFailure(400)
                    headers[name] = text.substring(split + 1).trim()
                }
                // Exact numeric Host also prevents DNS rebinding into this LAN server.
                if (headers["host"] != "$address:$port") throw HttpFailure(403)
                if (headers["origin"] != null && headers["origin"] != url) throw HttpFailure(403)
                if (headers.containsKey("transfer-encoding") || headers.containsKey("expect")) throw HttpFailure(400)
                val length = headers["content-length"]?.toIntOrNull() ?: if (method == "GET") 0 else throw HttpFailure(411)
                val limit = if (path == "/api/wallpaper") 4 * 1024 * 1024 else 65536
                if (length < 0 || length > limit) throw HttpFailure(413)
                if (method == "GET" && length != 0) throw HttpFailure(400)
                val isApi = path.startsWith("/api/")
                if (isApi && !authorized(headers["authorization"])) throw HttpFailure(401)
                val body = ByteArray(length)
                var read = 0
                while (read < length) {
                    val count = input.read(body, read, length - read)
                    if (count < 0) throw HttpFailure(400)
                    read += count
                }
                val response = when {
                    method == "POST" && path == "/pair" -> pair(body)
                    method == "GET" && path in listOf("/", "/app.css", "/app.js", "/google-sans.ttf") -> {
                        val name = if (path == "/") "index.html" else path.drop(1)
                        val mime = when (name) { "app.css" -> "text/css"; "app.js" -> "text/javascript"; "google-sans.ttf" -> "font/ttf"; else -> "text/html" }
                        Response(200, if (name == "google-sans.ttf") mime else "$mime; charset=utf-8", asset(name))
                    }
                    isApi -> api(method, path, body)
                    else -> throw HttpFailure(404)
                }
                send(client, response)
            } catch (e: HttpFailure) { runCatching { send(client, error(e.status)) } }
            catch (_: java.net.SocketTimeoutException) { runCatching { send(client, error(408)) } }
            catch (_: Exception) { runCatching { send(client, error(500)) } }
            finally { clients.remove(client) }
        }
    }
    private fun authorized(value: String?): Boolean = synchronized(gate) {
        val now = System.nanoTime()
        sessions.entries.removeAll { now - it.value > TimeUnit.HOURS.toNanos(1) }
        value?.startsWith("Bearer ") == true && sessions.keys.any {
            MessageDigest.isEqual(it.toByteArray(), value.drop(7).toByteArray())
        }
    }
    private fun pair(body: ByteArray): Response = synchronized(gate) {
        val now = System.nanoTime()
        if (now - started > TimeUnit.MINUTES.toNanos(10)) throw HttpFailure(410)
        if (now - attemptWindow > TimeUnit.MINUTES.toNanos(1)) { attempts = 0; attemptWindow = now }
        if (attempts >= 8) throw HttpFailure(429)
        attempts++
        if (!MessageDigest.isEqual(body, code.toByteArray())) throw HttpFailure(401)
        if (sessions.size >= 8) throw HttpFailure(429)
        val token = ByteArray(32).also { random.nextBytes(it) }.joinToString("") { "%02x".format(it) }
        sessions[token] = now
        pairedCount = sessions.size
        Response.json("""{"token":"$token"}""")
    }
    private fun error(status: Int) = Response.json("""{"error":"${when (status) {
        401 -> "Pair with the code on your TV."
        410 -> "Pairing expired. Reconnect from the TV."
        413 -> "File too large. Use an image under 4 MB."
        429 -> "Too many pairing attempts. Wait one minute."
        503 -> "TV is busy. Try again."
        else -> "Request could not be completed ($status)."
    }}"}""", status)
    private fun send(client: Socket, response: Response) {
        val status = when (response.status) { 200 -> "OK"; 400 -> "Bad Request"; 401 -> "Unauthorized"; 403 -> "Forbidden"; 404 -> "Not Found"; else -> "Error" }
        val csp = "default-src 'none'; script-src 'self'; style-src 'self'; font-src 'self'; img-src 'self' data: blob:; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'"
        val head = "HTTP/1.1 ${response.status} $status\r\nContent-Type: ${response.mime}\r\nContent-Length: ${response.bytes.size}\r\nConnection: close\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\nContent-Security-Policy: $csp\r\n\r\n"
        client.getOutputStream().apply { write(head.toByteArray(Charsets.ISO_8859_1)); write(response.bytes); flush() }
    }
    override fun close() {
        closed = true
        runCatching { socket.close() }
        clients.forEach { runCatching { it.close() } }
        pool.shutdownNow()
        synchronized(gate) { sessions.clear(); pairedCount = 0 }
    }
}
