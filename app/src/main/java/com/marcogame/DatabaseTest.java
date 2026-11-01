
package com.marcogame;

import android.content.Context;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseTest {

    private static final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    public static void saveTestPost(Context context) {
        executor.execute(() -> {
            Post post = new Post(
                    "test-001",
                    "اختبار قاعدة البيانات",
                    "https://marcorx6.blogspot.com",
                    "<h1>MARCO RX6</h1><p>Offline is working!</p>",
                    "2026-11-01"
            );

            AppDatabase.getDatabase(context)
                    .postDao()
                    .insert(post);
        });
    }
}
