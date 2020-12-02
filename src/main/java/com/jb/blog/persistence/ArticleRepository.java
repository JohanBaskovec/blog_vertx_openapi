package com.jb.blog.persistence;

import org.openapitools.vertxweb.server.model.Article;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.List;

public interface ArticleRepository {
    void insertArticle(
            Article article,
            Handler<AsyncResult<Void>> handler
    );
    void getArticleById(String id, Handler<AsyncResult<Article>> resultHandler);
    void getAllArticles(Handler<AsyncResult<List<Article>>> resultHandler);
    void updateArticle(Article article, Handler<AsyncResult<Void>> handler);
}
