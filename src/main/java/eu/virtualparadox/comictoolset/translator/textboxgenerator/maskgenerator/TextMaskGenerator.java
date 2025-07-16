package eu.virtualparadox.comictoolset.translator.textboxgenerator.maskgenerator;

import java.nio.file.Path;
import java.util.List;

public interface TextMaskGenerator extends AutoCloseable {
    List<TextMaskRegion> getTextMask(Path imagePath) throws Exception;
}
