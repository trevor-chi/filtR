package filtr.dataset;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;

public class DatasetLoader {
    
    public static Dataset load(String path) throws IOException {
        if (path.endsWith(".csv")) {
            System.out.println("Loading CSV dataset from: " + path);
            return loadCSV(path);
        } else if (path.endsWith(".json")) {
            System.out.println("Loading JSON dataset from: " + path);
            return loadJSON(path);
        } else {
            throw new IOException("Only CSV and JSON files are supported: " + path);
        }
    }
    
    public static Dataset loadCSV(String path) throws IOException {
    List<String> lines = Files.readAllLines(Paths.get(path));
    if (lines.isEmpty()) throw new IOException("Empty CSV file.");

    // Read header row
    String[] headers = lines.get(0).split(",");
    List<String> columns = new ArrayList<>();
    columns.add("filtrID");                  // Built-in column
    columns.addAll(Arrays.asList(headers));  // Original columns

    List<Map<String, Object>> rows = new ArrayList<>();

    for (int i = 1; i < lines.size(); i++) {
        String line = lines.get(i).trim();
        if (line.isEmpty()) continue;

        // --- QUOTED FIELD PARSER ---
        List<String> valuesList = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                valuesList.add(cleanCell(current.toString()));
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        valuesList.add(cleanCell(current.toString()));

        String[] values = valuesList.toArray(new String[0]);
        if (values.length != headers.length) {
            System.err.println("Skipping malformed line " + (i + 1) + ": " + line);
            continue;
        }

        Map<String, Object> row = new LinkedHashMap<>();

        // ---- Built-in filtrID ----
        row.put("filtrID", i);  // row number (starting at 1 for first data row)

        // ---- Original columns ----
        for (int j = 0; j < headers.length; j++) {
            row.put(headers[j].trim(), inferType(values[j]));
        }

        rows.add(row);
    }

    return new Dataset(columns, rows);
}


/** Normalize and handle NULL/blank convention. */
private static String cleanCell(String cell) {
    cell = cell.trim();
    if (cell.equalsIgnoreCase("NULL") || cell.isEmpty()) {
        return null;
    }
    return cell;
}

/**
 * Attempts to convert a cell into a real datatype.
 * Order matters: most specific → least specific.
 */
private static Object inferType(String raw) {
    if (raw == null) return null;

    // Try Integer
    try {
        return Integer.parseInt(raw);
    } catch (NumberFormatException ignore) {}

    // Try Double
    try {
        return Double.parseDouble(raw);
    } catch (NumberFormatException ignore) {}

    // Try Boolean
    if (raw.equalsIgnoreCase("true")) return true;
    if (raw.equalsIgnoreCase("false")) return false;

    // Try LocalDate (ISO-8601)
    try {
        return java.time.LocalDate.parse(raw);
    } catch (Exception ignore) {}

    // Fallback to String
    return raw;
}

    
    public static Dataset loadJSON(String path) throws IOException {
    String content = new String(Files.readAllBytes(Paths.get(path)));
    JSONArray jsonArray = new JSONArray(content);

    if (jsonArray.isEmpty()) throw new IOException("Empty JSON array.");

    // Extract original column names from first JSON object
    JSONObject first = jsonArray.getJSONObject(0);

    List<String> columns = new ArrayList<>();
    columns.add("filtrID");                    // Built-in ID column
    columns.addAll(first.keySet());            // Existing fields

    List<Map<String, Object>> rows = new ArrayList<>();

    for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject obj = jsonArray.getJSONObject(i);
        Map<String, Object> row = new LinkedHashMap<>();

        // ---- Built-in filtrID ----
        row.put("filtrID", i + 1);

        // ---- Original fields ----
        for (String key : first.keySet()) {
            Object raw = obj.opt(key);
            row.put(key, normalizeJSONValue(raw));
        }

        rows.add(row);
    }

    return new Dataset(columns, rows);
}


/**
 * Normalize JSON values into real Java types:
 *
 * - JSONObject.NULL  -> null
 * - Integer numbers  -> Integer
 * - Double numbers   -> Double
 * - Boolean          -> Boolean
 * - Strings          -> String, Integer, Double, Boolean, LocalDate if detected
 */
private static Object normalizeJSONValue(Object raw) {
    if (raw == null || raw == JSONObject.NULL) {
        return null;
    }

    // JSON returns numbers as either Integer or Double depending on content
    if (raw instanceof Integer) return raw;
    if (raw instanceof Long) return ((Long) raw).intValue(); // normalize to Integer
    if (raw instanceof Double) {
        Double d = (Double) raw;
        
        // Convert whole-number doubles to Integers (e.g., 21.0 → 21)
        if (d % 1 == 0) return d.intValue();
        return d;
    }

    if (raw instanceof Boolean) return raw;

    // Strings might actually represent other datatypes
    if (raw instanceof String) {
        String s = ((String) raw).trim();
        return inferTypeFromString(s);
    }

    // Fallback: leave as-is
    return raw;
}

private static Object inferTypeFromString(String raw) {
    if (raw.equalsIgnoreCase("NULL") || raw.isEmpty()) {
        return null;
    }

    // Try Integer
    try {
        return Integer.parseInt(raw);
    } catch (NumberFormatException ignore) {}

    // Try Double
    try {
        return Double.parseDouble(raw);
    } catch (NumberFormatException ignore) {}

    // Try Boolean
    if (raw.equalsIgnoreCase("true")) return true;
    if (raw.equalsIgnoreCase("false")) return false;

    // Try LocalDate (ISO-8601)
    try {
        return java.time.LocalDate.parse(raw);
    } catch (Exception ignore) {}

    // Fallback
    return raw;
}

}
