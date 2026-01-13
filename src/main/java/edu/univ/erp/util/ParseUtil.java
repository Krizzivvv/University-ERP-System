package edu.univ.erp.util;

public final class ParseUtil {
    private ParseUtil() {}
    
    /**
     * Safely parse object to int
     */
    public static int toInt(Object obj, int defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(String.valueOf(obj).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public static int toInt(Object obj) {
        return toInt(obj, 0);
    }
    
    /**
     * Safely parse object to double
     */
    public static double toDouble(Object obj, double defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(obj).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static Number toNumber(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return (Number) obj;
        
        String s = String.valueOf(obj).trim();
        if (s.isEmpty()) return null;
        s = s.replace(",", "");
        
        try {
            if (!s.contains(".")) {
                return Long.parseLong(s);
            }
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}