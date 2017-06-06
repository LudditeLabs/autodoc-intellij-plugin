package com.ludditelabs.intellij.common.bundle;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class BundleInfoPanel {
    private JPanel content;
    private JLabel infoLabel;
    private JLabel currentVerLabel;
    private JLabel newVerLabel;
    private JLabel titleLabel;

    public BundleInfoPanel(@NotNull BundleMetadata remoteMetadata) {
        BundleManager mgr = BundleManager.getInstance();
        BundleMetadata local_meta = mgr.getLocalBundle().getMetadata();
        assert local_meta != null;

        titleLabel.setText(String.format(
            "<html>New version <b>%s</b> is available!</html>",
            remoteMetadata.version));

        StringBuilder builder = new StringBuilder();
        builder.append("<html>");

        if (!remoteMetadata.message.isEmpty()) {
            builder.append("<p>").append(remoteMetadata.message).append("</p>");
        }

        if (!remoteMetadata.changes.isEmpty()) {
            builder.append("Highlights:");
            builder.append("<ul>");
            for (String change : remoteMetadata.changes) {
                builder.append("<li>").append(change).append("</li>");
            }
            builder.append("</ul>");
        }
        builder.append("</html>");

        infoLabel.setText(builder.toString());

        currentVerLabel.setText(local_meta.version);
        newVerLabel.setText(remoteMetadata.version);
    }

    public JComponent getComponent() {
        return content;
    }
}
