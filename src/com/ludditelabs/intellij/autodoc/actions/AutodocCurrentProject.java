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
