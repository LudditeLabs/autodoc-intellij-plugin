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

package com.ludditelabs.intellij.autodoc.config;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.extensions.PluginId;
import com.ludditelabs.intellij.autodoc.statistics.StatisticsManager;
import com.ludditelabs.intellij.common.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


// http://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html
// https://github.com/JetBrains/intellij-sdk-docs/blob/5dcb02991cf828a7d4680d125ce56b4c10234146/basics/persisting_state_of_components.md
// We store configs on the application level (global IDE settings).
@State(
    name = "LudditeLabsAutodocApp",
    storages = @Storage("ludditelabs.autodoc.xml")
)
public class PluginSettings
    implements PersistentStateComponent<PluginSettings.State> {

    static class State {
        public boolean canCollectStatistics = true;
        public boolean showStatisticsNotification = true;
        public long statisticsLastUploadTimestamp = 0;
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
        return Utils.exeFilename(getPluginPath("autodoc-pkg", "autodoc"));
    }

    public static PluginSettings getInstance() {
        return ServiceManager.getService(PluginSettings.class);
    }

    /**
     * Get plugin's installation path.
     *
     * @return Absolute path.
     */
    @Nullable
    public static String getPluginInstallPath() {
        final IdeaPluginDescriptor desc = PluginManager.getPlugin(PluginId.getId(ID));

        if (desc != null) {
            final File desc_path = desc.getPath();

            // If "/classes" exists then we are in the plugin sandbox.
            final Path cls = Paths.get(desc_path.getAbsolutePath(), "classes");
            if (Files.exists(cls)) {
                return Paths.get(cls.toString()).toAbsolutePath().toString();
            }

            if (!desc_path.isDirectory())
                return Paths.get(desc_path.getParent()).toString();

            return desc_path.getAbsolutePath();
        }

        // This can happen only if plugin ID is wrong.
        // See <id> value in the resources/META-INF/plugin.xml file.
        return null;
    }

    /**
     * Build absolute path prefixed with the plugin installation path.
     *
     * @param path path relative to the plugin's installation path.
     * @return Absolute path.
     */
    @NotNull
    public static String getPluginPath(String... path) {
        String base_path = System.getProperty("ludditelabs.basepath");

        if (base_path == null) {
            base_path = getPluginInstallPath();
        }

        if (base_path != null) {
            return Paths.get(base_path.trim(), path).toAbsolutePath().toString();
        }

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
        StatisticsManager.setActive(m_state.canCollectStatistics);
    }

    // PluginSettings API

    public boolean canCollectStatistics() {
        return m_state.canCollectStatistics;
    }

    public void setCanCollectStatistics(boolean state) {
        m_state.canCollectStatistics = state;
        StatisticsManager.setActive(m_state.canCollectStatistics);
    }

    public boolean showStatisticsNotification() {
        return m_state.showStatisticsNotification;
    }

    public void setShowStatisticsNotification(boolean state) {
        m_state.showStatisticsNotification = state;
    }

    public long statisticsLastUploadTimestamp() {
        return m_state.statisticsLastUploadTimestamp;
    }

    public void setStatisticsLastUploadTimestamp(long value) {
        m_state.statisticsLastUploadTimestamp = value;
    }

    @NotNull
    public String exePath() {
        return m_exePath;
    }
}
