package eu.virtualparadox.comictoolset.downloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A utility class responsible for downloading raw image bytes from a given URL.
 * <p>
 * Uses a simple HTTP GET request with a user-agent header to fetch image content over the web.
 * Designed for use in comic page image downloading workflows.
 * </p>
 */
public final class ImageDownloader {

    /** User-Agent string to mimic a real browser (avoids 403 errors from some sites) */
    private static final String USER_AGENT = "Mozilla/5.0";

    /**
     * Downloads an image from the specified URL and returns its raw byte content.
     *
     * @param imageUrl the direct URL to the image file
     * @return a byte array containing the image data
     * @throws IOException if the download fails or the server returns a non-200 response
     */
    public byte[] readImageBytes(final String imageUrl) throws IOException {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Failed to fetch image: HTTP " + responseCode);
            }

            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

                byte[] temp = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(temp)) != -1) {
                    buffer.write(temp, 0, bytesRead);
                }

                return buffer.toByteArray();
            }

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
