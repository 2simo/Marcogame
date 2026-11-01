
package com.marcogame;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

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

    private static final String FEED_URL =
            "https://marcorx6.blogspot.com/feeds/posts/default?alt=rss&max-results=100";

    public BlogSyncWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        HttpURLConnection connection = null;

        try {
            URL url = new URL(FEED_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
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

            NodeList items = document.getElementsByTagName("item");

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

            input.close();

            if (!posts.isEmpty()) {
                AppDatabase.getDatabase(getApplicationContext())
                        .postDao()
                        .insertAll(posts);
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

    private String getText(Element element, String tag) {
        NodeList nodes = element.getElementsByTagName(tag);

        if (nodes.getLength() == 0) {
            return "";
        }

        return nodes.item(0).getTextContent();
    }
}
