package com.ludditelabs.intellij.autodoc.actions;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionHelper;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.ludditelabs.intellij.autodoc.PluginApp;
import com.ludditelabs.intellij.autodoc.PluginUtils;
import com.ludditelabs.intellij.autodoc.ui.AutodocToolWindow;
import com.ludditelabs.intellij.common.execution.ExternalCommand;
import com.ludditelabs.intellij.common.execution.ExternalCommandListener;
import com.ludditelabs.intellij.common.execution.ExternalCommandResult;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;


/**
 * Task to process single file by the autodoc tool.
 */
public class AutodocFileTask extends AutodocBaseCommandTask {
    private static final Logger logger = Logger.getInstance("ludditelabs.autodoc.task.file");
    private final @NotNull VirtualFile m_file;

    /**
     * Construct task.
     *
     * @param project the project for which the task is created.
     * @param file file to process.
     */
    public AutodocFileTask(@NotNull Project project,
                           @NotNull final VirtualFile file) {
        super(project);
        m_file = file;
    }

    @Override
    protected void doCollectStatistics()
        throws Exception {
        // TODO: update statistics
//        PluginApp.getInstance().statistics().countUsage(m_file);
    }

    @Override
    public void execute(@NotNull final ProgressIndicator indicator) {
        ExternalCommand cmd = createCommand();
        cmd.setTitle("Autodoc " + m_file.getName());
        cmd.setWorkingDirectory(PluginUtils.getRootPath(project(), m_file));
        cmd.addParameters(m_file.getPath());

        cmd.addListener(new ExternalCommandListener() {
            @Override
            public void startNotified(ProcessEvent event) {
                setCurrentHandler(event.getProcessHandler());
            }

            @Override
            public void consume(ExternalCommandResult result) {
                if (indicator.isCanceled() || isCanceled()) {
                    AutodocToolWindow.clearConsole(project(), m_file);
                    return;
                }

                if (!result.isSuccess())
                    showError("Finished with errors.");
                else
                    VfsUtil.markDirtyAndRefresh(true, true, true, m_file);

                PluginUtils.showOutput(
                    project(), result.allContent().trim(), m_file);
            }
        });

        try {
            indicator.setText(cmd.title() + "...");
            logger.debug(cmd.commandLineString());
            cmd.execute();
        }
        catch (ExecutionException e) {
            ExecutionHelper.showErrors(
                project(), Collections.singletonList(e),
                cmd.title(), m_file);
        }
    }
}
