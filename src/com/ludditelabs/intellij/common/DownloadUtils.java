package com.ludditelabs.intellij.common;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.net.NetUtils;
import com.ludditelabs.intellij.common.bundle.BundleMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Locale;

// Inspired by com.intellij.platform.templates.github.DownloadUtil
public class DownloadUtils {
    private static final Logger logger = Logger.getInstance("ludditelabs.common.DownloadUtils");

    private static String sizeToString(int size) {
        if (size < 0) {
            return "N/A";
        }
        final int kilo = 1024;
        if (size < kilo) {
            return String.format(Locale.US, "%d bytes", size);
        }
        if (size < kilo * kilo) {
            return String.format(Locale.US, "%.1f kB", size / (1.0 * kilo));
        }
        return String.format(Locale.US, "%.1f MB", size / (1.0 * kilo * kilo));
    }

    // size in bytes.
    private static void replaceSize(@Nullable final ProgressIndicator indicator,
                                    @Nullable String text, int size) {
        if (indicator != null && text != null) {
            String placeholder = "${length}";
            int i = text.indexOf(placeholder);
            if (i != -1) {
                String txt = text.substring(0, i) + sizeToString(size)
                    + text.substring(i + placeholder.length());
                indicator.setText(txt);
            }
        }
    }

    public static void download(@NotNull String url,
                                @NotNull final OutputStream output,
                                @Nullable final ProgressIndicator indicator,
                                @Nullable final String errorMessage) throws IOException {

        final String progress_text = indicator != null ? indicator.getText() : null;
        replaceSize(indicator, progress_text, -1);

        if (indicator != null) {
            try {
                String[] parts = URI.create(url).getPath().split("/");
                if (parts.length > 0)
                    indicator.setText2("Downloading " + parts[parts.length - 1]);
            }
            catch (IllegalArgumentException e) {
                // Don't show extra text if something is wrong.
            }
        }

        HttpRequests.request(url).productNameAsUserAgent()
            .connect(new HttpRequests.RequestProcessor<Object>() {
                @Override
                public BundleMetadata process(@NotNull HttpRequests.Request request) throws IOException {
                    try {
                        int sz = request.getConnection().getContentLength();
                        replaceSize(indicator, progress_text, sz);
                        NetUtils.copyStreamContent(indicator, request.getInputStream(), output, sz);
                    }
                    catch (IOException e) {
                        logger.debug(e);
                        HttpURLConnection conn = (HttpURLConnection)request.getConnection();

                        String msg = (errorMessage == null || errorMessage.isEmpty() ? "" : errorMessage + ": ") +
                            conn.getResponseCode() + " " +
                            conn.getResponseMessage();
                        throw new IOException(msg, e);
                    }

                    return null;
                }
            });
    }

    public static String downloadToString(@NotNull String url,
                                          @Nullable final ProgressIndicator indicator,
                                          @Nullable final String errorMessage) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        download(url, out, indicator, errorMessage);
        return out.toString();
    }

    public static void downloadToFile(@NotNull String url,
                                      @NotNull File outFile,
                                      @Nullable final ProgressIndicator indicator,
                                      @Nullable final String errorMessage) throws IOException {
        final FileOutputStream out = new FileOutputStream(outFile);
        download(url, out, indicator, errorMessage);
    }
}
