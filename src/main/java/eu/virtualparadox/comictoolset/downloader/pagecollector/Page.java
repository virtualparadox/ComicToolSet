package eu.virtualparadox.comictoolset.downloader.pagecollector;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents a single page of a comic issue.
 * <p>
 * Each page includes its index number, source URL, image data, and format (e.g., "jpg", "png").
 * This class also provides a padded filename for consistent output naming.
 * </p>
 */
@Data
public class Page {

    /** The zero-based page number within the issue */
    private final int number;

    /** The source URL from which the image was downloaded */
    private final String url;

    /** The raw image data as a byte array */
    private final byte[] data;

    /** The detected image format (e.g., "jpg", "png", "webp") */
    private final String format;

    /**
     * Constructs a new Page object with the given metadata and image content.
     *
     * @param number the page number
     * @param url the original image URL
     * @param data the raw image bytes
     * @param format the image format string
     */
    public Page(int number, String url, byte[] data, String format) {
        this.number = number;
        this.url = url;
        this.data = data;
        this.format = format;
    }

    /**
     * Returns a consistently padded filename based on the page number and format.
     * <p>
     * For example, page 5 in PNG format will yield "0005.png".
     * </p>
     *
     * @return the padded filename
     */
    public String getPaddedName() {
        final String paddedPage = StringUtils.leftPad(String.valueOf(number), 4, '0');
        return paddedPage + "." + format;
    }
}
