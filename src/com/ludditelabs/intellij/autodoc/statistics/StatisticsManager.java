package com.ludditelabs.intellij.autodoc.statistics;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.Alarm;
import com.ludditelabs.intellij.autodoc.config.PluginSettings;
import com.ludditelabs.intellij.common.DateUtils;
import org.jetbrains.annotations.NotNull;


public class StatisticsManager {
    private final StatisticsCollector m_collector;
    private final Alarm m_alarm;
    private static final int DELAY_IN_MINS = 5;

    public StatisticsManager() {
        m_collector = new StatisticsCollector();
        m_alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, ApplicationManager.getApplication());
    }

    // Add menu item to manually force stats uploading
    // This is for debugging purpose.
    private void addStatisticsUploadAction() {
        // This is for debugging/testing purpose.
        // You may add -Dludditelabs.autodoc.statistics.upload_action=true
        // to show "Code->Upload autodoc statistics" menu item
        // which forces statistics uploading process.
        String add = System.getProperty("ludditelabs.autodoc.statistics.upload_action");
        if (add.equals("true")) {
            AnAction action = new AnAction("Upload autodoc statistics") {
                @Override
                public void actionPerformed(AnActionEvent e) {
                    StatusBar statusBar = WindowManager.getInstance().getStatusBar(e.getProject());
                    if (!canUpload()) {
                        statusBar.setInfo("Upload autodoc statistics - skipped.");
                        return;
                    }
                    new StatisticsUploader(m_collector.getDatabase()).run();
                    statusBar.setInfo("Upload autodoc statistics - finished!");
                }
            };
            ActionManager am = ActionManager.getInstance();
            am.registerAction("com.ludditelabs.AutodocUploadStat", action);
            DefaultActionGroup menu = (DefaultActionGroup) am.getAction("CodeMenu");
            menu.add(action);
        }
    }

    // NOTE: we check upload timestamp twice:
    // In the main thread we check plugin settings and then in the background
    // thread we check db value.
    // This allows to not start a task in the bg thread if stat is already
    // uploaded today.
    //
    // NOTE: StatisticsUploader.updateSendTime() updates PluginSettings.
    private boolean canUpload() {
        if (!m_collector.isActive())
            return false;

        // NOTE: upload timestamp has no hours part - it provides only day time.
        long timestamp = PluginSettings.getInstance().statisticsLastUploadTimestamp();
        long now = DateUtils.utcDateNow().getTime();

        if (now <= timestamp)
            return false;

        // If db is null then something is wrong so we don't need to
        // put task in a separate thread, so just quit.
        final StatisticsDb db = m_collector.getDatabase();
        return db != null;
    }

    private void bgUpload() {
        // If stats collecting is disabled then do nothing in this session.
        if (!canUpload())
            return;

        m_alarm.addRequest(new Runnable() {
            @Override
            public void run() {
                // This runnable is executed in the UI thread because
                // our methods are not thread safe.
                // After delay we check again if we can collect stats,
                // if so then run upload in a background thread.
                if (canUpload()) {
                    ApplicationManager.getApplication().executeOnPooledThread(
                        new StatisticsUploader(m_collector.getDatabase()));
                }
            }
        }, DELAY_IN_MINS * 60 * 1000);
    }

    private void doInit() {
        m_collector.setActive(PluginSettings.getInstance().canCollectStatistics());
        addStatisticsUploadAction();
        bgUpload();
    }

    public static StatisticsManager getInstance() {
        return ServiceManager.getService(StatisticsManager.class);
    }

    public static void countUsage(@NotNull final VirtualFile file) {
        getInstance().m_collector.countUsage(file);
    }

    public static void setActive(boolean state) {
        getInstance().m_collector.setActive(state);
    }

    public static void init() {
        getInstance().doInit();
    }
}
