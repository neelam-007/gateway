package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.test.BugNumber;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for HTTP Digest assertion
 */
public class ServerHttpDigestTest {

//    static {
//        System.setProperty( "com.l7tech.server.policy.assertion.credential.http.oldDigestParser", "true" );
//    }

    @Test
    public void testExtractParameters() throws Exception {
        final String authorization = "Digest username=\"asfd\", realm=\"L7SSGDigestRealm\", nonce=\"3kgaF0E/SkzgpDgVayyZ48kZzcTfVKVmLDr8XrruwC92IFIdwATutQDaRdTns5xg+uLkw+qKBOpUvtf7dAxG2A==\", uri=\"/sophos\", response=\"aeb6551d1bde6526eb6aeca3b68be4a1\", qop=auth, nc=00000001, cnonce=\"f9c3831b32c988b3ff88c13fbee05cc3\", opaque=\"e7e50ba38040ceb663d67572bacb53b5\"";
        final Map<String,String> params = new HashMap<String,String>();
        ServerHttpDigest.populateAuthParams( params, authorization );
        assertEquals( "scheme", "Digest", params.get( HttpDigest.PARAM_SCHEME ) );
        assertEquals( "username", "asfd", params.get( HttpDigest.PARAM_USERNAME ) );
        assertEquals( "realm", "L7SSGDigestRealm", params.get( HttpDigest.PARAM_REALM ) );
        assertEquals( "nonce", "3kgaF0E/SkzgpDgVayyZ48kZzcTfVKVmLDr8XrruwC92IFIdwATutQDaRdTns5xg+uLkw+qKBOpUvtf7dAxG2A==", params.get( HttpDigest.PARAM_NONCE ) );
        assertEquals( "uri", "/sophos", params.get( HttpDigest.PARAM_URI ) );
        assertEquals( "response", "aeb6551d1bde6526eb6aeca3b68be4a1", params.get( HttpDigest.PARAM_RESPONSE ) );
        assertEquals( "qop", "auth", params.get( HttpDigest.PARAM_QOP ) );
        assertEquals( "nc", "00000001", params.get( HttpDigest.PARAM_NC ) );
        assertEquals( "cnonce", "f9c3831b32c988b3ff88c13fbee05cc3", params.get( HttpDigest.PARAM_CNONCE ) );
        assertEquals( "opaque", "e7e50ba38040ceb663d67572bacb53b5", params.get( HttpDigest.PARAM_OPAQUE ) );
    }

    @Test
    @BugNumber(10352)
    public void testExtractParameterUsernameWithSpace() throws Exception {
        final String authorization = "Digest username=\"as fd\", realm=\"L7SSGDigestRealm\", nonce=\"3kgaF0E/SkzgpDgVayyZ48kZzcTfVKVmLDr8XrruwC92IFIdwATutQDaRdTns5xg+uLkw+qKBOpUvtf7dAxG2A==\", uri=\"/sophos\", response=\"aeb6551d1bde6526eb6aeca3b68be4a1\", qop=auth, nc=00000001, cnonce=\"f9c3831b32c988b3ff88c13fbee05cc3\", opaque=\"e7e50ba38040ceb663d67572bacb53b5\"";
        final Map<String,String> params = new HashMap<String,String>();
        ServerHttpDigest.populateAuthParams( params, authorization );
        assertEquals( "username", "as fd", params.get( HttpDigest.PARAM_USERNAME ) );
    }

    @Test
    public void testExtractParameterUsernameWithComma() throws Exception {
        final String authorization = "Digest username=\"as,fd\", realm=\"L7SSGDigestRealm\", nonce=\"l9TBvddh5QcZu2rG5L/jpGsqZSw3+/10dS77Fp1g91D4GXF7LTQlCkF038pZiA/3hMykkChkDdxvGnureiPMFQ==\", uri=\"/sophos\", response=\"62f375e12efc03bcb7ad6b8dc8d4714a\", qop=auth, nc=00000001, cnonce=\"7ceb198b9b30a4d7774cd6266a5abb26\", opaque=\"3a9fb2b9ca35c32eacdb700ceb1b2044\"";
        final Map<String,String> params = new HashMap<String,String>();
        ServerHttpDigest.populateAuthParams( params, authorization );
        assertEquals( "username", "as,fd", params.get( HttpDigest.PARAM_USERNAME ) );
    }

    @Test
    public void testExtractParameterUsernameWithQuotes() throws Exception {
        final String authorization = "Digest username=\"a\\\"sf\\\",d\", realm=\"L7SSGDigestRealm\", nonce=\"kkSlKl8jNJKNSv/c7NH4geg1HaNzJb2dun7XjesCasiBjgJjanPbRjhl7Dch8jjUCz158ggGex4mKL5/VhE5Og==\", uri=\"/sophos\", response=\"39737272e2dda5fcd52e38445ffef321\", qop=auth, nc=00000001, cnonce=\"0e39ad44fbb2ea029441169594d8e5d1\", opaque=\"3349da94e3e1b0da38b114c27aa51c47\"";
        final Map<String,String> params = new HashMap<String,String>();
        ServerHttpDigest.populateAuthParams( params, authorization );
        assertEquals( "username", "a\"sf\",d", params.get( HttpDigest.PARAM_USERNAME ) );
    }

    @Test
    public void testExtractParameterUsernameWithEscaping() throws Exception {
        final String authorization = "Digest username=\"\\\\\", realm=\"L7SSGDigestRealm\", nonce=\"vWpuqaW4/W0RAhPbqRGUdGGsQ5dwB1CRYEuXenZffAdcIgFJuV9WHsX7IR8CbpZIUawiykpIMDsT+m0udgM3Dg==\", uri=\"/sophos\", response=\"b2b0210a33655c1e5da1ab1c1f63c983\", qop=auth, nc=00000001, cnonce=\"3f7824e8fa3e1a4f7e291e4317a3bede\", opaque=\"00da322d1ee63bf35ec18c8ba0811b1c\"";
        final Map<String,String> params = new HashMap<String,String>();
        ServerHttpDigest.populateAuthParams( params, authorization );
        assertEquals( "username", "\\", params.get( HttpDigest.PARAM_USERNAME ) );
    }
}
