/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.skunkworks;

import java.util.HashMap;
import java.util.Map;

/**
 * @author alex
 */
public class Table<RT extends Row> {
    private final Map<String, Map<Object, RT>> indexes = new HashMap<String, Map<Object, RT>>();

    public Table(String[] indexedColumnNames) {
        for (String colName : indexedColumnNames) {
            indexes.put(colName, new HashMap<Object, RT>());
        }
    }

    void add(RT row) {
        Map<String, Object> ivals = row.getIndexedValues();
        for (String colname : ivals.keySet()) {
            Object val = ivals.get(colname);
            Map<Object, RT> index = indexes.get(colname);
            synchronized(index) {
                index.put(val, row);
            }
        }
    }

    RT get(String colName, Object value) {
        Map<Object, RT> index = indexes.get(colName);
        return index.get(value);
    }
}
