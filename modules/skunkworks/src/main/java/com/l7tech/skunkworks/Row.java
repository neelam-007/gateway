/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.skunkworks;

import java.util.Map;

/**
 * @author alex
 */
public interface Row {
    Map<String, Object> getIndexedValues();
    String[] getIndexedColumnNames();
}
