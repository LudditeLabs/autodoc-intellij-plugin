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

import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.ludditelabs.intellij.autodoc.actions.AutodocFileTask;
import com.ludditelabs.intellij.autodoc.actions.AutodocProjectTask;
import org.jetbrains.annotations.NotNull;

public class PluginProjectComponent extends AbstractProjectComponent {
    private static final Logger LOG = Logger.getInstance(PluginProjectComponent.class);

    protected PluginProjectComponent(Project project) {
        super(project);
    }

    @Override
    public void projectOpened() {
        PluginApp.getInstance().initGlobalParts(myProject);
    }

    /**
     * Run autodoc on a single file.
     *
     * Processing will run in a cancelable background task.
     *
     * @param document document to process.
     * @see AutodocFileTask
     */
    public void runAutodoc(@NotNull final Document document) {
        ProgressManager.getInstance().run(
            new AutodocFileTask(myProject, document));
    }

    /**
     * Run autodoc on a whole project.
     *
     * Processing will run in a cancelable background task.
     *
     * @see AutodocFileTask
     */
    public void runAutodoc() {
        ProgressManager.getInstance().run(new AutodocProjectTask(myProject));
    }
}
