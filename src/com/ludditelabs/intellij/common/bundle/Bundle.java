package com.ludditelabs.intellij.common.bundle;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class represents platform bundle.
 *
 * Platform bundle is a set of files for a specific platform.
 */
public class Bundle {
    @NotNull private final String m_displayName;
    @Nullable private BundleMetadata m_metadata = null;

    /**
     * Construct bundle.
     * @param displayName Name to display in UI.
     */
    public Bundle(@NotNull String displayName) {
        m_displayName = displayName;
    }

    /** Bundle display name. */
    @NotNull
    public String getDisplayName() {
        return m_displayName;
    }

    /** Bundle metadata. */
    @Nullable
    public BundleMetadata getMetadata() {
        return m_metadata;
    }

    /** Set bundle metadata. */
    public void setMetadata(@Nullable BundleMetadata metadata) {
        m_metadata = metadata;
    }
}
