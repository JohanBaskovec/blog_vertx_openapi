package com.jb.blog.persistence.user;

import io.vertx.sqlclient.Row;
import org.openapitools.vertxweb.server.model.User;

public class UserDbConverterImpl implements UserDbConverter {
    @Override
    public User fromRow(Row row) {
        User user = new User();
        String username = row.getString("username");
        user.setUsername(username);
        String password = row.getString("password");
        user.setPassword(password);
        return user;
    }
}
