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
 *     <li>No local bundle - shows label and install button.</li>
 *     <li>Has local bundle - shows local version and button to check updates.
 *     </li>
 * </ul>
 */
public class BundleSettingsPanel {
    private JPanel content;
    private JLabel label;
    private JButton bttUpdate;
    private EventListenerList m_updateListeners = new EventListenerList();
    private EventListenerList m_installListeners = new EventListenerList();
    private BundleMetadata m_localMetadata = null;
    private BundleMetadata m_remoteMetadata = null;

    public BundleSettingsPanel() {
        updateState();
        bttUpdate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireUpdateEvent(e, m_localMetadata == null
                    ? m_installListeners : m_updateListeners);
            }
        });
    }

    private void updateState() {
        if (m_localMetadata == null) {
            label.setText("Platform bundle is not installed yet.");
            bttUpdate.setIcon(AllIcons.Actions.Download);
            bttUpdate.setText("Install");
            bttUpdate.setToolTipText("");
        }
        else if (m_remoteMetadata != null && m_remoteMetadata.isNewerThan(m_localMetadata)) {
            bttUpdate.setIcon(AllIcons.General.Information);
            bttUpdate.setText("Install new version ...");
            bttUpdate.setToolTipText("New version is available!");
        }
        else {
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

    /**
     * Subscribe on first install request.
     * @param l Action listener.
     */
    public void addInstallListener(ActionListener l) {
        m_installListeners.add(ActionListener.class, l);
    }

    /**
     * Unsubscribe given listener from the first install request.
     * @param l Action listener.
     */
    public void removeInstallListener(ActionListener l) {
        m_installListeners.remove(ActionListener.class, l);
    }

    /**
     * Subscribe on update request.
     * @param l Action listener.
     */
    public void addUpdateListener(ActionListener l) {
        m_updateListeners.add(ActionListener.class, l);
    }

    /**
     * Unsubscribe given listener from the update request.
     * @param l Action listener.
     */
    public void removeUpdateListener(ActionListener l) {
        m_updateListeners.remove(ActionListener.class, l);
    }
}
