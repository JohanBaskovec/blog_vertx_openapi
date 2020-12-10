package com.jb.blog.services;

import io.vertx.core.json.JsonObject;

public class DefaultJsonMapperImpl<T> implements JsonMapper<T> {
    private final Class<T> clazz;

    public DefaultJsonMapperImpl(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T fromJson(JsonObject json) {
        return json.mapTo(clazz);
    }

    @Override
    public JsonObject toJson(T user) {
        return JsonObject.mapFrom(user);
    }
}
