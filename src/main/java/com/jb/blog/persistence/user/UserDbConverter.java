package com.jb.blog.persistence.user;

import io.vertx.sqlclient.Row;
import org.openapitools.vertxweb.server.model.User;

public interface UserDbConverter {
    User fromRow(Row row);
}
