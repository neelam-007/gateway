/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.objectmodel;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.transaction.*;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

/**
 * @author alex
 */
public class JDBCTestServlet extends HttpServlet {
    private InitialContext _initialContext;
    private DataSource _dataSource;
    private TransactionManager _tmanager;

    public void init( ServletConfig config ) throws ServletException {
        super.init( config );

        try {
            _initialContext = new InitialContext();
            _dataSource = (DataSource)_initialContext.lookup( "java:comp/env/jdbc/ssg" );
            _tmanager = (TransactionManager)_initialContext.lookup( "java:comp/TransactionManager" );
        } catch ( Exception e ) {
            e.printStackTrace();
            throw new ServletException( e );
        }
    }

    public void doGet( HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException {
        response.setContentType( "text/html" );
        PrintWriter out = response.getWriter();
        Connection conn = null;

        try {
            _tmanager.begin();
            conn = _dataSource.getConnection();
            out.println( conn );
        } catch ( Exception e ) {
            throw new ServletException( e );
        }

        try {
            String op = request.getParameter("op");

            if ( op.equals("list") ) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery( "select * from identity_provider" );
                if ( rs.next() ) {
                    out.println( "<table>" );
                    do {
                        out.print( "<tr> ");
                        out.print( "<td>");
                        out.print( rs.getLong( "oid" ) );
                        out.print( "</td>" );
                        out.print( "<td>" );
                        out.print( rs.getString( "name" ) );
                        out.print( "</td>" );
                        out.println( "</tr>" );
                    } while ( rs.next() );
                    out.println( "</table>" );
                } else {
                    out.println( "<b>None!<b>" );
                }
            } else if ( op.equals( "get") ) {
                String soid = request.getParameter("oid");
                long oid = Long.parseLong( soid );
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery( "select * from identity_provider where oid = " + soid );
                if ( rs.next() ) {
                    out.println( "<table>" );

                    out.print( "<tr><td>oid</td>" );
                    out.print( oid );
                    out.println( "</td></tr>");

                    out.print( "<tr><td>name</td>" );
                    out.print( rs.getString("name") );
                    out.println( "</td></tr>");

                    out.print( "<tr><td>description</td>" );
                    out.print( rs.getString("description") );
                    out.println( "</td></tr>");

                    out.print( "<tr><td>type</td>" );
                    out.print( rs.getLong("type_oid") );
                    out.println( "</td></tr>");

                    out.println( "</table>" );
                } else {
                    out.println( "<b>Not found!</b>" );
                }
            } else if ( op.equals( "delete") ) {
                Statement stmt = conn.createStatement();

                String soid = request.getParameter("oid");

                int affected = stmt.executeUpdate( "delete from identity_provider where oid = " + soid );
                if ( affected == 0 )
                    out.println( "<b>Not deleted!</b>" );
                else
                    out.println( "Deleted " + soid );

            } else if ( op.equals( "create") ) {
                String soid = request.getParameter( "oid" );
                long oid;
                if ( soid != null && soid.length() > 0 )
                    oid = Long.valueOf(soid).longValue();
                else
                    oid = 123;

                String stype = request.getParameter( "type" );
                long type;
                if ( stype != null && stype.length() > 0 )
                    type = Long.valueOf(stype).longValue();
                else
                    type = 123;

                PreparedStatement pstmt = conn.prepareStatement( "insert into identity_provider (oid,type,name) values (?,?,?)" );
                pstmt.setLong( 1, oid );
                pstmt.setLong( 2, type );
                pstmt.setString( 3, "Identity Provider #" + oid );

                int affected = pstmt.executeUpdate();

                if ( affected == 0 )
                    out.println( "<b>Couldn't add IdentityProvider!</b>" );
                else
                    out.println( "Added IdentityProvider #" + oid );
            } else {
                out.println( "<b>Invalid op parameter (" + op + ")!" );
            }
        } catch ( Exception e ) {
            throw new ServletException( e );
        } finally {
            try {
                String rollback = request.getParameter("rollback");
                if ( rollback != null && rollback.equals("true") )
                    _tmanager.rollback();
                else
                    _tmanager.commit();

                if ( conn != null ) conn.close();
            } catch ( Exception e ) {
                throw new ServletException( e );
            }
        }
        out.close();
    }

}
