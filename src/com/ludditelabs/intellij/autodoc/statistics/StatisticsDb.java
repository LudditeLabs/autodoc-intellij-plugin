package com.ludditelabs.intellij.autodoc.statistics;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import java.nio.file.Paths;
import java.sql.*;

/**
 * Thin wrapper for the statistics sqlite database.
 */
public class StatisticsDb implements AutoCloseable {
    private static final Logger logger = Logger.getInstance(StatisticsDb.class);
    private Connection m_conn = null;

    public StatisticsDb() throws SQLException {
        m_conn = createConnection();
    }

    @NotNull
    private Connection createConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");

            String dbpath = Paths.get(
                PathManager.getOptionsPath(),
                "ludditelabs.autodoc.stats.db").toString();

            SQLiteConfig cfg = new SQLiteConfig();

            // Set serialized mode.
            // http://www.sqlite.org/threadsafe.html
            cfg.setOpenMode(SQLiteOpenMode.FULLMUTEX);

            Connection conn = DriverManager.getConnection(
                "jdbc:sqlite:" + dbpath,
                cfg.toProperties());

            setupDb(conn);
            return conn;
        }
        catch (ClassNotFoundException e) {
            logger.debug(e);
            close();
            throw new SQLException(e.getMessage());
        }
        catch (SQLException e) {
            logger.debug(e);
            close();
            throw e;
        }
    }

    private void setupDb(@NotNull Connection conn) throws SQLException {
        // NOTE: we don't call executeQuery() to reuse stmt.
        Statement stmt = conn.createStatement();

        // If has data then table 'meta' exists and we don't need to
        // create tables.
        try (ResultSet res = stmt.executeQuery(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name='meta'")) {
            if (res.next())
                return;
        }

        stmt.execute("CREATE TABLE IF NOT EXISTS meta(name TEXT UNIQUE, value TEXT)");
        stmt.execute("CREATE TABLE IF NOT EXISTS feature_usage(timestamp DATE, feature TEXT, lang TEXT, count INTEGER)");
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS ix_feature_usage ON feature_usage(timestamp, feature, lang)");
        // Schema version. This may be used later for backward compatibility.
        stmt.execute("INSERT OR REPLACE INTO meta(name, value) VALUES ('version', '1')");
    }

    @NotNull
    public Connection getConnection() {
        return m_conn;
    }

    public void close() throws SQLException {
        if (m_conn != null) {
            try {
                m_conn.close();
            }
            catch (SQLException e) {
                logger.debug(e);
                throw e;
            }
            finally {
                m_conn = null;
            }
        }
    }

    public boolean execute(String sql) throws SQLException {
        Statement stmt = getConnection().createStatement();
        return stmt.execute(sql);
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        Statement stmt = getConnection().createStatement();
        return stmt.executeQuery(sql);
    }

    public String getMetaValue(String name) throws SQLException {
        ResultSet r = executeQuery(
            "SELECT value FROM meta WHERE name='" + name + "'");
        if (r.next())
            return r.getString(1);
        return null;
    }

    public void setMetaValue(String name, String value) throws SQLException {
        execute("INSERT OR REPLACE INTO meta(name, value) VALUES ('" +
            name + "', '" + value +"')");
    }
}
