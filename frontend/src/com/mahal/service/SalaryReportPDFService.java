package com.mahal.service;

import com.mahal.model.StaffSalary;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.awt.Color;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class SalaryReportPDFService {

    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float MARGIN = 40;
    private static final float ROW_HEIGHT = 20;
    private static final float FONT_SIZE_HEADER = 9;
    private static final float FONT_SIZE_DATA = 8.5f;

    public static void generateSalaryReport(List<StaffSalary> salaries, LocalDate from, LocalDate to, String staffName,
            File outputFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream cs = new PDPageContentStream(document, page);

            float currentY = PAGE_HEIGHT - MARGIN;

            // Brand Header
            cs.setFont(PDType1Font.HELVETICA_BOLD, 20);
            cs.setNonStrokingColor(new Color(0x05, 0x96, 0x69)); // Emerald 600
            cs.beginText();
            cs.newLineAtOffset(MARGIN, currentY);
            cs.showText("Digital Mahal");
            cs.endText();

            currentY -= 25;
            cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
            cs.setNonStrokingColor(Color.BLACK);
            cs.beginText();
            cs.newLineAtOffset(MARGIN, currentY);
            cs.showText("Staff Salary Report");
            cs.endText();

            currentY -= 15;
            // Report Info Bar
            cs.setFont(PDType1Font.HELVETICA, 9);
            cs.setNonStrokingColor(new Color(107, 114, 128)); // Slate 500
            cs.beginText();
            cs.newLineAtOffset(MARGIN, currentY);
            String info = "Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            if (from != null && to != null) {
                info += "  |  Period: " + from.format(DateTimeFormatter.ofPattern("dd/MM/yy")) + " - "
                        + to.format(DateTimeFormatter.ofPattern("dd/MM/yy"));
            }
            if (staffName != null && !staffName.isEmpty() && !staffName.equals("All Staff")) {
                info += "  |  Filter: " + staffName;
            }
            cs.showText(info);
            cs.endText();

            currentY -= 30;

            // Table Column Widths (Total: 515)
            float[] colWidths = { 110, 80, 50, 60, 50, 45, 45, 75 };
            String[] headers = { "STAFF NAME", "DESIGNATION", "SALARY", "PAID DATE", "PAID", "BAL.", "MODE",
                    "REMARKS" };

            // Draw Header Row
            drawRow(cs, headers, colWidths, MARGIN, currentY, true, false);
            currentY -= ROW_HEIGHT;

            // Data Rows
            boolean alternate = false;
            for (StaffSalary sal : salaries) {
                if (currentY < MARGIN + ROW_HEIGHT) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    cs = new PDPageContentStream(document, page);
                    currentY = PAGE_HEIGHT - MARGIN;
                    drawRow(cs, headers, colWidths, MARGIN, currentY, true, false);
                    currentY -= ROW_HEIGHT;
                }

                String[] rowData = {
                        truncate(sal.getStaffName(), 25),
                        truncate(sal.getDesignation(), 18),
                        formatBigDecimal(sal.getSalary()),
                        sal.getPaidDate() != null ? sal.getPaidDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                                : "-",
                        formatBigDecimal(sal.getPaidAmount()),
                        formatBigDecimal(sal.getBalance()),
                        truncate(sal.getPaymentMode(), 10),
                        truncate(sal.getRemarks(), 20)
                };

                drawRow(cs, rowData, colWidths, MARGIN, currentY, false, alternate);
                currentY -= ROW_HEIGHT;
                alternate = !alternate;
            }

            cs.close();
            document.save(outputFile);
        }
    }

    private static void drawRow(PDPageContentStream cs, String[] data, float[] widths, float x, float y,
            boolean isHeader, boolean isShaded) throws IOException {
        // Draw background
        if (isHeader) {
            cs.setNonStrokingColor(new Color(243, 244, 246)); // Slate 100
            cs.addRect(x, y - 5, PAGE_WIDTH - (MARGIN * 2), ROW_HEIGHT);
            cs.fill();
        } else if (isShaded) {
            cs.setNonStrokingColor(new Color(249, 250, 251)); // Slate 50
            cs.addRect(x, y - 5, PAGE_WIDTH - (MARGIN * 2), ROW_HEIGHT);
            cs.fill();
        }

        // Draw top border for header or bottom border for all
        cs.setStrokingColor(new Color(209, 213, 219)); // Slate 300
        cs.setLineWidth(0.5f);
        if (isHeader) {
            cs.moveTo(x, y + ROW_HEIGHT - 5);
            cs.lineTo(PAGE_WIDTH - MARGIN, y + ROW_HEIGHT - 5);
            cs.stroke();
        }
        cs.moveTo(x, y - 5);
        cs.lineTo(PAGE_WIDTH - MARGIN, y - 5);
        cs.stroke();

        // Draw text
        float currentX = x + 5;
        cs.setNonStrokingColor(isHeader ? Color.BLACK : new Color(31, 41, 55)); // Gray 800
        cs.setFont(isHeader ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA,
                isHeader ? FONT_SIZE_HEADER : FONT_SIZE_DATA);

        for (int i = 0; i < data.length; i++) {
            cs.beginText();
            cs.newLineAtOffset(currentX, y);
            cs.showText(data[i]);
            cs.endText();
            currentX += widths[i];
        }
    }

    private static String formatBigDecimal(BigDecimal val) {
        if (val == null)
            return "0.00";
        return val.setScale(2, RoundingMode.HALF_UP).toString();
    }

    private static String truncate(String text, int maxLen) {
        if (text == null)
            return "";
        if (text.length() <= maxLen)
            return text;
        return text.substring(0, maxLen - 3) + "...";
    }
}
