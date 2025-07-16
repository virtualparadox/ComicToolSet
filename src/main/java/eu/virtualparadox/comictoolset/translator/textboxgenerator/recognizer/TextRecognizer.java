package eu.virtualparadox.comictoolset.translator.textboxgenerator.recognizer;

import eu.virtualparadox.comictoolset.translator.textboxgenerator.assigner.RecognizedTextWithMask;
import eu.virtualparadox.comictoolset.translator.bubblecollector.DetectedBubbleBox;

import java.nio.file.Path;
import java.util.*;

public interface TextRecognizer extends AutoCloseable {

    List<RecognizedTextWithMask> recognize(final Path imagePath,
                                           final List<DetectedBubbleBox> bubbleBoxes) throws Exception;
}
