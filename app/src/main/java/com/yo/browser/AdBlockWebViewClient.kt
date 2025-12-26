package com.yo.browser

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

class AdBlockWebViewClient(
    private val onPageStarted: ((WebView?, String?, Bitmap?) -> Unit)? = null,
    private val onPageFinished: ((WebView?, String?) -> Unit)? = null
) : WebViewClient() {

    // 최소한의 광고 관련 URL 패턴만 차단 (영상 스트림은 절대 차단하지 않음)
    private val adPatterns = listOf(
        "youtube.com/api/stats/ads",
        "youtube.com/pagead/",
        "doubleclick",
        "googlesyndication",
        "adservice",
        "/tracking",
        "/beacon"
    )

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

        // 영상 스트림은 절대 차단하지 않음
        if (url.contains("googlevideo.com/videoplayback") || 
            url.contains("videoplayback")) {
            return super.shouldInterceptRequest(view, request)
        }

        // 광고 URL만 차단
        if (isAdRequest(url)) {
            return WebResourceResponse("text/plain", "utf-8", java.io.ByteArrayInputStream(ByteArray(0)))
        }

        return super.shouldInterceptRequest(view, request)
    }

    private fun isAdRequest(url: String): Boolean {
        val lowerUrl = url.lowercase()
        return adPatterns.any { pattern ->
            lowerUrl.contains(pattern.lowercase())
        }
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
    ): Boolean {
        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStarted?.invoke(view, url, favicon)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinished?.invoke(view, url)
    }
}
