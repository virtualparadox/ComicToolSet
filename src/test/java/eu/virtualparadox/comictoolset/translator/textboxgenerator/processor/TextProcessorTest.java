package eu.virtualparadox.comictoolset.translator.textboxgenerator.processor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextProcessorTest {

    private final TextProcessor processor = new TextProcessor();

    @Test
    void testNullInputReturnsEmpty() {
        assertEquals("", processor.processText(null), "Null input should return empty string");
    }

    @Test
    void testBlankInputReturnsEmpty() {
        assertEquals("", processor.processText("   "), "Blank input should return empty string");
    }

    @Test
    void testSingleSentenceUpperCase() {
        String input = "HELLO WORLD!";
        String expected = "Hello world!";
        assertEquals(expected, processor.processText(input), "Should capitalize first letter and lowercase the rest");
    }

    @Test
    void testMultipleSentencesUpperCase() {
        String input = "THIS IS HORROR!!! WE SHOULD RUN!!! HELP!!";
        String expected = "This is horror!!! We should run!!! Help!!";
        assertEquals(expected, processor.processText(input), "Should handle multiple sentences and preserve punctuation");
    }

    @Test
    void testSentenceWithMixedCaseAndPunctuation() {
        String input = "hOW aRe yOu? i aM fiNe!";
        String expected = "How are you? I am fine!";
        assertEquals(expected, processor.processText(input), "Should normalize case and preserve sentence structure");
    }

    @Test
    void testConsecutivePunctuation() {
        String input = "WHAT?! REALLY?!";
        String expected = "What?! Really?!";
        assertEquals(expected, processor.processText(input), "Should correctly preserve mixed punctuation sequences");
    }

    @Test
    void testHyphenatedBreakRemoval() {
        String input = "THIS IS A TEST - string split across lines.";
        String expected = "This is a test string split across lines.";
        assertEquals(expected, processor.processText(input), "Should remove hyphenated line breaks");
    }

    @Test
    void testSentenceEndingWithoutFinalPunctuation() {
        String input = "this is strange";
        String expected = "This is strange";
        assertEquals(expected, processor.processText(input), "Should capitalize the sentence even without final punctuation");
    }

    @Test
    void testSentenceWithExtraSpaces() {
        String input = "  WOW   !!   OKAY   ?  ";
        String expected = "Wow!! Okay?";
        assertEquals(expected, processor.processText(input), "Should normalize casing and spacing");
    }
}
