package eu.virtualparadox.comictoolset.translator.textboxgenerator;

import eu.virtualparadox.comictoolset.translator.bubblecollector.ComicBubbleBox;

import java.nio.file.Path;
import java.util.*;

public interface TextBoxGenerator extends AutoCloseable {

    List<ComicBubbleTextBox> generateTextBoxes(final Path imagePath,
                                               final List<ComicBubbleBox> bubbleBoxes) throws Exception;
}
