package com.ludditelabs.intellij.common.bundle;

import com.intellij.openapi.diagnostic.Logger;
import com.ludditelabs.intellij.common.Utils;
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
 *         {@code <baseUrl>/<os>/<arch>/meta.json}
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
     * @param displayName Bundle display name.
     */
    public RemoteBundle(@NotNull String baseUrl, @NotNull String displayName) {
        super(displayName);

        m_baseUrl = baseUrl;
        String filename = getMetadataName();

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
    // See also s3bundle repo: https://bitbucket.org/ludditelabs/s3bundle.
    private static String getMetadataName() {
        return String.format("%s/%s/meta.json",
            Utils.getPlatform(), Utils.getArch());
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
