
package com.marcogame;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import android.content.Intent;
import android.net.Uri;

public class MainActivity extends Activity {

    WebView web;
    ProgressBar progress;
    LinearLayout root;
    LinearLayout toolbar;

    String homeUrl = "https://marcorx6.blogspot.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // أشرطة الهاتف باللون الأسود مع إبقائها ظاهرة
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(0);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        // شريط التقدم
        progress = new ProgressBar(
            this, null,
            android.R.attr.progressBarStyleHorizontal
        );
        progress.setMax(100);
        progress.setProgressTintList(
            android.content.res.ColorStateList.valueOf(Color.RED)
        );

        root.addView(progress,
            new LinearLayout.LayoutParams(-1, 5));

        // المتصفح
        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);

        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setLoadWithOverviewMode(true);
        web.getSettings().setUseWideViewPort(true);

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int value) {
                progress.setProgress(value);
                progress.setVisibility(
                    value == 100 ? View.GONE : View.VISIBLE
                );
            }
        });

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

            @Override
            public void onReceivedError(
                WebView view,
                WebResourceRequest request,
                WebResourceError error
            ) {
                if (request.isForMainFrame()) {
                    Toast.makeText(
                        MainActivity.this,
                        "تحقق من اتصال الإنترنت",
                        Toast.LENGTH_LONG
                    ).show();
                }
            }
        });

        // روابط التحميل
        web.setDownloadListener((url, agent, disposition,
                mimetype, length) -> {
            try {
                startActivity(new Intent(
                    Intent.ACTION_VIEW, Uri.parse(url)
                ));
            } catch (Exception e) {
                Toast.makeText(
                    MainActivity.this,
                    "تعذر فتح رابط التحميل",
                    Toast.LENGTH_SHORT
                ).show();
            }
        });

        root.addView(web,
            new LinearLayout.LayoutParams(-1, 0, 1));

        // شريط الأزرار السفلي
        toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER);
        toolbar.setBackgroundColor(Color.rgb(20, 20, 20));
        toolbar.setPadding(5, 8, 5, 8);

        addButton("❮", () -> {
            if (web.canGoBack()) {
                web.goBack();
            } else {
                Toast.makeText(this,
                    "لا توجد صفحة سابقة",
                    Toast.LENGTH_SHORT).show();
            }
        });

        addButton("⟳", () -> web.reload());

        addButton("⌂", () -> web.loadUrl(homeUrl));

        root.addView(toolbar,
            new LinearLayout.LayoutParams(-1, 55));

        setContentView(root);

        web.loadUrl(homeUrl);
    }

    private void addButton(String text, Runnable action) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(Color.RED);
        button.setTextSize(25);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundColor(Color.TRANSPARENT);

        toolbar.addView(button,
            new LinearLayout.LayoutParams(0, -1, 1));

        button.setOnClickListener(v -> action.run());
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
