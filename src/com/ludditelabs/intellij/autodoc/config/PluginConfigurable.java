package com.ludditelabs.intellij.autodoc.config;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;


// https://confluence.jetbrains.com/display/IDEADEV/Customizing+the+IDEA+Settings+Dialog
public class PluginConfigurable implements Configurable  {
    private PluginSettings m_settings;
    private PluginSettingsPanel m_panel = null;

    public PluginConfigurable() {
        m_settings = PluginSettings.getInstance();
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Autodoc Configuration";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        m_panel = new PluginSettingsPanel();
        m_panel.loadFrom(m_settings);
        return m_panel.getComponent();
    }

    @Override
    public boolean isModified() {
        return m_panel.isModified(m_settings);
    }

    @Override
    public void apply() throws ConfigurationException {
        m_panel.saveTo(m_settings);
    }

    @Override
    public void reset() {
        m_panel.loadFrom(m_settings);
    }

    @Override
    public void disposeUIResources() {
        Disposer.dispose(m_panel);
    }
}
