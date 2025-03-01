package org.MangaTranslator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class MangaTranslator extends JFrame {
    private BufferedImage mangaImage;
    private JLabel imageLabel;
    private JTextArea translationBox;
    private OCRProcessor ocrProcessor;
    private List<File> imageFiles;
    private int currentImageIndex = 0;
    private double scaleFactor = 1.0; // Fator de zoom
    private Point selectionStart;
    private Rectangle selectionRect;

    public MangaTranslator(OCRProcessor ocrProcessor) {
        this.ocrProcessor = ocrProcessor;

        // Configuração da janela
        setTitle("Manga Translator Pro");
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

        // Listeners de mouse para seleção
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

        // Zoom com scroll do mouse
        imageLabel.addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double zoomIntensity = 0.1; // Intensidade do zoom
                if (e.getWheelRotation() < 0) {
                    // Scroll para cima (aumentar zoom)
                    scaleFactor *= (1 + zoomIntensity);
                } else {
                    // Scroll para baixo (reduzir zoom)
                    scaleFactor /= (1 + zoomIntensity);
                }
                updateImage();
            }
        });

        // Área de tradução
        translationBox = new JTextArea(3, 20);
        translationBox.setWrapStyleWord(true);
        translationBox.setLineWrap(true);
        translationBox.setFont(new Font("Arial", Font.PLAIN, 16));

        // Painel de navegação
        JPanel navigationPanel = new JPanel();
        JButton prevButton = new JButton("Anterior");
        JButton nextButton = new JButton("Próximo");
        navigationPanel.add(prevButton);
        navigationPanel.add(nextButton);

        // Listeners dos botões de navegação
        prevButton.addActionListener(e -> showPreviousImage());
        nextButton.addActionListener(e -> showNextImage());

        // Montagem da interface
        JScrollPane imageScrollPane = new JScrollPane(imageLabel);
        add(imageScrollPane, BorderLayout.CENTER);
        add(new JScrollPane(translationBox), BorderLayout.SOUTH);
        add(navigationPanel, BorderLayout.NORTH);

        // Selecionar pasta com as imagens
        selectImageFolder();
    }

    private void selectImageFolder() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File folder = fileChooser.getSelectedFile();
            loadImagesFromFolder(folder);
            if (!imageFiles.isEmpty()) {
                loadImage(imageFiles.get(currentImageIndex));
            }
        }
    }

    private void loadImagesFromFolder(File folder) {
        imageFiles = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) ->
                name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg")
        );

        if (files != null) {
            for (File file : files) {
                imageFiles.add(file);
            }
        }
    }

    private void loadImage(File file) {
        try {
            mangaImage = ImageIO.read(file);
            scaleFactor = calculateScaleFactor(mangaImage); // Calcula o fator de escala inicial
            updateImage();
            setTitle("Manga Translator Pro - Página " + (currentImageIndex + 1) + " de " + imageFiles.size());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar imagem: " + e.getMessage());
        }
    }

    private double calculateScaleFactor(BufferedImage image) {
        // Define o tamanho máximo da imagem para caber na tela
        int maxWidth = Toolkit.getDefaultToolkit().getScreenSize().width - 100;
        int maxHeight = Toolkit.getDefaultToolkit().getScreenSize().height - 200;

        double widthFactor = (double) maxWidth / image.getWidth();
        double heightFactor = (double) maxHeight / image.getHeight();

        // Usa o menor fator para manter a proporção
        return Math.min(widthFactor, heightFactor);
    }

    private void updateImage() {
        if (mangaImage == null) return;

        // Redimensiona a imagem com base no fator de escala
        int newWidth = (int) (mangaImage.getWidth() * scaleFactor);
        int newHeight = (int) (mangaImage.getHeight() * scaleFactor);
        Image scaledImage = mangaImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaledImage));

        // Ajusta o tamanho da janela para caber na tela
        pack();
        setLocationRelativeTo(null); // Centraliza a janela
    }

    private void showPreviousImage() {
        if (currentImageIndex > 0) {
            currentImageIndex--;
            loadImage(imageFiles.get(currentImageIndex));
        }
    }

    private void showNextImage() {
        if (currentImageIndex < imageFiles.size() - 1) {
            currentImageIndex++;
            loadImage(imageFiles.get(currentImageIndex));
        }
    }

    private void processSelectedArea() {
        if (mangaImage == null) return;

        // Converte as coordenadas da imagem redimensionada para a original
        int x = (int) (selectionRect.x / scaleFactor);
        int y = (int) (selectionRect.y / scaleFactor);
        int width = (int) (selectionRect.width / scaleFactor);
        int height = (int) (selectionRect.height / scaleFactor);

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
        try {
            // Usar a implementação real do Tesseract
            OCRProcessor ocrProcessor = new TesseractOCR();

            SwingUtilities.invokeLater(() -> {
                MangaTranslator app = new MangaTranslator(ocrProcessor);
                app.setVisible(true);
                app.setLocationRelativeTo(null);
            });
        } catch (Exception e) {
            System.err.println("Erro ao inicializar OCR: " + e.getMessage());
            e.printStackTrace();

            // Fallback para DummyOCR se o Tesseract falhar
            JOptionPane.showMessageDialog(null,
                    "Não foi possível inicializar o Tesseract OCR: " + e.getMessage() +
                            "\nUsando modo de teste sem OCR.",
                    "Erro de Inicialização", JOptionPane.ERROR_MESSAGE);

            SwingUtilities.invokeLater(() -> {
                MangaTranslator app = new MangaTranslator(new DummyOCR());
                app.setVisible(true);
                app.setLocationRelativeTo(null);
            });
        }
    }
}