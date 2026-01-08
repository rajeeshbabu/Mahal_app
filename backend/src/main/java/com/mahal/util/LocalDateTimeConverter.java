package com.mahal.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Converter to ensure LocalDateTime is stored in SQLite as a string
 * with format yyyy-MM-dd HH:mm:ss.SSS
 */
@Converter(autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public String convertToDatabaseColumn(LocalDateTime attribute) {
        if (attribute == null) {
            return null;
        }
        String formatted = attribute.format(FORMATTER);
        System.out.println("💾 Converting LocalDateTime to DB String: " + formatted);
        return formatted;
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty() || "null".equals(dbData)) {
            return null;
        }
        try {
            // Handle cases where SQLite might have space or T
            String cleanData = dbData.replace("T", " ");
            if (cleanData.contains("+")) {
                cleanData = cleanData.substring(0, cleanData.indexOf("+"));
            }
            if (cleanData.endsWith("Z")) {
                cleanData = cleanData.substring(0, cleanData.length() - 1);
            }

            // If it's just YYYY-MM-DD
            if (cleanData.length() == 10) {
                cleanData += " 00:00:00.000";
            }

            // Add padding for milliseconds if missing
            if (!cleanData.contains(".")) {
                cleanData += ".000";
            } else {
                int dotIdx = cleanData.indexOf(".");
                String ms = cleanData.substring(dotIdx + 1);
                if (ms.length() < 3) {
                    cleanData = cleanData.substring(0, dotIdx + 1) + String.format("%-3s", ms).replace(' ', '0');
                } else if (ms.length() > 3) {
                    cleanData = cleanData.substring(0, dotIdx + 4);
                }
            }

            return LocalDateTime.parse(cleanData, FORMATTER);
        } catch (Exception e) {
            System.err.println("⚠️ Error parsing LocalDateTime from DB: " + dbData + " - " + e.getMessage());
            try {
                return LocalDateTime.parse(dbData);
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
