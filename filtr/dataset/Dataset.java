package filtr.dataset;

import java.util.*;

public class Dataset {
    private List<String> columns;
    private List<Map<String, Object>> rows;

    public Dataset(List<String> columns, List<Map<String, Object>> rows) {
        this.columns = columns;
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

    @Override
    public String toString() {
        return "Dataset(" + columns + ", " + rows.size() + " rows)";
    }
}
