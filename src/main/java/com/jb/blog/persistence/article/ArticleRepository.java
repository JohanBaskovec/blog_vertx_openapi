package com.jb.blog.persistence.article;

import io.vertx.sqlclient.SqlClient;
import org.openapitools.vertxweb.server.model.Article;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import org.openapitools.vertxweb.server.model.EntityVersionId;

import java.util.List;

public interface ArticleRepository {
    void insertArticle(
            SqlClient sqlClient,
            Article article,
            Handler<AsyncResult<Void>> handler
    );
    void getArticleById(
            SqlClient sqlClient,
            String id,
            Handler<AsyncResult<Article>> resultHandler
    );
    void getAllArticles(
            SqlClient sqlClient,
            Handler<AsyncResult<List<Article>>> resultHandler
    );
    void updateArticle(
            SqlClient sqlClient,
            Article article,
            Handler<AsyncResult<Void>> handler
    );
}
