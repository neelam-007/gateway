/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import com.l7tech.identity.*;
import com.l7tech.identity.imp.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.naming.InitialContext;
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
        IdentityProviderTypeManager iptm;
        try {
            context = PersistenceManager.getContext();
            context.beginTransaction();
            ipcm = new IdentityProviderConfigManagerImp( context );
            iptm = new IdentityProviderTypeManagerImp( context );
        } catch ( Exception e ) {
            throw new ServletException( e );
        }

        try {
            String op = request.getParameter("op");

            if ( op.equals("list") ) {
                Iterator i = ipcm.findAll().iterator();
                if ( !i.hasNext() ) {
                    out.println( "None!" );
                } else {
                    IdentityProviderConfig config;
                    while ( i.hasNext() ) {
                        config = (IdentityProviderConfig)i.next();
                        out.println( config.getOid() );
                        out.println( config.getName() );
                    }
                }
            } else if ( op.equals( "get") ) {
                String soid = request.getParameter("oid");
                long oid = Long.parseLong( soid );
                IdentityProviderConfig config = ipcm.findByPrimaryKey( oid );
                out.println( config );
            } else if ( op.equals( "delete") ) {
                String soid = request.getParameter("oid");
                long oid = Long.parseLong( soid );
                IdentityProviderConfig config = ipcm.findByPrimaryKey( oid );
                if ( config == null )
                    out.println( "Couldn't find " + oid );
                else {
                    ipcm.delete(config);

                    out.println( oid + " deleted." );
                }
            } else if ( op.equals( "create") ) {
                IdentityProviderType type = new IdentityProviderTypeImp();
                type.setClassName( "com.l7tech.identity.internal.InternalIdentityProvider" );
                type.setName( "Internal Identity Provider" );
                iptm.save(type);

                IdentityProviderConfig config = new IdentityProviderConfigImp();
                config.setName( new Integer( new Random().nextInt() ).toString() );
                config.setDescription("This object is bogus.");
                config.setType( type );

                long oid = ipcm.save( config );
                config.setName( "Identity Provider #" + oid );
                ipcm.update( config );

                out.println( "Saved " + oid );
            }
        } catch ( Exception e ) {
            throw new ServletException( e );
        } finally {
            try {
                String rollback = request.getParameter("rollback");
                if ( "true".equals( rollback ) )
                    context.rollbackTransaction();
                else
                    context.commitTransaction();
            } catch ( Exception te ) {
                throw new ServletException( te );
            }
        }
        out.close();
    }

}
