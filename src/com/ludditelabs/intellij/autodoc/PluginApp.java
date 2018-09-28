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
