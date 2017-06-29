package com.ludditelabs.intellij.common;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.text.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Utils {
    @NotNull
    public static String getPluginVersion(String id) {
        IdeaPluginDescriptor desc = PluginManager.getPlugin(PluginId.getId(id));
        return desc == null ? "" : desc.getVersion();
    }

    /**
     * Convert time string in the format {@code EEE, dd MMM yyyy HH:mm:ss z}
     * to the UNIX timestamp.
     *
     * @param text Timestamp string.
     * @return the number of milliseconds since January 1, 1970, 00:00:00 GMT.
     */
    public static long timestampToTime(@Nullable final String text) {
        if (text == null)
            return 0;

        final SimpleDateFormat format = new SimpleDateFormat(
            "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

        try {
            return format.parse(text).getTime();
        }
        catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static void sortVersions(@NotNull final List<String> versions) {
        Collections.sort(versions, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return VersionComparatorUtil.compare(o1, o2);
            }
        });
    }

    // TODO: there must be more optimal way to do that.
    /**
     * Find version string closest to the given one.
     *
     * This functions searches for version <= given one.
     *
     * @param versions List of <em>sorted</em> version strings.
     * @param version Reference version.
     * @return String or null.
     */
    @Nullable
    public static String findClosestVersion(
        @NotNull final List<String> versions,
        @NotNull final String version) {

        int i;
        int cmp;
        int sz = versions.size();

        for (i = 0; i < sz; i++) {
            cmp = VersionComparatorUtil.compare(version, versions.get(i));
            if (cmp == 0) {
                return versions.get(i);
            }
            else if (cmp < 0) {
                return i == 0 ? null : versions.get(i - 1);
            }
        }

        if (i > 0)
            return versions.get(sz - 1);
        return null;
    }

    /**
     * Get current platform name.
     */
    public static String getPlatform() {
        if (SystemInfo.isMac)
            return "darwin";
        else if (SystemInfo.isLinux)
            return "linux";
        else if (SystemInfo.isWindows)
            return "win";
        return "unsupported";
    }

    /**
     * Get current platform architecture.
     */
    public static String getArch() {
        // NOTE: we support only x32 binaries for windows (for now).
        if (SystemInfo.is64Bit && !SystemInfo.isWindows)
            return "64bit";
        else
            return "32bit";
    }
}
