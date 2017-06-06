package com.ludditelabs.intellij.common.bundle;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;


/**
 * This class represents remote platform bundle located in the
 * Amazon S3 bucket.
 */
public class S3Bundle extends RemoteBundle {
    private static final String S3_URL = "https://s3.amazonaws.com/";

    /**
     * Construct S3 remote bundle.
     *
     * It constructs bundle URL:
     * {@code https://s3.amazonaws.com/<bucket>[/<folder>]/<name>}
     *
     * @param bucket Name of the S3 bucket.
     * @param folder Folder in the S3 bucket.
     * @param name Bundle name.
     */
    public S3Bundle(String bucket,
                    String folder,
                    @NotNull String name,
                    @NotNull String displayName) {
        super(buildUrl(bucket, folder), name, displayName);
    }

    private static String buildUrl(String bucket, String folder) {
        String url = System.getProperty(
            "ludditelabs.bundle.s3url", S3_URL).trim();

        // Force default URL for empty values.
        if (url.isEmpty())
            url = S3_URL;

        StringBuilder builder = new StringBuilder();
        builder.append(clean(url) + "/");

        append(builder, System.getProperty(
            "ludditelabs.bundle.bucket", bucket));

        append(builder, System.getProperty(
            "ludditelabs.bundle.folder", folder));

        return builder.toString();
    }

    private static String clean(String part) {
        String str = StringUtils.stripStart(part.trim(), "/");
        return StringUtils.stripEnd(str, "/");
    }

    private static void append(StringBuilder builder, String part) {
        part = clean(part);
        if (!part.isEmpty()) {
            builder.append(part);
            builder.append('/');
        }
    }
}
