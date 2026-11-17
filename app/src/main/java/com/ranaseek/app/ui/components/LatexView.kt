package com.ranaseek.app.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

/**
 * LaTeX 公式渲染（WebView + 本地 KaTeX，离线可用）。
 * @param text 纯公式（displayMode=true）或含 $...$ 的混合行（displayMode=false）
 */
@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
@Composable
fun LatexView(
    text: String,
    displayMode: Boolean,
    textColor: Int,
    modifier: Modifier = Modifier
) {
    // 初始高度给足，渲染完由 JS 回传真实高度
    var heightDp by remember(text, displayMode) {
        mutableIntStateOf(if (displayMode) 120 else 48)
    }
    val html = buildLatexHtml(text, displayMode, textColor)

    key(html) {
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .height(heightDp.dp),
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(0)
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.loadsImagesAutomatically = true
                    addJavascriptInterface(object {
                        @JavascriptInterface
                        fun onHeight(h: Int) {
                            // WebView 设置了 device-width viewport，CSS px ≈ dp，无需再除以 density
                            heightDp = h.coerceIn(24, 800)
                        }
                    }, "Android")
                    loadDataWithBaseURL(
                        "file:///android_asset/katex/",
                        html,
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        )
    }
}

private fun buildLatexHtml(text: String, displayMode: Boolean, textColor: Int): String {
    val color = String.format("#%06X", 0xFFFFFF and textColor)
    val json = JSONObject.quote(text)
    val mode = if (displayMode) "true" else "false"
    return """
        <!doctype html>
        <html>
        <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <link rel="stylesheet" href="katex.min.css">
        <style>
          html,body{margin:0;padding:0;background:transparent;}
          body{color:$color;font-size:16px;line-height:1.6;word-break:break-word;}
          .katex{color:$color;}
          #out{display:inline-block;max-width:100%;}
        </style>
        </head>
        <body>
        <div id="out"></div>
        <script src="katex.min.js"></script>
        <script>
        (function(){
          function esc(s){return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');}
          function measure(){
            try{
              var rect = document.getElementById('out').getBoundingClientRect();
              var h = Math.ceil(rect.height + 10);
              if (window.Android && window.Android.onHeight) window.Android.onHeight(h);
            }catch(e){}
          }
          var raw = $json;
          var out = document.getElementById('out');
          try {
            if ($mode) {
              out.innerHTML = katex.renderToString(raw, {displayMode: true, throwOnError: false});
            } else {
              var parts = raw.split(/(\$\$[^$]+\$\$|\$[^$\n]+\$)/g);
              var html = '';
              for (var i = 0; i < parts.length; i++) {
                var p = parts[i];
                if (p.length > 2 && p.charAt(0) === '$' && p.charAt(p.length-1) === '$') {
                  var inner = p.slice(1, -1);
                  var disp = false;
                  if (inner.length > 1 && inner.charAt(0) === '$' && inner.charAt(inner.length-1) === '$') {
                    inner = inner.slice(1, -1); disp = true;
                  }
                  html += katex.renderToString(inner, {displayMode: disp, throwOnError: false});
                } else {
                  html += esc(p);
                }
              }
              out.innerHTML = html;
            }
          } catch (e) { out.textContent = raw; }

          // 多次测量：公式渲染 / 字体加载完成后再上报高度
          try {
            if (window.ResizeObserver) {
              new ResizeObserver(measure).observe(out);
            }
          } catch (e) {}
          requestAnimationFrame(measure);
          setTimeout(measure, 100);
          setTimeout(measure, 400);
          setTimeout(measure, 900);
          if (document.fonts && document.fonts.ready) {
            document.fonts.ready.then(measure).catch(function(){});
          }
        })();
        </script>
        </body>
        </html>
    """.trimIndent()
}
