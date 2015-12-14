package com.l7tech.external.assertions.sha2aliaser.server;

import com.l7tech.util.SyspropUtil;
import org.springframework.context.ApplicationContext;

import java.security.Provider;
import java.security.Security;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sha2AliaserModuleLoadListener {
    private static final Logger logger = Logger.getLogger( Sha2AliaserModuleLoadListener.class.getName() );

    public static final String PROP_ADD_ALIASES = "com.l7tech.server.algaliaser.aliases";
    public static final String DEFAULT_ADD_ALIASES =
            "SUN:Alg.Alias.MessageDigest.SHA256=SHA-256,SUN:Alg.Alias.MessageDigest.SHA384=SHA-384,SUN:Alg.Alias.MessageDigest.SHA512=SHA-512";

    public static synchronized void onModuleLoaded( ApplicationContext context ) throws Exception {
        String allAliases = SyspropUtil.getString( PROP_ADD_ALIASES, DEFAULT_ADD_ALIASES );

        Pattern pattern = Pattern.compile( "^([^:]+):([^=]+)=(.+)$" );

        String[] aliases = allAliases.split( "," );
        for ( String alias : aliases ) {
            Matcher matcher = pattern.matcher( alias );
            if ( !matcher.matches() )
                throw new RuntimeException( "Invalid syntax for system property " + PROP_ADD_ALIASES );

            String providerName = matcher.group( 1 );
            String aliasName = matcher.group( 2 );
            String cname = matcher.group( 3 );

            Provider p = Security.getProvider( providerName );
            if ( null == p )
                throw new RuntimeException( "Invalid value for system property " + PROP_ADD_ALIASES + ": No such provider: " + providerName );
            p.put( aliasName, cname );
            logger.info( "Installed alias in " + providerName + " provider: " + aliasName + " -> " + cname );
        }
    }
}
