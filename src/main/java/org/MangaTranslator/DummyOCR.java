package org.MangaTranslator;

import java.awt.image.BufferedImage;

public class DummyOCR implements OCRProcessor {
    @Override
    public String extractText(BufferedImage image, int x, int y, int width, int height) {
        // Simula a extração de texto
        return "Texto simulado da região (" + x + ", " + y + ", " + width + ", " + height + ")";
    }

    @Override
    public void dispose() {
        // Nada a fazer aqui
    }
}