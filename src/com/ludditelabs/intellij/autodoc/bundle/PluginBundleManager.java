package com.ludditelabs.intellij.autodoc.bundle;

import com.intellij.notification.*;
import com.ludditelabs.intellij.autodoc.config.PluginSettings;
import com.ludditelabs.intellij.common.bundle.BundleManager;

import java.io.IOException;


public class PluginBundleManager extends BundleManager {
    /**
     * Construct autodoc platform bundle manager.
     */
    public PluginBundleManager() {
        String path = PluginSettings.getPluginPath();

        init(new PluginRemoteBundle(), new PluginLocalBundle(path));

        setFirstDownloadText(
            "You need to download Platform Bundle " +
            "before using Autodoc features.");

        setNewVersionText(
            "New version of the Autodoc Platform Bundle is available!");

        setAfterFirstDownloadText("Autodoc Platform Bundle is installed.");
        setAfterUpdateText("Autodoc Platform Bundle is updated.");

        subscribe(new BundleManager.NotifierAdapter() {
            @Override
            public void ioError(IOException e) {
                NotificationGroup group = new NotificationGroup(
                    "Autodoc Platform Bundle Error",
                    NotificationDisplayType.STICKY_BALLOON,
                    true
                );
                Notification notification = group.createNotification(
                    "Autodoc Platform Bundle Error",
                    e.getLocalizedMessage(),
                    NotificationType.ERROR, null);
                Notifications.Bus.notify(notification, null);
            }
        });
    }
}
