package com.l7tech.security.token;

import static com.l7tech.security.token.SecurityTokenType.*;

import static junit.framework.Assert.*;
import org.junit.Test;

import java.lang.reflect.Field;

/**
 * Unit tests for security tokens
 */
public class SecurityTokenTypeTest {

    @Test
    public void testAllTokensListed() throws Exception {
        for( final Field field : SecurityTokenType.class.getDeclaredFields() ) {
            if ( SecurityTokenType.class.isAssignableFrom( field.getType() ) ) { // we'll check all fields of this type
                field.setAccessible( true );
                final SecurityTokenType tokenType = (SecurityTokenType) field.get( null );
                assertNotNull( "Security token number " + tokenType.getNum() + " not listed in VALUES array", getByNum(tokenType.getNum()) );
            }
        }
    }

    @Test
    public void testTokenNumbersPreserved() {
        assertEquals( "Security token number " +  0, SAML_ASSERTION, getByNum(0) );
        assertEquals( "Security token number " +  1, WSSC_CONTEXT, getByNum(1) );
        assertEquals( "Security token number " +  2, WSSC_DERIVED_KEY, getByNum(2) );
        assertEquals( "Security token number " +  3, WSS_USERNAME, getByNum(3) );
        assertEquals( "Security token number " +  4, WSS_X509_BST, getByNum(4) );
        assertEquals( "Security token number " +  5, WSS_ENCRYPTEDKEY, getByNum(5) );
        assertEquals( "Security token number " +  6, WSS_KERBEROS_BST, getByNum(6) );
        assertEquals( "Security token number " +  7, HTTP_BASIC, getByNum(7) );
        assertEquals( "Security token number " +  8, HTTP_DIGEST, getByNum(8) );
        assertEquals( "Security token number " +  9, HTTP_CLIENT_CERT, getByNum(9) );
        assertEquals( "Security token number " + 10, UNKNOWN, getByNum(10) );
        assertEquals( "Security token number " + 11, SAML2_ASSERTION, getByNum(11) );
        assertEquals( "Security token number " + 12, XPATH_CREDENTIALS, getByNum(12) );
        assertEquals( "Security token number " + 13, HTTP_KERBEROS, getByNum(13) );
        assertEquals( "Security token number " + 14, FTP_CREDENTIAL, getByNum(14) );
        assertEquals( "Security token number " + 15, X509_ISSUER_SERIAL, getByNum(15) );
        assertEquals( "Security token number " + 16, SSH_CREDENTIAL, getByNum(16) );
        //TODO Add new tokens here
    }
}
