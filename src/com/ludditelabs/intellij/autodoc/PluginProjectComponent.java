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
