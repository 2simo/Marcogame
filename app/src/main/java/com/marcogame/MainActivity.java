
package com.marcogame;

import android.app.*;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;

public class MainActivity extends Activity {

    WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // وضع ملء الشاشة
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);

        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setLoadWithOverviewMode(true);
        web.getSettings().setUseWideViewPort(true);

        // إبقاء المدونة داخل التطبيق
        web.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                WebView view,
                WebResourceRequest request
            ) {
                Uri uri = request.getUrl();
                String host = uri.getHost();

                if (host != null &&
                    (host.equals("marcorx6.blogspot.com") ||
                     host.equals("www.marcorx6.blogspot.com"))) {
                    return false;
                }

                if ("http".equals(uri.getScheme()) ||
                    "https".equals(uri.getScheme())) {

                    try {
                        startActivity(new Intent(
                            Intent.ACTION_VIEW, uri
                        ));
                    } catch (Exception e) {
                        Toast.makeText(
                            MainActivity.this,
                            "تعذر فتح الرابط",
                            Toast.LENGTH_SHORT
                        ).show();
                    }

                    return true;
                }

                return true;
            }
        });

        // روابط التحميل
        web.setDownloadListener((url, userAgent,
                contentDisposition, mimetype, contentLength) -> {
            try {
                startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                ));
            } catch (Exception e) {
                Toast.makeText(
                    MainActivity.this,
                    "تعذر فتح رابط التحميل",
                    Toast.LENGTH_SHORT
                ).show();
            }
        });

        web.loadUrl("https://marcorx6.blogspot.com");

        setContentView(web);
    }

    // زر الرجوع
    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
