package com.ludditelabs.intellij.autodoc.bundle;

import com.ludditelabs.intellij.common.bundle.S3Bundle;
import org.jetbrains.annotations.NotNull;


/**
 * This class represents autodoc remote platform bundle.
 */
public class PluginRemoteBundle extends S3Bundle {
    private static final String BUCKET = "intellij-plugins-data";
    private static final String FOLDER = "autodoc";
    private static final String NAME = "autodoc";
    private static final String DISPLAY_NAME = "Autodoc";

    /**
     * Construct autodoc remote platform bundle.
     */
    public PluginRemoteBundle() {
        super(BUCKET, FOLDER, NAME, DISPLAY_NAME);
    }
}
