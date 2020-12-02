package com.jb.blog.services;

import io.vertx.core.json.JsonObject;
import org.openapitools.vertxweb.server.model.Article;

public interface ArticleMapper {
    Article fromJson(JsonObject json);
    JsonObject toJson(Article article);
}
