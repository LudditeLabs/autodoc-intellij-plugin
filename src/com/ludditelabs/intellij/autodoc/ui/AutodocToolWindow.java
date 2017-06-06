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
            // reset that user data (otherwise next time we'll thing what
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
    private static ConsoleView createConsole(@NotNull Project project,
                                             @Nullable Module module,
                                             String title,
                                             @NotNull ToolWindow win) {
        // Create console view.
        ContentFactory factory = ContentFactory.SERVICE.getInstance();
        TextConsoleBuilder consoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
        ConsoleView console = consoleBuilder.getConsole();

        // We store console in the module (if not null) or
        // in the project (for files outside modules).
        UserDataHolder holder = ObjectUtils.chooseNotNull(module, project);
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
     * Activate console view for the given module.
     *
     * Each module has separate console view. Info about console is attached
     * to the module or project (if module is null).
     *
     * This method creates console if it's not present yet and shows it
     * to the user.
     *
     * @param project current project.
     * @param module module for which to create console view.
     * @return console view.
     */
    public static @NotNull
    ConsoleView activateConsole(@NotNull Project project,
                                @Nullable Module module) {
        ToolWindow win = getToolWindow(project);

        // We store console in the module (if not null) or
        // in the project (for files outside modules).
        UserDataHolder holder = ObjectUtils.chooseNotNull(module, project);
        ConsoleView console = holder.getUserData(CONSOLE_KEY);
        String title = module == null ? "info" : module.getName();

        if (console == null)
            console = createConsole(project, module, title, win);

        Content content = win.getContentManager().getContent(console.getComponent());
        win.getContentManager().setSelectedContent(content);

        win.show(null);
        return console;
    }

    /**
     * Activate console view for the given file.
     *
     * @param project current project.
     * @param file file for which to create console view.
     * @return console view.
     */
    public static @NotNull
    ConsoleView activateConsole(@NotNull Project project,
                                @Nullable VirtualFile file) {
        Module module = file != null ? ModuleUtilCore.findModuleForFile(file, project) : null;
        return activateConsole(project, module);
    }

    /**
     * Clear console content for the given module.
     *
     * This method doesn't activate/show tool window, it only performs
     * cleanup if console is present.
     *
     * @param project current project.
     * @param module module to which console is linked.
     */
    public static void clearConsole(@NotNull Project project,
                                    @Nullable Module module) {
        UserDataHolder holder = ObjectUtils.chooseNotNull(module, project);
        ConsoleView console = holder.getUserData(CONSOLE_KEY);

        if (console == null)
            return;

        console.clear();
    }

    /**
     * Clear console content for the given file.
     *
     * @param project current project.
     * @param file file to which console is linked.
     */
    public static void clearConsole(@NotNull Project project,
                                    @Nullable VirtualFile file) {
        Module module = file != null ? ModuleUtilCore.findModuleForFile(file, project) : null;
        clearConsole(project, module);
    }
}
