package com.bingo.service;

import com.bingo.model.BingoGrid;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Generates PDF files for bingo grids using Apache PDFBox.
 *
 * Fonts are loaded from TTF files bundled under src/main/resources/fonts/,
 * so PDFBox never touches the system font directory.
 */
@Service
public class PdfService {

    private static final float PAGE_WIDTH  = PDRectangle.LETTER.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.LETTER.getHeight();
    private static final float MARGIN      = 60f;
    private static final int   GRID_SIZE   = 5;

    // Classpath paths — these files live in src/main/resources/fonts/
    private static final String FONT_REGULAR = "/fonts/LiberationSans-Regular.ttf";
    private static final String FONT_BOLD    = "/fonts/LiberationSans-Bold.ttf";
    private static final String FONT_ITALIC  = "/fonts/LiberationSans-Italic.ttf";

    /**
     * Render a single BingoGrid as a PDF and return it as a byte array.
     */
    public byte[] generateGridPdf(BingoGrid grid, String gameTitle) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            // PDType0Font.load() reads directly from the InputStream — no system
            // font scanning, no FontMapperImpl, no FileSystemFontProvider.
            PDType0Font fontBold    = loadFont(doc, FONT_BOLD);
            PDType0Font fontRegular = loadFont(doc, FONT_REGULAR);
            PDType0Font fontItalic  = loadFont(doc, FONT_ITALIC);

            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                drawGrid(cs, grid, gameTitle, fontBold, fontRegular, fontItalic);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private PDType0Font loadFont(PDDocument doc, String classpathPath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(classpathPath)) {
            if (is == null) {
                throw new IOException("Font resource not found on classpath: " + classpathPath);
            }
            return PDType0Font.load(doc, is, true);
        }
    }

    // ── Drawing ───────────────────────────────────────────────────

    private void drawGrid(PDPageContentStream cs, BingoGrid grid, String gameTitle,
                          PDType0Font fontBold, PDType0Font fontRegular, PDType0Font fontItalic)
            throws IOException {

        float usableWidth = PAGE_WIDTH - 2 * MARGIN;

        // Title
        String title = gameTitle + "  -  Player " + grid.getPlayerNumber();
        float titleSize = 18f;
        cs.beginText();
        cs.setFont(fontBold, titleSize);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.newLineAtOffset(MARGIN, PAGE_HEIGHT - MARGIN - titleSize);
        cs.showText(title);
        cs.endText();

        // Column headers: B I N G O
        float headerTop = PAGE_HEIGHT - MARGIN - titleSize - 20f;
        float cellSize  = Math.min(usableWidth / GRID_SIZE, (headerTop - MARGIN - 30f) / (GRID_SIZE + 1));
        float gridLeft  = MARGIN + (usableWidth - cellSize * GRID_SIZE) / 2f;
        float gridTop   = headerTop - 5f;

        String[] headers = {"B", "I", "N", "G", "O"};
        for (int col = 0; col < GRID_SIZE; col++) {
            float cx = gridLeft + col * cellSize + cellSize / 2f;
            float cy = gridTop - cellSize / 2f;
            drawCenteredText(cs, fontBold, 20f, headers[col], cx, cy, cellSize - 8f);
        }

        // Grid cells
        float dataTop = gridTop - cellSize;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                float x = gridLeft + col * cellSize;
                float y = dataTop  - row * cellSize;
                boolean isFree = (row == 2 && col == 2);

                if (isFree) {
                    cs.setNonStrokingColor(0f, 0f, 0f);
                    cs.addRect(x, y - cellSize, cellSize, cellSize);
                    cs.fill();
                }

                cs.setStrokingColor(0f, 0f, 0f);
                cs.setLineWidth(1.5f);
                cs.addRect(x, y - cellSize, cellSize, cellSize);
                cs.stroke();

                if (!isFree) {
                    String word = grid.getCells()[row][col];
                    if (word != null) {
                        cs.setNonStrokingColor(0f, 0f, 0f);
                        drawCenteredText(cs, fontRegular, 10f, word,
                                x + cellSize / 2f, y - cellSize / 2f, cellSize - 8f);
                    }
                }
            }
        }

        // Footer
        cs.beginText();
        cs.setFont(fontItalic, 9f);
        cs.setNonStrokingColor(0.5f, 0.5f, 0.5f);
        cs.newLineAtOffset(MARGIN, MARGIN - 20f);
        cs.showText("Bingo Game  -  Player " + grid.getPlayerNumber());
        cs.endText();
    }

    private void drawCenteredText(PDPageContentStream cs, PDType0Font font, float fontSize,
                                   String text, float cx, float cy, float maxW) throws IOException {
        float fs = fontSize;
        while (fs > 5f) {
            if (font.getStringWidth(text) / 1000f * fs <= maxW) break;
            fs -= 0.5f;
        }
        float textWidth = font.getStringWidth(text) / 1000f * fs;
        cs.beginText();
        cs.setFont(font, fs);
        cs.setNonStrokingColor(0f, 0f, 0f);
        cs.newLineAtOffset(cx - textWidth / 2f, cy - fs / 4f);
        cs.showText(text);
        cs.endText();
    }
}
