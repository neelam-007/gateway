/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * @author alex
 */
public class HibernatePersistenceManagerServlet extends javax.servlet.http.HttpServlet {
    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
        try {
            HibernatePersistenceManager.initialize();
        } catch ( Exception e ) {
            e.printStackTrace();
            throw new ServletException( e );
        }
    }
}
