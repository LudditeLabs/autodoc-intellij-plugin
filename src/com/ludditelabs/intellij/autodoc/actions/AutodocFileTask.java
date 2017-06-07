package com.ludditelabs.intellij.autodoc.actions;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionHelper;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.actionSystem.DocCommandGroupId;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.ludditelabs.intellij.autodoc.PluginApp;
import com.ludditelabs.intellij.autodoc.PluginUtils;
import com.ludditelabs.intellij.autodoc.ui.AutodocToolWindow;
import com.ludditelabs.intellij.common.execution.ExternalCommand;
import com.ludditelabs.intellij.common.execution.ExternalCommandListener;
import com.ludditelabs.intellij.common.execution.ExternalCommandResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;


/**
 * Task to process single file by the autodoc tool.
 */
public class AutodocFileTask extends AutodocBaseCommandTask {
    private static final Logger logger = Logger.getInstance("ludditelabs.autodoc.task.file");
    private final @NotNull Document m_document;

    /**
     * Construct task.
     *
     * @param project the project for which the task is created.
     * @param document document to process.
     */
    public AutodocFileTask(@NotNull Project project,
                           @NotNull final Document document) {
        super(project);
        m_document = document;
    }

    @Override
    protected void doCollectStatistics()
        throws Exception {
        // TODO: update statistics
//        PluginApp.getInstance().statistics().countUsage(m_file);
    }

    @Nullable
    private String getTempFilename() {
        try {
            return FileUtil.generateRandomTemporaryPath().getAbsolutePath();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    // Replace current document content with the given one.
    private void replaceContent(final String content) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {

                CommandProcessor.getInstance().executeCommand(project(), new Runnable() {
                    @Override
                    public void run() {
                        WriteCommandAction.runWriteCommandAction(project(), new Runnable() {
                            @Override
                            public void run() {
                                m_document.setText(content);
                            }
                        });
                    }
                }, "Autodoc", DocCommandGroupId.noneGroupId(m_document));

                PsiDocumentManager.getInstance(project()).commitDocument(m_document);
            }
        });
    }

    @Override
    public void execute(@NotNull final ProgressIndicator indicator) {
        final VirtualFile file = FileDocumentManager.getInstance().getFile(
            m_document);
        if (file == null) {
            showError("Can't detect document's file path.");
            return;
        }

        String out_filename = getTempFilename();

        ExternalCommand cmd = createCommand();
        cmd.setTitle("Autodoc " + file.getName());
        cmd.setWorkingDirectory(PluginUtils.getRootPath(project(), file));
        cmd.addParameters(file.getPath());

        // If we can create temp filename then save result in it.
        if (out_filename != null) {
            cmd.addParameters("--no-fix");
            cmd.addParameters("-o", out_filename);
        }
        // Otherwise just overwrite original file.
        else
            cmd.addParameters("--fix");

        final String out_path = out_filename;

        cmd.addListener(new ExternalCommandListener() {
            @Override
            public void startNotified(ProcessEvent event) {
                setCurrentHandler(event.getProcessHandler());
            }

            @Override
            public void consume(ExternalCommandResult result) {
                if (indicator.isCanceled() || isCanceled()) {
                    AutodocToolWindow.clearConsole(project(), file);
                    return;
                }

                PluginUtils.showOutput(
                    project(), result.allContent().trim(), file);

                if (!result.isSuccess()) {
                    showError("Finished with errors.");
                    return;
                }

                // If original file is updated then refresh it in the IDE.
                if (out_path == null) {
                    VfsUtil.markDirtyAndRefresh(
                        true,
                        true,
                        true,
                        file);
                }
                // If result is saved in the temp file then replace original
                // document with its content.
                else {
                    try {
                        replaceContent(FileUtil.loadFile(
                            new File(out_path), file.getCharset()));
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        showError(e.getLocalizedMessage());
                    }
                }
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
                cmd.title(), file);
        }
    }
}
