package com.ludditelabs.intellij.autodoc;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.ludditelabs.intellij.autodoc.actions.AutodocFileTask;
import com.ludditelabs.intellij.autodoc.bundle.PluginBundleManager;
import com.ludditelabs.intellij.autodoc.statistics.StatisticsManager;
import org.jetbrains.annotations.NotNull;


/**
 * Autodoc application service.
 */
public class PluginApp implements ApplicationComponent {
    private boolean m_needInitGlobalParts = true;

    // Check for platform bundle and propose to update/download.
    private void checkPlatformBundle(Project project) {
        PluginBundleManager manager = PluginBundleManager.getInstance();

        if (!manager.getLocalBundle().isExist()) {
            if (manager.isPlatformSupported()) {
                manager.showFirstDownloadNotification(project);
            }
            else {
                manager.showUnsupportedPlatformNotification(project);
            }
        }
        else {
            // If we have bundle then platform is supported.
            manager.setPlatformSupported(true);
            // TODO: delay new version check.
            // For example, if last check was less than 2 hrs then don't check.
            manager.checkUpdateSilent(project);
        }
    }

    private void doInitGlobalParts(Project project) {
        StatisticsManager.init();
        checkPlatformBundle(project);
    }

    @Override
    public void initComponent() {
    }

    @Override
    public void disposeComponent() {

    }

    @NotNull
    @Override
    public String getComponentName() {
        return getClass().getName();
    }

    /** Get plugin's application service instance. */
    public static PluginApp getInstance() {
        return ApplicationManager.getApplication().getComponent(
            PluginApp.class);
    }

    public void initGlobalParts(Project project) {
        if (m_needInitGlobalParts) {
            m_needInitGlobalParts = false;
            doInitGlobalParts(project);
        }
    }
}
