package com.ludditelabs.intellij.autodoc;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;


/**
 * Autodoc application service.
 */
public class PluginApp implements ApplicationComponent {
    protected static final Logger logger = Logger.getInstance("ludditelabs.autodoc.PluginApp");

    @Override
    public void initComponent() {

    }

    @Override
    public void disposeComponent() {

    }

    @NotNull
    @Override
    public String getComponentName() {
        return "LudditeLabsAutodocApp";
    }
}
