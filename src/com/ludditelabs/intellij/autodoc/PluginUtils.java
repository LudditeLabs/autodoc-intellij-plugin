package com.ludditelabs.intellij.autodoc;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.ludditelabs.intellij.autodoc.ui.AutodocToolWindow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Various helper utils.
 */
public class PluginUtils {
    /**
     * Get content root path for the given file.
     *
     * @param project current project.
     * @param file virtual file.
     *
     * @return absolute path to file's content root (parent module root path)
     *         or null if the file does not belong to this project.
     */
    public static @Nullable String getRootPath(@NotNull final Project project,
                                               VirtualFile file) {
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        VirtualFile root = projectFileIndex.getContentRootForFile(file);
        return root != null ? root.getCanonicalPath() : null;
    }

    /**
     * Show notification popup.
     *
     * @param project current project.
     * @param title notification title.
     * @param message message to show.
     * @param type notification type.
     */
    public static void showNotification(@Nullable Project project,
                                        @NotNull String title,
                                        @NotNull String message,
                                        @NotNull NotificationType type) {
        showNotification(project, title, message, type, null);
    }

    public static void showNotification(@Nullable Project project,
                                        @NotNull String title,
                                        @NotNull String message,
                                        @NotNull NotificationType type,
                                        @Nullable NotificationListener listener) {
        Notification n = new Notification(
            "Autodoc", title, message, type, listener);
        Notifications.Bus.notify(n, project);
    }

    /**
     * Show content in the tool window console.
     *
     * This method popups tool window with console view if there is something
     * to show. Otherwise console will be silently cleared (without showing).
     *
     * @param project current project.
     * @param content content to show.
     * @param file file to which console is linked.
     */
    public static void showOutput(@NotNull final Project project,
                                  @NotNull final String content,
                                  @Nullable final VirtualFile file) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                if (content.isEmpty())
                    AutodocToolWindow.clearConsole(project, file);
                else {
                    ConsoleView console = AutodocToolWindow.activateConsole(project, file);
                    console.clear();
                    console.print(content, ConsoleViewContentType.NORMAL_OUTPUT);
                }
            }
        });
    }
}
