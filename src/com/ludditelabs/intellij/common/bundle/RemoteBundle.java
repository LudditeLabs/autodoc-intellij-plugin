package com.ludditelabs.intellij.common.bundle;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;


/**
 * This class represents remote bundle.
 *
 * Remote bundle is a set of two files located on a remote server:
 * <ul>
 *     <li>
 *         JSON file with bundle metadata
 *         {@code <baseUrl>/<name>-<os>-<arch>.json}
 *     </li>
 *     <li>
 *         Archive with platform dependent files (executables, libs, etc).
 *         It's URL is provided by the metadata.
 *     </li>
 * </ul>
 */
public class RemoteBundle extends Bundle {
    protected static final Logger logger = Logger.getInstance("ludditelabs.bundle");

    private String m_baseUrl;
    private String m_metadataUrl = null;

    /**
     * Construct bundle.
     *
     * @param baseUrl Base url for the bundle. It used to build metadata and
     *                archive urls.
     * @param name Bundle name.
     */
    public RemoteBundle(@NotNull String baseUrl, @NotNull String name,
                        @NotNull String displayName) {
        super(displayName);

        m_baseUrl = baseUrl;
        String filename = getMetadataName(name);

        try {
            URI uri = new URI(m_baseUrl);
            m_metadataUrl = uri.resolve(filename).toString();
        }
        catch (URISyntaxException e) {
            logger.error(e);
        }
    }

    // Helper method to construct metadata file name
    // based on current runtime environment.
    private static String getMetadataName(String name) {
        String os;
        String arch;

        if (SystemInfo.isMac)
            os = "darwin";
        else if (SystemInfo.isLinux)
            os = "linux";
        else if (SystemInfo.isWindows)
            os = "win";
        else
            os = "unsupported";

        // NOTE: we support only x32 binaries for windows (for now).
        if (SystemInfo.is64Bit && !SystemInfo.isWindows)
            arch = "64bit";
        else
            arch = "32bit";

        return String.format("%s-%s-%s.meta.json", name, os, arch);
    }

    /** Base URL for the bundle .*/
    public String getBaseUrl() {
        return m_baseUrl;
    }

    /** Metadata URL .*/
    public String getMetadataUrl() {
        return m_metadataUrl;
    }
}
