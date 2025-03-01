package org.MangaTranslator;

import org.bytedeco.javacpp.*;
import org.bytedeco.tesseract.TessBaseAPI;
import org.bytedeco.leptonica.PIX;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import static org.bytedeco.leptonica.global.leptonica.*;

public class TesseractOCR implements OCRProcessor {
    private TessBaseAPI tesseract;
    private boolean initialized = false;

    public TesseractOCR() {
        // Habilitar logs para depuração
        System.setProperty("org.bytedeco.javacpp.logger.debug", "true");

        tesseract = new TessBaseAPI();

        // Procurar pelo diretório tessdata em locais comuns
        String[] tessDataPaths = {
                "/usr/share/tesseract-ocr/4.00/tessdata",
                "/usr/share/tesseract-ocr/tessdata",
                "/usr/local/share/tessdata",
                "/usr/share/tessdata",
                System.getProperty("user.home") + "/.tesseract-ocr/tessdata",
                System.getenv("TESSDATA_PREFIX")
        };

        for (String path : tessDataPaths) {
            if (path != null && !path.isEmpty()) {
                try {
                    System.out.println("Tentando inicializar Tesseract com caminho: " + path);
                    if (tesseract.Init(path, "jpn") == 0) {
                        System.out.println("Tesseract inicializado com sucesso usando: " + path);
                        initialized = true;
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Falha ao inicializar com caminho " + path + ": " + e.getMessage());
                }
            }
        }

        if (!initialized) {
            throw new RuntimeException("Não foi possível inicializar o Tesseract. Verifique se o diretório tessdata existe e contém os arquivos de idioma necessários.");
        }

        // Configurações para melhorar o OCR para mangás
        tesseract.SetPageSegMode(6); // Assume um único bloco de texto uniforme
        tesseract.SetVariable("preserve_interword_spaces", "1");
        tesseract.SetVariable("textord_min_linesize", "2.5");

        // Melhorar a detecção de texto vertical
        tesseract.SetVariable("textord_use_tesseract_11", "1");
        tesseract.SetVariable("textord_tablefind_recognize_tables", "0");
    }

    @Override
    public String extractText(BufferedImage image, int x, int y, int width, int height) {
        if (!initialized) {
            return "Tesseract não foi inicializado corretamente.";
        }

        if (x < 0) x = 0;
        if (y < 0) y = 0;
        if (width <= 0 || x + width > image.getWidth()) width = image.getWidth() - x;
        if (height <= 0 || y + height > image.getHeight()) height = image.getHeight() - y;

        if (width <= 0 || height <= 0) {
            return "Área de seleção inválida.";
        }

        // Extrair a sub-imagem selecionada
        BufferedImage selection = image.getSubimage(x, y, width, height);

        try {
            // Salvar a imagem temporariamente
            File tempFile = File.createTempFile("manga_ocr", ".png");
            ImageIO.write(selection, "png", tempFile);

            // Usar a API leptonica para carregar a imagem
            PIX pix = pixRead(tempFile.getAbsolutePath());
            if (pix == null) {
                throw new RuntimeException("Não foi possível carregar a imagem para OCR");
            }

            // Pré-processamento para melhorar a qualidade do OCR
            PIX grayPix = pixConvertRGBToGray(pix, 0.3f, 0.5f, 0.2f);  // Converter para escala de cinza
            PIX enhancedPix = pixContrastTRC(grayPix, grayPix, 1.5f);  // Melhorar contraste

            // Definir a imagem para o Tesseract
            tesseract.SetImage(enhancedPix);

            // Extrair texto
            BytePointer outText = tesseract.GetUTF8Text();
            String result = outText != null ? outText.getString().trim() : "";

            // Limpar recursos
            outText.deallocate();
            pixDestroy(pix);
            pixDestroy(grayPix);
            pixDestroy(enhancedPix);
            tempFile.delete();

            return result;
        } catch (IOException e) {
            return "Erro de E/S: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "Erro OCR: " + e.getMessage();
        }
    }

    @Override
    public void dispose() {
        if (tesseract != null) {
            try {
                tesseract.End();
                tesseract.close();
            } catch (Exception e) {
                System.err.println("Erro ao liberar recursos do Tesseract: " + e.getMessage());
            }
        }
    }
}