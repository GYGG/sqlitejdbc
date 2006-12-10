/* Copyright 2006 David Crawshaw, see LICENSE file for licensing [BSD]. */
package org.sqlite;

import java.io.File;
import java.sql.SQLException;

/** This class provides a thin JNI layer over the SQLite3 C API. */
final class NativeDB extends DB
{
    private static Boolean loaded = null;

    static boolean load() {
        if (loaded != null) return loaded == Boolean.TRUE;

        String libpath = System.getProperty("org.sqlite.lib.path");
        String libname = System.getProperty("org.sqlite.lib.name");
        if (libname == null) libname = System.mapLibraryName("sqlitejdbc");

        try {
            if (libpath == null) System.loadLibrary("sqlitejdbc");
            else System.load(new File(libpath, libname).getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
            loaded = Boolean.FALSE;
            return false;
        }

        loaded = Boolean.TRUE;
        return true;
    }


    /** linked list of all instanced UDFDatas */
    private long udfdatalist = 0;


    // WRAPPER FUNCTIONS ////////////////////////////////////////////

    native synchronized void open(String filename) throws SQLException;
    native synchronized void close() throws SQLException;
    native synchronized void interrupt();
    native synchronized void busy_timeout(int ms);
    native synchronized void exec(String sql) throws SQLException;
    native synchronized long prepare(String sql) throws SQLException;
    native synchronized String errmsg();
    native synchronized String libversion();
    native synchronized int changes();

    native synchronized int finalize(long stmt);
    native synchronized int step(long stmt);
    native synchronized int reset(long stmt);
    native synchronized int clear_bindings(long stmt);

    native synchronized int bind_parameter_count(long stmt);

    native synchronized int    column_count      (long stmt);
    native synchronized int    column_type       (long stmt, int col);
    native synchronized String column_decltype   (long stmt, int col);
    native synchronized String column_table_name (long stmt, int col);
    native synchronized String column_name       (long stmt, int col);
    native synchronized String column_text       (long stmt, int col);
    native synchronized byte[] column_blob       (long stmt, int col);
    native synchronized double column_double     (long stmt, int col);
    native synchronized long   column_long       (long stmt, int col);
    native synchronized int    column_int        (long stmt, int col);

    native synchronized int bind_null  (long stmt, int pos);
    native synchronized int bind_int   (long stmt, int pos, int    v);
    native synchronized int bind_long  (long stmt, int pos, long   v);
    native synchronized int bind_double(long stmt, int pos, double v);
    native synchronized int bind_text  (long stmt, int pos, String v);
    native synchronized int bind_blob  (long stmt, int pos, byte[] v);

    native synchronized void result_null  (long context);
    native synchronized void result_text  (long context, String val);
    native synchronized void result_blob  (long context, byte[] val);
    native synchronized void result_double(long context, double val);
    native synchronized void result_long  (long context, long   val);
    native synchronized void result_int   (long context, int    val);
    native synchronized void result_error (long context, String err);

    native synchronized int    value_bytes (Function f, int arg);
    native synchronized String value_text  (Function f, int arg);
    native synchronized byte[] value_blob  (Function f, int arg);
    native synchronized double value_double(Function f, int arg);
    native synchronized long   value_long  (Function f, int arg);
    native synchronized int    value_int   (Function f, int arg);
    native synchronized int    value_type  (Function f, int arg);

    native synchronized int create_function(String name, Function func);
    native synchronized int destroy_function(String name);
    native synchronized void free_functions();

    // COMPOUND FUNCTIONS (for optimisation) /////////////////////////

    /** Provides metadata for the columns of a statement. Returns:
     *   res[col][0] = true if column constrained NOT NULL
     *   res[col][1] = true if column is part of the primary key
     *   res[col][2] = true if column is auto-increment
     */
    native synchronized boolean[][] column_metadata(long stmt);

    static void throwex(String msg) throws SQLException {
        throw new SQLException(msg);
    }
}
