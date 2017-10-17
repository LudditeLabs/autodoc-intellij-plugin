package com.ludditelabs.intellij.autodoc.actions;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.ludditelabs.intellij.autodoc.PluginUtils;
import com.ludditelabs.intellij.autodoc.bundle.PluginBundleManager;
import com.ludditelabs.intellij.autodoc.config.PluginSettings;
import com.ludditelabs.intellij.common.execution.ExternalCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;


/**
 * This class runs in a background and performs the following actions:
 *
 * - At first, it tries to run <pre>autodoc --help</pre> to test
 *   if the app runs without issues (for example, missing runtime libs)
 * - If above step fails then it shows error notification.
 * - Otherwise it calls execute().
 *
 * Also it supports task cancellation.
 */
public class AutodocBaseCommandTask extends Task.Backgroundable {
    private static final Logger LOG = Logger.getInstance("ludditelabs.autodoc.task");
    @NotNull private final Project m_project;
    private final String m_exePath;
    private boolean m_canceled = false;
    private ProcessHandler m_handler = null;

    /**
     * Construct task.
     *
     * @param project the project for which the task is created.
     */
    public AutodocBaseCommandTask(@NotNull Project project) {
        super(project, "Autodoc", true);
        m_project = project;
        m_exePath = PluginSettings.getInstance().exePath();
    }

    /**
     * Show error notification with the given message.
     *
     * @param message error message.
     */
    protected void showError(String message) {
        PluginUtils.showNotification(m_project, "Autodoc Error", message,
            NotificationType.ERROR);
    }

    /**
     * Show notification on empty autodoc output.
     *
     * Usually this means what it didn't detect any issues.
     */
    protected void showInfoOnEmptyOutput() {
        PluginUtils.showNotification(m_project, "Autodoc",
            "Everything is ok.",
            NotificationType.INFORMATION);
    }

    /**
     * Set currently running process handler.
     *
     * If the task gets canceled process of the handler will be destroyed.
     *
     * @param handler process handler.
     */
    protected void setCurrentHandler(@Nullable ProcessHandler handler) {
        if (m_handler != null) {
            m_handler.destroyProcess();
        }
        m_handler = handler;
    }

    /**
     * Create common autodoc command.
     * @return autodoc command with basic parameters.
     */
    protected ExternalCommand createCommand() {
        ExternalCommand cmd = new ExternalCommand(project(), m_exePath);
        return cmd;
    }

    /**
     * This method gets called before processing.
     */
    protected void onBeforeRun() {

    }

    /**
     * This method gets called after autodoc tool validation process.
     * It supposed to be overridden by the subclass to do actual work.
     */
    protected void execute(@NotNull final ProgressIndicator indicator) {

    }

    /**
     * This method gets called after executing external command and processing
     * its result.
     */
    protected void onAfterRun() {

    }

    /** Return true if the task is canceled. */
    public boolean isCanceled() {
        return m_canceled;
    }

    /** Parent project. */
    public @NotNull Project project() {
        return m_project;
    }

    // NOTE: This callback will be invoked on AWT dispatch thread.
    // TODO: is it ok what we destroy process here?
    @Override
    public void onCancel() {
        if (m_handler != null) {
            setCurrentHandler(null);
        }
    }

    private boolean checkExe() {
        // Check if autodoc tool exists.
        File exe = new File(m_exePath);
        if (!exe.exists()) {
            PluginBundleManager mgr = PluginBundleManager.getInstance();
            if (!mgr.getLocalBundle().isExist()) {
                mgr.showFirstDownloadNotification(m_project);
                return false;
            }
            else {
                showError(
                    "Can't find autodoc tool.\n" +
                        "Platform bundle is malformed.");
            }
            return false;
        }

        // Ensure it's runnable.
        if (!exe.canExecute()) {
            boolean ok = false;
            try {
                ok = exe.setExecutable(true);
            }
            catch (SecurityException e) {
                LOG.debug(e);
            }

            if (!ok) {
                showError(
                    "Can't run autodoc tool.\n" +
                        "Platform bundle is malformed.");
                return false;
            }
        }
        return true;
    }

    private void doRun(@NotNull final ProgressIndicator indicator) {
        if (!checkExe())
            return;

        else if (indicator.isCanceled() || m_project.isDisposed())
            return;

        execute(indicator);
    }

    @Override
    public void run(@NotNull final ProgressIndicator indicator) {
        try {
            onBeforeRun();
            indicator.setIndeterminate(true);
            doRun(indicator);
        }
        finally {
            setCurrentHandler(null);
            if (indicator.isCanceled()) {
                m_canceled = true;
                PluginUtils.showNotification(project(),
                    "Autodoc", "Canceled.", NotificationType.WARNING);
            }
            onAfterRun();
        }
    }
}
