package com.jb.blog.persistence;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.sqlclient.*;

import java.util.ArrayList;
import java.util.List;

public abstract class CrudRepositoryImpl<T> implements CrudRepository<T> {
    private String selectQuery;
    private String insertQuery;
    private String updateQuery;

    public CrudRepositoryImpl(String selectQuery, String insertQuery, String updateQuery) {
        this.selectQuery = selectQuery;
        this.insertQuery = insertQuery;
        this.updateQuery = updateQuery;
    }

    @Override
    public void getById(SqlClient sqlClient, String id, Handler<AsyncResult<T>> handler) {
        PreparedQuery<RowSet<Row>> preparedQuery = sqlClient.preparedQuery(
                selectQuery + " where id = $1\n"
        );
        preparedQuery.execute(
                Tuple.of(id),
                (AsyncResult<RowSet<Row>> event) -> {
                    if (event.failed()) {
                        handler.handle(Future.failedFuture(event.cause()));
                        return;
                    }
                    try {
                        RowSet<Row> rowSet = event.result();
                        if (rowSet.size() == 0) {
                            handler.handle(Future.succeededFuture());
                            return;
                        }
                        RowIterator<Row> it = rowSet.iterator();
                        Row row = it.next();
                        handler.handle(Future.succeededFuture(fromRow(row)));
                    } catch (Throwable t) {
                        handler.handle(Future.failedFuture(t));
                    }
                });
    }

    @Override
    public void getAll(SqlClient sqlClient, Handler<AsyncResult<List<T>>> handler) {
        PreparedQuery<RowSet<Row>> preparedQuery = sqlClient.preparedQuery(selectQuery);
        preparedQuery.execute((AsyncResult<RowSet<Row>> event) -> {
            if (event.failed()) {
                handler.handle(Future.failedFuture(event.cause()));
                return;
            }
            try {
                RowSet<Row> rowSet = event.result();
                List<T> entities = new ArrayList<>();
                for (Row row : rowSet) {
                    entities.add(fromRow(row));
                }
                handler.handle(Future.succeededFuture(entities));
            } catch (Throwable t) {
                handler.handle(Future.failedFuture(t));
            }
        });
    }

    @Override
    public void insert(
            SqlClient sqlClient,
            T entity,
            Handler<AsyncResult<Void>> handler
    ) {
        PreparedQuery<RowSet<Row>> preparedQuery = sqlClient.preparedQuery(insertQuery);
        preparedQuery.execute(
                insertTuple(entity),
                event -> {
                    if (event.failed()) {
                        handler.handle(Future.failedFuture(event.cause()));
                        return;
                    }
                    handler.handle(Future.succeededFuture());
                });
    }

    @Override
    public void update(SqlClient sqlClient, T entity, Handler<AsyncResult<Void>> handler) {
        PreparedQuery<RowSet<Row>> preparedQuery = sqlClient.preparedQuery(updateQuery);
        preparedQuery.execute(
                updateTuple(entity),
                event -> {
                    if (event.failed()) {
                        handler.handle(Future.failedFuture(event.cause()));
                        return;
                    }
                    try {
                        RowSet<Row> rows = event.result();
                        Row row = rows.iterator().next();
                        handler.handle(Future.succeededFuture());
                    } catch (Throwable t) {
                        handler.handle(Future.failedFuture(t));
                    }
                });
    }

    protected abstract T fromRow(Row row);

    protected abstract Tuple insertTuple(T entity);

    protected abstract Tuple updateTuple(T entity);
}
