
package com.marcogame;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import android.content.Intent;
import android.net.Uri;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(12,12,12));

        TextView title = new TextView(this);
        title.setText("🎮 MARCOGAME");
        title.setTextColor(Color.RED);
        title.setTextSize(27);
        title.setGravity(Gravity.CENTER);
        title.setPadding(10,25,10,25);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("عالم ألعاب PlayStation بين يديك");
        subtitle.setTextColor(Color.WHITE);
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(10,10,10,20);
        root.addView(subtitle);

        WebView web = new WebView(this);
        web.setBackgroundColor(Color.rgb(12,12,12));

        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);

        // إبقاء المدونة داخل التطبيق
        web.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                WebView view,
                WebResourceRequest request
            ) {
                Uri uri = request.getUrl();
                String host = uri.getHost();

                // روابط المدونة تفتح داخل التطبيق
                if (host != null &&
                    (host.equals("marcorx6.blogspot.com") ||
                     host.equals("www.marcorx6.blogspot.com"))) {
                    return false;
                }

                // الروابط الخارجية تفتح خارج التطبيق
                if ("http".equals(uri.getScheme()) ||
                    "https".equals(uri.getScheme())) {

                    Intent intent = new Intent(
                        Intent.ACTION_VIEW, uri
                    );

                    startActivity(intent);
                    return true;
                }

                return false;
            }
        });

        // روابط التحميل تفتح خارج التطبيق
        web.setDownloadListener((url, userAgent,
                contentDisposition, mimetype, contentLength) -> {

            try {
                Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                );
                startActivity(intent);

            } catch (Exception e) {
                Toast.makeText(
                    MainActivity.this,
                    "تعذر فتح رابط التحميل",
                    Toast.LENGTH_SHORT
                ).show();
            }
        });

        web.loadUrl("https://marcorx6.blogspot.com");

        root.addView(web, new LinearLayout.LayoutParams(
            -1, 0, 1
        ));

        setContentView(root);
    }
}
