package com.ludditelabs.intellij.autodoc.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.ludditelabs.intellij.autodoc.PluginApp;
import com.ludditelabs.intellij.autodoc.statistics.StatisticsManager;


/**
 * Action to run autodoc on current file.
 */
public class AutodocCurrentFile extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null)
            return;

        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null)
            return;

        final Document document = editor.getDocument();
        final VirtualFile file = FileDocumentManager.getInstance().getFile(document);

        if (file == null)
            return;

        FileDocumentManager.getInstance().saveDocument(document);
        PluginApp.getInstance().run(project, document);
        StatisticsManager.countUsage(file);
    }
}
