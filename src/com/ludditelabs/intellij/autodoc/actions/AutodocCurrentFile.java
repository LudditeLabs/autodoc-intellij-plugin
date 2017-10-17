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
import com.ludditelabs.intellij.autodoc.PluginUtils;
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
    private Project m_project;
    private Editor m_editor;
    private Document m_document;
    private VirtualFile m_file;

    private boolean updateFields(AnActionEvent e) {
        m_project = e.getProject();
        m_editor = null;
        m_document = null;
        m_file = null;

        if (m_project != null) {
            m_editor = FileEditorManager.getInstance(m_project).getSelectedTextEditor();

            if (m_editor != null) {
                m_document = m_editor.getDocument();
                m_file = FileDocumentManager.getInstance().getFile(m_document);
                return true;
            }
        }

        return false;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (!updateFields(e))
            return;

        // If state is true then the file is already processing.
        if (PluginUtils.getLockState(m_file))
            return;

        PluginProjectComponent component =
            m_project.getComponent(PluginProjectComponent.class);

        FileDocumentManager.getInstance().saveDocument(m_document);
        component.runAutodoc(m_document);
        StatisticsManager.countUsage(m_file);
    }

    @Override
    public void update(AnActionEvent e) {
        // Disable action if platform is not supported.
        final PluginBundleManager manager = PluginBundleManager.getInstance();
        if (!manager.isPlatformSupported() || !updateFields(e)) {
            e.getPresentation().setEnabled(false);
            return;
        }

        // Disable if current file type is not supported.
        FileType type = m_file.getFileType();
        if (!type.getDefaultExtension().equals("py")) {
            e.getPresentation().setEnabled(false);
            return;
        }

        boolean is_locked = PluginUtils.getLockState(m_file);
        e.getPresentation().setEnabled(!is_locked);
    }
}
