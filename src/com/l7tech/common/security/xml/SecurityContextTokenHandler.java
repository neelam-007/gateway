package com.l7tech.common.security.xml;

import sun.misc.BASE64Encoder;
import sun.misc.BASE64Decoder;

import java.security.SecureRandom;
import java.io.IOException;

import org.w3c.dom.Document;

/**
 * Appends and parses out xml security session information in/out of soap messages.
 *
 * This is not yet plugged in and will eventually phase out the class SecureConversationTokenHandler.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 10, 2004<br/>
 * $Id$
 */
public class SecurityContextTokenHandler {

    /**
     * Generates a new random session id of 16 bytes.
     * @return
     */
    public static byte[] generateNewSessionId() {
        byte[] sessionid = new byte[16];
        rand.nextBytes(sessionid);
        return sessionid;
    }

    /**
     * Converts raw session id to its URI representation.
     * @param rawSessionId a session id 16 bytes long
     * @return URI representation (looks like uuid:base64edrawsessionid)
     */
    public static String sessionIdToURI(byte[] rawSessionId) {
        // the sanity check
        if (rawSessionId == null || rawSessionId.length != 16) {
            throw new IllegalArgumentException("This is not a proper sessionid");
        }
        BASE64Encoder encoder = new BASE64Encoder();
        return URI_PREFIX + encoder.encode(rawSessionId);
    }

    /**
     * does the reverse of sessionIdToURI
     * @param uri URI representation (looks like uuid:base64edrawsessionid)
     * @return a 16 bytes session id
     */
    public static byte[] URIToSessionId(String uri) throws IOException {
        if (uri == null || !uri.startsWith(URI_PREFIX)) {
            throw new IllegalArgumentException("This is not a proper sessionid URI (" + uri + ")");
        }
        BASE64Decoder decoder = new BASE64Decoder();
        return decoder.decodeBuffer(uri.substring(URI_PREFIX.length()));
    }

    public static void appendSessionInfoToSoapMessage(Document soapmsg, byte[] sessionid, long seqnumber) {
        // todo
    }

    private static final SecureRandom rand = new SecureRandom();
    private static final String URI_PREFIX = "uuid:";
}
