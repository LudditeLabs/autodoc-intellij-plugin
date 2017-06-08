package com.ludditelabs.intellij.common.bundle;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class BundleInfoDialog extends DialogWrapper {
    @NotNull private final BundleMetadata m_localMetadata;
    @NotNull private final BundleMetadata m_remoteMetadata;
    public BundleInfoDialog(@NotNull String title,
                            @NotNull BundleMetadata localMetadata,
                            @NotNull BundleMetadata remoteMetadata) {
        super(null);

        m_localMetadata = localMetadata;
        m_remoteMetadata = remoteMetadata;

        init();
        setTitle(title);
        setOKButtonText("Update");
        setCancelButtonText("Remind Me Later");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        BundleInfoPanel panel = new BundleInfoPanel(
            m_localMetadata, m_remoteMetadata);
        return panel.getComponent();
    }
}
