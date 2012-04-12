package com.l7tech.console.util;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FullQName;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

/**
 *
 */
public class QnameValidator {

    /**
     * Check if a QName is valid according to FullQName.
     * This checks that any namespace URI is a valid URI, and that
     * there's a local name, and that the local name and any prefix
     * are valid ncnames.
     *
     * @param text a qname to validate, expected to be in one of the formats NAME, PREFIX:NAME, {URI}NAME, or {URI}PREFIX:NAME.  Required.
     * @throws AssertionPropertiesOkCancelSupport.ValidationException if the given text is not a valid LocalQName.
     */
    public static void validateQname(String text) throws AssertionPropertiesOkCancelSupport.ValidationException {
        final String msg = "Attribute name should be in one of the formats: NAME, PREFIX:NAME, {URI}NAME, or {URI}PREFIX:NAME";

        if (text == null || text.length() < 1)
            throw new AssertionPropertiesOkCancelSupport.ValidationException("Attribute name is empty.\n\n" + msg);

        try {
            FullQName parsedQname = FullQName.valueOf(text);
            String nsUri = parsedQname.getNsUri();
            String prefix = parsedQname.getPrefix();
            String local = parsedQname.getLocal();

            // Check URI
            if (nsUri != null && nsUri.length() > 0)
                new URI(nsUri);

            // Check prefix
            if (prefix != null && prefix.length() > 0 && !DomUtils.isValidXmlNcName(prefix))
                throw new AssertionPropertiesOkCancelSupport.ValidationException("Attribute name has an invalid namespace prefix: " + prefix + "\n\n" + msg);

            // Check local name
            if (local == null || local.length() < 1)
                throw new AssertionPropertiesOkCancelSupport.ValidationException("Attribute name is missing a local part.\n\n" + msg);
            if (!DomUtils.isValidXmlNcName(local))
                throw new AssertionPropertiesOkCancelSupport.ValidationException("Attribute name local part is not valid.\n\n" + msg);

            // Ok
        } catch (URISyntaxException e) {
            throw new AssertionPropertiesOkCancelSupport.ValidationException("Invalid attribute namespace URI: " + ExceptionUtils.getMessage(e), e);
        } catch (ParseException e) {
            throw new AssertionPropertiesOkCancelSupport.ValidationException(ExceptionUtils.getMessage(e) + "\n\n" + msg, e);
        }
    }
}
