package com.jb.blog.services;

import io.vertx.core.json.JsonObject;
import org.openapitools.vertxweb.server.model.User;

public interface JsonMapper<T> {
    T fromJson(JsonObject json);

    JsonObject toJson(T user);
}
