
package com.marcogame;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingWorkPolicy;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

public class BlogSyncWorker extends Worker {

    private static final int PAGE_SIZE = 100;
    private static final int PAGES_PER_RUN = 5;

    private static final String PREFS = "marco_archive";
    private static final String KEY_INDEX = "next_index";
    private static final String KEY_DONE = "archive_done";

    public BlogSyncWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        Context context = getApplicationContext();

        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        if (prefs.getBoolean(KEY_DONE, false)) {
            return Result.success();
        }

        int startIndex = prefs.getInt(KEY_INDEX, 1);

        for (int page = 0; page < PAGES_PER_RUN; page++) {

            if (isStopped()) {
                return Result.retry();
            }

            HttpURLConnection connection = null;

            try {
                String feedUrl =
                        "https://marcorx6.blogspot.com/feeds/posts/default"
                        + "?alt=rss"
                        + "&max-results=" + PAGE_SIZE
                        + "&start-index=" + startIndex;

                URL url = new URL(feedUrl);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setConnectTimeout(20000);
                connection.setReadTimeout(30000);
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() != 200) {
                    return Result.retry();
                }

                InputStream input = connection.getInputStream();

                DocumentBuilderFactory factory =
                        DocumentBuilderFactory.newInstance();

                factory.setFeature(
                        "http://apache.org/xml/features/disallow-doctype-decl",
                        true
                );

                Document document =
                        factory.newDocumentBuilder().parse(input);

                input.close();

                NodeList items =
                        document.getElementsByTagName("item");

                List<Post> posts = new ArrayList<>();

                for (int i = 0; i < items.getLength(); i++) {

                    Element item = (Element) items.item(i);

                    String title = getText(item, "title");
                    String link = getText(item, "link");
                    String guid = getText(item, "guid");
                    String content = getText(item, "description");
                    String published = getText(item, "pubDate");

                    if (guid.isEmpty()) {
                        guid = link;
                    }

                    if (!guid.isEmpty()) {
                        posts.add(new Post(
                                guid,
                                title,
                                link,
                                content,
                                published
                        ));
                    }
                }

                if (connection != null) {
                    connection.disconnect();
                    connection = null;
                }

                if (posts.isEmpty()) {
                    prefs.edit()
                            .putBoolean(KEY_DONE, true)
                            .apply();

                    return Result.success();
                }

                AppDatabase.getDatabase(context)
                        .postDao()
                        .insertAll(posts);

                startIndex += posts.size();

                prefs.edit()
                        .putInt(KEY_INDEX, startIndex)
                        .apply();

                if (posts.size() < PAGE_SIZE) {
                    prefs.edit()
                            .putBoolean(KEY_DONE, true)
                            .apply();

                    return Result.success();
                }

            } catch (Exception e) {

                return Result.retry();

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        // جدولة الدفعة الموالية تلقائياً
        OneTimeWorkRequest nextBatch =
                new OneTimeWorkRequest.Builder(
                        BlogSyncWorker.class
                ).build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(
                        "MARCO_RX6_ARCHIVE_SYNC",
                        ExistingWorkPolicy.APPEND_OR_REPLACE,
                        nextBatch
                );

        return Result.success();
    }

    private String getText(Element element, String tag) {

        NodeList nodes =
                element.getElementsByTagName(tag);

        if (nodes.getLength() == 0) {
            return "";
        }

        return nodes.item(0).getTextContent();
    }
}
