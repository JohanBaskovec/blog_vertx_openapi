package com.jb.blog.services;

import org.openapitools.vertxweb.server.model.Article;
import io.vertx.sqlclient.Row;

public interface ArticleDbConverter {
    Article fromRow(Row row);
}
