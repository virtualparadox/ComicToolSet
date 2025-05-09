package eu.virtualparadox.comictoolset.downloader.pagecollector;

import eu.virtualparadox.comictoolset.downloader.issuecollector.Issue;

import java.io.IOException;
import java.util.List;

/**
 * Interface for collecting individual comic pages from a given {@link Issue}.
 * <p>
 * Implementations are responsible for downloading or extracting page images and returning them as {@link Page} objects.
 * </p>
 */
public interface PageCollector {

    /**
     * Collects all pages (images) of a specific comic issue.
     *
     * @param issue the comic issue to collect pages from
     * @return a list of {@link Page} objects containing image data and metadata
     * @throws IOException if downloading or reading page data fails
     */
    List<Page> collectPages(final Issue issue) throws IOException;
}
