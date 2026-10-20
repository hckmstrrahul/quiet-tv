package dev.rahul.quiettv

import org.junit.Assert.*
import org.junit.Test
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

class CompanionServerTest {
    private fun server(calls: AtomicInteger = AtomicInteger()) = CompanionServer(
        "127.0.0.1", 0, { "asset:$it".toByteArray() },
        { _, _, _ -> calls.incrementAndGet(); CompanionServer.Response.json("""{"ok":true}""") }
    )
    private fun raw(s: CompanionServer, request: String): String = Socket(s.address, s.port).use {
        it.soTimeout = 2000
        it.getOutputStream().write(request.toByteArray())
        it.getInputStream().readBytes().toString(Charsets.UTF_8)
    }
    private fun request(s: CompanionServer, path: String, body: String? = null, headers: String = ""): String =
        raw(s, "${if (body == null) "GET" else "POST"} $path HTTP/1.1\r\nHost: ${s.address}:${s.port}\r\n$headers" +
            (body?.let { "Content-Length: ${it.toByteArray().size}\r\n" } ?: "") + "\r\n" + (body ?: ""))
    private fun token(s: CompanionServer): String {
        val result = request(s, "/pair", s.code)
        assertTrue(result.startsWith("HTTP/1.1 200"))
        return Regex("\"token\":\"([0-9a-f]{64})\"").find(result)!!.groupValues[1]
    }
    @Test fun assetsArePublicButMutationsNeedPairing() {
        val calls = AtomicInteger()
        server(calls).use { s ->
            assertTrue(request(s, "/").contains("asset:index.html"))
            assertTrue(request(s, "/").contains("frame-ancestors 'none'"))
            assertTrue(request(s, "/api/state").startsWith("HTTP/1.1 401"))
            assertTrue(request(s, "/api/action", "{}").startsWith("HTTP/1.1 401"))
            assertEquals(0, calls.get())
            val t = token(s)
            assertTrue(request(s, "/api/action", "{}", "Authorization: Bearer $t\r\nOrigin: ${s.url}\r\n").startsWith("HTTP/1.1 200"))
            assertEquals(1, calls.get())
        }
    }
    @Test fun rejectsCrossOriginAndDnsRebinding() {
        server().use { s ->
            val t = token(s)
            assertTrue(request(s, "/api/state", headers = "Authorization: Bearer $t\r\nOrigin: https://evil.example\r\n").startsWith("HTTP/1.1 403"))
            assertTrue(raw(s, "GET / HTTP/1.1\r\nHost: evil.example:${s.port}\r\n\r\n").startsWith("HTTP/1.1 403"))
        }
    }
    @Test fun localFontIsServedWithBrowserPolicyWithoutExposingOtherFiles() {
        server().use { s ->
            val font = request(s, "/google-sans.ttf")
            assertTrue(font.startsWith("HTTP/1.1 200"))
            assertTrue(font.contains("Content-Type: font/ttf\r\n"))
            assertTrue(font.contains("font-src 'self'"))
            assertTrue(font.endsWith("asset:google-sans.ttf"))
            assertTrue(request(s, "/other.ttf").startsWith("HTTP/1.1 404"))
            assertTrue(request(s, "/../font/google_sans.ttf").startsWith("HTTP/1.1 404"))
        }
    }
    @Test fun wrongCodesAreRateLimited() {
        server().use { s ->
            repeat(8) { assertTrue(request(s, "/pair", "invalid").startsWith("HTTP/1.1 401")) }
            assertTrue(request(s, "/pair", s.code).startsWith("HTTP/1.1 429"))
        }
    }
    @Test fun requestBoundsAndFramingAreEnforced() {
        server().use { s ->
            val head = "POST /pair HTTP/1.1\r\nHost: ${s.address}:${s.port}\r\n"
            assertTrue(raw(s, head + "Content-Length: 65537\r\n\r\n").startsWith("HTTP/1.1 413"))
            assertTrue(raw(s, head + "Content-Length: 0\r\nContent-Length: 0\r\n\r\n").startsWith("HTTP/1.1 400"))
            assertTrue(raw(s, head + "Transfer-Encoding: chunked\r\n\r\n").startsWith("HTTP/1.1 400"))
            assertTrue(request(s, "/../../private").startsWith("HTTP/1.1 404"))
        }
    }
    @Test fun tokensDoNotSurviveServerRestart() {
        val old = server().use { token(it) }
        server().use { s ->
            assertTrue(request(s, "/api/state", headers = "Authorization: Bearer $old\r\n").startsWith("HTTP/1.1 401"))
        }
    }
}
