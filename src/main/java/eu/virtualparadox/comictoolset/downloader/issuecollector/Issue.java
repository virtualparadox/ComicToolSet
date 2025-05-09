package eu.virtualparadox.comictoolset.downloader.issuecollector;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents a single comic issue with a numeric identifier and a URL.
 * <p>
 * This class is used as a simple data container and supports sorting based on the issue number.
 * </p>
 */
@Data
public class Issue implements Comparable<Issue> {

    /** The issue number (e.g., 1, 2, 3...) */
    private final int number;

    /** The full URL pointing to this issue's page */
    private final String url;

    /**
     * Constructs an {@code Issue} with the given number and URL.
     *
     * @param number the numeric identifier of the issue
     * @param url the full URL to the issue page
     */
    public Issue(int number, String url) {
        this.number = number;
        this.url = url;
    }

    /**
     * Returns a padded name for the issue, e.g., "Issue-001" for issue number 1.
     * Useful for consistent folder or file naming.
     *
     * @return the padded issue name as a string
     */
    public String getPaddedName() {
        return "Issue-" + StringUtils.leftPad(String.valueOf(number), 3, '0');
    }

    /**
     * Compares this issue with another based on their numeric identifiers.
     *
     * @param other the other issue to compare to
     * @return negative if this issue is less than the other, positive if greater, 0 if equal
     */
    @Override
    public int compareTo(Issue other) {
        return Integer.compare(this.number, other.number);
    }
}
