package com.marcogame;

import android.content.Context;
import android.content.SharedPreferences;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import android.content.pm.PackageManager;
import android.Manifest;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NewPostWorker extends Worker {

    private static final String RSS_URL =
        "https://marcorx6.blogspot.com/feeds/posts/default?alt=rss";

    private static final String CHANNEL_ID = "marco_posts";

    public NewPostWorker(
        @NonNull Context context,
        @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        HttpURLConnection connection = null;

        try {
            URL url = new URL(RSS_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("GET");

            try (InputStream input = connection.getInputStream()) {

                XmlPullParserFactory factory =
                    XmlPullParserFactory.newInstance();

                XmlPullParser parser =
                    factory.newPullParser();

                parser.setInput(input, "UTF-8");

                String title = "";
                String link = "";
                String guid = "";

                boolean insideItem = false;
                int event = parser.getEventType();

                while (event != XmlPullParser.END_DOCUMENT) {

                    if (event == XmlPullParser.START_TAG) {

                        String tag = parser.getName();

                        if ("item".equalsIgnoreCase(tag)) {
                            insideItem = true;
                        }

                        if (insideItem) {
                            if ("title".equalsIgnoreCase(tag)) {
                                title = parser.nextText();
                            } else if ("link".equalsIgnoreCase(tag)) {
                                link = parser.nextText();
                            } else if ("guid".equalsIgnoreCase(tag)) {
                                guid = parser.nextText();
                            }
                        }
                    }

                    if (event == XmlPullParser.END_TAG &&
                        "item".equalsIgnoreCase(parser.getName())) {
                        break;
                    }

                    event = parser.next();
                }

                if (guid.isEmpty()) {
                    guid = link;
                }

                if (guid.isEmpty()) {
                    return Result.retry();
                }

                SharedPreferences prefs =
                    getApplicationContext().getSharedPreferences(
                        "marco_posts", Context.MODE_PRIVATE
                    );

                String previous =
                    prefs.getString("last_post", "");

                if (previous.isEmpty()) {
                    prefs.edit().putString("last_post", guid).apply();
                    return Result.success();
                }

                if (!previous.equals(guid)) {
                    prefs.edit().putString("last_post", guid).apply();
                    showNotification(title, link);
                }
            }

            return Result.success();

        } catch (Exception e) {
            return Result.retry();

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void showNotification(String title, String link) {

        Context context = getApplicationContext();

        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager manager =
            (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE
            );

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel =
                new NotificationChannel(
                    CHANNEL_ID,
                    "MARCO RX6 - المنشورات الجديدة",
                    NotificationManager.IMPORTANCE_DEFAULT
                );

            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notification =
            new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("تدوينة جديدة في MARCO RX6")
                .setContentText(title)
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(title))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        manager.notify(1001, notification.build());
    }
}
