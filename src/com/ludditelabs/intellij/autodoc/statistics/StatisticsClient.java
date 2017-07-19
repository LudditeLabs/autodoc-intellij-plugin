package com.ludditelabs.intellij.autodoc.statistics;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.intellij.util.net.HttpConfigurable;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Class to communicate with remote autodoc statistics service.
 *
 * There is a possibility to specify custom URL to send statistics.
 * Add <pre>-Dludditelabs.autodoc.statistics.url=[URL]</pre> to IDE params.
 */
public class StatisticsClient {
    /**
     * Autodoc service JSON error.
     */
    class JsonError extends IOException {
        private int status = -1;
        @Nullable
        private String statusText = null;
        @Nullable private String code = null;
        @Nullable private String description = null;

        public JsonError() {

        }

        public String getMessage() {
            StringBuilder b = new StringBuilder();

            b.append(status);
            b.append(' ');
            b.append(statusText);
            b.append(' ');
            if (code != null)
                b.append(String.format("[%s] ", code));

            if (description != null)
                b.append(description);
            else
                b.append("<No description>");

            return b.toString();
        }
    }

    // Default statistics endpoint.
    // Can be overridden with the "ludditelabs.autodoc.statistics.url" system property.
    private static final String STAT_URL = "http://autodoc.ai/statistics/intellij_plugin";

    @NotNull private final HttpClient m_client;

    /**
     * Construct autodoc service client.
     */
    public StatisticsClient() {
        m_client = buildClient();
    }

    /**
     * Helper method to create response error.
     */
    private static IOException responseError(EntityEnclosingMethod method) {
        String type = method.getResponseHeader("content-type").getValue();
        if (!type.startsWith("application/json")) {
            return new IOException(String.format("%d %s",
                method.getStatusCode(), method.getStatusText()));
        }

        String response = null;
        try {
            response = method.getResponseBodyAsString();
            Gson gson = new Gson();
            JsonError err = gson.fromJson(response, JsonError.class);
            err.statusText = method.getStatusText();
            if (err.status == -1)
                err.status = method.getStatusCode();
            return err;
        }
        catch (JsonSyntaxException e) {
            String msg = e.getMessage();
            if (response != null)
                msg += '\n' + response;
            return new IOException(msg);
        }
        catch (IOException e) {
            return e;
        }
    }

    @Nullable
    private static Credentials getCredentials(String login, String password, String host) {
        int domainIndex = login.indexOf("\\");
        if (domainIndex > 0) {
            // if the username is in the form "user\domain"
            // then use NTCredentials instead of UsernamePasswordCredentials
            String domain = login.substring(0, domainIndex);
            if (login.length() > domainIndex + 1) {
                String user = login.substring(domainIndex + 1);
                return new NTCredentials(user, password, host, domain);
            }
            else {
                return null;
            }
        }
        else {
            return new UsernamePasswordCredentials(login, password);
        }
    }

    /**
     * Helper method to build HTTP client with IDE proxy settings.
     */
    private HttpClient buildClient() {
        HttpClient client = new HttpClient();
        HttpConfigurable proxy = HttpConfigurable.getInstance();
        if (proxy.PROXY_HOST != null) {
            client.getHostConfiguration().setProxy(proxy.PROXY_HOST, proxy.PROXY_PORT);
            if (proxy.PROXY_AUTHENTICATION) {
                AuthScope authScope = new AuthScope(proxy.PROXY_HOST, proxy.PROXY_PORT);
                Credentials credentials = getCredentials(
                    // Old API (141):
                    proxy.PROXY_LOGIN,
                    // New API: proxy.getProxyLogin(),
                    proxy.getPlainProxyPassword(), proxy.PROXY_HOST);
                client.getState().setProxyCredentials(authScope, credentials);
            }
        }

        return client;
    }

    /**
     * Send given statistics to the autodoc service.
     * @param json JSON string with statistics data.
     * @return true if data is successfully sent.
     * @throws IOException on service or communication errors.
     */
    public boolean sendStatistics(String json) throws IOException {
        String url = System.getProperty("ludditelabs.autodoc.statistics.url");
        if (url == null)
            url = STAT_URL;

        PostMethod method = new PostMethod(url);
        method.setRequestEntity(new StringRequestEntity(json, "application/json", "utf-8"));
        m_client.executeMethod(method);

        int status = method.getStatusCode();
        if (status != 200)
            throw responseError(method);
        return true;
    }
}
