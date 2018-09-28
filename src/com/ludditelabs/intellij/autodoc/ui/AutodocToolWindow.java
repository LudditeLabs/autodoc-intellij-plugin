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

package com.ludditelabs.intellij.autodoc.ui;

import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.*;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


/**
 * This class manages autodoc plugin tool window with console views.
 *
 * The views are used to display autodoc output.
 */
public class AutodocToolWindow {
    private static final String TOOL_WINDOW_KEY = "Autodoc";
    private static final String HOLDER_KEY = "autodoc_console_holder";
    private static final Key<ConsoleView> CONSOLE_KEY = new Key<>("AutodocOutConsole");
    private static final Icon TOOL_WINDOW_ICON = IconLoader.getIcon("/icons/tool_window.png");

    // Helper method to get or create & register tool window for the plugin.
    private static @NotNull ToolWindow getToolWindow(@NotNull Project project) {
        final ToolWindowManager manager = ToolWindowManager.getInstance(project);
        ToolWindow win = manager.getToolWindow(TOOL_WINDOW_KEY);

        if (win == null) {
            win = manager.registerToolWindow(TOOL_WINDOW_KEY, true, ToolWindowAnchor.BOTTOM);
            win.setIcon(TOOL_WINDOW_ICON);

            // We store console views as user data inside project or module
            // on create. When someone closes console UI we have to
            // reset that user data (otherwise next time we'll thing that
            // console still exists).
            //
            // Also when all consoles are removed we also close tool window
            // itself and unregister it to free resources.
            win.getContentManager().addContentManagerListener(new ContentManagerAdapter() {
                @Override
                public void contentRemoved(ContentManagerEvent event) {
                    // This component is a console UI where we stored
                    // user data, see createConsole().
                    JComponent c = event.getContent().getComponent();

                    // Extract console holder.
                    // We set it in the createConsole().
                    UserDataHolder holder = (UserDataHolder)c.getClientProperty(HOLDER_KEY);
                    if (holder == null)
                        return;

                    // We don't need holder pointer anymore.
                    c.putClientProperty(HOLDER_KEY, null);

                    // Since console UI is closed we need to drop reference
                    // to it in the holder. Holder - project or module object.
                    holder.putUserData(CONSOLE_KEY, null);

                    // Check if all content is removed from the tool window
                    // and unregister it if yes.
                    ContentManager m = (ContentManager)event.getSource();
                    if (m != null && m.getContentCount() == 0) {
                        m.removeContentManagerListener(this);
                        manager.unregisterToolWindow(TOOL_WINDOW_KEY);
                    }
                }
            });
        }

        return win;
    }

    // Helper method to create console view.
    // This method adds the console to the plugin's tool window.
    private static ConsoleView createConsole(@NotNull ToolWindow win,
                                             @NotNull Project project,
                                             @NotNull UserDataHolder holder,
                                             String title) {
        // Create console view.
        ContentFactory factory = ContentFactory.SERVICE.getInstance();
        TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
        ConsoleView console = consoleBuilder.getConsole();

        holder.putUserData(CONSOLE_KEY, console);

        // This holder will be extracted from the component
        // UI gets closed, see getToolWindow().
        console.getComponent().putClientProperty(HOLDER_KEY, holder);

        // Add the console to the tool window.
        Content content = factory.createContent(console.getComponent(), title, true);
        win.getContentManager().addContent(content);
        return console;
    }

    /**
     * Activate console view.
     *
     * This method creates a console if it's not present yet and shows it
     * to the user.
     *
     * Each holder has separate console view. Info about console is attached
     * to the holder.
     *
     * @param project current project.
     * @param holder console view data holder.
     * @param title console title.
     * @return console view.
     */
    public static @NotNull
    ConsoleView activateConsole(@NotNull Project project,
                                @NotNull UserDataHolder holder,
                                String title) {
        ToolWindow win = getToolWindow(project);
        ConsoleView console = holder.getUserData(CONSOLE_KEY);
        if (console == null)
            console = createConsole(win, project, holder, title);

        Content content = win.getContentManager().getContent(console.getComponent());
        win.getContentManager().setSelectedContent(content);

        win.show(null);
        return console;
    }

    /**
     * Activate console view.
     *
     * @param project current project.
     * @return console view.
     */
    public static @NotNull
    ConsoleView activateConsole(@NotNull Project project) {
        return activateConsole(project, project, project.getName());
    }

    /**
     * Activate console view.
     *
     * @param project current project.
     * @return console view.
     */
    public static @NotNull
    ConsoleView activateConsole(@NotNull Project project, @NotNull final VirtualFile file) {
        return activateConsole(project, file, file.getName());
    }

    /**
     * Clear console content.
     *
     * @param holder to which console is linked.
     */
    public static void clearConsole(@NotNull UserDataHolder holder) {
        ConsoleView console = holder.getUserData(CONSOLE_KEY);
        if (console == null)
            return;
        console.clear();
    }
}
