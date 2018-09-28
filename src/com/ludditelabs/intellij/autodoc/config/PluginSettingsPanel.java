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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.IdeBorderFactory;
import com.ludditelabs.intellij.autodoc.bundle.PluginBundleManager;
import com.ludditelabs.intellij.common.bundle.BundleMetadata;
import com.ludditelabs.intellij.common.bundle.BundleSettingsPanel;
import com.ludditelabs.intellij.common.bundle.LocalBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;


public class PluginSettingsPanel implements Disposable {
    private JPanel panel;
    private JPanel infoPanelHolder;
    private JPanel contentPanel;
    private JPanel statisticsPanel;
    private JCheckBox statisticsCheck;
    private BundleSettingsPanel m_bundlePanel;

    public PluginSettingsPanel() {
        setupPlatformBundlePanel();

        statisticsPanel.setBorder(IdeBorderFactory.createTitledBorder(
            "Statistics", true));
    }

    private void setupPlatformBundlePanel() {
        PluginBundleManager manager = PluginBundleManager.getInstance();

        m_bundlePanel = new BundleSettingsPanel();
        m_bundlePanel.setBundleManager(manager);

        m_bundlePanel.addInstallListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PluginBundleManager.getInstance().download();
            }
        });

        m_bundlePanel.addUpdateListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PluginBundleManager.getInstance().checkUpdate(new Runnable() {
                    @Override
                    public void run() {
                        Messages.showMessageDialog(
                            "Everything is up to date.",
                            "Autodoc", null);
                    }
                });
            }
        });

        manager.subscribe(new PluginBundleManager.NotifierAdapter() {
            @Override
            public void ioError(IOException e) {
                if (panel.isVisible()) {
                    Messages.showErrorDialog(e.getLocalizedMessage(),
                        "Autodoc Bundle");
                }
            }
            @Override
            public void unpacked() {
                if (panel.isVisible()) {
                    setBundle(PluginBundleManager.getInstance().getLocalBundle());
                }
            }
        }, this);

        infoPanelHolder.add(m_bundlePanel.getComponent());
        setBundle(manager.getLocalBundle());
    }

    private void setBundle(@NotNull final LocalBundle bundle) {
        BundleMetadata meta = bundle.getMetadata();
        m_bundlePanel.setLocalMetadata(meta);
        contentPanel.setVisible(meta != null);
    }

    public JComponent getComponent() {
        return panel;
    }

    public void loadFrom(@NotNull PluginSettings settings) {
        m_bundlePanel.setRemoteMetadata(PluginBundleManager.getInstance().getRemoteBundle().getMetadata());
        setBundle(PluginBundleManager.getInstance().getLocalBundle());
        statisticsCheck.setSelected(settings.canCollectStatistics());
    }

    public void saveTo(@NotNull PluginSettings settings) {
        settings.setCanCollectStatistics(statisticsCheck.isSelected());
    }

    public boolean isModified(@NotNull PluginSettings settings) {
        if (!contentPanel.isVisible())
            return false;
        return settings.canCollectStatistics() != statisticsCheck.isSelected();
    }

    @Override
    public void dispose() {

    }
}
