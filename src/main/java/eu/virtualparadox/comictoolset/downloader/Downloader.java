package eu.virtualparadox.comictoolset.downloader;

import eu.virtualparadox.comictoolset.downloader.issuecollector.Issue;
import eu.virtualparadox.comictoolset.downloader.issuecollector.IssueCollector;
import eu.virtualparadox.comictoolset.downloader.issuecollector.IssueCollectorFactory;
import eu.virtualparadox.comictoolset.downloader.pagecollector.Page;
import eu.virtualparadox.comictoolset.downloader.pagecollector.PageCollector;
import eu.virtualparadox.comictoolset.downloader.pagecollector.PageCollectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Downloads comic issues and their page images from a remote source to a local folder.
 * <p>
 * This class orchestrates the full process of issue discovery and image download,
 * using pluggable {@link IssueCollector} and {@link PageCollector} implementations
 * based on the comic source.
 * </p>
 */
public class Downloader {

    private static final Logger logger = LoggerFactory.getLogger(Downloader.class);

    private final Path comicFolder;
    private final IssueCollector issueCollector;
    private final PageCollector pageCollector;

    /**
     * Constructs a {@code Downloader} for the given comic source and output directory.
     *
     * @param comicRoot   the root URL of the comic (e.g., title page on ReadComicOnline)
     * @param comicFolder the local folder where downloaded issues will be saved
     */
    public Downloader(final String comicRoot, final Path comicFolder) {
        this.comicFolder = comicFolder;
        this.issueCollector = IssueCollectorFactory.createIssueCollector(comicRoot);
        this.pageCollector = PageCollectorFactory.createPageCollector(comicRoot);
    }

    /**
     * Starts the download process for all available issues from the source.
     * <p>
     * Issues already downloaded (i.e., folders exist) are skipped.
     * </p>
     */
    public void download() {
        final List<Issue> issues = issueCollector.collectIssues();

        for (Issue issue : issues) {
            final Path issueFolder = comicFolder.resolve(issue.getPaddedName());
            downloadContent(issueFolder, issue);
        }
    }

    /**
     * Downloads and saves all pages for a given issue, unless the target folder already exists.
     *
     * @param issueFolder the destination folder for the issue's pages
     * @param issue       the issue to download
     */
    private void downloadContent(final Path issueFolder, final Issue issue) {
        if (Files.exists(issueFolder)) {
            logger.info("Skipping '{}': already downloaded", issue.getPaddedName());
            return;
        }

        try {
            final List<Page> pages = pageCollector.collectPages(issue);
            writePages(issueFolder, pages);
        } catch (IOException e) {
            logger.error("Failed to download issue '{}': {}", issue.getPaddedName(), e.getMessage());

            try {
                Files.deleteIfExists(issueFolder);
            } catch (IOException cleanupException) {
                logger.error("Cleanup failed for '{}': {}", issueFolder, cleanupException.getMessage());
            }
        }
    }

    /**
     * Writes page image data to the specified issue folder.
     * <p>
     * Skips individual pages that already exist to support partial retries.
     * </p>
     *
     * @param issueFolder the target folder to write page files into
     * @param pages       the list of pages to write
     */
    private void writePages(final Path issueFolder, final List<Page> pages) {
        try {
            if (!Files.exists(issueFolder)) {
                Files.createDirectories(issueFolder);
            }

            for (Page page : pages) {
                final Path pagePath = issueFolder.resolve(page.getPaddedName());

                if (!Files.exists(pagePath)) {
                    Files.write(pagePath, page.getData());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write pages to: " + issueFolder, e);
        }
    }
}
