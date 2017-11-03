package com.ludditelabs.intellij.autodoc.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBLabel;
import com.ludditelabs.intellij.autodoc.bundle.PluginBundleManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


// https://confluence.jetbrains.com/display/IDEADEV/Customizing+the+IDEA+Settings+Dialog
public class PluginConfigurable implements Configurable  {
    private PluginSettings m_settings;
    private PluginSettingsPanel m_panel = null;
    private JBLabel m_unsupportedLabel = null;

    public PluginConfigurable() {
        m_settings = PluginSettings.getInstance();
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Autodoc";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        PluginBundleManager manager = PluginBundleManager.getInstance();
        if (manager.isPlatformSupported()) {
            m_panel = new PluginSettingsPanel();
            m_panel.loadFrom(m_settings);
            return m_panel.getComponent();
        }
        else {
            m_unsupportedLabel = new JBLabel("Your OS is not supported by autodoc platform bundle.");
            m_unsupportedLabel.setHorizontalAlignment(JBLabel.CENTER);
            m_unsupportedLabel.setVerticalAlignment(JBLabel.CENTER);
            return m_unsupportedLabel;
        }
    }

    @Override
    public boolean isModified() {
        return m_panel != null && m_panel.isModified(m_settings);
    }

    @Override
    public void apply() throws ConfigurationException {
        if (m_panel != null)
            m_panel.saveTo(m_settings);
    }

    @Override
    public void reset() {
        if (m_panel != null)
            m_panel.loadFrom(m_settings);
    }

    @Override
    public void disposeUIResources() {
        if (m_panel != null)
            Disposer.dispose(m_panel);
        else if (m_unsupportedLabel != null)
            m_unsupportedLabel = null;
    }
}
