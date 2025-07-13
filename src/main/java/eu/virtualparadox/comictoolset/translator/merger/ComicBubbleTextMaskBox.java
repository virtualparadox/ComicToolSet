package eu.virtualparadox.comictoolset.translator.merger;

import eu.virtualparadox.comictoolset.translator.textboxgenerator.ComicBubbleTextBox;
import eu.virtualparadox.comictoolset.translator.textmaskgenerator.TextMaskBox;

import java.util.ArrayList;
import java.util.List;

public class ComicBubbleTextMaskBox {
    public final ComicBubbleTextBox comicBubbleTextBox;
    public final List<TextMaskBox> textMaskBox;

    public ComicBubbleTextMaskBox(final ComicBubbleTextBox comicBubbleTextBox) {
        this.comicBubbleTextBox = comicBubbleTextBox;
        this.textMaskBox = new ArrayList<>();
    }

    public void addTextMaskBox(final TextMaskBox textMaskBox) {
        this.textMaskBox.add(textMaskBox);
    }
}
