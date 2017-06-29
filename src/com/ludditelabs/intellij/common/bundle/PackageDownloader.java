package com.ludditelabs.intellij.common.bundle;

import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.ludditelabs.intellij.common.DownloadUtils;
import com.ludditelabs.intellij.common.Utils;
import com.ludditelabs.intellij.common.ZipUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

public class PackageDownloader {
    protected static final Logger logger = Logger.getInstance("ludditelabs.bundle.PackageDownloader");

    @NotNull private final Updater m_updater;
    @NotNull private final BundleMetadata m_metadata;
    @Nullable private final ProgressIndicator m_indicator;

    public PackageDownloader(@NotNull Updater updater,
                             @NotNull BundleMetadata metadata,
                             @Nullable ProgressIndicator indicator) {
        m_updater = updater;
        m_metadata = metadata;
        m_indicator = indicator;
    }

    // TODO: use FileUtil.createTempFile() instead of File.createTempFile
    private File getTempFilename() throws IOException {
        String[] parts = m_metadata.dist.split(".");
        String ext = parts.length > 1 ? parts[1] : "";
        return File.createTempFile("bundle", ext);
    }

    private String doDownload() throws IOException {
        String url;

        // Seems this is an URL so use as is.
        if (m_metadata.dist.contains(":/")) {
            url = m_metadata.dist;
        }
        // otherwise construct URL:
        // <base>/<platform>/<arch>/<plugin version>/<dist filename>
        // For more info see s3bundle project docs.
        // NOTE: We don't put '/' between <base> and <platform> because
        // <base> already has it.
        else {
            url = String.format("%s%s/%s/%s/%s",
                m_updater.getRemoteBundle().getBaseUrl(),
                Utils.getPlatform(),
                Utils.getArch(),
                m_metadata.pluginVersion,
                m_metadata.dist);
        }

        if (m_indicator != null)
            m_indicator.setText("Downloading platform bundle (${length})");

        final File file = getTempFilename();
        final String filename = file.getAbsolutePath();

        logger.debug("Downloading %s - > %s", url, filename);

        DownloadUtils.downloadToFile(
            url, file, m_indicator, "Can't download file");

        return filename;
    }

    private void doUnpack(String fileName, String outPath) throws IOException {
        File zip_file = new File(fileName);
        File out_dir = new File(outPath);

        logger.debug("Unpacking %s -> %s", fileName, outPath);

        ZipUtils.unzipAtomic(zip_file, out_dir, m_indicator);

        File meta_file = Paths.get(outPath, "metadata.json").toFile();
        logger.debug("Saving %s", meta_file.getAbsolutePath());

        try (FileWriter writer = new FileWriter(meta_file)) {
            Gson gson = new Gson();
            gson.toJson(m_metadata, writer);
        }
    }

    public String download() throws IOException {
        try {
            m_updater.setBusy(true);
            return doDownload();
        }
        finally {
            m_updater.setBusy(false);
        }
    }

    public void unpack(String fileName, String outPath) throws IOException {
        try {
            m_updater.setBusy(true);
            doUnpack(fileName, outPath);
        }
        finally {
            m_updater.setBusy(false);
        }
    }

    public void downloadAndUnpack(String outPath) throws IOException {
        try {
            m_updater.setBusy(true);
            doUnpack(doDownload(), outPath);
        } finally {
            m_updater.setBusy(false);
        }
    }
}
