package filtr.dataset;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Dataset {
    private List<String> columns;
    private List<Map<String, Object>> rows;
    
    public Dataset(List<String> columns, List<Map<String, Object>> rows) {
        this.columns = new ArrayList<>(columns);
        this.rows = rows;
    }
    
    public List<String> getColumns() {
        return columns;
    }
    
    public List<Map<String, Object>> getRows() {
        return rows;
    }
    
    public int size() {
        return rows.size();
    }
    
    public void renameColumn(String oldName, String newName) {
        
        if (!columns.contains(oldName)) {
            throw new IllegalArgumentException("Column " + oldName + " does not exist.");
        }
        if (columns.contains(newName)) {
            throw new IllegalArgumentException("Column " + newName + " already exists.");
        }
        
        // Update column list
        int index = columns.indexOf(oldName);
        columns.set(index, newName);
        
        // Update each row
        for (Map<String, Object> row : rows) {
            Object value = row.remove(oldName);
            row.put(newName, value);
        }
    }
    
    public void viewDataset() {
        if (rows.isEmpty()) {
            System.out.println("No data in dataset.");
            return;
        }
        
        // Determine the maximum width for each column
        Map<String, Integer> columnWidths = new LinkedHashMap<>();
        for (String col : columns) {
            int maxWidth = col.length();
            for (Map<String, Object> row : rows) {
                Object value = row.get(col);
                if (value != null) {
                    maxWidth = Math.max(maxWidth, value.toString().length());
                } else {
                    maxWidth = Math.max(maxWidth, 4); // length of "NULL"
                }
            }
            columnWidths.put(col, maxWidth);
        }
        
        // Build header
        StringBuilder header = new StringBuilder("| ");
        for (String col : columns) {
            header.append(String.format("%-" + columnWidths.get(col) + "s | ", col));
        }
        System.out.println(header);
        
        // Build separator
        StringBuilder separator = new StringBuilder("|");
        for (String col : columns) {
            int width = columnWidths.get(col);
            separator.append("-".repeat(width + 2)).append('|');
        }
        System.out.println(separator);
        
        // Build rows
        for (Map<String, Object> row : rows) {
            StringBuilder line = new StringBuilder("| ");
            for (String col : columns) {
                Object value = row.get(col);
                String text = (value == null) ? "NULL" : value.toString();
                line.append(String.format("%-" + columnWidths.get(col) + "s | ", text));
            }
            System.out.println(line);
        }
    }
    
    
    public Dataset filterDataset(String columnName, String operator, Object value) {
        if (!columns.contains(columnName)) {
            throw new IllegalArgumentException("Column " + columnName + " does not exist.");
        }
        
        List<Map<String, Object>> filteredRows = new ArrayList<>();
        
        for (Map<String, Object> row : rows) {
            Object cellValue = row.get(columnName);
            if (cellValue == null) continue;
            
            // Normalize both to same type if possible
            Object left = coerce(cellValue);
            Object right = coerce(value);
            
            int cmp = compareValues(left, right);
            
            boolean keep = switch (operator) {
                case "==" -> Objects.equals(left, right);
                case "!=" -> !Objects.equals(left, right);
                case "<"  -> cmp < 0;
                case "<=" -> cmp <= 0;
                case ">"  -> cmp > 0;
                case ">=" -> cmp >= 0;
                default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
            };
            
            if (keep) filteredRows.add(row);
        }
        
        return new Dataset(new ArrayList<>(columns), filteredRows);
    }
    
    private Object coerce(Object v) {
        if (v instanceof Number) return v;
        if (v instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return s; // keep as string
            }
        }
        return v;
    }
    
    // @SuppressWarnings({"rawtypes", "unchecked"})
    private int compareValues(Object a, Object b) {
        if (a instanceof Comparable && a.getClass().isInstance(b)) {
            return ((Comparable)a).compareTo(b);
        }
        if (a instanceof Number && b instanceof Number) {
            double diff = ((Number)a).doubleValue() - ((Number)b).doubleValue();
            return (diff < 0) ? -1 : (diff > 0 ? 1 : 0);
        }
        return a.toString().compareTo(b.toString());
    }
    
    public void exportDataset(String path, String name, String format) throws java.io.IOException {
        path = path.replaceAll("^\"|\"$", ""); // remove surrounding quotes
        path = path + "/" + "filtr" + name + "." + format;
        System.out.println("Exporting dataset to: " + path);
        format = format.trim().toLowerCase();
        
        switch (format) {
            case "csv":
            exportAsCSV(path);
            break;
            case "json":
            exportAsJSON(path);
            break;
            default:
            throw new IllegalArgumentException("Unsupported format: " + format + ". Use 'csv' or 'json'.");
        }
    }
    
    private void exportAsCSV(String path) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            // Write header
            writer.write(String.join(",", columns));
            writer.newLine();
            
            // Write rows
            for (Map<String, Object> row : rows) {
                List<String> values = new ArrayList<>();
                for (String col : columns) {
                    Object value = row.get(col);
                    String str = (value != null ? value.toString() : "");
                    
                    // Handle commas and quotes in CSV fields
                    if (str.contains(",") || str.contains("\"")) {
                        str = "\"" + str.replace("\"", "\"\"") + "\"";
                    }
                    values.add(str);
                }
                writer.write(String.join(",", values));
                writer.newLine();
            }
        }
    }
    
    private void exportAsJSON(String path) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write("[\n");
            
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                writer.write("  {");
                int j = 0;
                for (String col : columns) {
                    Object value = row.get(col);
                    String strValue = (value != null ? value.toString() : "");
                    strValue = strValue.replace("\"", "\\\""); // escape quotes
                    
                    writer.write("\"" + col + "\": \"" + strValue + "\"");
                    if (j < columns.size() - 1) writer.write(", ");
                    j++;
                }
                writer.write("}");
                if (i < rows.size() - 1) writer.write(",");
                writer.newLine();
            }
            
            writer.write("]");
        }
    }
    
    /** Potentially make this a list of values instead of one default value */
    public void addColumn(String columnName, Object defaultValue) {
        if (columns.contains(columnName)) {
            throw new IllegalArgumentException("Column " + columnName + " already exists.");
        }
        
        columns.add(columnName);
        String nullString = "NULL";
        for (Map<String, Object> row : rows) {
            // check if we have a number, and if we do typecast it to be an integer
            if (defaultValue == null || defaultValue.equals(nullString)) {
                row.put(columnName, null);
            } else if (defaultValue instanceof Double d) {
                if (d % 1 != 0) {
                    row.put(columnName, d); // keep as double if fractional part exists
                } else {
                    row.put(columnName, (int) d.doubleValue()); // convert to int if whole number
                }
            } else {
                row.put(columnName, defaultValue);
            }
        }
    }
    
    public void addColumn(
        String newColumn,
        String baseColumn,
        String operator,
        Object conditionValue
    ) {
        if (columns.contains(newColumn)) {
            throw new IllegalArgumentException("Column " + newColumn + " already exists.");
        }
        if (!columns.contains(baseColumn)) {
            throw new IllegalArgumentException("Base column " + baseColumn + " does not exist.");
        }
        
        columns.add(newColumn);
        
        for (Map<String, Object> row : rows) {
            Object raw = row.get(baseColumn);
            
            // NULL → newColumn = NULL
            if (raw == null) {
                row.put(newColumn, null);
                continue;
            }
            
            Object left = coerce(raw);
            Object right = coerce(conditionValue);
            int cmp = compareValues(left, right);
            
            boolean result = switch (operator) {
                case ">"  -> cmp > 0;
                case ">=" -> cmp >= 0;
                case "<"  -> cmp < 0;
                case "<=" -> cmp <= 0;
                case "==" -> Objects.equals(left, right);
                case "!=" -> !Objects.equals(left, right);
                default   -> throw new IllegalArgumentException("Invalid operator: " + operator);
            };
            
            row.put(newColumn, result);
        }
    }
    
    public void fillValues(
        String columnName,
        Object value,
        String conditionColumn,
        String operator,
        Object expression,
        String keyword
    ) {
        if (!columns.contains(columnName)) {
            throw new IllegalArgumentException("Column " + columnName + " does not exist.");
        }
        
        // Determine blank/NULL criteria — same for both paths
        java.util.function.Predicate<Object> isMissingFn = (cell) -> {
            if (keyword.equalsIgnoreCase("blanks")) {
                return (cell instanceof String s && s.isBlank());
            } else if (keyword.equalsIgnoreCase("NULL")) {
                return (cell == null);
            } else {
                throw new IllegalArgumentException("Unsupported keyword: " + keyword);
            }
        };
        
        boolean hasCondition =
        conditionColumn != null &&
        operator != null &&
        expression != null;
        
        for (Map<String, Object> row : rows) {
            
            Object cellValue = row.get(columnName);
            boolean isMissing = isMissingFn.test(cellValue);
            
            if (!isMissing) continue;  // Only fill missing entries
            
            boolean meetsCondition = true;
            
            if (hasCondition) {
                if (!columns.contains(conditionColumn)) {
                    throw new IllegalArgumentException("Condition column " + conditionColumn + " does not exist.");
                }
                
                Object leftRaw = row.get(conditionColumn);
                if (leftRaw == null) continue; // null never meets a comparison
                
                Object left = coerce(leftRaw);
                Object right = coerce(expression);
                
                int cmp = compareValues(left, right);
                
                meetsCondition = switch (operator) {
                    case "==" -> Objects.equals(left, right);
                    case "!=" -> !Objects.equals(left, right);
                    case "<"  -> cmp < 0;
                    case "<=" -> cmp <= 0;
                    case ">"  -> cmp > 0;
                    case ">=" -> cmp >= 0;
                    default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
                };
            }
            
            if (meetsCondition) {
                // Write final value with numeric cleanup
                if (value == null || value.equals("NULL")) {
                    row.put(columnName, null);
                } else if (value instanceof Double d) {
                    if (d % 1 != 0) {
                        row.put(columnName, d);
                    } else {
                        row.put(columnName, (int) d.doubleValue());
                    }
                } else {
                    row.put(columnName, value);
                }
            }
        }
    }
    
    
    public void dropColumn(List<String> columnNames) {
        for (String colName : columnNames) {
            if (!columns.contains(colName)) {
                throw new IllegalArgumentException("Column " + colName + " does not exist.");
            }
        }
        
        // Remove from columns list
        columns.removeAll(columnNames);
        
        // Remove from each row
        for (Map<String, Object> row : rows) {
            for (String colName : columnNames) {
                row.remove(colName);
            }
        }
    }
    
    @Override
    public String toString() {
        return "Dataset(" + columns + ", " + rows.size() + " rows)";
    }
}
