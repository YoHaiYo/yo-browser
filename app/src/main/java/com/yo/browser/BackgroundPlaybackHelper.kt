package com.yo.browser

import android.webkit.WebView

class BackgroundPlaybackHelper {

    fun injectBackgroundPlaybackJS(webView: WebView) {
        val jsCode = """
            (function() {
                try {
                    // visibilitychange 이벤트 완전 차단
                    try {
                        Object.defineProperty(document, 'hidden', {
                            get: function() { return false; },
                            configurable: true,
                            enumerable: true
                        });
                    } catch(e) {}
                    
                    try {
                        Object.defineProperty(document, 'visibilityState', {
                            get: function() { return 'visible'; },
                            configurable: true,
                            enumerable: true
                        });
                    } catch(e) {}
                    
                    // pagehide, visibilitychange 이벤트 리스너 차단
                    if (!window._originalAddEventListener) {
                        window._originalAddEventListener = window.addEventListener;
                    }
                    if (!document._originalAddEventListener) {
                        document._originalAddEventListener = document.addEventListener;
                    }
                    
                    window.addEventListener = function(type, listener, options) {
                        if (type === 'pagehide' || type === 'visibilitychange' || type === 'blur' || type === 'focusout') {
                            return;
                        }
                        return window._originalAddEventListener.call(this, type, listener, options);
                    };
                    
                    document.addEventListener = function(type, listener, options) {
                        if (type === 'visibilitychange' || type === 'pagehide') {
                            return;
                        }
                        return document._originalAddEventListener.call(this, type, listener, options);
                    };
                    
                    // beforeunload 방지
                    try {
                        window.addEventListener('beforeunload', function(e) {
                            e.preventDefault();
                            e.stopPropagation();
                            e.stopImmediatePropagation();
                            return '';
                        }, true);
                    } catch(e) {}
                    
                    // 페이지 언로드 방지
                    window.onbeforeunload = function() { return ''; };
                    window.onpagehide = function() {};
                    window.onblur = function() {};
                    
                    // 사용자가 직접 pause한 경우 추적
                    if (!window._userPaused) {
                        window._userPaused = false;
                    }
                    
                    // 비디오 pause 이벤트 감지 (사용자가 직접 pause한 경우)
                    function setupPauseDetection() {
                        try {
                            var videos = document.querySelectorAll('video');
                            for (var i = 0; i < videos.length; i++) {
                                var video = videos[i];
                                if (!video._pauseDetected) {
                                    video._pauseDetected = true;
                                    video.addEventListener('pause', function(e) {
                                        // 사용자가 직접 pause 버튼을 누른 경우
                                        // visibilitychange로 인한 pause가 아닌 경우만
                                        if (document.visibilityState === 'visible' && !document.hidden) {
                                            window._userPaused = true;
                                            // 5초 후 자동 재생 허용 (사용자가 다시 재생할 수 있도록)
                                            setTimeout(function() {
                                                window._userPaused = false;
                                            }, 5000);
                                        }
                                    }, true);
                                    
                                    video.addEventListener('play', function(e) {
                                        // 사용자가 재생 버튼을 누른 경우
                                        window._userPaused = false;
                                    }, true);
                                }
                            }
                        } catch(e) {}
                    }
                    
                    // 유튜브 플레이어가 visibilitychange로 인해 pause되지 않도록
                    function keepPlaying() {
                        // 사용자가 직접 pause한 경우는 재생하지 않음
                        if (window._userPaused) {
                            return;
                        }
                        
                        try {
                            var videos = document.querySelectorAll('video');
                            for (var i = 0; i < videos.length; i++) {
                                var video = videos[i];
                                // visibilitychange로 인해 pause된 경우만 재생
                                // (사용자가 직접 pause한 경우는 제외)
                                if (video && video.paused && !video.ended && document.visibilityState === 'visible') {
                                    // 약간의 지연을 두고 재생 (사용자 의도와 구분)
                                    setTimeout(function(v) {
                                        if (v && v.paused && !window._userPaused) {
                                            v.play().catch(function(e) {});
                                        }
                                    }, 100, video);
                                }
                            }
                            
                            // 유튜브 플레이어 API 사용 (사용자가 pause하지 않은 경우만)
                            if (!window._userPaused && window.ytplayer && window.ytplayer.config && window.ytplayer.config.args) {
                                var player = window.ytplayer.config.args;
                                if (player.autoplay === 0) {
                                    player.autoplay = 1;
                                }
                            }
                            
                            // 유튜브 iframe API (사용자가 pause하지 않은 경우만)
                            if (!window._userPaused && window.YT && window.YT.Player) {
                                var players = window.YT.get('player');
                                if (players) {
                                    for (var key in players) {
                                        try {
                                            var ytPlayer = players[key];
                                            if (ytPlayer && ytPlayer.getPlayerState && ytPlayer.getPlayerState() === 2) {
                                                // pause 상태인데 사용자가 pause하지 않은 경우만 재생
                                                setTimeout(function(player) {
                                                    if (player && player.getPlayerState && player.getPlayerState() === 2 && !window._userPaused) {
                                                        player.playVideo();
                                                    }
                                                }, 100, ytPlayer);
                                            }
                                        } catch(e) {}
                                    }
                                }
                            }
                        } catch(e) {}
                    }
                    
                    // pause 감지 설정
                    setupPauseDetection();
                    
                    // 주기적으로 재생 상태 확인 (이미 있으면 중복 방지)
                    if (!window._keepPlayingInterval) {
                        window._keepPlayingInterval = setInterval(function() {
                            // visibility 속성 재설정
                            try {
                                Object.defineProperty(document, 'hidden', {
                                    get: function() { return false; },
                                    configurable: true,
                                    enumerable: true
                                });
                            } catch(e) {}
                            try {
                                Object.defineProperty(document, 'visibilityState', {
                                    get: function() { return 'visible'; },
                                    configurable: true,
                                    enumerable: true
                                });
                            } catch(e) {}
                            
                            // pause 감지 재설정 (새로운 비디오 요소가 추가될 수 있음)
                            setupPauseDetection();
                            
                            // 비디오 계속 재생 (사용자가 pause하지 않은 경우만)
                            keepPlaying();
                        }, 1000);
                    }
                } catch(e) {
                    console.log('JS injection error:', e);
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

