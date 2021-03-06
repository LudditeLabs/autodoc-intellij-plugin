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
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AppUIUtil;
import com.ludditelabs.intellij.autodoc.ui.AutodocToolWindow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Various helper utils.
 */
public class PluginUtils {
    private static final Key<Boolean> LOCK_KEY = new Key<>("AutodocLock");

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
     */
    public static void showOutput(@NotNull final Project project,
                                  @NotNull final String content) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                if (content.isEmpty())
                    AutodocToolWindow.clearConsole(project);
                else {
                    ConsoleView console = AutodocToolWindow.activateConsole(project);
                    console.clear();
                    console.print(content, ConsoleViewContentType.NORMAL_OUTPUT);
                }
            }
        });
    }

    /**
     * Get lock state for the given object.
     *
     * NOTE: must be called in AWT thread.
     */
    public static boolean getLockState(@NotNull final UserDataHolder holder) {
        try {
            return holder.getUserData(LOCK_KEY);
        }
        catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Set lock state for the given object.
     *
     * This state is used to indicate what item is processing.
     */
    public static void setLockState(@Nullable final UserDataHolder holder, final boolean state) {
        if (holder != null) {
            AppUIUtil.invokeOnEdt(new Runnable() {
                @Override
                public void run() {
                    holder.putUserData(LOCK_KEY, state);
                }
            });
        }
    }

}
