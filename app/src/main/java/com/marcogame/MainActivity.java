package com.marcogame;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.webkit.*;
import android.widget.*;

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
        web.loadUrl("https://marcorx6.blogspot.com");

        root.addView(web, new LinearLayout.LayoutParams(
            -1, 0, 1
        ));

        setContentView(root);
    }
}
