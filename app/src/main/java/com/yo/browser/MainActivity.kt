package com.yo.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

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
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                startAudioService()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectBackgroundPlaybackJS()
            }
        }

        webView.webChromeClient = WebChromeClient()

        // 유튜브 모바일 사이트 로드
        webView.loadUrl("https://m.youtube.com")
    }

    private fun startAudioService() {
        val serviceIntent = Intent(this, AudioService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun injectBackgroundPlaybackJS() {
        val jsCode = """
            (function() {
                try {
                    // visibilitychange 이벤트 방지
                    Object.defineProperty(document, 'hidden', {
                        get: function() { return false; },
                        configurable: true
                    });
                    
                    Object.defineProperty(document, 'visibilityState', {
                        get: function() { return 'visible'; },
                        configurable: true
                    });
                    
                    // pagehide 이벤트 방지
                    var originalAddEventListener = window.addEventListener;
                    window.addEventListener = function(type, listener, options) {
                        if (type === 'pagehide' || type === 'visibilitychange') {
                            return;
                        }
                        originalAddEventListener.call(this, type, listener, options);
                    };
                    
                    // beforeunload 방지
                    window.addEventListener('beforeunload', function(e) {
                        e.preventDefault();
                        e.stopPropagation();
                        return '';
                    }, true);
                    
                    // blur 이벤트 방지
                    window.addEventListener('blur', function(e) {
                        e.preventDefault();
                        e.stopPropagation();
                    }, true);
                } catch(e) {
                    console.log('JS injection error:', e);
                }
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(jsCode, null)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        injectBackgroundPlaybackJS()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
