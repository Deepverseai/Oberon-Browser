package com.antigravity.oberon.preview

import android.webkit.WebView

object VirtualCursor {

    private const val CURSOR_CSS_JS = """
        (function() {
            if (document.getElementById('__oberon_cursor__')) return;
            
            const style = document.createElement('style');
            style.id = '__oberon_cursor_style__';
            style.innerHTML = `
                #__oberon_cursor__ {
                    position: fixed;
                    top: 0;
                    left: 0;
                    width: 20px;
                    height: 20px;
                    border-radius: 50%;
                    background: radial-gradient(circle, #6EE7B7 30%, rgba(110, 231, 183, 0.4) 70%, transparent 100%);
                    box-shadow: 0 0 12px #6EE7B7, 0 0 24px rgba(110, 231, 183, 0.6);
                    pointer-events: none;
                    z-index: 2147483647;
                    transition: transform 0.35s cubic-bezier(0.25, 1, 0.5, 1), opacity 0.2s ease;
                    transform: translate(-100px, -100px);
                    opacity: 0;
                }
                .oberon-ripple {
                    position: fixed;
                    border-radius: 50%;
                    border: 2px solid #6EE7B7;
                    pointer-events: none;
                    z-index: 2147483646;
                    animation: oberon-ripple-anim 0.6s cubic-bezier(0, 0.2, 0.8, 1) forwards;
                }
                @keyframes oberon-ripple-anim {
                    0% { transform: scale(0.2); opacity: 1; }
                    100% { transform: scale(2.5); opacity: 0; }
                }
            `;
            document.head.appendChild(style);

            const cursor = document.createElement('div');
            cursor.id = '__oberon_cursor__';
            document.body.appendChild(cursor);

            window.__oberon_glide = function(x, y) {
                cursor.style.opacity = '1';
                cursor.style.transform = 'translate(' + (x - 10) + 'px, ' + (y - 10) + 'px)';
            };

            window.__oberon_ripple = function(x, y) {
                const ripple = document.createElement('div');
                ripple.className = 'oberon-ripple';
                ripple.style.width = '30px';
                ripple.style.height = '30px';
                ripple.style.left = (x - 15) + 'px';
                ripple.style.top = (y - 15) + 'px';
                document.body.appendChild(ripple);
                setTimeout(() => ripple.remove(), 650);
            };
        })();
    """

    fun injectCursor(webView: WebView) {
        webView.evaluateJavascript(CURSOR_CSS_JS, null)
    }

    fun glideCursor(webView: WebView, x: Float, y: Float) {
        val js = "window.__oberon_glide && window.__oberon_glide($x, $y);"
        webView.evaluateJavascript(js, null)
    }

    fun showClickRipple(webView: WebView, x: Float, y: Float) {
        val js = "window.__oberon_ripple && window.__oberon_ripple($x, $y);"
        webView.evaluateJavascript(js, null)
    }
}
