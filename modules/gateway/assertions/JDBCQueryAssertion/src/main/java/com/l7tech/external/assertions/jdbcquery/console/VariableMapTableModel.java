package com.l7tech.external.assertions.jdbcquery.console;

import com.l7tech.util.Pair;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jules
 */

public class VariableMapTableModel extends DefaultTableModel {

    private List<Pair<String, String>> mappings;
    private String prefix;

    public static final int COLUMN_NAME_INDEX = 0;
    public static final int VARIABLE_NAME_INDEX = 1;
    private static final String[] COLUMN_HEADER_NAMES = {"Column Name", "Variable Name"};
    
    public VariableMapTableModel(){
        this(new String[][]{}, COLUMN_HEADER_NAMES);
    }

    public VariableMapTableModel(Object[][] data, Object[] columnNames){
        super(data, columnNames);
        mappings = new ArrayList<Pair<String, String>>();
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false; //cells/rows are never editable
    }

    public Object getValueAt(int row, int col) {
        Pair tuple = mappings.get(row);
        switch (col) {
            case COLUMN_NAME_INDEX:
                return tuple.getKey();
            case VARIABLE_NAME_INDEX:
                return prefix + "." + tuple.getValue();
            default:
                throw new IllegalArgumentException("Bad Column");
        }
    }

    public int getRowCount() {
         return mappings == null ? 0 : mappings.size();
    }

    public int getColumnCount() {
        return COLUMN_HEADER_NAMES.length;
    }

    public void update(String prefix) {
        this.prefix = prefix;
        fireTableDataChanged();
    }

    public List<Pair<String, String>> getMappings() {
        return mappings;
    }

    public void setMappings(List<Pair<String, String>> mappings) {
        this.mappings = mappings;
    }

    public void addMapping(Pair<String, String> tuple){
        mappings.add(tuple);
    }

    public void removeMapping(int index){
        mappings.remove(index);
    }

    public void updateMapping(int index, Pair<String, String> tuple){
        mappings.remove(index);
        mappings.add(index, tuple);
    }

    public void clearMappings(){
        mappings.clear();
    }
}
