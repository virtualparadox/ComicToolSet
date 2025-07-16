package eu.virtualparadox.comictoolset.translator.textboxgenerator.processor;

/**
 * A utility class responsible for cleaning and normalizing OCR-detected or translated comic text.
 * <p>
 * It handles:
 * <ul>
 *     <li>Removing hyphenated line breaks ("- ") often caused by OCR on wrapped lines</li>
 *     <li>Collapsing multiple spaces into a single space</li>
 *     <li>Trimming unnecessary spaces before punctuation (e.g., "Hello !" → "Hello!")</li>
 *     <li>Splitting text into sentences based on punctuation (. ! ?)</li>
 *     <li>Normalizing case: each sentence is lowercased and then capitalized at the first letter</li>
 * </ul>
 * The final result is a cleaned and properly cased version of the text, suitable for re-rendering inside a comic bubble.
 */
public class TextProcessor {

    /**
     * Processes the input text and formats it into a clean, readable, sentence-cased structure.
     * <p>
     * Specifically:
     * <ol>
     *     <li>Removes hyphenated line breaks ("- ") from the text</li>
     *     <li>Collapses all whitespace (tabs, multiple spaces, line breaks) into single spaces</li>
     *     <li>Removes any space that appears directly before a punctuation mark (., !, ?)</li>
     *     <li>Splits the text into individual sentences based on sentence-ending punctuation</li>
     *     <li>For each sentence:</li>
     *     <ul>
     *         <li>Trims surrounding spaces</li>
     *         <li>Lowercases the entire sentence</li>
     *         <li>Capitalizes the first alphabetic character, if found</li>
     *     </ul>
     *     <li>Joins all the normalized sentences into a single string, separated by a space</li>
     * </ol>
     * <p>
     * This method preserves multi-character punctuation (like "!!!" or "?!") and makes sure no word is split mid-sentence.
     *
     * @param input the raw text from OCR or translation
     * @return a cleaned, properly cased string with sentence-level formatting
     */
    public String processText(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // Step 1: Remove hyphenated line breaks and trim leading/trailing space
        String cleaned = input.replace("- ", "").trim();

        // Step 2: Collapse multiple spaces/tabs/newlines into a single space
        cleaned = cleaned.replaceAll("\\s+", " ");

        // Step 3: Remove space before punctuation (! ? .)
        cleaned = cleaned.replaceAll(" +([!?\\.])", "$1");

        // Step 4: Split and normalize each sentence
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < cleaned.length()) {
            StringBuilder sentence = new StringBuilder();
            while (i < cleaned.length()) {
                char ch = cleaned.charAt(i);
                sentence.append(ch);

                // Check for sentence-ending punctuation
                if (ch == '.' || ch == '!' || ch == '?') {
                    // Capture runs like !!! or ?!
                    while (i + 1 < cleaned.length() &&
                            (cleaned.charAt(i + 1) == '!' || cleaned.charAt(i + 1) == '?' || cleaned.charAt(i + 1) == '.')) {
                        i++;
                        sentence.append(cleaned.charAt(i));
                    }
                    i++;
                    break;
                }
                i++;
            }

            String sent = sentence.toString().trim();
            if (!sent.isEmpty()) {
                // Lowercase all, then capitalize first letter
                sent = sent.toLowerCase();
                int firstAlpha = -1;
                for (int j = 0; j < sent.length(); j++) {
                    if (Character.isLetter(sent.charAt(j))) {
                        firstAlpha = j;
                        break;
                    }
                }
                if (firstAlpha != -1) {
                    sent = sent.substring(0, firstAlpha)
                            + Character.toUpperCase(sent.charAt(firstAlpha))
                            + sent.substring(firstAlpha + 1);
                }
                result.append(sent).append(" ");
            }
        }

        return result.toString().trim();
    }
}
