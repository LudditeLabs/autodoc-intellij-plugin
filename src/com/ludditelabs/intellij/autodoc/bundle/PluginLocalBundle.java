package com.ludditelabs.intellij.autodoc.bundle;

import com.ludditelabs.intellij.common.bundle.LocalBundle;
import org.jetbrains.annotations.NotNull;


/**
 * This class represents autodoc local platform bundle.
 */
public class PluginLocalBundle extends LocalBundle {
    private static String NAME = "autodoc";
    private static String DISPLAY_NAME = "Autodoc";

    /**
     * Construct autodoc platform bundle.
     * @param rootPath Root path where to unpack the bundle.
     */
    public PluginLocalBundle(@NotNull String rootPath) {
        super(rootPath, NAME, DISPLAY_NAME);
    }
}
