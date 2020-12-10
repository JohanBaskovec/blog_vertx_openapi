package com.jb.blog.persistence.article;

import com.jb.blog.services.ArticleDbConverter;
import io.vertx.sqlclient.Transaction;

public class ArticleRepositoryFactoryImpl implements ArticleRepositoryFactory {
    private final ArticleDbConverter articleDbConverter;

    public ArticleRepositoryFactoryImpl(ArticleDbConverter articleDbConverter) {
        this.articleDbConverter = articleDbConverter;
    }

    @Override
    public ArticleRepository create(Transaction transaction) {
        return new ArticleRepositoryImpl(this.articleDbConverter, transaction);
    }
}
