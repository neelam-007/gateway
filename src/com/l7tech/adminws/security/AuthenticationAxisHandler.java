package com.l7tech.adminws.security;

import org.apache.axis.MessageContext;
import org.apache.axis.AxisFault;
import org.apache.axis.utils.Messages;
import org.apache.axis.encoding.Base64;
import org.apache.axis.transport.http.HTTPConstants;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: June 2, 2003
 *
 * This axis handler class does the following
 * 1. reads a basic auth header from the http request
 * 2. unbase64 it
 * 3. MD5s it
 * 4. compare it to the hash value for this user in the database
 *
 * The password has in the database is expected to have the following format:
 * MD5(username::password) hex encoded
 *
 * It must be declared in the server-config.wsdd file in order to be invoked as per below:
 *
 *
 * <transport name="http">
 *   <requestFlow>
 *     <handler type="URLMapper"/>
 *     <handler type="java:com.l7tech.adminws.security.AuthenticationAxisHandler"/>
 *   </requestFlow>
 * </transport>
 */
public class AuthenticationAxisHandler extends InternalIDSecurityAxisHandler {
    /**
     * Invoked by the axis engine. if successful, will feed the messageContext with
     * a property whose key is AuthenticationAxisHandler.AUTHENTICATED_USER and whose
     * value is the authenticated user object. If authentication fails, this handler
     * will throw an exception (org.apache.axis.AxisFault).
     */
    public void invoke(MessageContext messageContext) throws AxisFault {

        // Process the Basic Auth stuff in the headers
        String tmp = (String)messageContext.getProperty(HTTPConstants.HEADER_AUTHORIZATION);

        if (tmp != null ) tmp = tmp.trim();
        if (tmp != null && tmp.startsWith("Basic ")) {
            // GET THE AUTH VALUE
            String login = null;
            String decodedAuthValue = new String(Base64.decode(tmp.substring(6)));
            int i = decodedAuthValue.indexOf( ':' );
            if (i == -1) login = decodedAuthValue;
            else login = decodedAuthValue.substring( 0, i);
            messageContext.setUsername(login);

            // MD5 IT
            java.security.MessageDigest md5Helper = null;
            try {
                md5Helper = java.security.MessageDigest.getInstance("MD5");
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new AxisFault(e.getMessage());
            }
            byte[] digest = md5Helper.digest(decodedAuthValue.getBytes());
            String md5edDecodedAuthValue = encodeDigest(digest);

            // COMPARE TO THE VALUE IN INTERNAL DB
            com.l7tech.identity.User internalUser = findUserByLoginAndRealm(login, null);
            if (internalUser == null) {
                throw new AxisFault("User " + login + "not registered in internal id provider");
            }
            if (internalUser.getPassword() == null) {
                throw new AxisFault("User " + login + "does not have a password");
            }
            if (internalUser.getPassword().equals(md5edDecodedAuthValue)) {
                // AUTHENTICATION SUCCESS
                // you're cool, get in
                // add the user to the context
                messageContext.setProperty(AUTHENTICATED_USER, new Long(internalUser.getOid()));
                return;
            }
        }
        // this forces the axis servlet to return a 401
        throw new AxisFault("Server.Unauthorized", "com.l7tech.adminws.security.AuthenticationAxisHandler failed", null, null );
    }

    /**
     * Encodes the 128 bit (16 bytes) MD5 into a 32 character String.
     *
     * @param binaryData Array containing the digest
     * @return Encoded MD5, or null if encoding failed
     */
    private static String encodeDigest(byte[] binaryData) {
        if (binaryData == null) return "";

        char[] hexadecimal ={'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        if (binaryData.length != 16) return "";

        char[] buffer = new char[32];

        for (int i = 0; i < 16; i++) {
            int low = (int) (binaryData[i] & 0x0f);
            int high = (int) ((binaryData[i] & 0xf0) >> 4);
            buffer[i*2] = hexadecimal[high];
            buffer[i*2 + 1] = hexadecimal[low];
        }
        return new String(buffer);
    }

    public static void main (String[] args) throws Exception {
        // calculate the digest for a specific set of values
        java.security.MessageDigest md5Helper = java.security.MessageDigest.getInstance("MD5");
        String digestValue = "ssgadmin:ssgadminpasswd";
        byte[] digest = md5Helper.digest(digestValue.getBytes());
        System.out.println(encodeDigest(digest));

        //result: 60189d5f68d564f9cb83e11bc2ae92e9 {for "ssgadmin::ssgadminpasswd"}
        //result: 309b9c7ab4c3ee2144fce9b071acd440 {for "ssgadmin:ssgadminpasswd"}

    }
}
