package com.jb.blog.services;

import io.swagger.client.model.Article;
import io.vertx.sqlclient.Row;

public interface ArticleDbConverter {
    Article fromRow(Row row);
}
