/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.PersistenceContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Random;

/**
 * @author alex
 */
public class HibernatePersistenceManagerServlet extends HttpServlet {
    public void init( ServletConfig config ) throws ServletException {
        super.init( config );
    }

    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException {
        response.setContentType( "text/html" );
        PrintWriter out = response.getWriter();

        IdentityProviderConfigManager ipcm;
        //IdentityProviderTypeManager iptm;
        try {
            PersistenceContext.getCurrent().beginTransaction();
            ipcm = new IdProvConfManagerClient();
        } catch ( Exception e ) {
            throw new ServletException( e );
        }

        try {
            String op = request.getParameter("op");

            if ( "list".equals(op) ) {
                Iterator i = ipcm.findAll().iterator();

                if ( i.hasNext() ) {
                    IdentityProviderConfig config;
                    while ( i.hasNext() ) {
                        config = (IdentityProviderConfig)i.next();
                        out.println( config.getOid() );
                        out.println( config.getName() );
                    }
                } else {
                    out.println( "None!" );
                }
            } else if ( "get".equals(op) ) {
                String soid = request.getParameter("oid");
                long oid = Long.parseLong( soid );
                IdentityProviderConfig config = ipcm.findByPrimaryKey( oid );
                out.println( config );
            } else if ( "delete".equals(op) ) {
                String soid = request.getParameter("oid");
                long oid = Long.parseLong( soid );
                IdentityProviderConfig config = ipcm.findByPrimaryKey( oid );
                if ( config == null )
                    out.println( "Couldn't find " + oid );
                else {
                    ipcm.delete(config);

                    out.println( oid + " deleted." );
                }
            } else if ( "create".equals(op) ) {
                IdentityProviderConfig config = new IdentityProviderConfig(IdentityProviderType.INTERNAL);
                config.setName( new Integer( new Random().nextInt() ).toString() );
                config.setDescription("This object is bogus.");

                long oid = ipcm.save( config );
                config.setName( "IdentityAssertion Provider #" + oid );
                ipcm.update( config );

                out.println( "Saved " + oid );
            } else {
                out.println( "<b>The <tt>op</tt> parameter must be one of { get, create, delete, list }</b>" );
            }
        } catch ( Exception e ) {
            e.printStackTrace(out);
            throw new ServletException( e );
        } finally {
            try {
                String rollback = request.getParameter("rollback");
                if ( "true".equals( rollback ) )
                    PersistenceContext.getCurrent().rollbackTransaction();
                else {
                    PersistenceContext.getCurrent().commitTransaction();
                }
            } catch ( Exception te ) {
                throw new ServletException( te );
            }
            out.close();
        }
    }

}
