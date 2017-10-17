package com.ludditelabs.intellij.autodoc.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.ludditelabs.intellij.autodoc.PluginProjectComponent;
import com.ludditelabs.intellij.autodoc.bundle.PluginBundleManager;
import com.ludditelabs.intellij.autodoc.statistics.StatisticsManager;
import org.jetbrains.annotations.Nullable;

// HINT: get list of selected files:
// DataContext dataContext = e.getDataContext();
// final VirtualFile[] files = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext);

/**
 * Action to run autodoc on current file.
 */
public class AutodocCurrentFile extends AnAction {
    /**
     * Get currently editing file type.
     *
     * @return FileType or null.
     */
    @Nullable
    private FileType getCurrentFileType(AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null) {
            return null;
        }

        final Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return null;
        }

        final Document document = editor.getDocument();
        final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) {
            return null;
        }

        return file.getFileType();
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        final Project project = e.getProject();
        if (project == null)
            return;

        PluginProjectComponent component =
            project.getComponent(PluginProjectComponent.class);

        // This must not happen because we registered this component.
        if (component == null)
            return;

        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null)
            return;

        final Document document = editor.getDocument();
        final VirtualFile file = FileDocumentManager.getInstance().getFile(document);

        if (file == null)
            return;

        FileDocumentManager.getInstance().saveDocument(document);
        component.runAutodoc(document);
        StatisticsManager.countUsage(file);
    }

    @Override
    public void update(AnActionEvent e) {
        boolean enabled = true;

        // Disable action if platform is not supported.
        final PluginBundleManager manager = PluginBundleManager.getInstance();
        if (!manager.isPlatformSupported()) {
            enabled = false;
        }

        // Disable if current file type is not supported.
        if (enabled) {
            FileType type = getCurrentFileType(e);
            if (type == null || !type.getDefaultExtension().equals("py")) {
                enabled = false;
            }
        }

        e.getPresentation().setEnabled(enabled);
    }
}
