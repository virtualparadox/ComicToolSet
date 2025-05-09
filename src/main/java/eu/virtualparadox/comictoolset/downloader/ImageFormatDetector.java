package eu.virtualparadox.comictoolset.downloader;

/**
 * Utility class for detecting common image formats based on file signature (magic bytes).
 * <p>
 * Supports detection of JPEG, PNG, GIF (87a and 89a), WEBP, and BMP formats.
 * This class is used to infer file extensions when saving raw image bytes.
 * </p>
 */
public final class ImageFormatDetector {

    /** JPEG file signature: FF D8 FF */
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    /** PNG file signature: 89 50 4E 47 0D 0A 1A 0A */
    private static final byte[] PNG_MAGIC  = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    /** GIF87a file signature */
    private static final byte[] GIF87_MAGIC = {'G', 'I', 'F', '8', '7', 'a'};

    /** GIF89a file signature */
    private static final byte[] GIF89_MAGIC = {'G', 'I', 'F', '8', '9', 'a'};

    /** WEBP file container header (first 4 bytes): RIFF */
    private static final byte[] WEBP_HEADER = {'R', 'I', 'F', 'F'};

    /** WEBP format identifier starting at byte 8: WEBP */
    private static final byte[] WEBP_MAGIC   = {'W', 'E', 'B', 'P'};

    /** BMP file signature: 42 4D */
    private static final byte[] BMP_MAGIC   = {0x42, 0x4D};

    /**
     * Detects the image format from the first few bytes of the image file (magic header).
     *
     * @param header the first 12+ bytes of an image file
     * @return a lowercase format string (e.g., "jpg", "png", "gif", "webp", "bmp"),
     *         or "unknown" if no known signature matches
     */
    public String detectFormatFromHeader(byte[] header) {
        if (header == null || header.length < 12) return "unknown";

        if (matchesAt(header, 0, JPEG_MAGIC)) {
            return "jpg";
        }

        if (matchesAt(header, 0, PNG_MAGIC)) {
            return "png";
        }

        if (matchesAt(header, 0, GIF87_MAGIC) || matchesAt(header, 0, GIF89_MAGIC)) {
            return "gif";
        }

        if (matchesAt(header, 0, WEBP_HEADER) && matchesAt(header, 8, WEBP_MAGIC)) {
            return "webp";
        }

        if (matchesAt(header, 0, BMP_MAGIC)) {
            return "bmp";
        }

        return "unknown";
    }

    /**
     * Compares a pattern of bytes to a subsection of the input array starting at a given offset.
     *
     * @param data the full byte array (e.g., image header)
     * @param offset the position in {@code data} where comparison should begin
     * @param pattern the known signature bytes to match
     * @return {@code true} if the pattern matches exactly at the given offset, {@code false} otherwise
     */
    private boolean matchesAt(byte[] data, int offset, byte[] pattern) {
        if (data.length < offset + pattern.length) return false;
        for (int i = 0; i < pattern.length; i++) {
            if (data[offset + i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }
}
