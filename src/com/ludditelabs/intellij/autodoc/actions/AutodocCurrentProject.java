package com.ludditelabs.intellij.autodoc.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.ludditelabs.intellij.autodoc.PluginProjectComponent;
import com.ludditelabs.intellij.autodoc.PluginUtils;
import com.ludditelabs.intellij.autodoc.bundle.PluginBundleManager;

public class AutodocCurrentProject extends AnAction {
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

        // If state is true then the project is already processing.
        if (PluginUtils.getLockState(project))
            return;

        FileDocumentManager.getInstance().saveAllDocuments();
        component.runAutodoc();
    }

    @Override
    public void update(AnActionEvent e) {
        final PluginBundleManager manager = PluginBundleManager.getInstance();
        if (!manager.isPlatformSupported()) {
            e.getPresentation().setEnabled(false);
            return;
        }

        final Project project = e.getProject();
        boolean is_locked  = project == null || PluginUtils.getLockState(project);
        e.getPresentation().setEnabled(!is_locked);
    }
}
