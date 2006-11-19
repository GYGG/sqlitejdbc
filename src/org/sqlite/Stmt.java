/* Copyright 2006 David Crawshaw, see LICENSE file for licensing [BSD]. */
package org.sqlite;

import java.sql.*;
import java.util.ArrayList;

/** See comment in RS.java to explain the strange inheritance hierarchy. */
class Stmt extends RS implements Statement, Codes
{
    private ArrayList batch = null;

    Stmt(Conn conn) { super(conn); }

    /** Calls sqlite3_step() and sets up results. Expects a clean stmt. */
    protected boolean exec() throws SQLException {
        if (pointer == 0) throw new SQLException(
            "SQLite JDBC internal error: pointer == 0 on exec.");
        if (isRS()) throw new SQLException(
            "SQLite JDBC internal error: isRS() on exec.");
        resultsWaiting = false;

        // deal with goofy interface
        int rc = db.step(pointer);
        if (rc == SQLITE_ERROR) rc = db.reset(pointer);

        switch (rc) {
            case SQLITE_DONE:   db.reset(pointer); break;
            case SQLITE_ROW:    resultsWaiting = true; break;
            case SQLITE_BUSY:   throw new SQLException("database locked");
            case SQLITE_MISUSE:
                 throw new SQLException("JDBC internal consistency error");
            case SQLITE_ERROR:
            default:
                 int ret = db.finalize(pointer);
                 pointer = 0;
                 db.throwex(); return false;
        }

        return db.column_count(pointer) != 0;
    }


    // PUBLIC INTERFACE /////////////////////////////////////////////

    public Statement getStatement() { return this; }

    /** More lax than JDBC spec, a Statement can be reused after close().
     *  This is to support Stmt and RS sharing a heap object. */
    public void close() throws SQLException {
        if (pointer == 0) return;
        clearRS();
        colsMeta = null;
        meta = null;
        batch = null;
        int resp = db.finalize(pointer);
        pointer = 0;
        if (resp != SQLITE_OK && resp != SQLITE_MISUSE)
            db.throwex();
    }

    protected void finalize() throws SQLException {
        Stmt.this.close();
        if (conn != null) conn.remove(this);;
    }

    public int getUpdateCount() throws SQLException {
        checkOpen();
        if (pointer == 0 || resultsWaiting) return -1;
        return db.changes();
    }

    public boolean execute(String sql) throws SQLException {
        checkOpen(); close();
        pointer = db.prepare(sql);
        return exec();
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        checkOpen(); close();
        pointer = db.prepare(sql);
        if (!exec()) {
            close();
            throw new SQLException("query does not return ResultSet");
        }
        return getResultSet();
    }

    public int executeUpdate(String sql) throws SQLException {
        checkOpen(); close();
        pointer = db.prepare(sql);
        int changes = 0;
        try { changes = db.executeUpdate(pointer, null); } finally { close(); }
        return changes;
    }

    public void addBatch(String sql) throws SQLException {
        checkOpen();
        if (batch == null) batch = new ArrayList();
        batch.add(sql);
    }

    public void clearBatch() throws SQLException {
        checkOpen(); if (batch != null) batch.clear(); }

    public int[] executeBatch() throws SQLException {
        // TODO: optimise
        checkOpen(); close();
        if (batch == null) return new int[] {};

        ArrayList run = batch;
        int[] changes = new int[run.size()];
        for (int i=0; i < changes.length; i++) {
            pointer = db.prepare((String)run.get(i));
            try {
                changes[i] = db.executeUpdate(pointer, null);
            } catch (SQLException e) {
                throw new BatchUpdateException(
                    "batch entry " + i + ": " + e.getMessage(), changes);
            } finally { close(); }
        }

        return changes;
    }
}
