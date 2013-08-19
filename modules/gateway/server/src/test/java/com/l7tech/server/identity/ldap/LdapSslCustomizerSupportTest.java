package com.l7tech.server.identity.ldap;

import com.l7tech.objectmodel.Goid;
import org.junit.Test;
import org.junit.Assert;
import com.l7tech.server.transport.http.SslClientHostnameAwareSocketFactory;

/**
 *
 */
public class LdapSslCustomizerSupportTest {

    @Test
    public void testCustomizerClassname() {
        Assert.assertEquals(
                "Expected classname for default SSL",
                SslClientHostnameAwareSocketFactory.class.getName(),
                LdapSslCustomizerSupport.getSSLSocketFactoryClassname( true, null, null ) );
    }

    @Test
    public void testLoadDefault() throws Exception {
        String classname = LdapSslCustomizerSupport.getSSLSocketFactoryClassname( true, null, null );
        Class sfClass = LdapSslCustomizerSupport.getSSLSocketFactoryClassLoader().loadClass( classname );
        Assert.assertTrue( "Supports pooling", implementsComparator(sfClass) );
    }

    @Test
    public void testLoadAnon() throws Exception {
        String classname = LdapSslCustomizerSupport.getSSLSocketFactoryClassname( false, null, null );
        Class sfClass = LdapSslCustomizerSupport.getSSLSocketFactoryClassLoader().loadClass( classname );
        Assert.assertTrue( "Supports pooling", implementsComparator(sfClass) );
    }

    @Test
    public void testLoadSpecificKey() throws Exception {
        String classname = LdapSslCustomizerSupport.getSSLSocketFactoryClassname( true, new Goid(0,2), "ssl" );
        Class sfClass = LdapSslCustomizerSupport.getSSLSocketFactoryClassLoader().loadClass( classname );
        Assert.assertTrue( "Supports pooling", implementsComparator(sfClass) );
    }

    private boolean implementsComparator( final Class sfClass ) {
        boolean hasImpl = false;

        String COMPARATOR = "java.util.Comparator";
        Class[] interfaces = sfClass.getInterfaces();
        for ( Class anInterface : interfaces ) {
            if ( anInterface.getCanonicalName().equals(COMPARATOR) ) {
                hasImpl = true;
            }
        }

        return hasImpl;
    }


}
