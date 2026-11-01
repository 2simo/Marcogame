
package com.marcogame;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.webkit.*;
import android.widget.*;
import android.content.Intent;
import android.net.Uri;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends Activity {

    private WebView web;
    private ProgressBar progress;
    private SwipeRefreshLayout refreshLayout;
    private FrameLayout mainFrame;
    private LinearLayout splashScreen;

    private boolean splashHidden = false;

    private final String homeUrl =
        "https://marcorx6.blogspot.com";

    private final int BLACK = Color.rgb(10, 8, 14);
    private final int PURPLE = Color.rgb(65, 35, 95);
    private final int WHITE = Color.WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BLACK);
        root.setFitsSystemWindows(true);

        // شريط التقدم البنفسجي
        progress = new ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        );

        progress.setMax(100);
        progress.setProgressTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(145, 75, 210)
            )
        );

        root.addView(
            progress,
            new LinearLayout.LayoutParams(-1, 4)
        );

        // إطار المحتوى
        mainFrame = new FrameLayout(this);

        refreshLayout = new SwipeRefreshLayout(this);
        refreshLayout.setColorSchemeColors(
            Color.rgb(160, 90, 220)
        );
        refreshLayout.setProgressBackgroundColorSchemeColor(BLACK);

        // إعداد WebView
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
                }

                return true;
            }

            @Override
            public void onPageFinished(
                WebView view, String url) {

                refreshLayout.setRefreshing(false);
                hideSplash();
            }

            @Override
            public void onReceivedError(
                WebView view,
                WebResourceRequest request,
                WebResourceError error) {

                if (request.isForMainFrame()) {
                    refreshLayout.setRefreshing(false);
                    hideSplash();

                    Toast.makeText(
                        MainActivity.this,
                        "تحقق من اتصال الإنترنت",
                        Toast.LENGTH_LONG
                    ).show();
                }
            }
        });

        // التعامل مع روابط التحميل
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

        mainFrame.addView(
            refreshLayout,
            new FrameLayout.LayoutParams(-1, -1)
        );

        // شاشة الافتتاح
        createSplashScreen();

        mainFrame.addView(
            splashScreen,
            new FrameLayout.LayoutParams(-1, -1)
        );

        root.addView(
            mainFrame,
            new LinearLayout.LayoutParams(-1, 0, 1)
        );

        setContentView(root);

        // فتح المدونة
        web.loadUrl(homeUrl);
    }

    private void createSplashScreen() {

        splashScreen = new LinearLayout(this);
        splashScreen.setOrientation(LinearLayout.VERTICAL);
        splashScreen.setGravity(Gravity.CENTER);
        splashScreen.setPadding(30, 30, 30, 30);

        GradientDrawable background = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[] {
                Color.rgb(8, 7, 12),
                Color.rgb(32, 17, 48),
                Color.rgb(10, 8, 14)
            }
        );

        splashScreen.setBackground(background);

        // شعار التطبيق
        ImageView logo = new ImageView(this);

        logo.setImageResource(R.mipmap.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);

        LinearLayout.LayoutParams logoParams =
            new LinearLayout.LayoutParams(190, 190);

        logoParams.gravity = Gravity.CENTER;
        logoParams.bottomMargin = 22;

        splashScreen.addView(logo, logoParams);

        // اسم التطبيق
        TextView title = new TextView(this);

        title.setText("MARCO RX6");
        title.setTextColor(WHITE);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);

        splashScreen.addView(
            title,
            new LinearLayout.LayoutParams(-1, -2)
        );

        // وصف صغير
        TextView subtitle = new TextView(this);

        subtitle.setText("WELCOME TO MARCO RX6");
        subtitle.setTextColor(
            Color.rgb(185, 145, 220)
        );
        subtitle.setTextSize(12);
        subtitle.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams subtitleParams =
            new LinearLayout.LayoutParams(-1, -2);

        subtitleParams.topMargin = 10;

        splashScreen.addView(subtitle, subtitleParams);

        // مؤشر التحميل
        ProgressBar loading = new ProgressBar(this);

        loading.setIndeterminate(true);
        loading.setIndeterminateTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(155, 90, 210)
            )
        );

        LinearLayout.LayoutParams loadingParams =
            new LinearLayout.LayoutParams(45, 45);

        loadingParams.gravity = Gravity.CENTER;
        loadingParams.topMargin = 35;

        splashScreen.addView(loading, loadingParams);
    }

    // إخفاء الشاشة عند جاهزية المدونة
    private void hideSplash() {

        if (splashHidden || splashScreen == null) {
            return;
        }

        splashHidden = true;

        AlphaAnimation animation =
            new AlphaAnimation(1.0f, 0.0f);

        animation.setDuration(350);
        animation.setFillAfter(false);

        splashScreen.startAnimation(animation);
        splashScreen.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {

        if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
        }
        super.onDestroy();
    }
}
