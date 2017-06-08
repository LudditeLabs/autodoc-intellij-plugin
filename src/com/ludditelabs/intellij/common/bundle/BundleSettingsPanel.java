package com.ludditelabs.intellij.common.bundle;

import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

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

    public BundleSettingsPanel() {
        setNeedInstall();
        bttUpdate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fireUpdateEvent(e, m_updateState == UpdateState.CheckUpdate
                    ? m_updateListeners : m_installListeners);
            }
        });
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
        });
    }

    private void fireUpdateEvent(ActionEvent e, EventListenerList listeners) {
        Object[] lst = listeners.getListenerList();

        for (int i = lst.length - 2; i >= 0; i -= 2) {
            if (lst[i] == ActionListener.class)
                ((ActionListener)lst[i + 1]).actionPerformed(e);
        }
    }

    public void setNeedInstall() {
        m_updateState = UpdateState.FirstInstall;
        label.setText("Platform bundle is not installed yet.");
        bttUpdate.setIcon(AllIcons.Actions.Download);
        bttUpdate.setText("Install");
        bttUpdate.setToolTipText("");
    }

    public void setVersion(String version) {
        label.setText(String.format("Platform bundle version: %s", version));
        setHasUpdate(false);
    }

    public void setHasUpdate(boolean state) {
        if (state) {
            m_updateState = UpdateState.NewInstall;
            bttUpdate.setIcon(AllIcons.General.Information);
            bttUpdate.setText("Install new version...");
            bttUpdate.setToolTipText("");
        }
        else {
            m_updateState = UpdateState.CheckUpdate;
            bttUpdate.setIcon(AllIcons.Actions.Refresh);
            bttUpdate.setText("");
            bttUpdate.setToolTipText("Check for updates");
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
