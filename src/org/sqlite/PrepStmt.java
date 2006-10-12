/* Copyright 2006 David Crawshaw, see LICENSE file for licensing [BSD]. */
package org.sqlite;

import java.io.Reader;
import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;

/** See comment in RS.java to explain the strange inheritance hierarchy. */
final class PrepStmt extends RS
        implements PreparedStatement, ParameterMetaData, Codes
{
    private int columnCount;
    private int paramCount;
    private int batchPos;
    private Object[] batch;

    PrepStmt(Conn conn, String sql) throws SQLException {
        super(conn);

        pointer = db.prepare(sql);
        colsMeta = db.column_names(pointer);
        columnCount = db.column_count(pointer);
        paramCount = db.bind_parameter_count(pointer);
        batch = new Object[paramCount];
        batchPos = 0;
    }

    /** Weaker close to support object overriding (see docs in RS.java). */
    public void close() throws SQLException {
        batch = null;
        if (pointer == 0 || db == null) clearRS(); else clearParameters();
    }

    public void clearParameters() throws SQLException {
        clearRS();
        db.clear_bindings(pointer);
        batchPos = 0;
        if (batch != null)
            for (int i=0; i < batch.length; i++)
                batch[i] = null;
    }

    protected void finalize() throws SQLException { close(); }


    public boolean execute() throws SQLException {
        checkExec();
        clearRS();
        db.reset(pointer);
        resultsWaiting = db.execute(pointer, batch);
        return columnCount != 0;
    }

    public ResultSet executeQuery() throws SQLException {
        checkExec();
        if (columnCount == 0)
            throw new SQLException("query does not return results");
        clearRS();
        db.reset(pointer);
        resultsWaiting = db.execute(pointer, batch);
        return getResultSet();
    }

    public int executeUpdate() throws SQLException {
        checkExec();
        if (columnCount != 0)
            throw new SQLException("query returns results");
        clearRS();
        db.reset(pointer);
        return db.executeUpdate(pointer, batch);
    }

    public int[] executeBatch() throws SQLException {
        return db.executeBatch(pointer, batchPos / paramCount, batch);
    }

    public int getUpdateCount() throws SQLException {
        checkOpen();
        if (pointer == 0 || resultsWaiting) return -1;
        return db.changes();
    }

    public void addBatch() throws SQLException {
        checkExec();
        batchPos += paramCount;
        if (batchPos + paramCount > batch.length) {
            Object[] nb = new Object[batch.length * 2];
            System.arraycopy(batch, 0, nb, 0, batch.length);
            batch = nb;
        }
    }

    public void clearBatch() throws SQLException { clearParameters(); }


    // ParameterMetaData FUNCTIONS //////////////////////////////////

    public ParameterMetaData getParameterMetaData() { return this; }

    public int getParameterCount() throws SQLException {
        checkExec(); return paramCount; }
    public String getParameterClassName(int param) throws SQLException {
        checkExec(); return "java.lang.String"; }
    public String getParameterTypeName(int pos) { return "VARCHAR"; }
    public int getParameterType(int pos) { return Types.VARCHAR; }
    public int getParameterMode(int pos) { return parameterModeIn; }
    public int getPrecision(int pos) { return 0; }
    public int getScale(int pos) { return 0; }
    public int isNullable(int pos) { return parameterNullable; }
    public boolean isSigned(int pos) { return true; }
    public Statement getStatement() { return this; }


    // PARAMETER FUNCTIONS //////////////////////////////////////////

    private void batch(int pos, Object value) throws SQLException {
        checkExec();
        if (batch == null) batch = new Object[paramCount];
        batch[batchPos + pos - 1] = value;
    }

    public void setBoolean(int pos, boolean value) throws SQLException {
        setInt(pos, value ? 1 : 0);
    }
    public void setByte(int pos, byte value) throws SQLException {
        setInt(pos, (int)value);
    }
    public void setBytes(int pos, byte[] value) throws SQLException {
        batch(pos, value);
    }
    public void setDouble(int pos, double value) throws SQLException {
        batch(pos, new Double(value));
    }
    public void setFloat(int pos, float value) throws SQLException {
        setDouble(pos, value);
    }
    public void setInt(int pos, int value) throws SQLException {
        batch(pos, new Integer(value));
    }
    public void setLong(int pos, long value) throws SQLException {
        batch(pos, new Long(value));
    }
    public void setNull(int pos, int u1) throws SQLException {
        setNull(pos, u1, null);
    }
    public void setNull(int pos, int u1, String u2) throws SQLException {
        batch(pos, null);
    }
    public void setObject(int pos , Object value) throws SQLException {
        // TODO: catch wrapped primitives
        batch(pos, value == null ? null : value.toString());
    }
    public void setObject(int p, Object v, int t) throws SQLException {
        setObject(p, v); }
    public void setObject(int p, Object v, int t, int s) throws SQLException {
        setObject(p, v); }
    public void setShort(int pos, short value) throws SQLException {
        setInt(pos, (int)value); }
    public void setString(int pos, String value) throws SQLException {
        batch(pos, value);
    }
    public void setDate(int pos, Date x) throws SQLException {
        setLong(pos, x.getTime()); }
    public void setDate(int pos, Date x, Calendar cal) throws SQLException {
        setLong(pos, x.getTime()); }
    public void setTime(int pos, Time x) throws SQLException {
        setLong(pos, x.getTime()); }
    public void setTime(int pos, Time x, Calendar cal) throws SQLException {
        setLong(pos, x.getTime()); }
    public void setTimestamp(int pos, Timestamp x) throws SQLException {
        setLong(pos, x.getTime()); }
    public void setTimestamp(int pos, Timestamp x, Calendar cal)
        throws SQLException { setLong(pos, x.getTime()); }


    // UNUSED ///////////////////////////////////////////////////////

    public boolean execute(String sql)
        throws SQLException { throw unused(); }
    public int executeUpdate(String sql)
        throws SQLException { throw unused(); }
    public ResultSet executeQuery(String sql)
        throws SQLException { throw unused(); }
    public void addBatch(String sql)
        throws SQLException { throw unused(); }

    private SQLException unused() {
        return new SQLException("not supported by PreparedStatment");
    }
}
