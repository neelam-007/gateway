/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import com.l7tech.identity.*;
import com.l7tech.identity.imp.IdentityProviderConfigManagerImp;
import com.l7tech.identity.imp.IdentityProviderConfigImp;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.sql.SQLException;

/**
 * @author alex
 */
public class HibernatePersistenceManagerServlet extends HttpServlet {
    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
        try {
            HibernatePersistenceManager.initialize();
        } catch ( Exception e ) {
            e.printStackTrace();
            throw new ServletException( e );
        }
    }

    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException {
        response.setContentType( "text/html" );
        PrintWriter out = response.getWriter();

        PersistenceContext context;
        IdentityProviderConfigManager ipcm;
        try {
            context = PersistenceManager.getContext();
            ipcm = new IdentityProviderConfigManagerImp( context );
        } catch ( SQLException se ) {
            throw new ServletException( se );
        }

        try {
            String op = request.getParameter("op");

            if ( op.equals("list") ) {
                Collection c = ipcm.findAll();
                out.println( c );
            } else if ( op.equals( "get") ) {
                String soid = request.getParameter("oid");
                long oid = Long.parseLong( soid );
                IdentityProviderConfig config = ipcm.findByPrimaryKey( oid );
                out.println( config );
            } else if ( op.equals( "create") ) {
                IdentityProviderConfig config = new IdentityProviderConfigImp();
                config.setName("Identity Provider #1");
                config.setDescription("This object is bogus.");

                PersistenceManager.beginTransaction( context );
                long oid = ipcm.save( config );
                out.println( "Saved " + oid );
                PersistenceManager.commitTransaction( context );
            }
        } catch ( ObjectModelException ome ) {
            throw new ServletException( ome );
        }
        out.close();
    }

}
