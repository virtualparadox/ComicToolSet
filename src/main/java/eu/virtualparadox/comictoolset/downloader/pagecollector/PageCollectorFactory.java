package eu.virtualparadox.comictoolset.downloader.pagecollector;

import eu.virtualparadox.comictoolset.downloader.pagecollector.readcomiconline.ReadComicOnlinePageCollector;

/**
 * Factory for creating {@link PageCollector} implementations based on the comic root URL.
 * <p>
 * This class inspects the root URL of the comic source and returns the appropriate
 * {@link PageCollector} implementation (e.g., for ReadComicOnline).
 * </p>
 */
public class PageCollectorFactory {

    /** Substring used to identify ReadComicOnline URLs */
    public static final String READ_COMIC_ONLINE_IDENTIFIER = "readcomiconline";

    private PageCollectorFactory() {
        // Prevent instantiation
    }

    /**
     * Creates a {@link PageCollector} instance for the given comic root URL.
     *
     * @param comicRoot the root URL of the comic series (e.g., homepage of a title)
     * @return a suitable {@link PageCollector} implementation
     * @throws IllegalArgumentException if the comic source is unsupported
     */
    public static PageCollector createPageCollector(final String comicRoot) {
        if (comicRoot.contains(READ_COMIC_ONLINE_IDENTIFIER)) {
            return new ReadComicOnlinePageCollector();
        }
        throw new IllegalArgumentException("Unsupported comic root: " + comicRoot);
    }
}
