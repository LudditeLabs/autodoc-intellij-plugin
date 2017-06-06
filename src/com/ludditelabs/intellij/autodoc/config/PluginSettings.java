package com.ludditelabs.intellij.autodoc.config;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


// http://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html
// https://github.com/JetBrains/intellij-sdk-docs/blob/5dcb02991cf828a7d4680d125ce56b4c10234146/basics/persisting_state_of_components.md
// We store configs on the application level (global IDE settings).
@State(
    name = "LudditeLabsAutodocApp",
    storages = {
        @Storage(id="other", file = StoragePathMacros.APP_CONFIG + "/ludditelabs.autodoc.xml")
    }
)
public class PluginSettings
    implements PersistentStateComponent<PluginSettings.State> {

    static class State {
        public boolean canCollectStatistics = true;
        public boolean showStatisticsNotification = true;
    }

    // See <id> value in the resources/META-INF/plugin.xml file.
    private static String ID = "com.ludditelabs.autodocintellij.plugin";

    @NotNull
    private State m_state = new State();

    @NotNull
    private final String m_exePath = buildExePath();

    public PluginSettings() {
    }

    @NotNull
    private static String buildExePath() {
        String ext = SystemInfo.isWindows ? ".exe" : ".bin";
        return getPluginPath("autodoc-pkg", "autodoc" + ext);
    }

    public static PluginSettings getInstance() {
        return ServiceManager.getService(PluginSettings.class);
    }

    /**
     * Build absolute path prefixed with the plugin installation path.
     *
     * @param path path relative to the plugin's installation path.
     * @return Absolute path.
     */
    @NotNull
    public static String getPluginPath(String... path) {
        IdeaPluginDescriptor desc = PluginManager.getPlugin(PluginId.getId(ID));
        if (desc != null) {
            // If "/classes" exists then we are in the plugin sandbox.
            Path cls = Paths.get(
                desc.getPath().getAbsolutePath(), "classes");
            if (Files.exists(cls)) {
                return Paths.get(
                    cls.toString(), path).toAbsolutePath().toString();
            }

            if (!desc.getPath().isDirectory())
                return Paths.get(desc.getPath().getParent(), path).toString();

            return Paths.get(
                desc.getPath().getAbsolutePath(),
                path).toAbsolutePath().toString();
        }
        // This can happen only if plugin ID is wrong.
        // See <id> value in the resources/META-INF/plugin.xml file.
        return "";
    }

    @Nullable
    @Override
    public State getState() {
        return m_state;
    }

    @Override
    public void loadState(State state) {
        m_state = state;
    }

    // PluginSettings API

    public boolean canCollectStatistics() {
        return m_state.canCollectStatistics;
    }

    public void setCanCollectStatistics(boolean state) {
        m_state.canCollectStatistics = state;
    }

    public boolean showStatisticsNotification() {
        return m_state.showStatisticsNotification;
    }

    public void setShowStatisticsNotification(boolean state) {
        m_state.showStatisticsNotification = state;
    }

    @NotNull
    public String exePath() {
        return m_exePath;
    }
}
