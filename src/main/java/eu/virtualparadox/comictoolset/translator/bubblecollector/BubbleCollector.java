package eu.virtualparadox.comictoolset.translator.bubblecollector;

import java.nio.file.Path;
import java.util.List;

public interface BubbleCollector extends AutoCloseable{
    List<ComicBubbleBox> extractBubbleBoxes(Path imagePath) throws Exception;
}
