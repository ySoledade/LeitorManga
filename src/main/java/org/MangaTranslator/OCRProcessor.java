package org.MangaTranslator;

import java.awt.image.BufferedImage;

public interface OCRProcessor {
    String extractText(BufferedImage image, int x, int y, int width, int height);
    void dispose();
}