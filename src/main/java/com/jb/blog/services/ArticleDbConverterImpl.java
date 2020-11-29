package com.jb.blog.services;

import io.swagger.client.model.Article;
import io.vertx.sqlclient.Row;

public class ArticleDbConverterImpl implements ArticleDbConverter {
    public Article fromRow(Row row) {
        Article article = new Article();
        String id = row.getString("id");
        article.setId(id);
        String title = row.getString("title");
        article.setTitle(title);
        String content = row.getString("content");
        article.setContent(content);
        return article;
    }
}
