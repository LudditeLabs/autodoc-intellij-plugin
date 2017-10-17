package com.ludditelabs.intellij.autodoc.statistics;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.containers.ContainerUtil;
import com.ludditelabs.intellij.autodoc.config.PluginSettings;
import com.ludditelabs.intellij.common.DateUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.ConnectException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;

/**
 * This class uploads cached statistics to remote server.
 *
 * How it works:
 *
 * At first, it checks last upload date and if it's not set or less than
 * current date then it gets list of stored daily stats. Then it sends
 * retrieved list to remote server in JSON format.
 */
public class StatisticsUploader implements Runnable {
    private static final Logger LOG = Logger.getInstance(StatisticsUploader.class);
    @Nullable private final StatisticsDb m_db;

    private class DailyUsage {
        @NotNull private final String date;
        @NotNull private final String feature;
        private final String lang;
        private final int count;

        DailyUsage(@NotNull String date, @NotNull String feature, String lang, int count) {
            this.date = date;
            this.feature = feature;
            this.lang = lang;
            this.count = count;
        }
    }

    private class Stat {
        @SerializedName("os_name") private final String osName;
        @SerializedName("os_arch") private final String osArch;
        @SerializedName("os_version") private final String osVersion;
        @SerializedName("ide_name") private final String ideName;
        @SerializedName("ide_version") private final String ideVersion;
        @SerializedName("uuid") private final String uuid;
        @NotNull private final Collection<DailyUsage> usage = ContainerUtil.newArrayList();

        Stat() {
            osName = SystemInfo.OS_NAME.toLowerCase();
            osArch = SystemInfo.OS_ARCH.toLowerCase();
            osVersion = SystemInfo.OS_VERSION.toLowerCase();

            ApplicationInfo info = ApplicationInfo.getInstance();
            ideName = info.getVersionName().toLowerCase();
            ideVersion = info.getFullVersion();
            uuid = Utils.getUuid();
        }

        void addUsage(DailyUsage info) {
            usage.add(info);
        }

        boolean isEmpty() {
            return usage.isEmpty();
        }
    }


    public StatisticsUploader(@Nullable StatisticsDb db) {
        m_db = db;
    }

    /**
     * This method checks last upload date <pre>last_upload</pre> and
     * returned true if it less than current date.
     *
     * @return true if statistics can be uploaded.
     */
    private boolean canUpload() throws SQLException {
        if (m_db == null) {
            LOG.debug("Database is NULL!");
            return false;
        }

        boolean need = false;
        boolean update_date = false;
        String val = m_db.getMetaValue("last_upload");

        // If last_upload is not set then most probably DB is just created.
        // Set to current date to prevent uploading today.
        if (val == null) {
            LOG.debug("Last upload date is not available.");
            update_date = true;
        }

        else {
            LOG.debug("Last upload date: " + val);
            try {
                need = DateUtils.utcDateNow().after(
                    DateUtils.dateFromString(val));
            }
            catch (ParseException e) {
                LOG.debug(e);
                // Exception may happen if last_upload value is malformed
                // so we force update value to fix that.
                update_date = true;
            }
        }

        if (update_date)
            updateSendTime();

        return need;
    }

    /**
     * Set upload timestamp to current date.
     */
    private void updateSendTime() throws SQLException {
        Date now = DateUtils.utcDateNow();
        final long timestamp = now.getTime();
        String val = DateUtils.toDateString(now);
        LOG.debug("Set last upload date to: " + val);
        m_db.setMetaValue("last_upload", val);

        // See StatisticsManager.canUpload()
        // NOTE: we update value in the UI thread since Statistics API
        // is not thread safe.
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                PluginSettings.getInstance().setStatisticsLastUploadTimestamp(timestamp);
            }
        });
    }

    /**
     * Remove data older than 60 days from the statistics db.
     *
     * We limit amount of data on each run because if there was no connection
     * to send statistics then db will grow up.
     *
     * @throws SQLException if something wrong with DB or SQL query.
     */
    private void dropOldStat() throws SQLException {
        Date dt = DateUtils.addDays(DateUtils.utcDateNow(), -60);
        String val = DateUtils.toDateString(dt);
        String sql = String.format(
            "DELETE FROM feature_usage WHERE timestamp <= '%s'", val);
        m_db.execute(sql);
    }

    /**
     * Remove all dta before today.
     *
     * This method is similar to dropOldStat() but called after successful
     * uploading.
     *
     * dropOldStat() is called before sending to keep only last X days of stat.
     *
     * @param now current date string.
     * @throws SQLException if something wrong with DB or SQL query.
     */
    private void dropSentStat(String now) throws SQLException {
        String sql = String.format(
            "DELETE FROM feature_usage WHERE timestamp < '%s'", now);
        m_db.execute(sql);
    }

    /**
     * Send usage statistics to the remove server.
     *
     * @param usage usage statistics.
     * @return true if the data is sent.
     * @throws IOException on connection errors.
     */
    private boolean send(Stat usage) throws IOException {
        Gson gson = new Gson();
        String json = gson.toJson(usage);

        StatisticsClient client = new StatisticsClient();
        boolean ok = client.sendStatistics(json);
        if (ok)
            LOG.debug("Statistics uploaded successfully.");
        return ok;
    }

    private void doRun() throws SQLException, IOException {
        String now = DateUtils.toDateString(DateUtils.utcDateNow());
        String sql = String.format(
            "SELECT timestamp, feature, lang, count " +
                "FROM feature_usage WHERE timestamp <= '%s'", now);

        Stat stat = new Stat();
        ResultSet res = m_db.executeQuery(sql);
        while (res.next()) {
            stat.addUsage(new DailyUsage(
                res.getString(1),
                res.getString(2),
                res.getString(3),
                res.getInt(4)));
        }

        if (stat.isEmpty()) {
            LOG.debug("No statistics collected, nothing to send yet.");
            return;
        }

        if (send(stat)) {
            updateSendTime();
            dropSentStat(now);
        }
    }

    @Override
    public void run() {
        try {
            if (!canUpload()) {
                LOG.debug("Statistics uploading is not required.");
                return;
            }
            dropOldStat();
            doRun();
        }
        catch (SQLException | IOException e) {
            // Don't print stack trace for failed connections.
            if (e instanceof ConnectException)
                LOG.debug("Uploading failed: " + e.getMessage());
            else
                LOG.debug(e);
        }
    }
}
