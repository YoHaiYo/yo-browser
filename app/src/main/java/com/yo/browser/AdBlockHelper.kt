package com.yo.browser

import android.webkit.WebView

class AdBlockHelper {

    fun injectAdBlockJS(webView: WebView) {
        val jsCode = """
            (function() {
                try {
                    // 광고 감지 및 강제 스킵 함수
                    function skipAds() {
                        try {
                            // 1. 광고 재생 상태 감지
                            var adShowing = document.querySelector('.ad-showing');
                            var adInterrupting = document.querySelector('.ad-interrupting');
                            
                            // 2. 비디오 요소 찾기
                            var videos = document.querySelectorAll('video');
                            
                            for (var i = 0; i < videos.length; i++) {
                                var video = videos[i];
                                if (!video || video.paused) continue;
                                
                                var isAd = false;
                                
                                // 광고 클래스로 감지
                                if (adShowing || adInterrupting) {
                                    var container = video.closest('.ad-showing, .ad-interrupting, #player-ads, .ytp-ad-module');
                                    if (container) {
                                        isAd = true;
                                    }
                                }
                                
                                // 비디오 시간으로 광고 감지
                                if (!isAd && video.currentTime < 1 && video.duration > 0 && video.duration < 60) {
                                    // 현재 시간이 1초 미만이고, 재생 시간이 60초 미만이면 광고로 판단
                                    // (일반 유튜브 영상은 최소 수십 초 이상)
                                    var parent = video.parentElement;
                                    if (parent && (parent.classList.contains('ad-showing') || 
                                                   parent.classList.contains('ad-interrupting') ||
                                                   parent.id === 'player-ads')) {
                                        isAd = true;
                                    }
                                }
                                
                                // 광고로 판단되면 강제 스킵
                                if (isAd && video.duration > 0) {
                                    try {
                                        video.currentTime = video.duration;
                                        video.play();
                                    } catch(e) {}
                                }
                            }
                            
                            // 3. 광고 스킵 버튼 자동 클릭
                            try {
                                var skipButtons = document.querySelectorAll('.ytp-ad-skip-button, .ytp-ad-skip-button-modern');
                                skipButtons.forEach(function(btn) {
                                    if (btn && btn.offsetParent !== null) {
                                        try {
                                            btn.click();
                                        } catch(e) {}
                                    }
                                });
                            } catch(e) {}
                            
                        } catch(e) {
                            console.log('Ad skip error:', e);
                        }
                    }
                    
                    // 4. 광고 DOM 요소 숨기기 (video element는 제거하지 않음)
                    function hideAdElements() {
                        try {
                            var adSelectors = [
                                'ytd-ad-slot-renderer',
                                'ytd-display-ad-renderer',
                                'ytd-companion-slot-renderer',
                                'ytd-video-masthead-ad-v3-renderer',
                                'ytd-promoted-sparkles-web-renderer',
                                'ytd-promoted-video-renderer',
                                '.ytp-ad-overlay-container',
                                '.ytp-ad-overlay-slot',
                                '.ytp-ad-text',
                                '.ytp-ad-overlay-close-button'
                            ];
                            
                            adSelectors.forEach(function(selector) {
                                try {
                                    var elements = document.querySelectorAll(selector);
                                    elements.forEach(function(el) {
                                        // video element는 제거하지 않음
                                        if (el && el.tagName !== 'VIDEO' && !el.querySelector('video')) {
                                            el.style.display = 'none';
                                        }
                                    });
                                } catch(e) {}
                            });
                            
                            // 광고 오버레이 숨기기
                            try {
                                var overlays = document.querySelectorAll('.ytp-ad-overlay-container, .ytp-ad-overlay-slot, .ad-overlay');
                                overlays.forEach(function(overlay) {
                                    if (overlay && overlay.tagName !== 'VIDEO') {
                                        overlay.style.display = 'none';
                                    }
                                });
                            } catch(e) {}
                            
                            // 광고 관련 iframe 숨기기
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
                                    }
                                });
                            } catch(e) {}
                            
                        } catch(e) {
                            console.log('Ad hide error:', e);
                        }
                    }
                    
                    // 즉시 실행
                    skipAds();
                    hideAdElements();
                    
                    // 주기적으로 실행 (500ms)
                    if (!window._adBlockInterval) {
                        window._adBlockInterval = setInterval(function() {
                            skipAds();
                            hideAdElements();
                        }, 500);
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
