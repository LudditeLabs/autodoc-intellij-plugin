package com.ludditelabs.intellij.common.bundle;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class BundleInfoDialog extends DialogWrapper {
    private BundleMetadata m_metadata;
    public BundleInfoDialog(@NotNull BundleMetadata metadata) {
        super(null);

        m_metadata = metadata;

        init();
        setTitle("Platform Bundle Update");
        setOKButtonText("Update");
        setCancelButtonText("Remind Me Later");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        BundleInfoPanel panel = new BundleInfoPanel(m_metadata);
        return panel.getComponent();
    }
}
