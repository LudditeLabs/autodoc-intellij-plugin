package com.ludditelabs.intellij.autodoc.statistics;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * This class collects statistics and stores it in sqlite getDatabase.
 */
public class StatisticsCollector {
    private static final Logger logger = Logger.getInstance(StatisticsCollector.class);
    @NotNull private final SimpleDateFormat m_dateFmt;
    @Nullable private StatisticsDb m_db = null;
    private PreparedStatement m_usageStmt = null;
    private boolean m_active = false;

    public StatisticsCollector() {
        m_dateFmt = new SimpleDateFormat("yyyy-MM-dd");
        m_dateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            setup();
        }
        catch (SQLException e) {
            cleanup();
            logger.debug(e);
        }
    }

    private void setup() throws SQLException {
        m_db = new StatisticsDb();
        m_usageStmt = m_db.getConnection().prepareStatement(
            "INSERT OR REPLACE INTO feature_usage(timestamp, feature, lang, count) " +
                "VALUES (?, ?, ?, COALESCE((SELECT count + 1 FROM feature_usage " +
                "where timestamp=? AND feature=? AND lang=?), 1))");
    }

    private void cleanup() {
        m_usageStmt = null;
        if (m_db != null) {
            try {
                m_db.close();
            }
            catch (SQLException e) {
                logger.debug(e);
            }
            finally {
                m_db = null;
            }
        }
    }

    public boolean isActive() {
        return m_active;
    }

    public void setActive(boolean state) {
        m_active = state;
    }

    @Nullable
    public StatisticsDb getDatabase() {
        return m_db;
    }

    public void countUsage(String feature, String... languages) {
        if (!isActive() || m_usageStmt == null)
            return;

        try {
            String date = m_dateFmt.format(new Date());
            String lang = StringUtil.join(languages, ",");

            m_usageStmt.setString(1, date);
            m_usageStmt.setString(2, feature);
            m_usageStmt.setString(3, lang);
            m_usageStmt.setString(4, date);
            m_usageStmt.setString(5, feature);
            m_usageStmt.setString(6, lang);
            m_usageStmt.executeUpdate();
        }
        catch (SQLException e) {
            logger.debug(e);
        }
    }

    public void countUsage(@NotNull final VirtualFile file) {
        countUsage("file", file.getExtension());
    }
}
