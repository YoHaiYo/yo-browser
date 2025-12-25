package com.yo.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private var jsInjectionRunnable: Runnable? = null
    private val backgroundPlaybackHelper = BackgroundPlaybackHelper()
    private val adBlockHelper = AdBlockHelper()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 전체 화면 모드
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        webView = findViewById(R.id.webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
            setSupportZoom(true)
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            javaScriptCanOpenWindowsAutomatically = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        }
        
        // 백그라운드 재생을 위해 화면이 꺼져도 계속 실행
        webView.keepScreenOn = false

        // 광고 차단 WebViewClient 설정
        webView.webViewClient = AdBlockWebViewClient(
            onPageStarted = { view, url, favicon ->
                startAudioService()
            },
            onPageFinished = { view, url ->
                // 페이지 로드 완료 후 JS 주입
                view?.postDelayed({
                    backgroundPlaybackHelper.injectBackgroundPlaybackJS(webView)
                    adBlockHelper.injectAdBlockJS(webView)
                    // 주기적으로 재주입 (유튜브가 JS를 덮어쓸 수 있음)
                    view.postDelayed({
                        backgroundPlaybackHelper.injectBackgroundPlaybackJS(webView)
                        adBlockHelper.injectAdBlockJS(webView)
                    }, 2000)
                    view.postDelayed({
                        backgroundPlaybackHelper.injectBackgroundPlaybackJS(webView)
                        adBlockHelper.injectAdBlockJS(webView)
                    }, 5000)
                }, 500)
            }
        )

        webView.webChromeClient = WebChromeClient()

        // 주기적으로 JS 재주입 시작
        startPeriodicJSInjection()

        // 유튜브 모바일 사이트 로드
        webView.loadUrl("https://m.youtube.com")
    }
    
    private fun startPeriodicJSInjection() {
        jsInjectionRunnable = object : Runnable {
            override fun run() {
                if (::webView.isInitialized) {
                    backgroundPlaybackHelper.injectBackgroundPlaybackJS(webView)
                    adBlockHelper.injectAdBlockJS(webView)
                    handler.postDelayed(this, 2000) // 2초마다 재주입
                }
            }
        }
        handler.postDelayed(jsInjectionRunnable!!, 1000)
    }
    
    private fun stopPeriodicJSInjection() {
        jsInjectionRunnable?.let {
            handler.removeCallbacks(it)
        }
    }

    private fun startAudioService() {
        val serviceIntent = Intent(this, AudioService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }


    override fun onResume() {
        super.onResume()
        // WebView를 resume하지 않음 - 백그라운드 재생을 위해
        // webView.onResume() 호출하지 않음
        // 즉시 JS 재주입
        if (::webView.isInitialized) {
            backgroundPlaybackHelper.injectBackgroundPlaybackJS(webView)
            adBlockHelper.injectAdBlockJS(webView)
            handler.postDelayed({
                backgroundPlaybackHelper.injectBackgroundPlaybackJS(webView)
                adBlockHelper.injectAdBlockJS(webView)
            }, 300)
            handler.postDelayed({
                backgroundPlaybackHelper.injectBackgroundPlaybackJS(webView)
                adBlockHelper.injectAdBlockJS(webView)
            }, 1000)
        }
    }

    override fun onPause() {
        super.onPause()
        // webView.onPause() 호출하지 않음 - 백그라운드 재생을 위해
        // 백그라운드로 가도 JS 재주입 계속
        if (::webView.isInitialized) {
            handler.postDelayed({
                backgroundPlaybackHelper.injectBackgroundPlaybackJS(webView)
                adBlockHelper.injectAdBlockJS(webView)
            }, 200)
            handler.postDelayed({
                backgroundPlaybackHelper.injectBackgroundPlaybackJS(webView)
                adBlockHelper.injectAdBlockJS(webView)
            }, 800)
        }
    }
    
    override fun onStop() {
        super.onStop()
        // 백그라운드로 가도 JS 재주입 계속
        if (::webView.isInitialized) {
            handler.postDelayed({
                backgroundPlaybackHelper.injectBackgroundPlaybackJS(webView)
                adBlockHelper.injectAdBlockJS(webView)
            }, 300)
            handler.postDelayed({
                backgroundPlaybackHelper.injectBackgroundPlaybackJS(webView)
                adBlockHelper.injectAdBlockJS(webView)
            }, 1500)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPeriodicJSInjection()
        // 서비스 종료
        val serviceIntent = Intent(this, AudioService::class.java)
        stopService(serviceIntent)
        if (::webView.isInitialized) {
            webView.destroy()
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
