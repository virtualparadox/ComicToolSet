package eu.virtualparadox.comictoolset.downloader.issuecollector;

import java.util.List;

/**
 * Interface for collecting comic issues from a given source.
 * <p>
 * Implementations of this interface are responsible for fetching and parsing
 * issue metadata (such as number and URL) from a comic website or API.
 * </p>
 */
public interface IssueCollector {

    /**
     * Collects all available issues for a comic.
     *
     * @return a list of {@link Issue} objects, typically sorted by issue number
     */
    List<Issue> collectIssues();
}
