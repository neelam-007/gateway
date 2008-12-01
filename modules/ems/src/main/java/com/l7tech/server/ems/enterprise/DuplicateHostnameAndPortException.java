package com.l7tech.server.ems.enterprise;

import com.l7tech.objectmodel.SaveException;

/**
 * Exception thrown when attampting to save a ssg cluster with a duplicate ssl hostname and admin port.
 * 
 * @Copyright: Layer 7 Tech. Inc.
 * @Author: ghuang
 * @Date: Dec 1, 2008
 */
public class DuplicateHostnameAndPortException extends SaveException {
    public DuplicateHostnameAndPortException(String message) {
        super(message);
    }
}
