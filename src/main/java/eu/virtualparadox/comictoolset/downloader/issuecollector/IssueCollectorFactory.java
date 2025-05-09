package eu.virtualparadox.comictoolset.downloader.issuecollector;

import eu.virtualparadox.comictoolset.downloader.issuecollector.readcomiconline.ReadComicOnlineIssueCollector;

/**
 * Factory for creating {@link IssueCollector} instances based on the comic source URL.
 * <p>
 * This class inspects the provided root URL and returns an appropriate collector implementation
 * (e.g., {@link ReadComicOnlineIssueCollector}) for parsing issues from that source.
 * </p>
 */
public class IssueCollectorFactory {

    /** Identifier substring used to detect ReadComicOnline URLs */
    public static final String READ_COMIC_ONLINE_IDENTIFIER = "readcomiconline";

    /** Private constructor to prevent instantiation */
    private IssueCollectorFactory() {
        // Prevent instantiation
    }

    /**
     * Creates an {@link IssueCollector} for the given comic root URL.
     *
     * @param comicRoot the full root URL of the comic series
     * @return an appropriate {@link IssueCollector} for the source
     * @throws IllegalArgumentException if the source is unsupported
     */
    public static IssueCollector createIssueCollector(final String comicRoot) {
        if (comicRoot.contains(READ_COMIC_ONLINE_IDENTIFIER)) {
            return new ReadComicOnlineIssueCollector(comicRoot);
        }
        throw new IllegalArgumentException("Unsupported comic root: " + comicRoot);
    }
}
