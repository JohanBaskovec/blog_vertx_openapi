package com.jb.blog.persistence;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlClient;
import io.vertx.sqlclient.Tuple;

import java.util.List;

public interface CrudRepository<T> {
    void getById(SqlClient sqlClient, String id, Handler<AsyncResult<T>> handler);

    void getAll(SqlClient sqlClient, Handler<AsyncResult<List<T>>> handler);

    void insert(
            SqlClient sqlClient,
            T entity,
            Handler<AsyncResult<Void>> handler
    );

    void update(SqlClient sqlClient, T entity, Handler<AsyncResult<Void>> handler);
}
