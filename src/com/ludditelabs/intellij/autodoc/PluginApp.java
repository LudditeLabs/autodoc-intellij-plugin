package com.ludditelabs.intellij.autodoc;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.ludditelabs.intellij.autodoc.bundle.PluginBundleManager;
import org.jetbrains.annotations.NotNull;


/**
 * Autodoc application service.
 */
public class PluginApp implements ApplicationComponent {
    protected static final Logger logger = Logger.getInstance("ludditelabs.autodoc.PluginApp");

    // Check for platform bundle and propose to update/download.
    private void checkPlatformBundle() {
        PluginBundleManager manager =
            (PluginBundleManager)PluginBundleManager.getInstance();

        if (!manager.getLocalBundle().isExist()) {
            manager.showFirstDownloadNotification();
        }
        else {
            // TODO: delay new version check.
            // For example, if last check was less than 2 hrs then don't check.
            manager.checkUpdateSilent();
        }
    }

    /** Get plugin's application service instance. */
    public static PluginApp getInstance() {
        return ApplicationManager.getApplication().getComponent(
            PluginApp.class);
    }

    @Override
    public void initComponent() {
        checkPlatformBundle();
    }

    @Override
    public void disposeComponent() {

    }

    @NotNull
    @Override
    public String getComponentName() {
        return "LudditeLabsAutodocApp";
    }
}
