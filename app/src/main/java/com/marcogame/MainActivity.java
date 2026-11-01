
package com.marcogame;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.Gravity;
import android.view.animation.AlphaAnimation;
import android.webkit.*;
import android.widget.*;
import android.content.Intent;
import android.net.Uri;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingPeriodicWorkPolicy;

import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    private WebView web;
    private ProgressBar progress;
    private SwipeRefreshLayout refreshLayout;
    private FrameLayout mainFrame;
    private LinearLayout splashScreen;

    private boolean splashHidden = false;

    private final String homeUrl =
        "https://marcorx6.blogspot.com";

    private final int BLACK = Color.rgb(10, 9, 13);
    private final int PURPLE = Color.rgb(48, 29, 66);
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

        progress = new ProgressBar(
            this, null,
            android.R.attr.progressBarStyleHorizontal
        );

        progress.setMax(100);
        progress.setProgressTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(155, 95, 195)
            )
        );

        root.addView(progress,
            new LinearLayout.LayoutParams(-1, 3));

        mainFrame = new FrameLayout(this);

        refreshLayout = new SwipeRefreshLayout(this);
        refreshLayout.setColorSchemeColors(
            Color.rgb(155, 95, 195)
        );
        refreshLayout.setProgressBackgroundColorSchemeColor(BLACK);

        web = new WebView(this);
        web.setBackgroundColor(WHITE);

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

        refreshLayout.setOnRefreshListener(() -> web.reload());

        mainFrame.addView(
            refreshLayout,
            new FrameLayout.LayoutParams(-1, -1)
        );

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

        startPostNotifications();

        web.loadUrl(homeUrl);
    }

    // تشغيل مراقبة التدوينات الجديدة
    private void startPostNotifications() {

        PeriodicWorkRequest request =
            new PeriodicWorkRequest.Builder(
                NewPostWorker.class,
                15,
                TimeUnit.MINUTES
            ).build();

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "MARCO_RX6_NEW_POSTS",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            );

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                new String[]{
                    Manifest.permission.POST_NOTIFICATIONS
                },
                101
            );
        }
    }

    private GradientDrawable roundedBackground(
        int color, int radius) {

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
        return shape;
    }

    private TextView makeText(
        String value, int size, int color, boolean bold) {

        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER_VERTICAL);

        if (bold) {
            text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return text;
    }

    private void createSplashScreen() {

        splashScreen = new LinearLayout(this);
        splashScreen.setOrientation(LinearLayout.VERTICAL);
        splashScreen.setBackgroundColor(BLACK);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(18, 14, 18, 14);
        header.setBackgroundColor(PURPLE);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);

        header.addView(logo,
            new LinearLayout.LayoutParams(48, 48));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(12, 0, 0, 0);

        TextView title = makeText(
            "MARCO RX6", 21, WHITE, true
        );

        TextView subtitle = makeText(
            "OFFICIAL BLOG", 10,
            Color.rgb(215, 195, 230), false
        );

        brand.addView(title);
        brand.addView(subtitle);

        header.addView(brand,
            new LinearLayout.LayoutParams(0, -2, 1));

        splashScreen.addView(header);

        View divider = new View(this);
        divider.setBackgroundColor(
            Color.rgb(125, 75, 160)
        );

        splashScreen.addView(divider,
            new LinearLayout.LayoutParams(-1, 2));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(18, 25, 18, 18);

        TextView heading = makeText(
            "مرحبا بك في MARCO RX6",
            20, WHITE, true
        );

        content.addView(heading);

        TextView description = makeText(
            "اكتشف آخر المنشورات والمحتويات",
            13, Color.LTGRAY, false
        );

        LinearLayout.LayoutParams descParams =
            new LinearLayout.LayoutParams(-1, -2);

        descParams.topMargin = 7;
        content.addView(description, descParams);

        addPostCard(content, "MARCO RX6", "اكتشف جديد المدونة");
        addPostCard(content, "LATEST POSTS", "آخر الأخبار والمنشورات");
        addPostCard(content, "EXPLORE", "المزيد من المحتوى في انتظارك");

        splashScreen.addView(content,
            new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(15, 15, 15, 25);

        ProgressBar loading = new ProgressBar(this);
        loading.setIndeterminate(true);
        loading.setIndeterminateTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(175, 115, 215)
            )
        );

        bottom.addView(loading,
            new LinearLayout.LayoutParams(38, 38));

        TextView loadingText = makeText(
            "جاري تحميل المدونة...",
            12, Color.rgb(205, 180, 225), false
        );

        loadingText.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams textParams =
            new LinearLayout.LayoutParams(-1, -2);

        textParams.topMargin = 10;
        bottom.addView(loadingText, textParams);

        splashScreen.addView(bottom);
    }

    private void addPostCard(
        LinearLayout parent,
        String heading,
        String description) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(15, 13, 15, 13);
        card.setBackground(
            roundedBackground(
                Color.rgb(29, 23, 35), 14
            )
        );

        TextView title = makeText(
            heading, 15, WHITE, true
        );

        TextView detail = makeText(
            description, 12,
            Color.rgb(195, 180, 205), false
        );

        card.addView(title);

        LinearLayout.LayoutParams detailParams =
            new LinearLayout.LayoutParams(-1, -2);

        detailParams.topMargin = 5;
        card.addView(detail, detailParams);

        LinearLayout.LayoutParams cardParams =
            new LinearLayout.LayoutParams(-1, -2);

        cardParams.topMargin = 15;

        parent.addView(card, cardParams);
    }

    private void hideSplash() {

        if (splashHidden || splashScreen == null) {
            return;
        }

        splashHidden = true;

        AlphaAnimation animation =
            new AlphaAnimation(1.0f, 0.0f);

        animation.setDuration(300);
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
