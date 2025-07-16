package eu.virtualparadox.comictoolset.translator.textremover;

import eu.virtualparadox.comictoolset.translator.textboxgenerator.maskgenerator.TextMaskRegion;

import java.nio.file.Path;
import java.util.List;

public interface TextRemover extends AutoCloseable {

    Path removeText(final Path originalImage, final List<TextMaskRegion> maskBoxes) throws Exception;

}
