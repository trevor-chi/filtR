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
        List<String> columns = Arrays.asList(headers);
        List<Map<String, Object>> rows = new ArrayList<>();
        
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            
            // Skip blank lines
            if (line.isEmpty()) continue;
            
            // Handle quoted fields properly (so commas inside quotes don’t break parsing)
            List<String> valuesList = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;
            
            for (char c : line.toCharArray()) {
                if (c == '"') {
                    inQuotes = !inQuotes;
                } else if (c == ',' && !inQuotes) {
                    String cell = current.toString().trim();
                    if (cell.equalsIgnoreCase("NULL") || cell.equals("")) {
                        valuesList.add(null);
                    } else {
                        valuesList.add(cell);
                    }
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
            String cell = current.toString().trim();
            if (cell.equalsIgnoreCase("NULL") || cell.equals("")) {
                valuesList.add(null);
            } else {
                valuesList.add(cell);
            }
            
            String[] values = valuesList.toArray(new String[0]);
            
            // If this row doesn't match the header length, skip or warn
            if (values.length != headers.length) {
                System.err.println("Skipping malformed line " + (i + 1) + ": " + line);
                continue;
            }
            
            Map<String, Object> row = new HashMap<>();
            for (int j = 0; j < headers.length; j++) {
                Object value = values[j]; // may be null
row.put(headers[j].trim(), value);
            }
            rows.add(row);
        }
        
        return new Dataset(columns, rows);
    }
    
    public static Dataset loadJSON(String path) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(path)));
        JSONArray jsonArray = new JSONArray(content);
        
        if (jsonArray.isEmpty()) throw new IOException("Empty JSON array.");
        
        // Collect columns dynamically
        JSONObject first = jsonArray.getJSONObject(0);
        List<String> columns = new ArrayList<>(first.keySet());
        List<Map<String, Object>> rows = new ArrayList<>();
        
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            Map<String, Object> row = new HashMap<>();
            for (String key : columns) {
                row.put(key, obj.opt(key));
            }
            rows.add(row);
        }
        
        return new Dataset(columns, rows);
    }
}
