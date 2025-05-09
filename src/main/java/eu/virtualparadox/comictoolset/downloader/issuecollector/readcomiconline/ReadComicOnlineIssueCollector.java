package eu.virtualparadox.comictoolset.downloader.issuecollector.readcomiconline;

import eu.virtualparadox.comictoolset.downloader.UrlUtils;
import eu.virtualparadox.comictoolset.downloader.issuecollector.Issue;
import eu.virtualparadox.comictoolset.downloader.issuecollector.IssueCollector;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects comic issue links from the ReadComicOnline website.
 * <p>
 * This implementation connects to a comic's root URL, parses the HTML content,
 * extracts issue URLs based on the naming convention (e.g., Issue-001), and wraps them into {@link Issue} objects.
 * </p>
 */
public class ReadComicOnlineIssueCollector implements IssueCollector {

    /** CSS selector template for locating issue links on the page */
    private static final String ISSUE_LINK_SELECTOR = "a[href^=/Comic/%s/Issue-]";

    /** Query parameter expected in valid issue URLs */
    private static final String ID_PARAM = "id=";

    /** Pattern to extract issue number from URLs */
    private static final Pattern ISSUE_PATTERN = Pattern.compile("Issue-(\\d+)");

    private final Logger logger = LoggerFactory.getLogger(ReadComicOnlineIssueCollector.class);
    private final String comicRoot;

    /**
     * Constructs a new issue collector for the given comic root URL.
     *
     * @param comicRoot the full URL to the main comic page on ReadComicOnline
     */
    public ReadComicOnlineIssueCollector(final String comicRoot) {
        this.comicRoot = comicRoot;
    }

    /**
     * Connects to the comic root page, parses the issue links, and returns a list of {@link Issue}s.
     * Only links containing an "id=" parameter and a valid issue number are considered.
     *
     * @return a sorted list of detected issues for the comic
     * @throws IllegalStateException if fetching or parsing the page fails
     */
    public List<Issue> collectIssues() {
        final String baseUrl = UrlUtils.getBaseUrl(comicRoot);
        final String comicName = getComicName(comicRoot);

        try {
            final List<String> issueUrls = new ArrayList<>();
            final Document doc = Jsoup.connect(comicRoot).get();
            final Elements issueLinks = doc.select(String.format(ISSUE_LINK_SELECTOR, comicName));

            for (Element link : issueLinks) {
                final String relativeUrl = link.attr("href");
                final String fullUrl = baseUrl + relativeUrl;

                if (fullUrl.contains(ID_PARAM)) {
                    issueUrls.add(fullUrl);
                }
            }

            final List<Issue> issues = processIssueUrls(issueUrls);
            issues.sort(Issue::compareTo);
            return issues;

        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch issues from " + comicRoot, e);
        }
    }

    /**
     * Converts raw issue URLs into {@link Issue} objects if a valid issue number can be extracted.
     *
     * @param issueUrls list of URLs pointing to individual comic issues
     * @return list of {@link Issue} objects
     */
    private List<Issue> processIssueUrls(final List<String> issueUrls) {
        final List<Issue> issues = new ArrayList<>();
        for (String issueUrl : issueUrls) {
            final Optional<Integer> issueNumber = extractIssueNumber(issueUrl);
            if (issueNumber.isPresent()) {
                issues.add(new Issue(issueNumber.get(), issueUrl));
            } else {
                logger.warn("Failed to extract issue number from URL: {}", issueUrl);
            }
        }
        return issues;
    }

    /**
     * Extracts the numeric issue number from the given URL.
     * <p>
     * Example: from "Issue-023" it extracts 23.
     * </p>
     *
     * @param issueUrl the URL to extract the issue number from
     * @return the extracted issue number, or empty if not matched
     */
    private Optional<Integer> extractIssueNumber(String issueUrl) {
        final Matcher matcher = ISSUE_PATTERN.matcher(issueUrl);
        if (matcher.find()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }
        return Optional.empty();
    }

    /**
     * Extracts the comic name from the path component of the URL.
     * <p>
     * Assumes a structure like: {@code /Comic/{ComicName}/...}
     * </p>
     *
     * @param url the full comic URL
     * @return the comic name, or {@code null} if not found
     */
    public String getComicName(final String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        try {
            final URL parsed = new URL(url);
            final String[] segments = parsed.getPath().split("/");
            for (int i = 0; i < segments.length - 1; i++) {
                if ("Comic".equalsIgnoreCase(segments[i]) && !segments[i + 1].isEmpty()) {
                    return segments[i + 1];
                }
            }
        } catch (MalformedURLException e) {
            return null;
        }

        return null;
    }

}
