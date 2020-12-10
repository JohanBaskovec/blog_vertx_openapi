package com.jb.blog.persistence.article;

import io.vertx.sqlclient.Transaction;

public interface ArticleRepositoryFactory {
    ArticleRepository create(Transaction transaction);
}
