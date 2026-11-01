
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
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingPeriodicWorkPolicy;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

public class MainActivity extends Activity {

    private WebView web;
    private ProgressBar progress;
    private SwipeRefreshLayout refreshLayout;
    private FrameLayout mainFrame;
    private LinearLayout splashScreen;

    private boolean splashHidden = false;
    private boolean offlinePage = false;

    private ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor();

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

        WebSettings settings = web.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int value) {
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

                if ("marco-post".equals(uri.getScheme())) {
                    String id = uri.getHost();

                    if (id != null) {
                        openSavedPost(id);
                    }
                    return true;
                }

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
            public void onPageFinished(WebView view, String url) {
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

                    if (!offlinePage) {
                        showOfflineArchive();
                    }
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
            offlinePage = false;
            web.loadUrl(homeUrl);
        });

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
        startBlogSync();

        web.loadUrl(homeUrl);
    }

    // مراقبة التدوينات الجديدة
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

    // مزامنة التدوينات
    private void startBlogSync() {

        OneTimeWorkRequest firstSync =
                new OneTimeWorkRequest.Builder(
                        BlogSyncWorker.class
                ).build();

        WorkManager.getInstance(this)
                .enqueueUniqueWork(
                        "MARCO_RX6_FIRST_SYNC",
                        androidx.work.ExistingWorkPolicy.KEEP,
                        firstSync
                );

        PeriodicWorkRequest periodicSync =
                new PeriodicWorkRequest.Builder(
                        BlogSyncWorker.class,
                        12,
                        TimeUnit.HOURS
                ).build();

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        "MARCO_RX6_BLOG_SYNC",
                        ExistingPeriodicWorkPolicy.KEEP,
                        periodicSync
                );
    }

    // عرض الأرشيف المحفوظ عند انقطاع الإنترنت
    private void showOfflineArchive() {

        offlinePage = true;
        progress.setVisibility(View.GONE);

        databaseExecutor.execute(() -> {

            List<Post> posts =
                    AppDatabase.getDatabase(getApplicationContext())
                            .postDao()
                            .getAllPosts();

            StringBuilder html = new StringBuilder();

            html.append("<html><head><meta charset='UTF-8'>");
            html.append("<meta name='viewport' content='width=device-width, initial-scale=1'>");
            html.append("<style>");
            html.append("body{background:#0a090d;color:white;font-family:sans-serif;padding:18px;direction:rtl;}");
            html.append("h2{color:#c49be0;text-align:center;}");
            html.append(".card{background:#1d1723;padding:15px;margin:12px 0;border-radius:12px;}");
            html.append("a{color:#d5b4f0;text-decoration:none;font-size:17px;font-weight:bold;}");
            html.append("p{color:#bbb;font-size:13px;}");
            html.append("</style></head><body>");

            html.append("<h2>MARCO RX6</h2>");
            html.append("<p style='text-align:center'>وضع عدم الاتصال - التدوينات المحفوظة</p>");

            if (posts == null || posts.isEmpty()) {

                html.append("<div class='card'>");
                html.append("<h3>لا توجد تدوينات محفوظة بعد</h3>");
                html.append("<p>اتصل بالإنترنت وافتح التطبيق لتحميل التدوينات.</p>");
                html.append("</div>");

            } else {

                for (Post post : posts) {

                    html.append("<div class='card'>");

                    html.append("<a href='marco-post://");
                    html.append(Uri.encode(post.id));
                    html.append("'>");
                    html.append(escapeHtml(post.title));
                    html.append("</a>");

                    if (post.published != null) {
                        html.append("<p>");
                        html.append(escapeHtml(post.published));
                        html.append("</p>");
                    }

                    html.append("</div>");
                }
            }

            html.append("</body></html>");

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                web.loadDataWithBaseURL(
                        homeUrl,
                        html.toString(),
                        "text/html",
                        "UTF-8",
                        null
                );

                hideSplash();
            });
        });
    }

    // فتح تدوينة محفوظة
    private void openSavedPost(String id) {

        databaseExecutor.execute(() -> {

            Post post =
                    AppDatabase.getDatabase(getApplicationContext())
                            .postDao()
                            .getPost(id);

            if (post == null) {
                return;
            }

            String html =
                    "<html><head><meta charset='UTF-8'>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
                    "<style>body{background:#0a090d;color:white;padding:18px;font-family:sans-serif;direction:rtl;line-height:1.8;}a{color:#c49be0;}img{max-width:100%;height:auto;}</style>" +
                    "</head><body>" +
                    "<h2>" + escapeHtml(post.title) + "</h2>" +
                    (post.contentHtml == null ? "" : post.contentHtml) +
                    "</body></html>";

            runOnUiThread(() -> {

                if (!isFinishing() && !isDestroyed()) {
                    offlinePage = true;

                    web.loadDataWithBaseURL(
                            homeUrl,
                            html,
                            "text/html",
                            "UTF-8",
                            null
                    );
                }
            });
        });
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private GradientDrawable roundedBackground(
            int color, int radius) {

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(radius);
        return shape;
    }

    // Splash Screen السينمائي
    private void createSplashScreen() {

        splashScreen = new LinearLayout(this);
        splashScreen.setOrientation(LinearLayout.VERTICAL);
        splashScreen.setGravity(Gravity.CENTER);
        splashScreen.setPadding(20, 30, 20, 30);

        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.rgb(3, 3, 5),
                        Color.rgb(24, 12, 34),
                        Color.rgb(5, 5, 8)
                }
        );

        splashScreen.setBackground(background);

        FrameLayout scene = new FrameLayout(this);

        // دخان سينمائي متحرك
        for (int i = 0; i < 7; i++) {

            View smoke = new View(this);

            GradientDrawable cloud = new GradientDrawable();
            cloud.setShape(GradientDrawable.OVAL);
            cloud.setColor(Color.rgb(105, 105, 115));

            smoke.setBackground(cloud);
            smoke.setAlpha(0.10f);

            FrameLayout.LayoutParams smokeParams =
                    new FrameLayout.LayoutParams(
                            130 + (i % 3) * 35,
                            65 + (i % 2) * 25
                    );

            smokeParams.gravity = Gravity.CENTER;
            smokeParams.leftMargin =
                    (i % 2 == 0 ? -65 : 65);
            smokeParams.topMargin = (i - 3) * 38;

            scene.addView(smoke, smokeParams);

            AlphaAnimation smokeAnimation =
                    new AlphaAnimation(0.04f, 0.20f);

            smokeAnimation.setDuration(2200 + i * 250);
            smokeAnimation.setRepeatMode(
                    AlphaAnimation.REVERSE
            );
            smokeAnimation.setRepeatCount(
                    AlphaAnimation.INFINITE
            );

            smoke.startAnimation(smokeAnimation);
        }

        // شعار الذئب
        ImageView wolf = new ImageView(this);
        wolf.setImageResource(R.mipmap.ic_launcher);
        wolf.setScaleType(ImageView.ScaleType.FIT_CENTER);

        FrameLayout.LayoutParams wolfParams =
                new FrameLayout.LayoutParams(210, 210);

        wolfParams.gravity = Gravity.CENTER;
        wolfParams.topMargin = -65;

        scene.addView(wolf, wolfParams);

        AlphaAnimation wolfAnimation =
                new AlphaAnimation(0.65f, 1f);

        wolfAnimation.setDuration(1800);
        wolfAnimation.setRepeatMode(
                AlphaAnimation.REVERSE
        );
        wolfAnimation.setRepeatCount(
                AlphaAnimation.INFINITE
        );

        wolf.startAnimation(wolfAnimation);

        splashScreen.addView(
                scene,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        // اسم التطبيق
        TextView title = new TextView(this);
        title.setText("MARCO RX6");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );
        title.setGravity(Gravity.CENTER);
        title.setLetterSpacing(0.12f);

        splashScreen.addView(title);

        // خط بنفسجي
        View line = new View(this);

        GradientDrawable lineBackground =
                new GradientDrawable();

        lineBackground.setColor(
                Color.rgb(145, 65, 190)
        );
        lineBackground.setCornerRadius(5);

        line.setBackground(lineBackground);

        LinearLayout.LayoutParams lineParams =
                new LinearLayout.LayoutParams(115, 3);

        lineParams.topMargin = 15;
        lineParams.bottomMargin = 18;

        splashScreen.addView(line, lineParams);

        // عبارة الترحيب
        TextView subtitle = new TextView(this);

        subtitle.setText("WELCOME TO THE WOLF SIDE");
        subtitle.setTextColor(
                Color.rgb(190, 175, 205)
        );
        subtitle.setTextSize(11);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLetterSpacing(0.16f);

        splashScreen.addView(subtitle);

        // مؤشر التحميل
        ProgressBar loading = new ProgressBar(this);
        loading.setIndeterminate(true);

        loading.setIndeterminateTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.rgb(175, 90, 220)
                )
        );

        LinearLayout.LayoutParams loadingParams =
                new LinearLayout.LayoutParams(35, 35);

        loadingParams.topMargin = 35;

        splashScreen.addView(
                loading,
                loadingParams
        );

        TextView loadingText = new TextView(this);

        loadingText.setText(
                "جاري تحضير عالم MARCO RX6..."
        );

        loadingText.setTextColor(
                Color.rgb(180, 165, 195)
        );

        loadingText.setTextSize(12);
        loadingText.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(-1, -2);

        textParams.topMargin = 12;

        splashScreen.addView(
                loadingText,
                textParams
        );
    }

    // إخفاء شاشة البداية
    private void hideSplash() {

        if (splashHidden || splashScreen == null) {
            return;
        }

        splashHidden = true;

        AlphaAnimation animation =
                new AlphaAnimation(1.0f, 0.0f);

        animation.setDuration(500);
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

        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
        }

        if (web != null) {
            web.destroy();
        }

        super.onDestroy();
    }
}
