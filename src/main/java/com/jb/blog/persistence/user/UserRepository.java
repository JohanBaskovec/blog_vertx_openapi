package com.jb.blog.persistence.user;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.sqlclient.SqlClient;
import org.openapitools.vertxweb.server.model.Article;
import org.openapitools.vertxweb.server.model.User;

public interface UserRepository {
    void getUserById(
            SqlClient sqlClient,
            String id,
            Handler<AsyncResult<User>> handler
    );
    void insert(
            SqlClient sqlClient,
            User user,
            Handler<AsyncResult<Void>> handler
    );
}
