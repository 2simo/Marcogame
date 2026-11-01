package com.marcogame;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Post post);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Post> posts);

    @Query("SELECT * FROM posts ORDER BY published DESC")
    List<Post> getAllPosts();

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    Post getPost(String postId);

    @Query("SELECT COUNT(*) FROM posts")
    int getPostCount();

    @Query("UPDATE posts SET contentHtml = :newContent WHERE id = :postId")
    void updateContentHtml(String postId, String newContent);
}
