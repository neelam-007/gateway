package com.l7tech.server.secureconversation;

import static com.l7tech.server.secureconversation.OutboundSecureConversationContextManager.*;

import com.l7tech.identity.UserBean;

import static org.junit.Assert.*;

import com.l7tech.objectmodel.Goid;
import org.junit.Test;

/**
 *
 */
public class OutboundSecureConversationContextManagerTest {

    @Test
    public void testKeyRoundTrip() {
        final UserBean user = new UserBean();
        user.setProviderId( new Goid(0,12) );
        user.setUniqueIdentifier( "uid" );
        user.setLogin( "login" );
        final OutboundSessionKey key = OutboundSecureConversationContextManager.newSessionKey( user, "service url" );
        
        final String keyString = key.toStringIdentifier();
        System.out.println( keyString );

        final OutboundSessionKey key2 = OutboundSessionKey.fromStringIdentifier( keyString );
        assertEquals( "Equal keys", key, key2 );
    }

}
