package com.ludditelabs.intellij.autodoc.config;

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


public class PluginSettingsPanel {
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
        m_bundlePanel = new BundleSettingsPanel();

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

        PluginBundleManager.getInstance().subscribe(new PluginBundleManager.NotifierAdapter() {
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
        });

        infoPanelHolder.add(m_bundlePanel.getComponent());
    }

    private boolean setBundle(@NotNull final LocalBundle bundle) {
        final boolean state = bundle.isExist();
        if (state) {
            final BundleMetadata meta = bundle.getMetadata();
            if (meta != null) {
                m_bundlePanel.setVersion(meta.version);
                contentPanel.setVisible(true);
                return true;
            }
        }

        // Something wrong or the bundle is not exists,
        // so we ask to download again.
        m_bundlePanel.setNeedInstall();
        contentPanel.setVisible(false);
        return state;
    }

    public JComponent getComponent() {
        return panel;
    }

    public void loadFrom(@NotNull PluginSettings settings) {
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
}
