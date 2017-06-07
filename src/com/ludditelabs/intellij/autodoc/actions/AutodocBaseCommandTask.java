package com.ludditelabs.intellij.autodoc.actions;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.ludditelabs.intellij.autodoc.PluginUtils;
import com.ludditelabs.intellij.autodoc.config.PluginSettings;
import com.ludditelabs.intellij.common.execution.ExternalCommand;
import com.ludditelabs.intellij.common.execution.ExternalCommandListener;
import com.ludditelabs.intellij.common.execution.ExternalCommandResult;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Paths;


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
    private static final Logger logger = Logger.getInstance("ludditelabs.autodoc.task");
    private final @NotNull Project m_project;
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

    private void collectStatistics() {
        if (!PluginSettings.getInstance().canCollectStatistics())
            return;

        try {
            doCollectStatistics();
        }
        catch (Exception e) {
            logger.debug(e);
        }
    }

    protected void doCollectStatistics() throws Exception {

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
     * Set currently running process handler.
     *
     * If the task gets canceled process of the handler will be destroyed.
     * So call this method each time you start new process in the task to
     * support cancellation (or reimplement onCancel() method).
     *
     * @param handler process handler.
     */
    protected void setCurrentHandler(ProcessHandler handler) {
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
     * This method gets called after autodoc tool validation process.
     * It supposed to be overridden by the subclass to do actual work.
     */
    protected void execute(@NotNull final ProgressIndicator indicator) {

    }

    /** Return true if the task is canceled. */
    public boolean isCanceled() {
        return m_canceled;
    }

    /** Parent project. */
    public @NotNull Project project() {
        return m_project;
    }

    /** Destroy current process. */
    @Override
    public void onCancel() {
        m_canceled = true;
        if (m_handler != null)
            m_handler.destroyProcess();
    }

    @Override
    public void run(@NotNull final ProgressIndicator indicator) {
        indicator.setIndeterminate(true);

        collectStatistics();

        // Check if autodoc tool exists.
        File exe = new File(m_exePath);
        if (!exe.exists()) {
            showError("Can't execute external tool.");
            return;
        }

        // Ensure it's runnable.
        if (!exe.canExecute())
            exe.setExecutable(true);

        // Make some simple autodoc call to see if there are any runtime
        // errors.
        //
        // This doesn't show all errors but helps to detect common deployment
        // problems.
        ExternalCommand cmd = new ExternalCommand(m_project, m_exePath);
        cmd.addParameters("--help");
        cmd.addListener(new ExternalCommandListener() {
            @Override
            public void startNotified(ProcessEvent event) {
                setCurrentHandler(event.getProcessHandler());
            }

            @Override
            public void consume(ExternalCommandResult result) {
                // Exit if someone canceled the task.
                if (indicator.isCanceled())
                    return;

                // If this method called then "autodoc --help" is finished.
                // But if it failed then show error.
                if (!result.isSuccess())
                    showError(result.stderr());

                // If everything is fine then finally call main task method.
                else if (!m_project.isDisposed())
                    execute(indicator);
            }
        });

        try {
            cmd.execute();
        }
        catch (ExecutionException e) {
            showError(e.getMessage());
        }
        finally {
            if (indicator.isCanceled()) {
                m_canceled = true;
                PluginUtils.showNotification(project(),
                    "Autodoc", "Canceled.", NotificationType.WARNING);
            }
        }
    }
}
