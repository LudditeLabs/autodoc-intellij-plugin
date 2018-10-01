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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionHelper;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.ludditelabs.intellij.autodoc.PluginUtils;
import com.ludditelabs.intellij.autodoc.ui.AutodocToolWindow;
import com.ludditelabs.intellij.common.execution.ExternalCommand;
import com.ludditelabs.intellij.common.execution.ExternalCommandListener;
import com.ludditelabs.intellij.common.execution.ExternalCommandResult;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class
AutodocProjectTask extends AutodocBaseCommandTask {
    private static final Logger LOG = Logger.getInstance("ludditelabs.autodoc.task.project");

    /**
     * Construct task.
     *
     * @param project the project for which the task is created.
     */
    public AutodocProjectTask(@NotNull Project project) {
        super(project);
    }

    @Override
    protected void onBeforeRun() {
        // NOTE: Lock state is checked in AutodocCurrentProject action.
        PluginUtils.setLockState(myProject, true);
    }

    @Override
    protected void onAfterRun() {
        // NOTE: Lock state is checked in AutodocCurrentProject action.
        PluginUtils.setLockState(myProject, false);
    }

    @Override
    protected void execute(@NotNull final ProgressIndicator indicator) {
        String path = myProject.getBasePath();
        if (path == null) {
            showError("Can't detect project's path.");
            return;
        }

        ExternalCommand cmd = createCommand();
        cmd.setTitle("Autodoc " + project().getName());
        cmd.setWorkingDirectory(path);
        cmd.addParameters("--fix");
        cmd.addParameters(path);

        cmd.addListener(new ExternalCommandListener() {
            @Override
            public void startNotified(ProcessEvent event) {
                setCurrentHandler(event.getProcessHandler());
            }

            @Override
            public void consume(ExternalCommandResult result) {
                if (indicator.isCanceled() || isCanceled()) {
                    AutodocToolWindow.clearConsole(project());
                    return;
                }

                String output = result.allContent().trim();

                // NOTE: we show output even on errors.
                if (!output.isEmpty()) {
                    PluginUtils.showOutput(project(), output);
                }

                if (!result.isSuccess()) {
                    showError("Finished with errors.");
                    return;
                }

                if (output.isEmpty()) {
                    showInfoOnEmptyOutput();
                }

                VirtualFile root_path = project().getBaseDir();
                VfsUtil.markDirtyAndRefresh(true, true, true, root_path);
            }
        });

        try {
            indicator.setText(cmd.title() + "...");
            LOG.debug(cmd.commandLineString());
            cmd.execute();
        }
        catch (ExecutionException e) {
            ExecutionHelper.showErrors(
                project(), Collections.singletonList(e),
                cmd.title(), null);
        }
    }
}
