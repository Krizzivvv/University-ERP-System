package edu.univ.erp.util;

public final class CsvUtil {
    private CsvUtil() {}
    
    /**
     * Convert null to empty string
     */
    public static String safe(Object obj) {
        return obj == null ? "" : String.valueOf(obj);
    }
    
    /**
     * Escape CSV special characters
     */
    public static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}