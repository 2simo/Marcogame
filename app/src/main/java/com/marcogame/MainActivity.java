
package com.marcogame;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.webkit.*;
import android.widget.*;
import android.content.Intent;
import android.net.Uri;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends Activity {

    private WebView web;
    private ProgressBar progress;
    private SwipeRefreshLayout refreshLayout;

    private final String homeUrl =
        "https://marcorx6.blogspot.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setFitsSystemWindows(true);

        progress = new ProgressBar(
            this, null,
            android.R.attr.progressBarStyleHorizontal
        );

        progress.setMax(100);
        progress.setProgressTintList(
            android.content.res.ColorStateList.valueOf(Color.RED)
        );

        root.addView(progress,
            new LinearLayout.LayoutParams(-1, 4));

        refreshLayout = new SwipeRefreshLayout(this);
        refreshLayout.setColorSchemeColors(Color.RED);
        refreshLayout.setProgressBackgroundColorSchemeColor(
            Color.BLACK
        );

        web = new WebView(this);
        web.setBackgroundColor(Color.WHITE);

        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setLoadWithOverviewMode(true);
        web.getSettings().setUseWideViewPort(true);

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(
                WebView view, int value) {

                progress.setProgress(value);

                progress.setVisibility(
                    value == 100 ? View.GONE : View.VISIBLE
                );

                if (value == 100) {
                    refreshLayout.setRefreshing(false);
                }
            }
        });

        web.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                WebView view,
                WebResourceRequest request) {

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
                WebResourceError error) {

                if (request.isForMainFrame()) {
                    refreshLayout.setRefreshing(false);

                    Toast.makeText(
                        MainActivity.this,
                        "تحقق من اتصال الإنترنت",
                        Toast.LENGTH_LONG
                    ).show();
                }
            }
        });

        web.setDownloadListener(
            (url, agent, disposition, mimetype, length) -> {
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
            }
        );

        refreshLayout.addView(
            web,
            new SwipeRefreshLayout.LayoutParams(-1, -1)
        );

        refreshLayout.setOnRefreshListener(() -> {
            web.reload();
        });

        root.addView(refreshLayout,
            new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);

        web.loadUrl(homeUrl);
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
