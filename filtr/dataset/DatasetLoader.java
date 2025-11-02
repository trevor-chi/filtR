package filtr.dataset;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;

public class DatasetLoader {

    public static Dataset load(String path) throws IOException {
        if (path.endsWith(".csv")) {
            return loadCSV(path);
        } else if (path.endsWith(".json")) {
            return loadJSON(path);
        } else {
            throw new IOException("Unsupported file format: " + path);
        }
    }

    private static Dataset loadCSV(String path) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path));
        if (lines.isEmpty()) throw new IOException("Empty CSV file.");

        String[] headers = lines.get(0).split(",");
        List<String> columns = Arrays.asList(headers);
        List<Map<String, Object>> rows = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String[] values = lines.get(i).split(",");
            Map<String, Object> row = new HashMap<>();
            for (int j = 0; j < headers.length; j++) {
                row.put(headers[j].trim(), values[j].trim());
            }
            rows.add(row);
        }

        return new Dataset(columns, rows);
    }

    private static Dataset loadJSON(String path) throws IOException {
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
