
package pro.pushpro.app.util

import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL

object Net {
    @JvmStatic
    fun postJson(urlStr: String, json: String, headers: Map<String, String> = emptyMap()): Pair<Int, String> {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            connectTimeout = 15000
            readTimeout = 15000
        }
        conn.outputStream.use { os ->
            BufferedOutputStream(os).use { bos ->
                val bytes = json.toByteArray(Charsets.UTF_8)
                bos.write(bytes)
            }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val sb = StringBuilder()
        stream?.use { s ->
            BufferedReader(InputStreamReader(s)).use { br ->
                var line: String?
                while (true) {
                    line = br.readLine() ?: break
                    sb.append(line).append('\n')
                }
            }
        }
        conn.disconnect()
        return Pair(code, sb.toString())
    }
}
