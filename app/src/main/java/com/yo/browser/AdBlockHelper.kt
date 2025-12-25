package com.yo.browser

import android.webkit.WebView

class AdBlockHelper {

    fun injectAdBlockJS(webView: WebView) {
        val jsCode = """
            (function() {
                try {
                    // 광고 요소 제거 함수
                    function removeAds() {
                        try {
                            // 유튜브 광고 관련 요소 제거
                            var adSelectors = [
                                'ytd-ad-slot-renderer',
                                'ytd-display-ad-renderer',
                                'ytd-companion-slot-renderer',
                                'ytd-video-masthead-ad-v3-renderer',
                                'ytd-promoted-sparkles-web-renderer',
                                'ytd-promoted-video-renderer',
                                '.video-ads',
                                '.ad-container',
                                '.ad-div',
                                '.advertisement',
                                '[class*="ad-"]',
                                '[id*="ad-"]',
                                '[id*="advertisement"]',
                                '.ytp-ad-module',
                                '.ytp-ad-overlay-container',
                                '.ytp-ad-overlay-slot',
                                '.ytp-ad-text',
                                '.ytp-ad-skip-button',
                                '.ytp-ad-overlay-close-button',
                                '#player-ads',
                                '.ad-showing',
                                '.ad-interrupting',
                                '.ad-overlay',
                                'ytd-ad-slot-renderer',
                                'ytd-display-ad-renderer',
                                'ytd-companion-slot-renderer'
                            ];
                            
                            adSelectors.forEach(function(selector) {
                                try {
                                    var elements = document.querySelectorAll(selector);
                                    elements.forEach(function(el) {
                                        if (el && el.parentNode) {
                                            el.style.display = 'none';
                                            el.remove();
                                        }
                                    });
                                } catch(e) {}
                            });
                            
                            // 광고 오버레이 제거
                            try {
                                var overlays = document.querySelectorAll('.ytp-ad-overlay-container, .ytp-ad-overlay-slot, .ad-overlay');
                                overlays.forEach(function(overlay) {
                                    overlay.style.display = 'none';
                                    overlay.remove();
                                });
                            } catch(e) {}
                            
                            // 광고 스킵 버튼 클릭 시도
                            try {
                                var skipButtons = document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, [class*="skip"]');
                                skipButtons.forEach(function(btn) {
                                    if (btn && btn.offsetParent !== null) {
                                        btn.click();
                                    }
                                });
                            } catch(e) {}
                            
                            // 비디오 광고 제거
                            try {
                                var videoAds = document.querySelectorAll('video.ad-showing, video.ad-interrupting');
                                videoAds.forEach(function(video) {
                                    if (video && video.parentNode) {
                                        var parent = video.parentNode;
                                        if (parent && parent.classList && (parent.classList.contains('ad-showing') || parent.classList.contains('ad-interrupting'))) {
                                            parent.style.display = 'none';
                                            parent.remove();
                                        }
                                    }
                                });
                            } catch(e) {}
                            
                            // 광고 관련 iframe 제거
                            try {
                                var iframes = document.querySelectorAll('iframe');
                                iframes.forEach(function(iframe) {
                                    var src = iframe.src || '';
                                    if (src.includes('googleads') || 
                                        src.includes('doubleclick') || 
                                        src.includes('googlesyndication') ||
                                        src.includes('adservice') ||
                                        src.includes('pagead')) {
                                        iframe.style.display = 'none';
                                        iframe.remove();
                                    }
                                });
                            } catch(e) {}
                            
                        } catch(e) {
                            console.log('Ad removal error:', e);
                        }
                    }
                    
                    // 즉시 실행
                    removeAds();
                    
                    // 주기적으로 광고 제거 (이미 있으면 중복 방지)
                    if (!window._adBlockInterval) {
                        window._adBlockInterval = setInterval(function() {
                            removeAds();
                        }, 1000);
                    }
                    
                    // DOM 변경 감지하여 새로 추가된 광고 제거
                    if (!window._adBlockObserver) {
                        window._adBlockObserver = new MutationObserver(function(mutations) {
                            removeAds();
                        });
                        
                        window._adBlockObserver.observe(document.body, {
                            childList: true,
                            subtree: true
                        });
                    }
                    
                } catch(e) {
                    console.log('Ad block JS error:', e);
                }
            })();
        """.trimIndent()
        
        try {
            webView.evaluateJavascript(jsCode, null)
        } catch(e: Exception) {
            // WebView가 destroy된 경우 무시
        }
    }
}

