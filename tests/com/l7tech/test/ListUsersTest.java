/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.test;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author alex
 * @version $Revision$
 */
public class ListUsersTest extends SsgAdminClient {
    public ListUsersTest() throws LoginException, IOException, FindException {
        super();
    }

    public ListUsersTest( String[] args ) throws LoginException, IOException, FindException {
        super( args );
    }

    public void run( String[] args ) throws Exception {
        System.out.println( "Listing internal users:" );
        Iterator i = _internalUserManager.findAll().iterator();
        User u;
        while ( i.hasNext() ) {
            u = (User)i.next();
            System.out.println( u );
        }

        System.out.println();

        System.out.println( "Listing LDAP users:" );
        i = _ldapUserManager.findAll().iterator();
        while ( i.hasNext() ) {
            u = (User)i.next();
            System.out.println( u );
        }
    }

    public static void main( String[] args ) throws Exception {
        try {
            ListUsersTest test = new ListUsersTest( args );
            test.run( args );
        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
