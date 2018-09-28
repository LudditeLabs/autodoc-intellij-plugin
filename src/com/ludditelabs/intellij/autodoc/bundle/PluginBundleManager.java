/*
 * Copyright 2018 Luddite Labs Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ludditelabs.intellij.autodoc.bundle;

import com.intellij.notification.*;
import com.intellij.openapi.components.ServiceManager;
import com.ludditelabs.intellij.autodoc.config.PluginSettings;
import com.ludditelabs.intellij.common.Utils;
import com.ludditelabs.intellij.common.bundle.BundleManager;

import java.io.IOException;

/**
 * Bundle manager service for the Autodoc plugin.
 */
public class PluginBundleManager extends BundleManager {
    // See <id> value in the resources/META-INF/plugin.xml file.
    private static String ID = "com.ludditelabs.autodocintellij.plugin";
    private static final NotificationGroup m_errGroup = new NotificationGroup(
        "Autodoc Platform Bundle Error",
        NotificationDisplayType.STICKY_BALLOON,
        true
    );

    /**
     * Construct autodoc platform bundle manager.
     */
    public PluginBundleManager() {
        super(Utils.getPluginVersion(ID), new PluginRemoteBundle(),
            new PluginLocalBundle(PluginSettings.getPluginPath()));

        setUnsupportedPlatformText(
            "Your OS is not supported by autodoc!"
        );
        setFirstDownloadText(
            "You need to download Platform Bundle " +
            "before using Autodoc features.");

        setNewVersionText(
            "New version of the Autodoc Platform Bundle is available!");

        setAfterFirstDownloadText("Autodoc Platform Bundle is installed.");
        setAfterUpdateText("Autodoc Platform Bundle is updated.");

        setInfoDialogTitle("Autodoc Platform Bundle Update");

        subscribe(new BundleManager.NotifierAdapter() {
            @Override
            public void ioError(IOException e) {
                Notification notification = m_errGroup.createNotification(
                    "Autodoc Platform Bundle Error",
                    e.getLocalizedMessage(),
                    NotificationType.ERROR, null);
                Notifications.Bus.notify(notification, null);
            }
        });
    }

    public static PluginBundleManager getInstance() {
        return ServiceManager.getService(PluginBundleManager.class);
    }
}
