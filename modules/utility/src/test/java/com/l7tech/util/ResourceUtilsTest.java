package com.l7tech.util;

import org.junit.Test;
import static com.l7tech.util.ResourceUtils.*;
import static com.l7tech.util.ResourceUtils.isSameResource;

import java.net.PasswordAuthentication;
import java.util.Arrays;

import static junit.framework.Assert.*;

/**
 *
 */
public class ResourceUtilsTest {

    @Test
    public void testIsSameResource() {
        assertTrue( "basic resource test", isSameResource( "http://a", "http://a" ) );
        assertTrue( "default http port test 1", isSameResource( "http://a:80", "http://a" ) );
        assertTrue( "default http port test 2", isSameResource( "http://a", "http://a:80" ) );
        assertTrue( "default https port test 1", isSameResource( "https://a", "https://a:443" ) );
        assertTrue( "default https port test 2", isSameResource( "https://a:443/path/goes/here?query", "https://a/path/goes/here?query" ) );
        assertTrue( "user info test 1", isSameResource( "http://user:pass@a", "http://a" ) );
        assertTrue( "user info test 2", isSameResource( "http://bbb:1231541@a", "http://user:pass@a" ) );
        assertFalse( "different resources", isSameResource( "http://a.host.com/resource1.wsdl?a", "http://a.host.com/resource2.wsdl?a") );
        assertFalse( "different resources query", isSameResource( "http://a.host.com/resource.wsdl?a", "http://a.host.com/resource.wsdl?b") );
        assertFalse( "malformed test", isSameResource( "########", "http://eere/adsf" ) );
    }

    @Test
    public void testGetPasswordAuthentication() {
        PasswordAuthentication auth = getPasswordAuthentication( "http://host" );
        assertNull( "no auth", auth );

        auth = getPasswordAuthentication( "http://username:password@host.domain.com/pathhere?a=b&e=21342" );
        assertNotNull( "no auth", auth );
        assertEquals( "username", "username", auth.getUserName() );
        assertEquals( "password", "password", new String(auth.getPassword()) );

        auth = getPasswordAuthentication( "http://user123:1231421@host.domain.com/pathhere?a=b&e=21342" );
        assertNotNull( "no auth", auth );
        assertEquals( "username", "user123", auth.getUserName() );
        assertEquals( "password", "1231421", new String(auth.getPassword()) );
    }

    @Test
    public void testAddPasswordAuthentication() {
        assertEquals( "basic test", "http://user:pass@host/", addPasswordAuthentication( "http://host/", new PasswordAuthentication("user","pass".toCharArray())) );
        assertEquals( "replacement test", "http://user:pass@host/", addPasswordAuthentication( "http://otheruser:otherpass@host/", new PasswordAuthentication("user","pass".toCharArray())) );
        assertEquals( "replacement odd chars test", "http://user:pass@host/", addPasswordAuthentication( "http://otheruser:!#$%^&*()_-+= @host/", new PasswordAuthentication("user","pass".toCharArray())) );
        assertEquals( "no creds test", "http://host/", addPasswordAuthentication( "http://host/", null) );
    }

    @Test
    public void testDispose() {
        final boolean[] disposed = new boolean[]{false};
        final Disposable disposable = new Disposable(){
            @Override
            public void dispose() {
                disposed[0] = true;
            }
        };

        // Ensure null is handled
        ResourceUtils.dispose( (Object) null );
        ResourceUtils.dispose( null, null );
        ResourceUtils.dispose( new Object[20] );
        ResourceUtils.dispose( (Iterable) null );

        ResourceUtils.dispose( disposable );
        assertTrue( "Not disposed", disposed[0] );

        disposed[0] = false;
        ResourceUtils.dispose( disposable, null );
        assertTrue( "Not disposed", disposed[0] );

        disposed[0] = false;
        ResourceUtils.dispose( Arrays.asList( disposable ) );
        assertTrue( "Not disposed", disposed[0] );

        disposed[0] = false;
        ResourceUtils.dispose( Arrays.asList( new Object(), new Object(), disposable, new Object() ) );
        assertTrue( "Not disposed", disposed[0] );
    }
}
