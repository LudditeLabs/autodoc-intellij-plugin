package com.ludditelabs.intellij.autodoc;

import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.ludditelabs.intellij.autodoc.actions.AutodocFileTask;
import org.jetbrains.annotations.NotNull;

public class PluginProjectComponent extends AbstractProjectComponent {
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
}
