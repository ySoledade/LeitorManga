package org.MangaTranslator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class MangaTranslator extends JFrame {
    private BufferedImage mangaImage;
    private BufferedImage scaledImage;
    private JLabel imageLabel;
    private JTextArea translationBox;
    private double scaleFactor = 1.0;
    private Point selectionStart;
    private Rectangle selectionRect;
    private OCRProcessor ocrProcessor;

    public MangaTranslator(OCRProcessor ocrProcessor) {
        this.ocrProcessor = ocrProcessor;

        // Configuração da janela
        setTitle("Manga Translator");
        setSize(1024, 768);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Componente de imagem com seleção
        imageLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (selectionRect != null) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setColor(new Color(255, 0, 0, 150));
                    g2d.drawRect(selectionRect.x, selectionRect.y, selectionRect.width, selectionRect.height);
                    g2d.dispose();
                }
            }
        };

        // Listeners de mouse
        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selectionStart = e.getPoint();
                selectionRect = new Rectangle(selectionStart.x, selectionStart.y, 0, 0);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (selectionRect != null && selectionRect.width > 10 && selectionRect.height > 10) {
                    processSelectedArea();
                }
                selectionStart = null;
                selectionRect = null;
                imageLabel.repaint();
            }
        });

        imageLabel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (selectionStart != null) {
                    int x = Math.min(selectionStart.x, e.getX());
                    int y = Math.min(selectionStart.y, e.getY());
                    int width = Math.abs(e.getX() - selectionStart.x);
                    int height = Math.abs(e.getY() - selectionStart.y);
                    selectionRect.setBounds(x, y, width, height);
                    imageLabel.repaint();
                }
            }
        });

        // Área de tradução
        translationBox = new JTextArea(3, 20);
        translationBox.setWrapStyleWord(true);
        translationBox.setLineWrap(true);
        translationBox.setFont(new Font("Arial", Font.PLAIN, 16));

        // Montagem da interface
        add(new JScrollPane(imageLabel), BorderLayout.CENTER);
        add(new JScrollPane(translationBox), BorderLayout.SOUTH);

        // Carrega a imagem
        loadImage("/home/yago/Downloads/Tesseract2clean.png");
    }

    private void loadImage(String path) {
        try {
            BufferedImage original = ImageIO.read(new File(path));
            mangaImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            mangaImage.getGraphics().drawImage(original, 0, 0, null);

            scaleFactor = Math.min(1.0, Math.min(800.0 / mangaImage.getWidth(), 600.0 / mangaImage.getHeight()));
            scaledImage = new BufferedImage(
                    (int)(mangaImage.getWidth() * scaleFactor),
                    (int)(mangaImage.getHeight() * scaleFactor),
                    BufferedImage.TYPE_3BYTE_BGR
            );
            scaledImage.getGraphics().drawImage(
                    mangaImage.getScaledInstance(scaledImage.getWidth(), scaledImage.getHeight(), Image.SCALE_SMOOTH),
                    0, 0, null
            );

            imageLabel.setIcon(new ImageIcon(scaledImage));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar imagem: " + e.getMessage());
        }
    }

    private void processSelectedArea() {
        int x = (int)(selectionRect.x / scaleFactor);
        int y = (int)(selectionRect.y / scaleFactor);
        int width = (int)(selectionRect.width / scaleFactor);
        int height = (int)(selectionRect.height / scaleFactor);

        try {
            String text = ocrProcessor.extractText(mangaImage, x, y, width, height);
            translationBox.setText("Texto extraído: " + text);
        } catch (Exception e) {
            translationBox.setText("Erro no OCR: " + e.getMessage());
        }
    }

    @Override
    public void dispose() {
        ocrProcessor.dispose();
        super.dispose();
    }

    public static void main(String[] args) {
        // Usar DummyOCR para testes sem Tesseract
        OCRProcessor ocrProcessor = new DummyOCR();

        // Ou usar TesseractOCR para a versão real
        // OCRProcessor ocrProcessor = new TesseractOCR();

        SwingUtilities.invokeLater(() -> {
            MangaTranslator app = new MangaTranslator(ocrProcessor);
            app.setVisible(true);
            app.setLocationRelativeTo(null);
        });
    }
}