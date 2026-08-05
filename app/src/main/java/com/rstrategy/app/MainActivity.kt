package com.rstrategy.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 缓存最新数据，供 JS 调用
    private var latestData: String = "{}"
    private var isPageLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)
        setupWebView()
        loadData()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        // 允许混合内容（HTTPS 页面加载 HTTP 资源）
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isPageLoaded = true
                // 页面加载完成后注入最新数据
                injectData()
            }
        }

        // 注册 JS 桥接接口
        webView.addJavascriptInterface(WebAppInterface(), "AndroidBridge")

        // 加载本地 HTML
        webView.loadUrl("file:///android_asset/dashboard.html")
    }

    private fun loadData() {
        scope.launch {
            val data = withContext(Dispatchers.IO) {
                fetchAllData()
            }
            latestData = data
            injectData()
        }
    }

    private fun injectData() {
        if (isPageLoaded && latestData != "{}") {
            val js = "javascript:window.receiveData('$latestData');"
            webView.evaluateJavascript(js, null)
        }
    }

    /**
     * 从腾讯财经接口获取指数数据
     * 创业板指: sz399006, 中证红利: sh000922
     */
    private fun fetchAllData(): String {
        return try {
            val chinext = fetchIndex("sz399006")
            val dividend = fetchIndex("sh000922")
            val history = fetchHistory("sz399006", "sh000922")

            val json = JSONObject()
            json.put("chinext", chinext)
            json.put("dividend", dividend)
            json.put("history", history)
            json.put("timestamp", System.currentTimeMillis())
            json.put("source", "tencent")
            json.toString()
        } catch (e: Exception) {
            android.util.Log.e("RStrategy", "数据获取失败: ${e.message}")
            fallbackData()
        }
    }

    private fun fetchIndex(code: String): JSONObject {
        val url = "https://qt.gtimg.cn/q=$code"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
        conn.setRequestProperty("Referer", "https://gu.qq.com/")

        val reader = BufferedReader(InputStreamReader(conn.inputStream, "GBK"))
        val sb = StringBuilder()
        reader.useLines { lines -> lines.forEach { sb.append(it) } }
        conn.disconnect()

        // 解析腾讯返回格式: v_sz399006="1~创业板指~399006~2128.02~..."
        val content = sb.toString()
        val regex = """v_\w+="([^"]+)"""".toRegex()
        val match = regex.find(content)
        val fields = match?.groupValues?.get(1)?.split("~") ?: return JSONObject()

        val obj = JSONObject()
        obj.put("name", fields.getOrNull(1) ?: "")
        obj.put("code", fields.getOrNull(2) ?: "")
        obj.put("price", fields.getOrNull(3)?.toDoubleOrNull() ?: 0.0)
        obj.put("yesterdayClose", fields.getOrNull(4)?.toDoubleOrNull() ?: 0.0)
        obj.put("open", fields.getOrNull(5)?.toDoubleOrNull() ?: 0.0)
        obj.put("high", fields.getOrNull(33)?.toDoubleOrNull() ?: 0.0)
        obj.put("low", fields.getOrNull(34)?.toDoubleOrNull() ?: 0.0)
        obj.put("volume", fields.getOrNull(36)?.toLongOrNull() ?: 0L)
        obj.put("change", fields.getOrNull(31)?.toDoubleOrNull() ?: 0.0)
        obj.put("changePercent", fields.getOrNull(32)?.toDoubleOrNull() ?: 0.0)
        return obj
    }

    /**
     * 获取历史日线数据用于绘制 R 值走势
     */
    private fun fetchHistory(code1: String, code2: String): JSONObject {
        val result = JSONObject()
        try {
            val h1 = fetchKline(code1)
            val h2 = fetchKline(code2)
            result.put(code1, h1)
            result.put(code2, h2)
        } catch (e: Exception) {
            android.util.Log.w("RStrategy", "历史数据获取失败: ${e.message}")
        }
        return result
    }

    private fun fetchKline(code: String): JSONArray {
        val url = "https://web.ifzq.gtimg.cn/appstock/app/fqkline/get?param=$code,day,,,60,qfq"
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10)")

        val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
        val sb = StringBuilder()
        reader.useLines { lines -> lines.forEach { sb.append(it) } }
        conn.disconnect()

        val json = JSONObject(sb.toString())
        val data = json.optJSONObject("data")?.optJSONObject(code)
        return data?.optJSONArray("day") ?: JSONArray()
    }

    private fun fallbackData(): String {
        // 网络失败时的兜底模拟数据
        val chinext = JSONObject().apply {
            put("name", "创业板指")
            put("code", "399006")
            put("price", 3535.14)
            put("yesterdayClose", 3489.12)
            put("open", 3495.0)
            put("high", 3550.0)
            put("low", 3480.0)
            put("volume", 120000000L)
            put("change", 46.02)
            put("changePercent", 1.32)
        }
        val dividend = JSONObject().apply {
            put("name", "中证红利")
            put("code", "000922")
            put("price", 5447.98)
            put("yesterdayClose", 5459.48)
            put("open", 5455.0)
            put("high", 5470.0)
            put("low", 5430.0)
            put("volume", 80000000L)
            put("change", -11.5)
            put("changePercent", -0.21)
        }
        val json = JSONObject()
        json.put("chinext", chinext)
        json.put("dividend", dividend)
        json.put("history", JSONObject())
        json.put("timestamp", System.currentTimeMillis())
        json.put("fallback", true)
        json.put("source", "fallback")
        return json.toString()
    }

    /**
     * JS 桥接接口
     */
    inner class WebAppInterface {

        @JavascriptInterface
        fun getData(): String {
            return latestData
        }

        @JavascriptInterface
        fun refreshData() {
            scope.launch {
                val data = withContext(Dispatchers.IO) { fetchAllData() }
                latestData = data
                withContext(Dispatchers.Main) { injectData() }
            }
        }

        @JavascriptInterface
        fun log(msg: String) {
            android.util.Log.d("RStrategy", msg)
        }

        @JavascriptInterface
        fun getVersion(): String {
            return "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
