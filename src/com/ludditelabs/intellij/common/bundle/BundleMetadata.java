package com.ludditelabs.intellij.common.bundle;

import com.intellij.util.text.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * Platform bundle metadata.
 *
 * This class provides platform bundle info.
 */
public class BundleMetadata {
    public String dist;
    public String os;
    public String python;
    public String timestamp;
    public String version;
    public String arch;
    public String message;
    public ArrayList<String> changes;

    /**
     * Return true if this metadata version is newer than
     * version of the given metadata.
     *
     * @param other Metadata to compare with.
     * @return boolean
     */
    public boolean isNewerThan(@Nullable BundleMetadata other) {
        return other == null || isNewerThan(other.version);
    }

    /**
     * Return true if this metadata version is newer than given version.
     *
     * @param version Version string to compare with.
     * @return boolean
     */
    public boolean isNewerThan(@NotNull String version) {
        return VersionComparatorUtil.compare(this.version, version) > 0;
    }

    /**
     * Return true if this metadata version is older than
     * version of the given metadata.
     *
     * @param other Metadata to compare with.
     * @return boolean
     */
    public boolean isOlderThan(@Nullable BundleMetadata other) {
        return other != null && isOlderThan(other.version);
    }

    /**
     * Return true if this metadata version is older than given version.
     *
     * @param version Version string to compare with.
     * @return boolean
     */
    public boolean isOlderThan(@NotNull String version) {
        return VersionComparatorUtil.compare(this.version, version) < 0;
    }
}
