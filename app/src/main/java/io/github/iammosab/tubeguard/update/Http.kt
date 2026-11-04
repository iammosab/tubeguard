package io.github.iammosab.tubeguard.update

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class HttpResult(val code: Int, val body: String = "")

interface Http {
    fun get(url: String): HttpResult
    /** [progress] gets the bytes written so far, now and then. */
    fun download(url: String, to: File, progress: (Long) -> Unit = {}): HttpResult
}

class UrlHttp : Http {
    override fun get(url: String): HttpResult = request(url) { c ->
        HttpResult(c.responseCode, (if (c.responseCode < 400) c.inputStream else c.errorStream)?.bufferedReader()?.readText().orEmpty())
    }

    override fun download(url: String, to: File, progress: (Long) -> Unit): HttpResult = request(url) { c ->
        if (c.responseCode == 200) {
            c.inputStream.use { input ->
                to.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var total = 0L
                    var reported = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        total += n
                        if (total - reported >= 512 * 1024) { reported = total; progress(total) }
                    }
                    progress(total)
                }
            }
        }
        HttpResult(c.responseCode)
    }

    private fun request(url: String, read: (HttpURLConnection) -> HttpResult): HttpResult = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000
        c.readTimeout = 60_000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("User-Agent", "TubeGuard")
        try { read(c) } finally { c.disconnect() }
    } catch (e: Exception) {
        HttpResult(-1, e.toString())
    }
}
