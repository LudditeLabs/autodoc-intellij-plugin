package com.ludditelabs.intellij.common.bundle;


import com.intellij.notification.*;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.util.Consumer;
import com.intellij.util.text.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.io.IOException;

/**
 * Application service to install and update platform bundle.
 *
 * It extend Updater class with the following features:
 * <ul>
 *     <li>Show notifications if new version is available.</li>
 *     <li>Show 'new version' dialog.</li>
 *     <li>Download and install bundle from notifications.</li>
 * </ul>
 *
 * Add to plugin.xml:
 * <pre>
 * <code>
 *  <applicationService
 *    serviceInterface="com.ludditelabs.intellij.common.bundle.BundleManager"
 *    serviceImplementation="com.ludditelabs.intellij.common.bundle.BundleManager" />
 * </code>
 * </pre>
 */
public class BundleManager extends Updater {
    public static class NotifierAdapter implements Updater.Notifier {
        @Override
        public void canceled() {

        }

        @Override
        public void ioError(IOException e) {

        }

        @Override
        public void stateChanged(boolean busy) {

        }

        @Override
        public void metadataDownloaded(@NotNull BundleMetadata metadata) {

        }

        @Override
        public void unpacked() {

        }
    }

    private NotificationGroup m_releaseGroup;
    private NotificationGroup m_infoGroup;
    private Notification m_notification = null;

    private String m_bundleName;
    private String m_firstDownloadText;
    private String m_newVersionText;
    private String m_afterUpdateText;
    private String m_afterFirstDownloadText;

    /**
     * Construct manager.
     *
     * Do nothing by default.
     */
    public BundleManager() {
    }

    // Updater API

    @Override
    public void init(@NotNull RemoteBundle remoteBundle,
                     @NotNull LocalBundle localBundle) {
        super.init(remoteBundle, localBundle);

        m_bundleName = localBundle.getDisplayName();

        m_releaseGroup = new NotificationGroup(
            m_bundleName + " Bundle Release",
            NotificationDisplayType.STICKY_BALLOON, true);

        m_infoGroup = new NotificationGroup(
            m_bundleName + " Bundle",
            NotificationDisplayType.BALLOON, true);
    }

    public void setBusy(boolean state) {
        if (m_notification != null) {
            m_notification.expire();
            m_notification = null;
        }
        super.setBusy(state);
    }

    // Display text

    /**
     * Set text to display in the notification if no bundle is installed yet.
     * @param text Text to display.
     */
    public void setFirstDownloadText(String text) {
        m_firstDownloadText = text;
    }

    /**
     * Set text to display in the notification if new version is available.
     * @param text Text to display.
     */
    public void setNewVersionText(String text) {
        m_newVersionText = text;
    }

    /**
     * Set text to display in the notification after bundle is updated.
     * @param text Text to display.
     */
    public void setAfterUpdateText(String text) {
        m_afterUpdateText = text;
    }

    /**
     * Set text to display in the notification after first bundle installation.
     * @param text Text to display.
     */
    public void setAfterFirstDownloadText(String text) {
        m_afterFirstDownloadText = text;
    }

    // Service API

    public static BundleManager getInstance() {
        return ServiceManager.getService(BundleManager.class);
    }

    // Bundle manager API.

    private void afterDownload(boolean isFirst) {
        Notification n = m_infoGroup.createNotification(
            m_bundleName,
            isFirst ? m_afterFirstDownloadText : m_afterUpdateText,
            NotificationType.INFORMATION, null);
        Notifications.Bus.notify(n);
    }

    private void showNewVersionDialog(final BundleMetadata metadata) {
        final BundleInfoDialog dlg = new BundleInfoDialog(metadata);
        dlg.show();

        if (dlg.getExitCode() == BundleInfoDialog.OK_EXIT_CODE) {
            download(metadata, new Runnable() {
                @Override
                public void run() {
                    afterDownload(false);
                }
            });
        }
    }

    private void showNewVersionNotification(final BundleMetadata metadata) {
        String html = "<html>" + m_newVersionText + "<br/>" +
            "Click <a href='#'>here</a> for more info.<br/><html>";

        Notification n = m_releaseGroup.createNotification(
            m_bundleName, html, NotificationType.INFORMATION, new NotificationListener.Adapter() {
                @Override
                protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
                    notification.expire();
                    showNewVersionDialog(metadata);
                }
            });
        Notifications.Bus.notify(n);
    }

    /**
     * Show download notification if no bundle is installed yet.
     */
    public void showFirstDownloadNotification() {
        String html = "<html>" + m_firstDownloadText
            + "<br/>Click <a href='#'>here</a> to download.</html>";

        // Download bundle on click and show final message.
        m_notification = m_releaseGroup.createNotification(
            m_bundleName, html, NotificationType.INFORMATION, new NotificationListener.Adapter() {
                @Override
                protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
                    download();
                }
            });
        Notifications.Bus.notify(m_notification);
    }

    /**
     * Download and install platform bundle.
     *
     * Notification will be shown after installation.
     */
    @Override
    public void download() {
        super.download(new Runnable() {
            @Override
            public void run() {
                afterDownload(true);
            }
        });
    }

    /**
     * Check for platform bundle updates with progress displaying.
     *
     * This method downloads remote bundle metadata and checks if it's newer
     * than local bundle. And shows install dialog if so.
     *
     * @param noUpdatesRunner Runnable to call if no update is found.
     */
    public void checkUpdate(@Nullable final Runnable noUpdatesRunner) {
        downloadMetadata(new Consumer<BundleMetadata>() {
            @Override
            public void consume(final BundleMetadata metadata) {
                if (isNewer(metadata))
                    showNewVersionDialog(metadata);
                else if (noUpdatesRunner != null)
                    noUpdatesRunner.run();
            }
        });
    }

    /**
     * Silently check for platform bundle updates.
     *
     * This method downloads remote bundle metadata and checks if it's newer
     * than local bundle. And shows install dialog if so.
     */
    public void checkUpdateSilent() {
        downloadMetadataSilent(new Consumer<BundleMetadata>() {
            @Override
            public void consume(final BundleMetadata metadata) {
                if (isNewer(metadata))
                    showNewVersionNotification(metadata);
            }
        });
    }

    private boolean isNewer(BundleMetadata remoteMetadata) {
        Bundle b = getLocalBundle();

        // This must not happen because we always call init() on startup.
        if (b == null || b.getMetadata() == null) {
            return true;
        }

        return VersionComparatorUtil.compare(
            remoteMetadata.version, b.getMetadata().version) > 0;
    }
}
