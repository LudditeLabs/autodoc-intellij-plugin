package com.ludditelabs.intellij.common.bundle;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.ui.AppUIUtil;
import com.intellij.util.Consumer;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * This class implements platform bundle download workflow.
 *
 * Features:
 * <ul>
 *     <li>Send notifications (state and errors).</li>
 *     <li>Download metadata.</li>
 *     <li>Download and unpack platform package.</li>
 * </ul>
 */
public class Updater {
    public static Topic<Notifier> TOPIC = Topic.create(
        "bundle updater state", Notifier.class);

    public interface Notifier {
        /** Gets called if used canceled background task. */
        void canceled();

        /** Gets called on various IO errors. */
        void ioError(IOException e);

        /** Gets called on busy state changes. */
        void stateChanged(boolean busy);

        /** Gets called each time new remote metadata is downloaded. */
        void metadataDownloaded(@NotNull BundleMetadata metadata);

        /** Gets called after remote package in unpacked.*/
        void unpacked();
    }

    protected static final Logger logger = Logger.getInstance("ludditelabs.bundle.Updater");
    private RemoteBundle m_remoteBundle = null;
    private LocalBundle m_localBundle = null;
    private boolean m_busy = false;

    public Updater() {
    }

    private void doSetBusy(boolean state) {
        if (m_busy != state) {
            m_busy = state;
            Notifier pub = ApplicationManager.getApplication()
                .getMessageBus().syncPublisher(TOPIC);
            pub.stateChanged(m_busy);
        }
    }

    private void notifyError(final IOException e) {
        AppUIUtil.invokeOnEdt(new Runnable() {
            @Override
            public void run() {
                final Notifier pub = ApplicationManager.getApplication()
                    .getMessageBus().syncPublisher(TOPIC);
                pub.ioError(e);
            }
        });
    }

    private void notifyCancel() {
        AppUIUtil.invokeOnEdt(new Runnable() {
            @Override
            public void run() {
                final Notifier pub = ApplicationManager.getApplication()
                    .getMessageBus().syncPublisher(TOPIC);
                pub.canceled();
            }
        });
    }

    private void notifyOnMetadata(@NotNull final BundleMetadata metadata,
                                  @Nullable final Consumer<BundleMetadata> consumer) {
        AppUIUtil.invokeOnEdt(new Runnable() {
            @Override
            public void run() {
                m_remoteBundle.setMetadata(metadata);
                final Notifier pub = ApplicationManager.getApplication()
                    .getMessageBus().syncPublisher(TOPIC);
                pub.metadataDownloaded(metadata);
                if (consumer != null)
                    consumer.consume(metadata);
            }
        });
    }

    private void doAfterUnpack(@Nullable final Runnable runnable) {
        AppUIUtil.invokeOnEdt(new Runnable() {
            @Override
            public void run() {
                m_localBundle.reloadMetadata();
                final Notifier pub = ApplicationManager.getApplication()
                    .getMessageBus().syncPublisher(TOPIC);
                pub.unpacked();
                if (runnable != null)
                    runnable.run();
            }
        });
    }

    /**
     * Initialize with remote and local bundles.
     *
     * @param remoteBundle Remote bundle.
     * @param localBundle Local bundle.
     */
    public void init(@NotNull RemoteBundle remoteBundle,
                     @NotNull LocalBundle localBundle) {
        m_remoteBundle = remoteBundle;
        m_localBundle = localBundle;
    }

    /**
     * Set busy state.
     *
     * @param state Busy state.
     */
    public void setBusy(final boolean state) {
        Application application = ApplicationManager.getApplication();

        if (application.isDispatchThread())
            doSetBusy(state);
        else {
            application.invokeLater(new Runnable() {
                @Override
                public void run() {
                    doSetBusy(state);
                }
            });
        }
    }

    /**
     * Subscribe on Updater notifications.
     *
     * Note: handler will be called in the EDT thread.
     *
     * @param handler Notifications handler.
     */
    public void subscribe(Notifier handler) {
        ApplicationManager.getApplication().getMessageBus()
            .connect().subscribe(TOPIC, handler);
    }

    /** Remote bundle. */
    public RemoteBundle getRemoteBundle() {
        return m_remoteBundle;
    }

    /** Local bundle. */
    public LocalBundle getLocalBundle() {
        return m_localBundle;
    }

    /**
     * Return true if updater is busy with doing something
     * (like metadata downloading or package unpacking).
     */
    public boolean isBusy() {
        return m_busy;
    }

    // Download API

    /**
     * Silently download remote metadata in a background without any error
     * notifications.
     *
     * This method publishes 'metadataDownloaded' notification.
     *
     * @param consumer Metadata consumer. It will be called in the EDT thread.
     */
    public void downloadMetadataSilent(@Nullable final Consumer<BundleMetadata> consumer) {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                try {
                    MetadataDownloader dl = new MetadataDownloader(
                        Updater.this, null);
                    BundleMetadata meta = dl.download();
                    notifyOnMetadata(meta, consumer);
                }
                catch (IOException e) {
                    logger.error(e);
                }
            }
        });
    }

    /**
     * Download metadata in the background task with progress indicator.
     *
     * This method publishes 'metadataDownloaded' notification.
     *
     * @param consumer Metadata consumer. It will be called in the EDT thread.
     */
    public void downloadMetadata(@NotNull final Consumer<BundleMetadata> consumer) {
        String title = m_remoteBundle.getDisplayName() + " Platform Bundle Info";
        new Task.Modal(null, title, true) {
            @Override
            public void onCancel() {
                notifyCancel();
            }

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    MetadataDownloader dl = new MetadataDownloader(
                        Updater.this, indicator);
                    BundleMetadata meta = dl.download();
                    notifyOnMetadata(meta, consumer);
                }
                catch (IOException e) {
                    notifyError(e);
                }
            }
        }.queue();
    }

    /**
     * Download remote package in a background thread with progress indicator.
     *
     * If metadata is not provided then it will be loaded.
     *
     * This method publishes 'metadataDownloaded' and 'unpacked' notifications.
     *
     * @param metadata Remote metadata.
     * @param runnable Runnable to call after unpacking the package. it will be
     *                 called in the EDT thread.
     */
    public void download(@Nullable final BundleMetadata metadata,
                         @Nullable final Runnable runnable) {
        String title = m_remoteBundle.getDisplayName() + " Platform Bundle";
        new Task.Modal(null, title, true) {
            @Override
            public void onCancel() {
                notifyCancel();
            }

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    BundleMetadata meta = metadata;
                    if (meta == null) {
                        MetadataDownloader dl = new MetadataDownloader(
                            Updater.this, indicator);
                        meta = dl.download();
                        notifyOnMetadata(meta, null);
                    }

                    PackageDownloader dl = new PackageDownloader(
                        Updater.this, meta, indicator);
                    dl.downloadAndUnpack(m_localBundle.getBundlePath());
                }
                catch (IOException e) {
                    notifyError(e);
                    return;
                }
                doAfterUnpack(runnable);
            }
        }.queue();
    }

    /**
     * Download remote bundle (both metadata and package) and unpack it.
     * @param runnable Runnable to call after unpacking. it will be called
     *                 in the EDT thread.
     */
    public void download(@NotNull final Runnable runnable) {
        download(null, runnable);
    }

    /**
     * Download remote bundle (both metadata and package) and unpack it.
     */
    public void download() {
        download(null, null);
    }
}
