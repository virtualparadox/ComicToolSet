package eu.virtualparadox.comictoolset.downloader;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Utility methods for extracting components from comic source URLs.
 */
public class UrlUtils {

    private UrlUtils(){
        // Prevent instantiation
    }

    /**
     * Extracts the base URL (scheme + host) from a full URL.
     * <p>
     * Example: {@code https://readcomiconline.li/Comic/Dylan-Dog} → {@code https://readcomiconline.li}
     * </p>
     *
     * @param url the full URL
     * @return the base URL, or {@code null} if invalid
     */
    public static String getBaseUrl(final String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        try {
            final URL parsed = new URL(url);
            return parsed.getProtocol() + "://" + parsed.getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
