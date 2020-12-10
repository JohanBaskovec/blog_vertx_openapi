package com.jb.blog.persistence.user;

import com.jb.blog.persistence.user.UserDbConverter;
import com.jb.blog.persistence.user.UserRepository;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.sqlclient.*;
import org.openapitools.vertxweb.server.model.Article;
import org.openapitools.vertxweb.server.model.User;

public class UserRepositoryImpl implements UserRepository {
    private final UserDbConverter userDbConverter;

    public UserRepositoryImpl(UserDbConverter userDbConverter) {
        this.userDbConverter = userDbConverter;
    }

    @Override
    public void getUserById(
            SqlClient sqlClient,
            String id,
            Handler<AsyncResult<User>> handler
    ) {
        PreparedQuery<RowSet<Row>> preparedQuery = sqlClient.preparedQuery(
                "select username, password from \"user\" where username=$1"
        );
        preparedQuery.execute(
                Tuple.of(id),
                (AsyncResult<RowSet<Row>> event) -> {
                    if (event.failed()) {
                        handler.handle(Future.failedFuture(event.cause()));
                        return;
                    }
                    RowSet<Row> rowSet = event.result();
                    if (rowSet.size() == 0) {
                        handler.handle(Future.succeededFuture());
                        return;
                    }
                    RowIterator<Row> it = rowSet.iterator();
                    Row row = it.next();
                    User user = this.userDbConverter.fromRow(row);
                    handler.handle(Future.succeededFuture(user));
                });
    }

    @Override
    public void insert(
            SqlClient sqlClient,
            User user,
            Handler<AsyncResult<Void>> handler
    ) {
        PreparedQuery<RowSet<Row>> preparedQuery = sqlClient.preparedQuery(
                "insert into \"user\"(username, password, password_salt) values ($1, $2, $3)"
        );
        preparedQuery.execute(
                Tuple.of(user.getUsername(), user.getPassword(), user.getPassword()),
                event -> {
                    if (event.failed()) {
                        handler.handle(Future.failedFuture(event.cause()));
                        return;
                    }
                    handler.handle(Future.succeededFuture());
                });
    }
}
