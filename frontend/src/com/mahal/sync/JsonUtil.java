package com.mahal.sync;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Simple JSON utility for converting objects to JSON strings without external dependencies.
 */
public class JsonUtil {
    
    /**
     * Convert an object to JSON string.
     * Handles common types: String, Number, Boolean, LocalDate, Collections, Maps, null.
     */
    public static String toJson(Object obj) {
        return toJson(obj, new IdentityHashMap<>());
    }
    
    private static String toJson(Object obj, Map<Object, Boolean> visited) {
        if (obj == null) {
            return "null";
        }
        
        // Handle primitive and simple types first (no recursion risk)
        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }
        
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        
        if (obj instanceof LocalDate || obj instanceof LocalDateTime || obj instanceof LocalTime) {
            return "\"" + obj.toString() + "\"";
        }
        
        if (obj instanceof Enum) {
            return "\"" + ((Enum<?>) obj).name() + "\"";
        }
        
        // Check for circular reference for complex objects
        if (visited.containsKey(obj)) {
            // Already processing this object - return a placeholder to avoid infinite recursion
            // Try to include ID if available for reference
            try {
                Field idField = obj.getClass().getDeclaredField("id");
                idField.setAccessible(true);
                Object id = idField.get(obj);
                if (id != null) {
                    return "{\"__ref\":" + id + "}";
                }
            } catch (Exception e) {
                // No ID field or can't access it
            }
            return "{\"__circular\":true}";
        }
        
        // Mark as being processed
        visited.put(obj, Boolean.TRUE);
        
        try {
            // Handle JSONObject specially - it implements Map but should be serialized as JSONObject
            // Check for org.json.JSONObject specifically before checking for Map
            if (obj.getClass().getName().equals("org.json.JSONObject")) {
                return obj.toString(); // JSONObject already has proper toString() method
            }
            
            if (obj instanceof Collection) {
                return collectionToJson((Collection<?>) obj, visited);
        }
        
        if (obj instanceof Map) {
                return mapToJson((Map<?, ?>) obj, visited);
        }
        
        // For other objects, use reflection
            return objectToJson(obj, visited);
        } finally {
            // Remove from visited set after processing is complete
            visited.remove(obj);
        }
    }
    
    private static String collectionToJson(Collection<?> collection, Map<Object, Boolean> visited) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : collection) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(toJson(item, visited));
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static String mapToJson(Map<?, ?> map, Map<Object, Boolean> visited) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            String key = entry.getKey() != null ? entry.getKey().toString() : "null";
            sb.append("\"").append(escapeJson(key)).append("\":");
            sb.append(toJson(entry.getValue(), visited));
        }
        sb.append("}");
        return sb.toString();
    }
    
    private static String objectToJson(Object obj, Map<Object, Boolean> visited) {
        StringBuilder sb = new StringBuilder("{");
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        boolean first = true;
        
        for (Field field : fields) {
            // Skip static and synthetic fields
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || 
                field.isSynthetic()) {
                continue;
            }
            
            try {
                field.setAccessible(true);
                String fieldName = field.getName();
                
                // Skip display/computed fields that aren't in the database schema
                // Fields ending with "Name" (except "name" itself) are typically display-only
                if (shouldSkipField(fieldName, obj)) {
                    continue;
                }
                
                Object value = field.get(obj);
                
                if (value != null) {
                    if (!first) {
                        sb.append(",");
                    }
                    first = false;
                    // Convert Java camelCase field names to snake_case for Supabase
                    // Handle special cases for field name mapping
                    String dbColumnName = getDbColumnName(fieldName, obj);
                    sb.append("\"").append(dbColumnName).append("\":");
                    sb.append(toJson(value, visited));
                }
            } catch (IllegalAccessException | RuntimeException e) {
                // Skip fields we can't access or that cause errors
            }
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * Determine if a field should be skipped during serialization.
     * Skips display-only/computed fields that aren't in the database schema.
     */
    private static boolean shouldSkipField(String fieldName, Object obj) {
        // Skip display fields from JOINs, but keep actual database fields
        // Context-aware checking: some fields are real DB fields in some models but display fields in others
        
        // masjidName - always a JOIN field (exists in Committee, Event, DueCollection, etc.)
        if ("masjidName".equals(fieldName)) {
            return true;
        }
        
        // memberName - REAL field in Committee (committees.member_name - REQUIRED)
        //              but JOIN field in DueCollection, Income (display only)
        if ("memberName".equals(fieldName)) {
            if (obj != null) {
                String className = obj.getClass().getSimpleName();
                if ("DueCollection".equals(className) || "Income".equals(className)) {
                    return true; // Skip for DueCollection and Income (JOIN fields)
                }
            }
            // Keep for Committee where it's a real database field
            return false;
        }
        
        // dueTypeName - always a JOIN field (exists in DueCollection)
        if ("dueTypeName".equals(fieldName)) {
            return true;
        }
        
        // staffName - always a JOIN field (exists in StaffSalary)
        if ("staffName".equals(fieldName)) {
            return true;
        }
        
        // inventoryItemName - always a JOIN field (exists in RentItem, DamagedItem)
        if ("inventoryItemName".equals(fieldName)) {
            return true;
        }
        
        // incomeTypeName - always a JOIN field (exists in Income)
        if ("incomeTypeName".equals(fieldName)) {
            return true;
        }
        
        // rentItemName - always a JOIN field (exists in Rent)
        if ("rentItemName".equals(fieldName)) {
            return true;
        }
        
        // These are actual DB fields and should NOT be skipped:
        // - itemName (inventory_items.item_name) - REQUIRED field
        // - eventName (events.event_name) - REQUIRED field
        // - dueName (due_types.due_name) - REQUIRED field
        // So we don't skip them!
        
        // Skip "designation" field in StaffSalary (it's a JOIN field from staff table, not stored in staff_salaries)
        // BUT keep it for Committee where it's a real database field (committees.designation)
        if ("designation".equals(fieldName) && obj != null && obj.getClass().getSimpleName().equals("StaffSalary")) {
            return true; // Skip designation only for StaffSalary, not for Committee
        }
        
        // Skip "type" field in Certificate (it's metadata used to determine table, not a DB column)
        if (fieldName.equals("type")) {
            return true;
        }
        
        // Special handling for Certificate.issueDate - skip for Marriage certificates (field doesn't exist)
        if ("issueDate".equals(fieldName) && obj != null && obj.getClass().getSimpleName().equals("Certificate")) {
            try {
                Field typeField = obj.getClass().getDeclaredField("type");
                typeField.setAccessible(true);
                String certType = (String) typeField.get(obj);
                if ("Marriage".equals(certType)) {
                    return true; // Marriage certificates don't have issueDate field
                }
            } catch (Exception e) {
                // If we can't determine type, don't skip it
            }
        }
        
        // Skip "address" field in DueCollection (it's a JOIN field from members table, not stored in due_collections)
        // Note: "address" exists in other models (Staff, Member) where it IS in the schema, so we only skip for DueCollection
        if ("address".equals(fieldName) && obj != null && obj.getClass().getSimpleName().equals("DueCollection")) {
            return true; // DueCollection.address comes from JOIN, not stored in table
        }
        
        return false;
    }
    
    /**
     * Get the database column name for a Java field name.
     * Handles special cases and converts camelCase to snake_case.
     */
    private static String getDbColumnName(String fieldName, Object obj) {
        // Special handling for Certificate.issueDate field
        // It maps to different columns depending on certificate type:
        // - Death/Custom: issued_date
        // - Jamath: date
        // - Marriage: doesn't exist (should be skipped, but we check type field)
        if ("issueDate".equals(fieldName) && obj.getClass().getSimpleName().equals("Certificate")) {
            try {
                // Get the type field to determine the certificate type
                Field typeField = obj.getClass().getDeclaredField("type");
                typeField.setAccessible(true);
                String certType = (String) typeField.get(obj);
                
                if ("Death".equals(certType) || "Custom".equals(certType)) {
                    return "issued_date"; // Death and Custom use issued_date
                } else if ("Jamath".equals(certType)) {
                    return "date"; // Jamath uses date
                } else {
                    // Marriage doesn't have issueDate, but if it's present, skip it
                    // Actually, we can't skip here, so return a value that will cause an error
                    // and we'll need to skip it in shouldSkipField for Marriage type
                    return "issued_date"; // Default fallback
                }
            } catch (Exception e) {
                // If we can't determine type, use default conversion
                return camelToSnakeCase(fieldName);
            }
        }
        
        // Standard conversion for all other fields
        return camelToSnakeCase(fieldName);
    }
    
    /**
     * Convert camelCase to snake_case for database column names.
     * Examples: itemName -> item_name, incomeTypeId -> income_type_id
     */
    private static String camelToSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
    
    private static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
