package com.jb.blog.services;

import io.vertx.core.json.JsonObject;
import org.openapitools.vertxweb.server.model.Article;

public class ArticleMapperImpl implements ArticleMapper {
    @Override
    public Article fromJson(JsonObject json) {
        return json.mapTo(Article.class);
    }

    @Override
    public JsonObject toJson(Article article) {
        return JsonObject.mapFrom(article);
    }
}
