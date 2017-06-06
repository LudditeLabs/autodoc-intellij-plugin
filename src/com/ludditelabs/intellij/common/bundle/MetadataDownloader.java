package com.ludditelabs.intellij.common.bundle;

import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.ludditelabs.intellij.common.DownloadUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class MetadataDownloader {
    protected static final Logger logger = Logger.getInstance("ludditelabs.bundle.MetadataDownloader");

    private final RemoteBundle m_bundle;
    @Nullable private final ProgressIndicator m_indicator;

    public MetadataDownloader(@Nullable final ProgressIndicator indicator) {
        m_bundle = BundleManager.getInstance().getRemoteBundle();
        m_indicator = indicator;
    }

    public BundleMetadata download() throws IOException {
        if (m_bundle == null)
            throw new IOException("INTERNAL ERROR: RemoteBundle is not set.");

        if (m_indicator != null)
            m_indicator.setText("Retrieving version info...");

        String url = m_bundle.getMetadataUrl();

        logger.debug("Downloading %s", url);

        try {
            BundleManager.getInstance().setBusy(true);

            String str = DownloadUtils.downloadToString(
                url, m_indicator, "Can't download version info");
            Gson gson = new Gson();
            return gson.fromJson(str, BundleMetadata.class);
        } finally {
            BundleManager.getInstance().setBusy(false);
        }
    }
}
