package com.ludditelabs.intellij.common.bundle;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Ref;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.HttpRequests;
import com.ludditelabs.intellij.common.DownloadUtils;
import com.ludditelabs.intellij.common.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MetadataDownloader {
    protected static final Logger logger = Logger.getInstance("ludditelabs.bundle.MetadataDownloader");

    @NotNull private final Updater m_updater;
    @Nullable private final ProgressIndicator m_indicator;

    public MetadataDownloader(@NotNull Updater updater,
                              @Nullable final ProgressIndicator indicator) {
        m_updater = updater;
        m_indicator = indicator;
    }

    /**
     * Return true if metadata last modified date is newer than local one.
     * @return boolean
     * @throws IOException on network I/O errors.
     */
    public boolean needDownloadRemoteMetadata() throws IOException {
        RemoteBundle bundle = m_updater.getRemoteBundle();
        if (bundle == null)
            throw new IOException("INTERNAL ERROR: RemoteBundle is not set.");

        if (m_indicator != null)
            m_indicator.setText("Checking platform bundle timestamp ...");

        BundleMetadata local_meta = m_updater.getLocalBundle().getMetadata();
        if (local_meta == null || local_meta.lastModified == 0)
            return true;

        // Sent HTTP HEAD request to get 'Last-Modified' header.
        URL url = new URL(bundle.getMetadataUrl());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("HEAD");
        String last = con.getHeaderField("Last-Modified");
        long remote_date = Utils.timestampToTime(last);
        return remote_date == 0 || remote_date > local_meta.lastModified;
    }

    public BundleMetadata download() throws IOException {
        RemoteBundle bundle = m_updater.getRemoteBundle();
        if (bundle == null)
            throw new IOException("INTERNAL ERROR: RemoteBundle is not set.");

        if (m_indicator != null)
            m_indicator.setText("Retrieving version info...");

        String url = bundle.getMetadataUrl();

        logger.debug("Downloading ", url);

        try {
            m_updater.setBusy(true);

            final Ref<String> last_modified = new Ref<>();
            HttpRequests.RequestProcessor<Void> processor = new HttpRequests.RequestProcessor<Void>() {
                @Override
                public Void process(@NotNull HttpRequests.Request request) throws IOException {
                    last_modified.set(request.getConnection().getHeaderField(
                        "last-modified"));
                    return null;
                }
            };

            String str = DownloadUtils.downloadToString(
                url, m_indicator, "Can't download version info", processor);

            BundleMetadata meta =  getMeta(str, last_modified.get());
            if (!meta.isValid())
                throw new IOException("INTERNAL ERROR: Invalid bundle metadata.");
            return meta;
        } finally {
            m_updater.setBusy(false);
        }
    }

    /**
     * Build version string list from the JSON object.
     *
     * @param data JSON object with versions as fields.
     * @return List of ordered versions.
     */
    private static List<String> extractVersions(JsonObject data) {
        List<String> versions = ContainerUtil.newArrayList();
        Set<Map.Entry<String, JsonElement>> entries = data.entrySet();
        for (Map.Entry<String, JsonElement> entry: entries) {
            versions.add(entry.getKey());
        }
        Utils.sortVersions(versions);
        return versions;
    }

    /**
     * Create bundle metadata from the JSON object.
     *
     * @param json JSON object with remote bundle metadata.
     * @param lastModified Remote metadata last modified timestamp string.
     * @return BundleMetadata.
     */
    @NotNull
    private BundleMetadata createMetadata(JsonObject json, String pluginVersion,
                                          String lastModified) {
        BundleMetadata meta = new BundleMetadata();
        meta.lastModified = Utils.timestampToTime(lastModified);
        meta.dist = json.get("dist").getAsString();
        meta.message = json.get("message").getAsString();
        meta.version = json.get("version").getAsString();
        meta.pluginVersion = pluginVersion;
        meta.timestamp = json.get("timestamp").getAsString();

        JsonArray changes = json.getAsJsonArray("changes");
        if (changes != null && changes.size() > 0) {
            meta.changes = ContainerUtil.newArrayList();
            int sz = changes.size();
            for (int i = 0; i < sz; ++i) {
                String val = changes.get(i).getAsString();
                if (val != null)
                    meta.changes.add(val);
            }
        }
        return meta;
    }

    /**
     * Remote metadata structure is:
     * <pre>
     * {@code
     *    {
     *      "versions": {
     *         "x.y.z": {...}
     *      }
     *    }
     * }
     * </pre>
     * For more info refer to s3bundle project.
     *
     * @param metaContent Remote metadata JSON string.
     * @param lastModified Remote metadata last modified timestamp string.
     * @return BundleMetadata.
     * @throws IOException
     */
    @NotNull
    private BundleMetadata getMeta(String metaContent,
                                   String lastModified) throws IOException {
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(metaContent, JsonObject.class);
        json = json.getAsJsonObject("versions");

        if (json == null)
            throw new IOException("Remote metadata is malformed.");

        List<String> versions = extractVersions(json);
        String match = Utils.findClosestVersion(versions, m_updater.getPluginVersion());
        if (match == null)
            throw new IOException("Can't find suitable bundle.");

        json = json.getAsJsonObject(match);
        if (json == null)
            throw new IOException("INTERNAL ERROR: can't get meta by version.");

        return createMetadata(json, match, lastModified);
    }
}
