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

        IdentityProviderConfigManager ipcm = new IdentityProviderConfigManagerImp();

        String op = request.getParameter("op");
        if ( op.equals("list") ) {
            Collection c = ipcm.findAll();
            out.println( c );
        } else if ( op.equals( "get") ) {
            String soid = request.getParameter("oid");
            long oid = Long.parseLong( soid );
            IdentityProviderConfig config = null;
            try {
                config = ipcm.findByPrimaryKey( oid );
            } catch (FindException e) {
                throw new ServletException(e);
            }
            out.println( config );
        } else if ( op.equals( "create") ) {
            IdentityProviderConfig config = new IdentityProviderConfigImp();
            config.setName("Identity Provider #1");
            config.setDescription("This object is bogus.");
            long oid = 0;
            try {
                oid = ipcm.save( config );
            } catch (SaveException e) {
                throw new ServletException(e);
            }
            out.println( "Saved " + oid );
        }
        out.close();
    }

}
