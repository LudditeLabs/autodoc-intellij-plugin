package com.ludditelabs.intellij.common.bundle;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// NOTE: icons are located in the <sdk>/platform/icons/src/

/**
 * Panel with multiple states.
 *
 * It displays various UI depending on a state:
 * <ul>
 *     <li>"First install" - shows label and install button.</li>
 *     <li>"New install" - shows label and update button.</li>
 *     <li>"Check install" - shows version label and "check for new version"
 *     button.
 *     </li>
 * </ul>
 */
public class BundleSettingsPanel {
    public enum UpdateState {FirstInstall, NewInstall, CheckUpdate}

    private JPanel content;
    private JLabel label;
    private JButton bttUpdate;
    private EventListenerList m_updateListeners = new EventListenerList();
    private EventListenerList m_installListeners = new EventListenerList();
    private UpdateState m_updateState;
    @Nullable BundleMetadata m_localMetadata = null;
    @Nullable BundleMetadata m_remoteMetadata = null;

    public BundleSettingsPanel() {
        updateState();
        bttUpdate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireUpdateEvent(e, m_updateState == UpdateState.CheckUpdate
                    ? m_updateListeners : m_installListeners);
            }
        });
    }

    private void updateState() {
        if (m_localMetadata == null) {
            m_updateState = UpdateState.FirstInstall;
            label.setText("Platform bundle is not installed yet.");
            bttUpdate.setIcon(AllIcons.Actions.Download);
            bttUpdate.setText("Install");
            bttUpdate.setToolTipText("");
        }
        else if (m_remoteMetadata != null && m_remoteMetadata.isNewerThan(m_localMetadata)) {
            m_updateState = UpdateState.NewInstall;
            bttUpdate.setIcon(AllIcons.General.Information);
            bttUpdate.setText(String.format("Install version %s...", m_remoteMetadata.version));
            bttUpdate.setToolTipText(m_remoteMetadata.message);
        }
        else {
            m_updateState = UpdateState.CheckUpdate;
            bttUpdate.setIcon(AllIcons.Actions.Refresh);
            bttUpdate.setText("");
            bttUpdate.setToolTipText("Check for updates");
        }
    }

    public void setLocalMetadata(@Nullable BundleMetadata metadata) {
        m_localMetadata = metadata;
        if (m_localMetadata != null) {
            label.setText(String.format("Platform bundle version: %s",
                m_localMetadata.version));
        }
        else
            label.setText("");
        updateState();
    }

    public void setRemoteMetadata(@Nullable BundleMetadata metadata) {
        m_remoteMetadata = metadata;
        updateState();
    }

    public JComponent getComponent() {
        return content;
    }

    public void setBundleManager(@NotNull BundleManager manager) {
        manager.subscribe(new BundleManager.NotifierAdapter() {
            @Override
            public void stateChanged(boolean busy) {
                bttUpdate.setEnabled(!busy);
            }

            @Override
            public void metadataDownloaded(@NotNull BundleMetadata metadata) {
                setRemoteMetadata(metadata);
            }
        });
    }

    private void fireUpdateEvent(ActionEvent e, EventListenerList listeners) {
        Object[] lst = listeners.getListenerList();

        for (int i = lst.length - 2; i >= 0; i -= 2) {
            if (lst[i] == ActionListener.class)
                ((ActionListener)lst[i + 1]).actionPerformed(e);
        }
    }

    public UpdateState getUpdateState() {
        return m_updateState;
    }

    public void addInstallListener(ActionListener l) {
        m_installListeners.add(ActionListener.class, l);
    }

    public void removeInstallListener(ActionListener l) {
        m_installListeners.remove(ActionListener.class, l);
    }

    public void addUpdateListener(ActionListener l) {
        m_updateListeners.add(ActionListener.class, l);
    }

    public void removeUpdateListener(ActionListener l) {
        m_updateListeners.remove(ActionListener.class, l);
    }
}
