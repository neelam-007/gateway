package com.l7tech.external.assertions.websocket;

import com.l7tech.external.assertions.websocket.console.WebSocketNumberFormatException;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.InvalidGenericEntityException;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;

import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;

/**
 * User: cirving
 * Date: 6/6/12
 * Time: 4:20 PM
 */
public class WebSocketUtils {

    /*
     * Validates whether or not a string is an Int. Returns 0 for null or empty string
     * @throws NumberFormatException
     */
    public static int isInt(String input, String fieldName) throws WebSocketNumberFormatException {
        if (input == null || "".equals(input))
        {
            return 0;
        }
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new WebSocketNumberFormatException(fieldName);
        }
    }

    /*
    * Simple normalizer to make the hostname:port entry into the form ws://hostname:port. It doesn't check hostname validity,
    * but it does check for a port.  If no port specified, default is set to 80 (for ws://) or 443 (for wss://). (MAG-173)
    * This part just looks for the ws:// or wss:// prefix, and prepends them if necessary.
    */
    public static String normalizeUrl(String url, boolean useSSL) {

        if (url == null || "".equals(url)) {
            return validatePort(url, useSSL);
        }

        if (url.contains("://")) {
            if (useSSL) {
                return validatePort("wss://" + url.substring(url.indexOf("://")+3), useSSL);
            }
            return validatePort("ws://" + url.substring(url.indexOf("://")+3), useSSL);
        }
        if (useSSL)
        {
            return validatePort("wss://" + url, useSSL);
        }
        return validatePort("ws://" + url, useSSL);
    }

    /*
     * Validates the port of the passed string.  Returns the string with a proper port if not already set.
     * This part looks for the port number and appends/inserts it if needed.
     * If the port is already specified, even if the protocol changes (like SSL on but port 80 already
     * specified), the port is NOT CHANGED.  80 and 443 are HTTP defaults in a worse case when no port
     * is specified.  If a port exists, it's considered explicit and the user needs to change it manually.
     */
    private static String validatePort(String url, boolean useSSL) {
        if (url == null || "".equals(url)) return url;  // Same contract as caller.

        StringBuilder newURL = new StringBuilder(url);
        // Look for a third "/"...(ie: wss://blah.ca/test)...
        //                   or...(    ws://blah.ca/test)...
        //                             0123456
        int location = (newURL.indexOf("/", (useSSL ? 6 : 5)));
        if (location == -1) {
            // There are no other slashes in the URL.  Port can be appended as per normal.
            // Check all other URLs
            if (newURL.indexOf(":") == newURL.lastIndexOf(":")) {
                // There is no port number attached to this URL, and needs to be appended
                newURL.append((useSSL ? ":443" : ":80"));
            }
        } else {
            // There is at least one additional slash indicating a path.
            // The port needs to be inserted as opposed to appended.
            if (newURL.indexOf(":") == newURL.lastIndexOf(":")) {
                // There is no port number attached to this URL, and needs to be appended
                newURL.insert(location, (useSSL ? ":443" : ":80"));
            }
        }

        if (!newURL.toString().endsWith("/")) {
            newURL.append("/");
        }

        return newURL.toString();
    }

    /*
     * returns the lower value of a local or global value. If local is 0, the global value take precedence
     */
    public static int returnLowerValue(int local, int global) {
        if (local == 0) {
            return global;
        }

        if ( local < global ) {
            return local;
        }
        return global;
    }

    /*
     * Return the String value of an positive int. For an int less or equal to zero an empty string is returned
     */
    public static String getDisplayValueOfInt(int input) {
        if ( input > 0) {
            return String.valueOf(input);
        }

        return "";
    }

    /*
     * Creates a clone of WebSocketConnectionEntity with a unique name
     */
    public static WebSocketConnectionEntity cloneEntity(WebSocketConnectionEntity source) {
        WebSocketConnectionEntity target = new WebSocketConnectionEntity();
        try {
            target.setInboundClientAuth(source.getInboundClientAuth());
            target.setInboundListenPort(source.getInboundListenPort());
            target.setInboundMaxConnections(source.getInboundMaxConnections());
            target.setInboundMaxIdleTime(source.getInboundMaxIdleTime());
            target.setInboundPolicyOID(source.getInboundPolicyOID());
            target.setInboundPrivateKeyAlias(source.getInboundPrivateKeyAlias());
            target.setInboundPrivateKeyId(source.getInboundPrivateKeyId());
            target.setInboundSsl(source.isInboundSsl());

            target.setOutboundClientAuthentication(source.isOutboundClientAuthentication());
            target.setOutboundMaxIdleTime(source.getOutboundMaxIdleTime());
            target.setOutboundPolicyOID(source.getOutboundPolicyOID());
            target.setOutboundPrivateKeyAlias(source.getOutboundPrivateKeyAlias());
            target.setOutboundPrivateKeyId(source.getInboundPrivateKeyId());
            target.setOutboundSsl(source.isOutboundSsl());
            target.setOutboundUrl(source.getOutboundUrl());

            target.setDescription(source.getDescription());
            target.setEnabled(source.isEnabled());
            target.setName("Copy of " + source.getName());
        } catch (InvalidRangeException e) {
            //Meant to do nothing
        }
        return target;
    }

    /*
     * Converts a Generic Entity to its Concrete Entity implementation
     */
    public static <ET extends GenericEntity> ET asConcreteEntity(GenericEntity that, Class<ET> entityClass) throws InvalidGenericEntityException {
        final String entityClassName = that.getEntityClassName();
        if (!entityClass.getName().equals(entityClassName))
            throw new InvalidGenericEntityException("generic entity is not of expected class " + entityClassName + ": actual classname is " + entityClass.getName());

        final String xml = that.getValueXml();
        if (xml == null || xml.length() < 1) {
            // New object -- leave non-base fields as default
            try {
                ET ret = entityClass.newInstance();
                GenericEntity.copyBaseFields(that, ret);
                return ret;
            } catch (Exception e) {
                throw new InvalidGenericEntityException("Unable to instantiate " + entityClass.getName() + ": " + ExceptionUtils.getMessage(e), e);
            }
        }

        XMLDecoder decoder = null;
        try {
            decoder = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(Charsets.UTF8)), null, null, entityClass.getClassLoader());
            Object obj = decoder.readObject();
            if (entityClass.isInstance(obj)) {
                ET ret = entityClass.cast(obj);
                GenericEntity.copyBaseFields(that, ret);
                return ret;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidGenericEntityException("Stream does not contain any entities", e);
        } finally {
            if (decoder != null) decoder.close();
        }

        throw new InvalidGenericEntityException("Generic entity XML stream did not contain an instance of " + entityClassName);
    }

    /**
     * Quick check to see if a String has a value.
     *
     * @param s String to validate as empty
     */
    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }
}
