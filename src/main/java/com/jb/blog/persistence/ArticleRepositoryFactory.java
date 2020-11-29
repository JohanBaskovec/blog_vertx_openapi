package com.jb.blog.persistence;

import io.vertx.sqlclient.Transaction;

public interface ArticleRepositoryFactory {
    ArticleRepository create(Transaction transaction);
}
