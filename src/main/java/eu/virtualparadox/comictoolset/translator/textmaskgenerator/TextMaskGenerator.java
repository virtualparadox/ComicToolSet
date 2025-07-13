package eu.virtualparadox.comictoolset.translator.textmaskgenerator;

import java.nio.file.Path;
import java.util.List;

public interface TextMaskGenerator extends AutoCloseable {
    List<TextMaskBox> getTextMask(Path imagePath) throws Exception;
}
