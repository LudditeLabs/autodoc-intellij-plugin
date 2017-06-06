package com.ludditelabs.intellij.common.execution;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionHelper;
import com.intellij.execution.ExecutionModes;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;


/**
 * This class allows to run external command and get it's output.
 *
 * It's also possible to subscribe on various events.
 */
public class ExternalCommand {
    private final GeneralCommandLine m_cmd;
    private final Project m_project;
    private String m_title = "shell command";
    private KillableColoredProcessHandler m_processHandler = null;
    private final Collection<ExternalCommandListener> m_listeners = ContainerUtil.newArrayList();

    /**
     * Construct external command.
     *
     * @param project the project for which the command is created.
     * @param exePath path to external executable.
     */
    public ExternalCommand(@NotNull Project project, @NotNull String exePath) {
        m_project = project;
        m_cmd = createCmd(exePath);
    }

    // Helper method to construct basic command line.
    // It configures PATH environment and streams charset (UTF-8).
    private GeneralCommandLine createCmd(String exePath) {
        GeneralCommandLine cmd = new GeneralCommandLine();
        cmd.setExePath(exePath);

        // Update PATH env.
        Collection<String> paths = ContainerUtil.newArrayList();
        ContainerUtil.addIfNotNull(
            paths, StringUtil.nullize(cmd.getEnvironment().get("PATH"), true));
        ContainerUtil.addIfNotNull(
            paths, StringUtil.nullize(EnvironmentUtil.getValue("PATH"), true));
        cmd.getEnvironment().put(
            "PATH", StringUtil.join(paths, File.pathSeparator));

        // Configure other stuff.
        cmd.withCharset(CharsetToolkit.UTF8_CHARSET);

        // Old API (141):
        cmd.withEnvironment(EnvironmentUtil.getEnvironmentMap());

        // New API:
        // cmd.withParentEnvironmentType(
        //     GeneralCommandLine.ParentEnvironmentType.CONSOLE);

        return cmd;
    }

    /** Parent project. */
    public Project project() {
        return m_project;
    }

    /** Command line which is used to run external command. */
    public GeneralCommandLine commandLine() {
        return m_cmd;
    }

    /** Single-string representation of external command line. */
    public String commandLineString() {
        return m_cmd.getCommandLineString();
    }

    /** Add parameters to the external command. */
    public void addParameters(@NotNull String... params) {
        m_cmd.addParameters(params);
    }

    /**
     * Set command line working directory.
     * @param path Working directory.
     */
    public void setWorkingDirectory(String path) {
        m_cmd.withWorkDirectory(path);
    }

    /** Command title. */
    public String title() {
        return m_title;
    }

    /**
     * Set command title.
     *
     * It displayed in various situations in UI.
     */
    public void setTitle(String title) {
        m_title = title;
    }

    /**
     * Add external command line listener.
     *
     * The listener will subscribe on process events and also will get
     * external command line result.
     *
     * @param listener listener to add.
     */
    public void addListener(ExternalCommandListener listener) {
        m_listeners.add(listener);
    }

    /**
     * Unsubscribe from the external command events.
     *
     * @param listener listener to unsubscribe.
     */
    public void removeListener(ExternalCommandListener listener) {
        m_listeners.remove(listener);
        if (m_processHandler != null) {
            m_processHandler.removeProcessListener(listener);
        }
    }

    /**
     * Execute external command.
     *
     * The command will be executed in cancelable process in the caller thread.
     *
     * @throws ExecutionException
     */
    public void execute() throws ExecutionException {
        Logger.getInstance(getClass()).assertTrue(
            m_processHandler == null,
            "Process has already run with this instance.");

        m_processHandler = new KillableColoredProcessHandler(m_cmd);
        m_processHandler.setShouldDestroyProcessRecursively(true);
        ProcessTerminatedListener.attach(m_processHandler);

        CapturingProcessAdapter adapter = new CapturingProcessAdapter();
        m_processHandler.addProcessListener(adapter);

        for (ProcessListener listener : m_listeners)
            m_processHandler.addProcessListener(listener);

        m_processHandler.startNotify();

        ExecutionModes.SameThreadMode thread_mode =
            new ExecutionModes.SameThreadMode(m_title);
        ExecutionHelper.executeExternalProcess(m_project, m_processHandler,
            thread_mode, m_cmd);

        ProcessOutput output = adapter.getOutput();
        ExternalCommandResult result = new ExternalCommandResult(output);

        for (ExternalCommandListener listener : m_listeners)
            listener.consume(result);
    }
}
