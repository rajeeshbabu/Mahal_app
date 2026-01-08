package com.mahal.service;

import com.mahal.model.Certificate;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// PDFBox imports for PDF generation from image template
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;

public class CertificatePDFService {

    private static final String CERTIFICATES_DIR = "certificates";
    private static final String TEMPLATES_DIR = "templates";
    private static final String MARRIAGE_CERT_TEMPLATE = "marriage_certificate_template.png";

    // PDF dimensions (A4 size in points: 595 x 842)
    private static final float PAGE_WIDTH = 595;
    private static final float PAGE_HEIGHT = 842;

    public static String saveMarriageCertificateHTML(Certificate cert) throws IOException {
        // Create certificates directory if it doesn't exist
        File certDir = new File(CERTIFICATES_DIR);
        if (!certDir.exists()) {
            certDir.mkdirs();
        }

        // Determine base file name
        String baseFileName = cert.getCertificateNo() != null && !cert.getCertificateNo().isEmpty()
                ? cert.getCertificateNo()
                : "marriage_cert_" + System.currentTimeMillis();

        // Generate and save PDF file from image template
        File pdfFile = new File(certDir, baseFileName + ".pdf");
        try {
            generatePDFFromImageTemplate(cert, pdfFile);
            // Verify PDF was successfully created
            if (pdfFile.exists() && pdfFile.length() > 0) {
                return pdfFile.getAbsolutePath();
            } else {
                throw new IOException("PDF file was not created properly");
            }
        } catch (Exception e) {
            // If PDF generation fails, log the error
            System.err.println("Warning: PDF generation failed for " + baseFileName);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            // Delete invalid PDF file if it exists
            if (pdfFile.exists()) {
                pdfFile.delete();
            }
            throw new IOException("Failed to generate PDF certificate: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a PDF certificate with the marriage certificate content and layout.
     * This uses a simple bordered design with justified paragraphs and signature
     * blocks.
     */
    private static void generatePDFFromImageTemplate(Certificate cert, File outputFile) throws IOException {
        // Extract certificate data
        String groomName = cert.getGroomName() != null ? cert.getGroomName() : "";
        String brideName = cert.getBrideName() != null ? cert.getBrideName() : "";
        String groomParent = cert.getParentNameOfGroom() != null ? cert.getParentNameOfGroom() : "";
        String brideParent = cert.getParentNameOfBride() != null ? cert.getParentNameOfBride() : "";
        String groomAddress = cert.getAddressOfGroom() != null ? cert.getAddressOfGroom() : "";
        String brideAddress = cert.getAddressOfBride() != null ? cert.getAddressOfBride() : "";
        String placeOfMarriage = cert.getPlaceOfMarriage() != null ? cert.getPlaceOfMarriage() : "";
        String certNo = cert.getCertificateNo() != null ? cert.getCertificateNo() : "";
        LocalDate marriageDate = cert.getMarriageDate();
        LocalDate issueDate = cert.getIssueDate() != null ? cert.getIssueDate() : LocalDate.now();

        String marriageDateStr = marriageDate != null ? marriageDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                : "";
        String issueDateStr = issueDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));

        String groomLine = (groomName.isEmpty() ? "_____________________" : groomName)
                + (groomParent.isEmpty() ? "" : ", son of " + groomParent)
                + (groomAddress.isEmpty() ? "" : ", residing at " + groomAddress);
        String brideLine = (brideName.isEmpty() ? "_____________________" : brideName)
                + (brideParent.isEmpty() ? "" : ", daughter of " + brideParent)
                + (brideAddress.isEmpty() ? "" : ", residing at " + brideAddress);

        String paragraph1 = "This is to certify that a lawful Nikah (Islamic marriage) was solemnized between "
                + groomLine + ", and " + brideLine
                + (marriageDateStr.isEmpty() ? "" : ", on Date of Nikah " + marriageDateStr)
                + (placeOfMarriage.isEmpty() ? "" : " at Place of Nikah " + placeOfMarriage)
                + ", in accordance with the principles of Islamic Shariah and the customs and traditions of the Muslim community.";

        String paragraph2 = "This certificate is issued as an official record of the Nikah, bearing Certificate No. "
                + (certNo.isEmpty() ? "_____________________" : certNo) + " and Date of Issue " + issueDateStr + ". "
                + "It is signed by the groom, bride, witnesses, and the Qazi/Imam, and affixed with the official seal of the issuing Masjid/Mahal. "
                + "This document serves as valid proof of Muslim marriage and should be preserved carefully for legal and personal purposes.";

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            // Colors
            java.awt.Color borderColor = new java.awt.Color(0x16, 0x65, 0x34); // Dark green
            java.awt.Color titleColor = borderColor;
            java.awt.Color textColor = java.awt.Color.BLACK;

            // Layout
            float borderMargin = 40;
            float borderPaddingLeft = 20;
            float borderPaddingRight = 60; // keep extra right padding as requested
            float pageW = PAGE_WIDTH;
            float pageH = PAGE_HEIGHT;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // Border
                cs.setStrokingColor(borderColor);
                cs.setLineWidth(3);
                cs.addRect(borderMargin, borderMargin, pageW - (borderMargin * 2), pageH - (borderMargin * 2));
                cs.stroke();

                // Title
                float titleY = pageH - borderMargin - 20;
                cs.setFont(PDType1Font.TIMES_BOLD, 24);
                cs.setNonStrokingColor(titleColor);
                String title = "MARRIAGE (NIKAH) CERTIFICATE";
                float titleWidth = PDType1Font.TIMES_BOLD.getStringWidth(title) / 1000 * 24;
                cs.beginText();
                cs.newLineAtOffset((pageW - titleWidth) / 2, titleY);
                cs.showText(title);
                cs.endText();

                // Main content
                float startY = titleY - 50;
                float leftMargin = borderMargin + borderPaddingLeft;
                float rightMargin = pageW - borderMargin - borderPaddingRight;
                float lineHeight = 20;
                float currentY = startY;

                cs.setFont(PDType1Font.TIMES_ROMAN, 12);
                cs.setNonStrokingColor(textColor);

                // Intro line
                String introLine = "This certificate is proudly presented to";
                float introWidth = PDType1Font.TIMES_ROMAN.getStringWidth(introLine) / 1000 * 12;
                cs.beginText();
                cs.newLineAtOffset((pageW - introWidth) / 2, currentY);
                cs.showText(introLine);
                cs.endText();
                currentY -= lineHeight + 5;

                // Names line
                String namesLine = (groomName.isEmpty() ? "_____________________" : groomName)
                        + " & "
                        + (brideName.isEmpty() ? "_____________________" : brideName);
                cs.setFont(PDType1Font.TIMES_BOLD, 18);
                float namesWidth = PDType1Font.TIMES_BOLD.getStringWidth(namesLine) / 1000 * 18;
                cs.beginText();
                cs.newLineAtOffset((pageW - namesWidth) / 2, currentY);
                cs.showText(namesLine);
                cs.endText();
                currentY -= lineHeight * 2;

                // Paragraphs
                float maxWidth = rightMargin - leftMargin;
                float fontSize = 12;
                cs.setFont(PDType1Font.TIMES_ROMAN, fontSize);
                currentY = drawParagraph(cs, paragraph1, leftMargin, currentY, maxWidth, fontSize, lineHeight, doc);
                currentY -= 15;
                currentY = drawParagraph(cs, paragraph2, leftMargin, currentY, maxWidth, fontSize, lineHeight, doc);

                // Signatures
                currentY -= lineHeight;
                float sigY = currentY;
                float sigWidth = (rightMargin - leftMargin) / 3;

                cs.setFont(PDType1Font.TIMES_ROMAN, 11);
                cs.beginText();
                cs.newLineAtOffset(leftMargin, sigY);
                cs.showText("Groom Signature");
                cs.endText();
                cs.setLineWidth(1);
                cs.setStrokingColor(java.awt.Color.BLACK);
                cs.moveTo(leftMargin, sigY - 5);
                cs.lineTo(leftMargin + sigWidth - 10, sigY - 5);
                cs.stroke();

                cs.beginText();
                cs.newLineAtOffset(leftMargin + sigWidth, sigY);
                cs.showText("Bride Signature");
                cs.endText();
                cs.moveTo(leftMargin + sigWidth, sigY - 5);
                cs.lineTo(leftMargin + sigWidth * 2 - 10, sigY - 5);
                cs.stroke();

                cs.beginText();
                cs.newLineAtOffset(leftMargin + sigWidth * 2, sigY);
                cs.showText("Qazi / Imam Signature");
                cs.endText();
                cs.moveTo(leftMargin + sigWidth * 2, sigY - 5);
                cs.lineTo(rightMargin, sigY - 5);
                cs.stroke();

                // Footer
                sigY -= 40;
                cs.setFont(PDType1Font.TIMES_ROMAN, 11);
                cs.beginText();
                cs.newLineAtOffset(leftMargin, sigY);
                cs.showText("Issued by (Masjid / Mahal):");
                cs.endText();
                cs.moveTo(leftMargin, sigY - 5);
                cs.lineTo(rightMargin, sigY - 5);
                cs.stroke();

                // Seal
                float sealY = borderMargin + borderPaddingLeft;
                cs.setFont(PDType1Font.TIMES_ROMAN, 10);
                cs.beginText();
                cs.newLineAtOffset(rightMargin - 100, sealY);
                cs.showText("Official Seal / Stamp");
                cs.endText();
            }

            doc.save(outputFile);
        }

        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new IOException("PDF file was not created or is empty");
        }
        byte[] bytes = java.nio.file.Files.readAllBytes(outputFile.toPath());
        if (bytes.length < 4 || !new String(bytes, 0, 4).equals("%PDF")) {
            throw new IOException("Generated file is not a valid PDF");
        }
    }

    /**
     * Generates a PDF certificate without template image (formatted text with
     * borders)
     * 
     * @param cert       The certificate data
     * @param outputFile The output PDF file
     * @throws IOException If PDF generation fails
     */
    private static void generatePDFWithoutTemplate(Certificate cert, File outputFile) throws IOException {
        // Extract certificate data
        String groomName = cert.getGroomName() != null ? cert.getGroomName() : "";
        String brideName = cert.getBrideName() != null ? cert.getBrideName() : "";
        String groomParent = cert.getParentNameOfGroom() != null ? cert.getParentNameOfGroom() : "";
        String brideParent = cert.getParentNameOfBride() != null ? cert.getParentNameOfBride() : "";
        String groomAddress = cert.getAddressOfGroom() != null ? cert.getAddressOfGroom() : "";
        String brideAddress = cert.getAddressOfBride() != null ? cert.getAddressOfBride() : "";
        String placeOfMarriage = cert.getPlaceOfMarriage() != null ? cert.getPlaceOfMarriage() : "";
        String certNo = cert.getCertificateNo() != null ? cert.getCertificateNo() : "";
        LocalDate marriageDate = cert.getMarriageDate();
        LocalDate issueDate = cert.getIssueDate() != null ? cert.getIssueDate() : LocalDate.now();

        String marriageDateStr = marriageDate != null ? marriageDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                : "";
        String issueDateStr = issueDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                PDType1Font fontTitle = PDType1Font.TIMES_BOLD;
                PDType1Font font = PDType1Font.TIMES_ROMAN;

                // Draw border (40 points from each edge)
                float borderMargin = 40;
                float borderPaddingLeft = 20; // Padding inside border for left side
                float borderPaddingRight = 60; // Extra padding for right side to keep text farther from right margin
                contentStream.setStrokingColor(0, 102, 51); // Dark green
                contentStream.setLineWidth(3);
                contentStream.addRect(borderMargin, borderMargin, PAGE_WIDTH - (borderMargin * 2),
                        PAGE_HEIGHT - (borderMargin * 2));
                contentStream.stroke();

                // Title
                float titleY = PAGE_HEIGHT - borderMargin - 20;
                contentStream.setFont(fontTitle, 24);
                contentStream.setNonStrokingColor(0, 102, 51); // Dark green
                String title = "MARRIAGE (NIKAH) CERTIFICATE";
                float titleWidth = fontTitle.getStringWidth(title) / 1000 * 24;
                contentStream.beginText();
                contentStream.newLineAtOffset((PAGE_WIDTH - titleWidth) / 2, titleY);
                contentStream.showText(title);
                contentStream.endText();

                // Main content - positioned inside border with padding
                float startY = titleY - 50;
                float leftMargin = borderMargin + borderPaddingLeft; // Inside border with padding
                float rightMargin = PAGE_WIDTH - borderMargin - borderPaddingRight; // Inside border with extra right
                                                                                    // padding
                float lineHeight = 20;
                float currentY = startY;

                contentStream.setFont(font, 12);
                contentStream.setNonStrokingColor(0, 0, 0); // Black

                // Certificate text in paragraph format (no age mentioned)
                String paragraph1 = "This is to certify that a lawful Nikah (Islamic marriage) was solemnized between "
                        +
                        (groomName.isEmpty() ? "_____________________" : groomName) + ", son of " +
                        (groomParent.isEmpty() ? "_____________________" : groomParent) + ", residing at " +
                        (groomAddress.isEmpty() ? "_____________________" : groomAddress) + ", and " +
                        (brideName.isEmpty() ? "_____________________" : brideName) + ", daughter of " +
                        (brideParent.isEmpty() ? "_____________________" : brideParent) + ", residing at " +
                        (brideAddress.isEmpty() ? "_____________________" : brideAddress) + ", on Date of Nikah " +
                        (marriageDateStr.isEmpty() ? "_____________________" : marriageDateStr) + " at Place of Nikah "
                        +
                        (placeOfMarriage.isEmpty() ? "_____________________" : placeOfMarriage) +
                        ", in accordance with the principles of Islamic Shariah and the customs and traditions of the Muslim community.";

                String paragraph2 = "This certificate is issued as an official record of the Nikah, bearing Certificate No. "
                        +
                        (certNo.isEmpty() ? "_____________________" : certNo) + " and Date of Issue " + issueDateStr +
                        ". It is signed by the groom, bride, witnesses, and the Qazi/Imam, and affixed with the official seal of the issuing Masjid/Mahal. "
                        +
                        "This document serves as valid proof of Muslim marriage and should be preserved carefully for legal and personal purposes.";

                // Draw paragraphs with proper word wrapping
                float maxWidth = rightMargin - leftMargin;
                float fontSize = 12;
                float paragraphSpacing = 15;

                // Draw first paragraph
                currentY = drawParagraph(contentStream, paragraph1, leftMargin, currentY, maxWidth, fontSize,
                        lineHeight, document);
                currentY -= paragraphSpacing;

                // Draw second paragraph
                currentY = drawParagraph(contentStream, paragraph2, leftMargin, currentY, maxWidth, fontSize,
                        lineHeight, document);

                // Signatures section
                currentY -= lineHeight;
                float sigY = currentY;
                float sigWidth = (rightMargin - leftMargin) / 3;

                // Groom Signature
                contentStream.setFont(font, 11);
                contentStream.beginText();
                contentStream.newLineAtOffset(leftMargin, sigY);
                contentStream.showText("Groom Signature");
                contentStream.endText();
                contentStream.setLineWidth(1);
                contentStream.setStrokingColor(0, 0, 0);
                contentStream.moveTo(leftMargin, sigY - 5);
                contentStream.lineTo(leftMargin + sigWidth - 10, sigY - 5);
                contentStream.stroke();

                // Bride Signature
                contentStream.beginText();
                contentStream.newLineAtOffset(leftMargin + sigWidth, sigY);
                contentStream.showText("Bride Signature");
                contentStream.endText();
                contentStream.moveTo(leftMargin + sigWidth, sigY - 5);
                contentStream.lineTo(leftMargin + sigWidth * 2 - 10, sigY - 5);
                contentStream.stroke();

                // Qazi/Imam Signature
                contentStream.beginText();
                contentStream.newLineAtOffset(leftMargin + sigWidth * 2, sigY);
                contentStream.showText("Qazi / Imam Signature");
                contentStream.endText();
                contentStream.moveTo(leftMargin + sigWidth * 2, sigY - 5);
                contentStream.lineTo(rightMargin, sigY - 5);
                contentStream.stroke();

                // Footer
                sigY -= 40;
                contentStream.setFont(font, 11);
                contentStream.beginText();
                contentStream.newLineAtOffset(leftMargin, sigY);
                contentStream.showText("Issued by (Masjid / Mahal):");
                contentStream.endText();
                contentStream.moveTo(leftMargin, sigY - 5);
                contentStream.lineTo(rightMargin, sigY - 5);
                contentStream.stroke();

                // Seal - positioned inside border
                float sealY = borderMargin + borderPaddingLeft;
                contentStream.setFont(font, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(rightMargin - 100, sealY);
                contentStream.showText("Official Seal / Stamp");
                contentStream.endText();
            }

            // Save the PDF
            document.save(outputFile);
        }

        // Verify PDF was created and is valid
        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new IOException("PDF file was not created or is empty");
        }

        // Verify it's a valid PDF by checking header
        byte[] bytes = java.nio.file.Files.readAllBytes(outputFile.toPath());
        if (bytes.length < 4 || !new String(bytes, 0, 4).equals("%PDF")) {
            throw new IOException("Generated file is not a valid PDF");
        }
    }

    /**
     * Helper method to draw a paragraph with word wrapping and justified alignment
     * Supports Unicode characters using appropriate fonts
     * 
     * @param contentStream The PDF content stream
     * @param text          The text to draw
     * @param x             Starting X position
     * @param y             Starting Y position
     * @param maxWidth      Maximum width for the paragraph
     * @param fontSize      Font size
     * @param lineHeight    Height between lines
     * @param doc           The PDDocument (needed for Unicode fonts)
     * @return The Y position after drawing the paragraph
     */
    private static float drawParagraph(PDPageContentStream contentStream, String text,
            float x, float y, float maxWidth, float fontSize, float lineHeight, PDDocument doc) throws IOException {
        float currentY = y;
        String[] words = text.split(" ");
        java.util.List<String> currentLineWords = new java.util.ArrayList<>();

        // Use Unicode font if text contains Unicode characters
        PDFont font;
        if (containsUnicode(text)) {
            font = getUnicodeFont(doc, false);
            System.out.println(
                    "Using Unicode font for text containing: " + text.substring(0, Math.min(50, text.length())));
        } else {
            font = PDType1Font.TIMES_ROMAN;
        }

        float spaceWidth;
        try {
            spaceWidth = font.getStringWidth(" ") / 1000 * fontSize;
        } catch (Exception e) {
            spaceWidth = fontSize * 0.3f; // Fallback estimate
        }

        for (String word : words) {
            if (word.isEmpty())
                continue;
            // Calculate width of current line with new word
            float currentLineWidth = 0;
            for (String w : currentLineWords) {
                try {
                    currentLineWidth += font.getStringWidth(w) / 1000 * fontSize;
                } catch (Exception e) {
                    // Estimate width if measurement fails
                    currentLineWidth += w.length() * fontSize * 0.6f;
                }
            }
            if (currentLineWords.size() > 0) {
                currentLineWidth += spaceWidth * currentLineWords.size(); // spaces between words
            }
            float wordWidth = 0;
            try {
                wordWidth = font.getStringWidth(word) / 1000 * fontSize;
            } catch (Exception e) {
                // If word can't be measured, estimate based on character count
                wordWidth = word.length() * fontSize * 0.6f;
            }
            float testWidth = currentLineWidth + (currentLineWords.size() > 0 ? spaceWidth : 0) + wordWidth;

            if (testWidth > maxWidth && currentLineWords.size() > 0) {
                // Draw current line with justification and start new line
                drawJustifiedLine(contentStream, currentLineWords, x, currentY, maxWidth, fontSize, font, spaceWidth);
                currentY -= lineHeight;
                currentLineWords.clear();
                currentLineWords.add(word);
            } else {
                currentLineWords.add(word);
            }
        }

        // Draw the last line (left-aligned, not justified)
        if (currentLineWords.size() > 0) {
            float currentX = x;
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(currentX, currentY);
            for (int i = 0; i < currentLineWords.size(); i++) {
                if (i > 0) {
                    contentStream.endText();
                    currentX += spaceWidth;
                    contentStream.beginText();
                    contentStream.setFont(font, fontSize);
                    contentStream.newLineAtOffset(currentX, currentY);
                }
                try {
                    // Ensure font is set before showing text
                    contentStream.setFont(font, fontSize);
                    String word = currentLineWords.get(i);
                    // For Unicode fonts (PDType0Font), showText should work directly
                    contentStream.showText(word);
                    currentX += font.getStringWidth(word) / 1000 * fontSize;
                } catch (Exception e) {
                    // Log the error for debugging
                    System.err.println("Error rendering word '"
                            + currentLineWords.get(i).substring(0, Math.min(20, currentLineWords.get(i).length()))
                            + "': " + e.getMessage());
                    e.printStackTrace();
                    // Try to continue with next word
                    contentStream.endText();
                    contentStream.beginText();
                    contentStream.setFont(font, fontSize);
                    contentStream.newLineAtOffset(currentX, currentY);
                    // Estimate width for skipped word
                    currentX += currentLineWords.get(i).length() * fontSize * 0.6f;
                }
            }
            contentStream.endText();
            currentY -= lineHeight;
        }

        return currentY;
    }

    /**
     * Gets a Unicode-supporting font for PDF rendering.
     * Tries to load a system font that supports Unicode, falls back to Times-Roman
     * if not available.
     * 
     * @param doc  The PDDocument to add the font to
     * @param bold Whether to use bold variant
     * @return A PDFont that supports Unicode
     */
    private static PDFont getUnicodeFont(PDDocument doc, boolean bold) {
        // First, try to load bundled font from resources
        try {
            // Try to load Noto Sans Malayalam from resources (if bundled)
            InputStream bundledFont = CertificatePDFService.class
                    .getResourceAsStream("/fonts/NotoSansMalayalam-Regular.ttf");
            if (bundledFont != null) {
                try {
                    PDFont font = PDType0Font.load(doc, bundledFont, true);
                    System.out.println("Successfully loaded bundled Unicode font: Noto Sans Malayalam");
                    return font;
                } catch (Exception e) {
                    System.err.println("Failed to load bundled font: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // Continue to system fonts
        }

        // Try to load from fonts directory in project root
        try {
            File fontsDir = new File("fonts");
            if (fontsDir.exists() && fontsDir.isDirectory()) {
                File[] fontFiles = fontsDir.listFiles(
                        (dir, name) -> name.toLowerCase().endsWith(".ttf") || name.toLowerCase().endsWith(".otf"));
                if (fontFiles != null && fontFiles.length > 0) {
                    // Try Noto Sans Malayalam first
                    for (File fontFile : fontFiles) {
                        String fileName = fontFile.getName().toLowerCase();
                        if (fileName.contains("malayalam") || fileName.contains("noto")) {
                            try (InputStream fontStream = new FileInputStream(fontFile)) {
                                PDFont font = PDType0Font.load(doc, fontStream, true);
                                System.out.println(
                                        "Successfully loaded font from fonts directory: " + fontFile.getName());
                                return font;
                            } catch (Exception e) {
                                System.err.println(
                                        "Failed to load font file " + fontFile.getName() + ": " + e.getMessage());
                            }
                        }
                    }
                    // If no Malayalam-specific font, try any TTF/OTF file
                    for (File fontFile : fontFiles) {
                        try (InputStream fontStream = new FileInputStream(fontFile)) {
                            PDFont font = PDType0Font.load(doc, fontStream, true);
                            System.out.println("Successfully loaded font from fonts directory: " + fontFile.getName());
                            return font;
                        } catch (Exception e) {
                            // Continue to next font
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking fonts directory: " + e.getMessage());
        }

        // Then try system fonts
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            String[] fontNames = ge.getAvailableFontFamilyNames();

            // Preferred fonts that typically support Unicode (in order of preference)
            String[] preferredFonts = {
                    "Noto Sans Malayalam", "Noto Sans", "Arial Unicode MS", "Arial",
                    "DejaVu Sans", "Liberation Sans", "Tahoma", "Verdana", "Calibri",
                    "Mangal", "Lohit Malayalam", "Rachana", "Meera"
            };

            for (String preferredFont : preferredFonts) {
                for (String availableFont : fontNames) {
                    if (availableFont.equalsIgnoreCase(preferredFont)) {
                        try {
                            Font javaFont = new Font(availableFont, bold ? Font.BOLD : Font.PLAIN, 12);
                            if (javaFont.canDisplay('\u0D30')) { // Malayalam character 'ra'
                                String fontPath = findFontFile(availableFont, bold);
                                if (fontPath != null) {
                                    try (InputStream fontStream = new FileInputStream(fontPath)) {
                                        PDFont pdfFont = PDType0Font.load(doc, fontStream, true);
                                        System.out.println("Successfully loaded system Unicode font: " + availableFont);
                                        return pdfFont;
                                    } catch (Exception e) {
                                        System.err.println(
                                                "Failed to load font file " + fontPath + ": " + e.getMessage());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Continue to next font
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading system fonts: " + e.getMessage());
        }

        // Final fallback: Show warning and use Times-Roman (will show boxes for
        // unsupported chars)
        System.err.println("========================================");
        System.err.println("WARNING: No Unicode font found!");
        System.err.println("Malayalam text will NOT display correctly in PDF.");
        System.err.println("========================================");
        System.err.println("SOLUTION:");
        System.err.println("1. Run: download_malayalam_font.bat");
        System.err.println("   OR");
        System.err.println("2. Download from: https://fonts.google.com/noto/specimen/Noto+Sans+Malayalam");
        System.err.println("3. Copy 'NotoSansMalayalam-Regular.ttf' to: " + new File("fonts").getAbsolutePath());
        System.err.println("========================================");
        return bold ? PDType1Font.TIMES_BOLD : PDType1Font.TIMES_ROMAN;
    }

    /**
     * Attempts to find the file path for a system font.
     * Searches common font directories on Windows, Linux, and macOS.
     */
    private static String findFontFile(String fontName, boolean bold) {
        String os = System.getProperty("os.name").toLowerCase();
        String[] fontDirs;

        if (os.contains("win")) {
            // Windows font directory
            String winDir = System.getenv("WINDIR");
            fontDirs = new String[] {
                    winDir != null ? winDir + "\\Fonts\\" : "C:\\Windows\\Fonts\\",
                    System.getProperty("user.home") + "\\AppData\\Local\\Microsoft\\Windows\\Fonts\\"
            };
        } else if (os.contains("mac")) {
            // macOS font directories
            fontDirs = new String[] {
                    "/Library/Fonts/",
                    "/System/Library/Fonts/",
                    System.getProperty("user.home") + "/Library/Fonts/"
            };
        } else {
            // Linux font directories
            fontDirs = new String[] {
                    "/usr/share/fonts/",
                    "/usr/local/share/fonts/",
                    System.getProperty("user.home") + "/.fonts/"
            };
        }

        // Common font file extensions
        String[] extensions = { ".ttf", ".otf", ".TTF", ".OTF" };

        // Try to find font file
        for (String fontDir : fontDirs) {
            File dir = new File(fontDir);
            if (!dir.exists())
                continue;

            // Search for font files matching the font name
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName().toLowerCase();
                    String searchName = fontName.toLowerCase().replace(" ", "");

                    // Check if filename contains font name (more flexible matching)
                    String fontNameLower = fontName.toLowerCase();
                    String fileNameLower = fileName.toLowerCase();

                    // Try multiple matching strategies
                    boolean matches = fileNameLower.contains(fontNameLower.replace(" ", "")) ||
                            fileNameLower.contains(fontNameLower.replace(" ", "-")) ||
                            fileNameLower.contains(fontNameLower.replace(" ", "_")) ||
                            (fontNameLower.length() > 3 && fileNameLower.startsWith(
                                    fontNameLower.replace(" ", "").substring(0, Math.min(5, fontNameLower.length()))));

                    if (matches) {
                        for (String ext : extensions) {
                            if (fileName.endsWith(ext)) {
                                // For bold fonts, prefer files with "bold" in name, but also accept regular if
                                // bold not found
                                if (bold) {
                                    if (fileNameLower.contains("bold")) {
                                        System.out.println("Found bold font file: " + file.getAbsolutePath());
                                        return file.getAbsolutePath();
                                    }
                                } else {
                                    if (!fileNameLower.contains("bold") && !fileNameLower.contains("italic")) {
                                        System.out.println("Found regular font file: " + file.getAbsolutePath());
                                        return file.getAbsolutePath();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Checks if text contains Unicode characters beyond basic Latin
     * 
     * @param text The text to check
     * @return true if text contains Unicode characters
     */
    private static boolean containsUnicode(String text) {
        if (text == null)
            return false;
        for (char c : text.toCharArray()) {
            if (c > 0x7F && !(c >= 0xA0 && c <= 0xFF)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Draws a line with justified text (stretched to fill width)
     * Handles Unicode characters using appropriate fonts
     * 
     * @param contentStream The PDF content stream
     * @param words         List of words in the line
     * @param x             Starting X position
     * @param y             Y position
     * @param maxWidth      Maximum width to fill
     * @param fontSize      Font size
     * @param font          The font to use
     * @param spaceWidth    Width of a single space
     */
    private static void drawJustifiedLine(PDPageContentStream contentStream, java.util.List<String> words,
            float x, float y, float maxWidth, float fontSize,
            PDFont font, float spaceWidth) throws IOException {
        if (words.isEmpty())
            return;

        // Calculate total width of words
        float wordsWidth = 0;
        for (String word : words) {
            try {
                wordsWidth += font.getStringWidth(word) / 1000 * fontSize;
            } catch (Exception e) {
                // Estimate width for Unicode characters
                wordsWidth += word.length() * fontSize * 0.6f;
            }
        }

        // Calculate number of spaces needed
        int numSpaces = words.size() - 1;

        // Calculate extra space to distribute
        float extraSpace = maxWidth - wordsWidth;
        float spaceBetweenWords = numSpaces > 0 ? (extraSpace / numSpaces) + spaceWidth : 0;

        // Draw words with adjusted spacing
        float currentX = x;
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(currentX, y);

        for (int i = 0; i < words.size(); i++) {
            if (i > 0) {
                // End text, move position, begin text again
                contentStream.endText();
                currentX += spaceBetweenWords;
                contentStream.beginText();
                contentStream.setFont(font, fontSize);
                contentStream.newLineAtOffset(currentX, y);
            }
            try {
                contentStream.showText(words.get(i));
                currentX += font.getStringWidth(words.get(i)) / 1000 * fontSize;
            } catch (Exception e) {
                // Skip words that can't be rendered
                contentStream.endText();
                contentStream.beginText();
                contentStream.setFont(font, fontSize);
                contentStream.newLineAtOffset(currentX, y);
                // Estimate width for skipped word
                currentX += words.get(i).length() * fontSize * 0.6f;
            }
        }
        contentStream.endText();
    }

    /**
     * Generates and saves a death certificate PDF
     * 
     * @param cert The death certificate data
     * @return The absolute path to the generated PDF file
     * @throws IOException If PDF generation fails
     */
    public static String saveDeathCertificateHTML(Certificate cert) throws IOException {
        // Create certificates directory if it doesn't exist
        File certDir = new File(CERTIFICATES_DIR);
        if (!certDir.exists()) {
            certDir.mkdirs();
        }

        // Determine base file name
        String baseFileName = cert.getCertificateNo() != null && !cert.getCertificateNo().isEmpty()
                ? cert.getCertificateNo()
                : "death_cert_" + System.currentTimeMillis();

        // Generate and save PDF file
        File pdfFile = new File(certDir, baseFileName + ".pdf");
        try {
            generateDeathCertificatePDF(cert, pdfFile);
            // Verify PDF was successfully created
            if (pdfFile.exists() && pdfFile.length() > 0) {
                return pdfFile.getAbsolutePath();
            } else {
                throw new IOException("PDF file was not created properly");
            }
        } catch (Exception e) {
            // If PDF generation fails, log the error
            System.err.println("Warning: PDF generation failed for " + baseFileName);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            // Delete invalid PDF file if it exists
            if (pdfFile.exists()) {
                pdfFile.delete();
            }
            throw new IOException("Failed to generate PDF certificate: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a PDF certificate for death certificate with the format matching
     * the HTML template.
     * This uses a simple bordered design with centered title, name, justified
     * paragraphs and signature blocks.
     */
    private static void generateDeathCertificatePDF(Certificate cert, File outputFile) throws IOException {
        // Extract certificate data
        String name = cert.getName() != null ? cert.getName() : "";
        String parentName = cert.getParentName() != null ? cert.getParentName() : "";
        String address = cert.getAddress() != null ? cert.getAddress() : "";
        String thalook = cert.getThalook() != null ? cert.getThalook() : "";
        LocalDate dateOfDeath = cert.getDateOfDeath();
        String placeOfDeath = cert.getPlaceOfDeath() != null ? cert.getPlaceOfDeath() : "";
        String cause = cert.getCause() != null ? cert.getCause() : "";
        String certNo = cert.getCertificateNo() != null ? cert.getCertificateNo() : "";
        LocalDate issueDate = cert.getIssueDate() != null ? cert.getIssueDate() : LocalDate.now();

        String dateOfDeathStr = dateOfDeath != null ? dateOfDeath.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
                : "";
        String issueDateStr = issueDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));

        // Build paragraphs matching the HTML template
        String paragraph1 = "This is to certify that " + (name.isEmpty() ? "_____________________" : name) + ", "
                + (parentName.isEmpty() ? "" : "son/daughter of " + parentName + ", ")
                + (address.isEmpty() ? "" : "residing at " + address + " ")
                + (thalook.isEmpty() ? "" : "under " + thalook + ", ")
                + "passed away on " + (dateOfDeathStr.isEmpty() ? "_____________________" : dateOfDeathStr)
                + (placeOfDeath.isEmpty() ? "" : " at " + placeOfDeath) + ".";

        String paragraph2 = "The cause of death was recorded as "
                + (cause.isEmpty() ? "_____________________" : cause) + ". "
                + "The above details have been verified and registered as per the records maintained by the "
                + "Mahal Management System.";

        String paragraph3 = "This certificate is issued as an official record of death, bearing Certificate No: "
                + (certNo.isEmpty() ? "_____________________" : certNo) + " and Date of Issue: " + issueDateStr + ". "
                + "This document serves as valid proof for legal, administrative, and personal purposes.";

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            // Colors
            java.awt.Color borderColor = new java.awt.Color(0x16, 0x65, 0x34); // Dark green
            java.awt.Color titleColor = borderColor;
            java.awt.Color textColor = java.awt.Color.BLACK;

            // Layout
            float borderMargin = 40;
            float borderPaddingLeft = 20;
            float borderPaddingRight = 60;
            float pageW = PAGE_WIDTH;
            float pageH = PAGE_HEIGHT;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // Border
                cs.setStrokingColor(borderColor);
                cs.setLineWidth(3);
                cs.addRect(borderMargin, borderMargin, pageW - (borderMargin * 2), pageH - (borderMargin * 2));
                cs.stroke();

                // Title: DEATH CERTIFICATE
                float titleY = pageH - borderMargin - 20;
                cs.setFont(PDType1Font.TIMES_BOLD, 24);
                cs.setNonStrokingColor(titleColor);
                String title = "DEATH CERTIFICATE";
                float titleWidth = PDType1Font.TIMES_BOLD.getStringWidth(title) / 1000 * 24;
                cs.beginText();
                cs.newLineAtOffset((pageW - titleWidth) / 2, titleY);
                cs.showText(title);
                cs.endText();

                // Subtitle: "This certificate is respectfully presented in memory of"
                float subtitleY = titleY - 40;
                cs.setFont(PDType1Font.TIMES_ROMAN, 18);
                cs.setNonStrokingColor(textColor);
                String subtitle = "This certificate is respectfully presented in memory of";
                float subtitleWidth = PDType1Font.TIMES_ROMAN.getStringWidth(subtitle) / 1000 * 18;
                cs.beginText();
                cs.newLineAtOffset((pageW - subtitleWidth) / 2, subtitleY);
                cs.showText(subtitle);
                cs.endText();

                // Name in large bold centered text
                float nameY = subtitleY - 40;
                cs.setFont(PDType1Font.TIMES_BOLD, 26);
                cs.setNonStrokingColor(textColor);
                String displayName = name.isEmpty() ? "_____________________" : name;
                float nameWidth = PDType1Font.TIMES_BOLD.getStringWidth(displayName) / 1000 * 26;
                cs.beginText();
                cs.newLineAtOffset((pageW - nameWidth) / 2, nameY);
                cs.showText(displayName);
                cs.endText();

                // Main content paragraphs
                float startY = nameY - 50;
                float leftMargin = borderMargin + borderPaddingLeft;
                float rightMargin = pageW - borderMargin - borderPaddingRight;
                float lineHeight = 20;
                float currentY = startY;

                cs.setFont(PDType1Font.TIMES_ROMAN, 16);
                cs.setNonStrokingColor(textColor);

                // Paragraphs with justified text
                float maxWidth = rightMargin - leftMargin;
                float fontSize = 16;
                cs.setFont(PDType1Font.TIMES_ROMAN, fontSize);
                currentY = drawParagraph(cs, paragraph1, leftMargin, currentY, maxWidth, fontSize, lineHeight, doc);
                currentY -= 20;
                currentY = drawParagraph(cs, paragraph2, leftMargin, currentY, maxWidth, fontSize, lineHeight, doc);
                currentY -= 20;
                currentY = drawParagraph(cs, paragraph3, leftMargin, currentY, maxWidth, fontSize, lineHeight, doc);

                // Signatures section
                currentY -= 60;
                float sigY = currentY;
                float sigWidth = (rightMargin - leftMargin) / 3;

                cs.setFont(PDType1Font.TIMES_ROMAN, 16);

                // Authorized Signature
                cs.beginText();
                cs.newLineAtOffset(leftMargin, sigY);
                cs.showText("Authorized Signature");
                cs.endText();
                cs.setLineWidth(1);
                cs.setStrokingColor(java.awt.Color.BLACK);
                cs.moveTo(leftMargin, sigY - 5);
                cs.lineTo(leftMargin + sigWidth - 10, sigY - 5);
                cs.stroke();

                // Mahal Secretary
                cs.beginText();
                cs.newLineAtOffset(leftMargin + sigWidth, sigY);
                cs.showText("Mahal Secretary");
                cs.endText();
                cs.moveTo(leftMargin + sigWidth, sigY - 5);
                cs.lineTo(leftMargin + sigWidth * 2 - 10, sigY - 5);
                cs.stroke();

                // Qazi / Imam
                cs.beginText();
                cs.newLineAtOffset(leftMargin + sigWidth * 2, sigY);
                cs.showText("Qazi / Imam");
                cs.endText();
                cs.moveTo(leftMargin + sigWidth * 2, sigY - 5);
                cs.lineTo(rightMargin, sigY - 5);
                cs.stroke();

                // Footer: Issued by (Masjid / Mahal)
                sigY -= 40;
                cs.setFont(PDType1Font.TIMES_ROMAN, 16);
                cs.beginText();
                cs.newLineAtOffset(leftMargin, sigY);
                cs.showText("Issued by (Masjid / Mahal):");
                cs.endText();
                // Note: MAHAL_NAME would need to come from certificate or masjid data
                // For now, leaving a line for it
                cs.moveTo(leftMargin, sigY - 5);
                cs.lineTo(rightMargin, sigY - 5);
                cs.stroke();

                // Official Seal / Stamp
                float sealY = borderMargin + borderPaddingLeft;
                cs.setFont(PDType1Font.TIMES_ROMAN, 16);
                cs.beginText();
                cs.newLineAtOffset(leftMargin, sealY);
                cs.showText("Official Seal / Stamp");
                cs.endText();
            }

            doc.save(outputFile);
        }

        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new IOException("PDF file was not created or is empty");
        }
        byte[] bytes = java.nio.file.Files.readAllBytes(outputFile.toPath());
        if (bytes.length < 4 || !new String(bytes, 0, 4).equals("%PDF")) {
            throw new IOException("Generated file is not a valid PDF");
        }
    }

    /**
     * Generates and saves a Jamath certificate PDF
     * 
     * @param cert The Jamath certificate data
     * @return The absolute path to the generated PDF file
     * @throws IOException If PDF generation fails
     */
    public static String saveJamathCertificateHTML(Certificate cert) throws IOException {
        // Create certificates directory if it doesn't exist
        File certDir = new File(CERTIFICATES_DIR);
        if (!certDir.exists()) {
            certDir.mkdirs();
        }

        // Determine base file name
        String baseFileName = cert.getCertificateNo() != null && !cert.getCertificateNo().isEmpty()
                ? cert.getCertificateNo()
                : "jamath_cert_" + System.currentTimeMillis();

        // Generate and save PDF file
        File pdfFile = new File(certDir, baseFileName + ".pdf");
        try {
            generateJamathCertificatePDF(cert, pdfFile);
            // Verify PDF was successfully created
            if (pdfFile.exists() && pdfFile.length() > 0) {
                return pdfFile.getAbsolutePath();
            } else {
                throw new IOException("PDF file was not created properly");
            }
        } catch (Exception e) {
            // If PDF generation fails, log the error
            System.err.println("Warning: PDF generation failed for " + baseFileName);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            // Delete invalid PDF file if it exists
            if (pdfFile.exists()) {
                pdfFile.delete();
            }
            throw new IOException("Failed to generate PDF certificate: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a PDF certificate for Jamath certificate with the format matching
     * the HTML template.
     * This uses a simple bordered design with centered title, name, justified
     * paragraphs and signature blocks.
     */
    private static void generateJamathCertificatePDF(Certificate cert, File outputFile) throws IOException {
        // Extract certificate data
        String name = cert.getName() != null ? cert.getName() : "";
        String parentName = cert.getParentName() != null ? cert.getParentName() : "";
        String address = cert.getAddress() != null ? cert.getAddress() : "";
        String thalook = cert.getThalook() != null ? cert.getThalook() : "";
        LocalDate date = cert.getIssueDate() != null ? cert.getIssueDate() : LocalDate.now(); // Jamath uses issueDate
                                                                                              // as the date field
        String certNo = cert.getCertificateNo() != null ? cert.getCertificateNo() : "";

        String dateStr = date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));

        // Build paragraphs matching the HTML template
        String paragraph1 = "This is to certify that " + (name.isEmpty() ? "_____________________" : name) + ", "
                + (parentName.isEmpty() ? "" : "son/daughter of " + parentName + ", ")
                + (address.isEmpty() ? "" : "residing at " + address + " ")
                + (thalook.isEmpty() ? "" : "under " + thalook + ", ")
                + "is a registered member of the Jamath as per the records maintained by the "
                + "Mahal Management System.";

        String paragraph2 = "This certificate is issued on " + dateStr
                + " for official and administrative purposes.";

        String paragraph3 = "This certificate bears Certificate No: "
                + (certNo.isEmpty() ? "_____________________" : certNo)
                + " and is issued based on the verification of Jamath records.";

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            // Colors
            java.awt.Color borderColor = new java.awt.Color(0x16, 0x65, 0x34); // Dark green
            java.awt.Color titleColor = borderColor;
            java.awt.Color textColor = java.awt.Color.BLACK;

            // Layout
            float borderMargin = 40;
            float borderPaddingLeft = 20;
            float borderPaddingRight = 60;
            float pageW = PAGE_WIDTH;
            float pageH = PAGE_HEIGHT;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                // Border
                cs.setStrokingColor(borderColor);
                cs.setLineWidth(3);
                cs.addRect(borderMargin, borderMargin, pageW - (borderMargin * 2), pageH - (borderMargin * 2));
                cs.stroke();

                // Title: JAMATH CERTIFICATE
                float titleY = pageH - borderMargin - 20;
                cs.setFont(PDType1Font.TIMES_BOLD, 24);
                cs.setNonStrokingColor(titleColor);
                String title = "JAMATH CERTIFICATE";
                float titleWidth = PDType1Font.TIMES_BOLD.getStringWidth(title) / 1000 * 24;
                cs.beginText();
                cs.newLineAtOffset((pageW - titleWidth) / 2, titleY);
                cs.showText(title);
                cs.endText();

                // Subtitle: "This certificate is hereby issued to"
                float subtitleY = titleY - 40;
                cs.setFont(PDType1Font.TIMES_ROMAN, 18);
                cs.setNonStrokingColor(textColor);
                String subtitle = "This certificate is hereby issued to";
                float subtitleWidth = PDType1Font.TIMES_ROMAN.getStringWidth(subtitle) / 1000 * 18;
                cs.beginText();
                cs.newLineAtOffset((pageW - subtitleWidth) / 2, subtitleY);
                cs.showText(subtitle);
                cs.endText();

                // Name in large bold centered text
                float nameY = subtitleY - 40;
                cs.setFont(PDType1Font.TIMES_BOLD, 26);
                cs.setNonStrokingColor(textColor);
                String displayName = name.isEmpty() ? "_____________________" : name;
                float nameWidth = PDType1Font.TIMES_BOLD.getStringWidth(displayName) / 1000 * 26;
                cs.beginText();
                cs.newLineAtOffset((pageW - nameWidth) / 2, nameY);
                cs.showText(displayName);
                cs.endText();

                // Main content paragraphs
                float startY = nameY - 50;
                float leftMargin = borderMargin + borderPaddingLeft;
                float rightMargin = pageW - borderMargin - borderPaddingRight;
                float lineHeight = 20;
                float currentY = startY;

                cs.setFont(PDType1Font.TIMES_ROMAN, 16);
                cs.setNonStrokingColor(textColor);

                // Paragraphs with justified text
                float maxWidth = rightMargin - leftMargin;
                float fontSize = 16;
                cs.setFont(PDType1Font.TIMES_ROMAN, fontSize);
                currentY = drawParagraph(cs, paragraph1, leftMargin, currentY, maxWidth, fontSize, lineHeight, doc);
                currentY -= 20;
                currentY = drawParagraph(cs, paragraph2, leftMargin, currentY, maxWidth, fontSize, lineHeight, doc);
                currentY -= 20;
                currentY = drawParagraph(cs, paragraph3, leftMargin, currentY, maxWidth, fontSize, lineHeight, doc);

                // Signatures section
                currentY -= 60;
                float sigY = currentY;
                float sigWidth = (rightMargin - leftMargin) / 3;

                cs.setFont(PDType1Font.TIMES_ROMAN, 16);

                // Authorized Signature
                cs.beginText();
                cs.newLineAtOffset(leftMargin, sigY);
                cs.showText("Authorized Signature");
                cs.endText();
                cs.setLineWidth(1);
                cs.setStrokingColor(java.awt.Color.BLACK);
                cs.moveTo(leftMargin, sigY - 5);
                cs.lineTo(leftMargin + sigWidth - 10, sigY - 5);
                cs.stroke();

                // Jamath Secretary
                cs.beginText();
                cs.newLineAtOffset(leftMargin + sigWidth, sigY);
                cs.showText("Jamath Secretary");
                cs.endText();
                cs.moveTo(leftMargin + sigWidth, sigY - 5);
                cs.lineTo(leftMargin + sigWidth * 2 - 10, sigY - 5);
                cs.stroke();

                // President / Imam
                cs.beginText();
                cs.newLineAtOffset(leftMargin + sigWidth * 2, sigY);
                cs.showText("President / Imam");
                cs.endText();
                cs.moveTo(leftMargin + sigWidth * 2, sigY - 5);
                cs.lineTo(rightMargin, sigY - 5);
                cs.stroke();

                // Footer: Issued by (Masjid / Mahal)
                sigY -= 40;
                cs.setFont(PDType1Font.TIMES_ROMAN, 16);
                cs.beginText();
                cs.newLineAtOffset(leftMargin, sigY);
                cs.showText("Issued by (Masjid / Mahal):");
                cs.endText();
                // Note: MAHAL_NAME would need to come from certificate or masjid data
                // For now, leaving a line for it
                cs.moveTo(leftMargin, sigY - 5);
                cs.lineTo(rightMargin, sigY - 5);
                cs.stroke();

                // Official Seal / Stamp
                float sealY = borderMargin + borderPaddingLeft;
                cs.setFont(PDType1Font.TIMES_ROMAN, 16);
                cs.beginText();
                cs.newLineAtOffset(leftMargin, sealY);
                cs.showText("Official Seal / Stamp");
                cs.endText();
            }

            doc.save(outputFile);
        }

        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new IOException("PDF file was not created or is empty");
        }
        byte[] bytes = java.nio.file.Files.readAllBytes(outputFile.toPath());
        if (bytes.length < 4 || !new String(bytes, 0, 4).equals("%PDF")) {
            throw new IOException("Generated file is not a valid PDF");
        }
    }

    /**
     * Generates and saves a Custom certificate PDF from template content
     * 
     * @param cert The Custom certificate data with template content
     * @return The absolute path to the generated PDF file
     * @throws IOException If PDF generation fails
     */
    public static String saveCustomCertificateHTML(Certificate cert) throws IOException {
        // Create certificates directory if it doesn't exist
        File certDir = new File(CERTIFICATES_DIR);
        if (!certDir.exists()) {
            certDir.mkdirs();
        }

        // Determine base file name
        String baseFileName = cert.getCertificateNo() != null && !cert.getCertificateNo().isEmpty()
                ? cert.getCertificateNo()
                : "custom_cert_" + System.currentTimeMillis();

        // Generate and save PDF file
        File pdfFile = new File(certDir, baseFileName + ".pdf");
        try {
            generateCustomCertificatePDF(cert, pdfFile);
            // Verify PDF was successfully created
            if (pdfFile.exists() && pdfFile.length() > 0) {
                return pdfFile.getAbsolutePath();
            } else {
                throw new IOException("PDF file was not created properly");
            }
        } catch (Exception e) {
            // If PDF generation fails, log the error
            System.err.println("Warning: PDF generation failed for " + baseFileName);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            // Delete invalid PDF file if it exists
            if (pdfFile.exists()) {
                pdfFile.delete();
            }
            throw new IOException("Failed to generate PDF certificate: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a PDF certificate from custom template content (HTML/Text).
     * Parses HTML tags and renders content with appropriate formatting.
     */
    private static void generateCustomCertificatePDF(Certificate cert, File outputFile) throws IOException {
        String templateContent = cert.getTemplateContent() != null ? cert.getTemplateContent() : "";
        if (templateContent.isEmpty()) {
            throw new IOException("Template content is empty");
        }

        // Get template name and issue date
        String templateName = cert.getTemplateName() != null ? cert.getTemplateName() : "CERTIFICATE";
        LocalDate issueDate = cert.getIssueDate() != null ? cert.getIssueDate() : LocalDate.now();
        String issueDateStr = issueDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));

        // Extract text from HTML (remove HTML tags for basic rendering)
        // This is a simple HTML tag stripper - handles common tags
        String plainText = templateContent
                .replaceAll("<br\\s*/?>", "\n")
                .replaceAll("<p[^>]*>", "\n")
                .replaceAll("</p>", "\n\n")
                .replaceAll("<div[^>]*>", "\n")
                .replaceAll("</div>", "\n")
                .replaceAll("<h1[^>]*>", "\n\n")
                .replaceAll("</h1>", "\n\n")
                .replaceAll("<h2[^>]*>", "\n\n")
                .replaceAll("</h2>", "\n\n")
                .replaceAll("<h3[^>]*>", "\n\n")
                .replaceAll("</h3>", "\n\n")
                .replaceAll("<b[^>]*>", "")
                .replaceAll("</b>", "")
                .replaceAll("<strong[^>]*>", "")
                .replaceAll("</strong>", "")
                .replaceAll("<i[^>]*>", "")
                .replaceAll("</i>", "")
                .replaceAll("<em[^>]*>", "")
                .replaceAll("</em>", "")
                .replaceAll("<[^>]+>", "") // Remove all remaining HTML tags
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .trim();

        // Split into lines
        String[] lines = plainText.split("\n");

        try (PDDocument doc = new PDDocument()) {
            // Colors
            java.awt.Color borderColor = new java.awt.Color(0x16, 0x65, 0x34); // Dark green
            java.awt.Color titleColor = borderColor;
            java.awt.Color textColor = java.awt.Color.BLACK;

            // Layout
            float borderMargin = 40;
            float borderPaddingLeft = 20;
            float borderPaddingRight = 60;
            float pageW = PAGE_WIDTH;
            float pageH = PAGE_HEIGHT;
            float leftMargin = borderMargin + borderPaddingLeft;
            float rightMargin = pageW - borderMargin - borderPaddingRight;
            float maxWidth = rightMargin - leftMargin;
            float lineHeight = 18;
            float fontSize = 12;

            PDPage currentPage = new PDPage(PDRectangle.A4);
            doc.addPage(currentPage);
            PDPageContentStream cs = new PDPageContentStream(doc, currentPage);

            try {
                // Border
                cs.setStrokingColor(borderColor);
                cs.setLineWidth(3);
                cs.addRect(borderMargin, borderMargin, pageW - (borderMargin * 2), pageH - (borderMargin * 2));
                cs.stroke();

                // Title: Template Name (with Unicode support)
                float titleY = pageH - borderMargin - 20;
                PDFont titleFont = containsUnicode(templateName)
                        ? getUnicodeFont(doc, true)
                        : PDType1Font.TIMES_BOLD;
                cs.setFont(titleFont, 24);
                cs.setNonStrokingColor(titleColor);
                String titleText = templateName.toUpperCase();
                float titleWidth;
                try {
                    titleWidth = titleFont.getStringWidth(titleText) / 1000 * 24;
                } catch (Exception e) {
                    titleWidth = titleText.length() * 24 * 0.6f; // Estimate
                }
                cs.beginText();
                cs.setFont(titleFont, 24);
                cs.newLineAtOffset((pageW - titleWidth) / 2, titleY);
                try {
                    cs.showText(titleText);
                } catch (Exception e) {
                    System.err.println("Error rendering title text: " + e.getMessage());
                    e.printStackTrace();
                    // Try to render at least part of the text
                    try {
                        // Filter to ASCII only as fallback
                        String asciiTitle = titleText.replaceAll("[^\\x00-\\x7F]", "?");
                        cs.showText(asciiTitle);
                    } catch (Exception e2) {
                        System.err.println("Failed to render even ASCII title: " + e2.getMessage());
                    }
                }
                cs.endText();

                // Issue Date (right-aligned at top)
                float dateY = titleY - 25;
                PDFont dateFont = containsUnicode(issueDateStr)
                        ? getUnicodeFont(doc, false)
                        : PDType1Font.TIMES_ROMAN;
                cs.setFont(dateFont, 12);
                cs.setNonStrokingColor(textColor);
                String dateLabel = "Issued Date: " + issueDateStr;
                float dateWidth;
                try {
                    dateWidth = dateFont.getStringWidth(dateLabel) / 1000 * 12;
                } catch (Exception e) {
                    dateWidth = dateLabel.length() * 12 * 0.6f; // Estimate
                }
                cs.beginText();
                cs.setFont(dateFont, 12);
                cs.newLineAtOffset(rightMargin - dateWidth, dateY);
                try {
                    cs.showText(dateLabel);
                } catch (Exception e) {
                    System.err.println("Error rendering date label: " + e.getMessage());
                    e.printStackTrace();
                }
                cs.endText();

                // Content area - start below title and date
                float currentY = dateY - 30;

                // Render each line with Unicode support
                for (String line : lines) {
                    if (line.trim().isEmpty()) {
                        currentY -= lineHeight;
                        continue;
                    }

                    // Check if we need a new page
                    if (currentY < borderMargin + 40) {
                        cs.close();

                        // Add new page
                        currentPage = new PDPage(PDRectangle.A4);
                        doc.addPage(currentPage);

                        // Draw border on new page
                        PDPageContentStream borderCs = new PDPageContentStream(doc, currentPage);
                        borderCs.setStrokingColor(borderColor);
                        borderCs.setLineWidth(3);
                        borderCs.addRect(borderMargin, borderMargin, pageW - (borderMargin * 2),
                                pageH - (borderMargin * 2));
                        borderCs.stroke();
                        borderCs.close();

                        // Start new content stream
                        cs = new PDPageContentStream(doc, currentPage);
                        cs.setNonStrokingColor(textColor);
                        currentY = pageH - borderMargin - 40;
                    }

                    // Word wrap and render line with Unicode support
                    currentY = drawParagraph(cs, line, leftMargin, currentY, maxWidth, fontSize, lineHeight, doc);
                    currentY -= lineHeight;
                }
            } finally {
                if (cs != null) {
                    cs.close();
                }
            }

            doc.save(outputFile);
        }

        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new IOException("PDF file was not created or is empty");
        }
        byte[] bytes = java.nio.file.Files.readAllBytes(outputFile.toPath());
        if (bytes.length < 4 || !new String(bytes, 0, 4).equals("%PDF")) {
            throw new IOException("Generated file is not a valid PDF");
        }
    }
}
