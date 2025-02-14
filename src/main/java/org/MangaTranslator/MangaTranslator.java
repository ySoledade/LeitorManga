package org.MangaTranslator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.bytedeco.tesseract.TessBaseAPI;

public class MangaTranslator extends JFrame {
    private BufferedImage mangaImage;
    private JLabel imageLabel;
    private JTextArea translationBox;
    private TessBaseAPI tesseract;
    private double scaleFactor = 1.0; // Para ajuste de coordenadas

    public MangaTranslator() {
        setTitle("Manga Translator");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        imageLabel = new JLabel();
        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                processOCR(
                        (int)(e.getX() / scaleFactor),
                        (int)(e.getY() / scaleFactor)
                );
            }
        });
        add(new JScrollPane(imageLabel), BorderLayout.CENTER);

        translationBox = new JTextArea("Click on a text bubble to translate");
        translationBox.setEditable(false);
        add(translationBox, BorderLayout.SOUTH);

        setupTesseract();
        loadMangaImage("manga_page.jpg");
    }

    private void setupTesseract() {
        tesseract = new TessBaseAPI();
        String tessDataPath = "/usr/share/tesseract-ocr/4.00/"; // Ajuste para seu sistema

        if (tesseract.Init(tessDataPath, "eng") != 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to initialize Tesseract OCR.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }
    }

    private void loadMangaImage(String path) {
        try {
            mangaImage = ImageIO.read(new File(path));

            // Ajusta a escala da imagem para caber na janela
            int newWidth = Math.min(mangaImage.getWidth(), 780);
            scaleFactor = (double) newWidth / mangaImage.getWidth();
            int newHeight = (int)(mangaImage.getHeight() * scaleFactor);

            Image scaledImage = mangaImage.getScaledInstance(
                    newWidth,
                    newHeight,
                    Image.SCALE_SMOOTH
            );

            imageLabel.setIcon(new ImageIcon(scaledImage));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to load image.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void processOCR(int x, int y) {
        try {
            // Simulação de ROI (Region of Interest) - área de 100x50 pixels ao redor do clique
            // Na implementação real, você precisaria detectar os limites do balão de texto
            tesseract.SetImage(mangaImage);
            tesseract.SetRectangle(x, y, 100, 50);

            String extractedText = tesseract.GetUTF8Text().trim();
            String translatedText = translateText(extractedText);
            translationBox.setText("Original: " + extractedText + "\nTradução: " + translatedText);
        } catch (Exception e) {
            translationBox.setText("Error processing OCR: " + e.getMessage());
        }
    }

    private String translateText(String text) {
        // Simulação de tradução (substituir por API real)
        return "[Traduzido] " + text;
    }

    @Override
    public void dispose() {
        if (tesseract != null) {
            tesseract.close();
        }
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MangaTranslator().setVisible(true));
    }
}