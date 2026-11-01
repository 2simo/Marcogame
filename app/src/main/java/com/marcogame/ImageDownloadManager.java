package com.marcogame;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageDownloadManager {

    private static final int TIMEOUT = 15000;
    private static final int MAX_IMAGE_SIZE = 10 * 1024 * 1024;

    private final File imageFolder;

    public ImageDownloadManager(Context context) {
        imageFolder = new File(
                context.getFilesDir(),
                "offline_images"
        );

        if (!imageFolder.exists()) {
            imageFolder.mkdirs();
        }
    }

    public String downloadAndReplaceImages(String html) {

        if (html == null || html.isEmpty()) {
            return html;
        }

        Pattern pattern = Pattern.compile(
                "(?i)(<img\\b[^>]*?\\bsrc\\s*=\\s*)([\"'])(.*?)\\2"
        );

        Matcher matcher = pattern.matcher(html);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {

            String imageUrl = matcher.group(3);
            String localPath = downloadImage(imageUrl);

            if (localPath != null) {
                String replacement =
                        matcher.group(1)
                        + matcher.group(2)
                        + Uri.fromFile(new File(localPath))
                                .toString()
                        + matcher.group(2);

                matcher.appendReplacement(
                        result,
                        Matcher.quoteReplacement(replacement)
                );
            }
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private String downloadImage(String imageUrl) {

        HttpURLConnection connection = null;

        try {
            URL url = new URL(imageUrl);

            String protocol = url.getProtocol();

            if (!"http".equalsIgnoreCase(protocol)
                    && !"https".equalsIgnoreCase(protocol)) {
                return null;
            }

            String fileName = hashUrl(imageUrl) + ".img";
            File imageFile = new File(imageFolder, fileName);

            if (imageFile.exists() && imageFile.length() > 0) {
                return imageFile.getAbsolutePath();
            }

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() != 200) {
                return null;
            }

            String contentType =
                    connection.getContentType();

            if (contentType == null
                    || !contentType.toLowerCase()
                    .startsWith("image/")) {
                return null;
            }

            int contentLength =
                    connection.getContentLength();

            if (contentLength > MAX_IMAGE_SIZE) {
                return null;
            }

            File tempFile =
                    new File(imageFolder, fileName + ".tmp");

            try (InputStream input =
                         connection.getInputStream();
                 FileOutputStream output =
                         new FileOutputStream(tempFile)) {

                byte[] buffer = new byte[8192];
                int count;
                int total = 0;

                while ((count = input.read(buffer)) != -1) {

                    total += count;

                    if (total > MAX_IMAGE_SIZE) {
                        tempFile.delete();
                        return null;
                    }

                    output.write(buffer, 0, count);
                }
            }

            if (!tempFile.renameTo(imageFile)) {
                tempFile.delete();
                return null;
            }

            return imageFile.getAbsolutePath();

        } catch (Exception e) {

            return null;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String hashUrl(String value) throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance("SHA-256");

        byte[] hash = digest.digest(
                value.getBytes("UTF-8")
        );

        StringBuilder result = new StringBuilder();

        for (byte b : hash) {
            result.append(
                    String.format("%02x", b)
            );
        }

        return result.toString();
    }
}
