/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.test;

import com.l7tech.objectmodel.FindException;
import org.python.util.PythonInterpreter;

import javax.security.auth.login.LoginException;
import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class JythonTest extends SsgAdminClient {
    public JythonTest( String[] args ) throws LoginException, IOException, FindException {
        super( args );
    }

    public void run( String[] args ) throws Exception {
        String scriptPath = null;
        String arg;
        for ( int i = 0; i < args.length; i++ ) {
            arg = args[i];
            if ( "-s".equals(arg) ) {
                if ( i < args.length-1 )
                    scriptPath = args[++i];
                else
                    throw new IllegalArgumentException( "The -s switch requires a script path!" );
            }
        }

        if ( scriptPath == null ) throw new IllegalArgumentException( "The -s switch and a script path are required arguments!" );

        PythonInterpreter python = new PythonInterpreter();
        python.set( "args", args );
        python.set( "internalIdentityProvider", _internalIdentityProvider );
        python.set( "internalUserManager", _internalUserManager );
        python.set( "internalGroupManager", _internalGroupManager );
        python.set( "ldapIdentityProvider", _ldapIdentityProvider );
        python.set( "ldapUserManager", _ldapUserManager );
        python.set( "ldapGroupManager", _ldapGroupManager );
        python.set( "serviceManager", _serviceManager );
        python.execfile( scriptPath );
    }

    public static void main( String[] args ) throws Exception {
        try {
            JythonTest test = new JythonTest( args );
            test.run( args );
        } catch ( Exception e ) {
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }
}
