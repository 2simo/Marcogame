
package com.marcogame;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "posts")
public class Post {

    @PrimaryKey
    @NonNull
    public String id;

    public String title;
    public String link;
    public String contentHtml;
    public String published;

    public Post(String id, String title, String link,
                String contentHtml, String published) {
        this.id = id;
        this.title = title;
        this.link = link;
        this.contentHtml = contentHtml;
        this.published = published;
    }
}
