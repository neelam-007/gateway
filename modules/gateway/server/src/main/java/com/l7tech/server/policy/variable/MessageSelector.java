/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.message.*;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.IOUtils;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.X509SecurityToken;
import com.l7tech.security.token.SigningSecurityToken;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

/**
 * Extracts values by name from {@link Message}s.
 * @author alex
*/
class MessageSelector implements ExpandVariables.Selector {
    // TODO these must all be lower case
    private static final String HTTP_HEADER_PREFIX = "http.header.";
    private static final String HTTP_HEADERVALUES_PREFIX = "http.headervalues.";
    private static final String STATUS_NAME = "http.status";
    private static final String MAINPART_NAME = "mainpart";
    // TODO parts?

    private static final String WSS_PREFIX = "wss.";
    private static final String WSS_CERT_COUNT = WSS_PREFIX + "certificates.count";
    private static final String WSS_CERT_VALUES_PREFIX = WSS_PREFIX + "certificates.value.";
    private static final String WSS_SIGN_CERT_COUNT = WSS_PREFIX + "signingcertificates.count";
    private static final String WSS_SIGN_CERT_VALUES_PREFIX = WSS_PREFIX + "signingcertificates.value.";

    @Override
    public Class getContextObjectClass() {
        return Message.class;
    }

    @Override
    public Selection select(Object context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
        Message message;
        if (context instanceof Message) {
            message = (Message) context;
        } else {
            throw new IllegalArgumentException();
        }

        final MessageAttributeSelector selector;

        final String lname = name.toLowerCase();
        if (lname.startsWith(HTTP_HEADER_PREFIX))
            selector = singleHeaderSelector;
        else if (lname.startsWith(HTTP_HEADERVALUES_PREFIX))
            selector = multiHeaderSelector;
        else if (STATUS_NAME.equals(lname)) {
            selector = statusSelector;
        } else if (MAINPART_NAME.equals(lname)) {
            selector = mainPartSelector;
        } else if (lname.startsWith(WSS_PREFIX)) {
            selector = wssSelector;
        } else {
            // TODO other Message attributes
            String msg = handler.handleBadVariable(name + " in " + context.getClass().getName());
            if (strict) throw new IllegalArgumentException(msg);
            return null;
        }

        return selector.select(message, name, handler, strict);
    }

    private static interface MessageAttributeSelector {
        Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict);
    }

    private static final MessageAttributeSelector statusSelector = new MessageAttributeSelector() {
        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            HttpResponseKnob hrk = context.getKnob(HttpResponseKnob.class);
            if (hrk == null) {
                String msg = handler.handleBadVariable(name);
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
            return new Selection(hrk.getStatus());
        }
    };

    private final MessageAttributeSelector mainPartSelector = new MessageAttributeSelector() {
        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            try {
                final MimeKnob mk = context.getMimeKnob();
                ContentTypeHeader cth = mk.getFirstPart().getContentType();
                if (cth.isText() || cth.isXml()) {
                    // TODO maximum size? This could be huge and OOM
                    byte[] bytes = IOUtils.slurpStream(mk.getFirstPart().getInputStream(false));
                    return new Selection(new String(bytes, cth.getEncoding()));
                } else {
                    String msg = handler.handleBadVariable("Message is not text");
                    if (strict) throw new IllegalArgumentException(msg);
                    return null;
                }
            } catch (IOException e) {
                throw new RuntimeException(e); // Can't happen
            } catch (NoSuchPartException e) {
                throw new RuntimeException(e); // Can't happen
            }
        }
    };

    private static final MessageAttributeSelector wssSelector = new MessageAttributeSelector() {
        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            final String lname = name.toLowerCase();

            if (WSS_CERT_COUNT.equals(lname)) {
                return new Selection(getCertificateCount(context, false), null);
            }
            else if (WSS_SIGN_CERT_COUNT.equals(lname)) {
                return new Selection(getCertificateCount(context, true), null);
            } else { // cert value request
                String prefix = getCertValPrefix(lname);
                String rname = prefix == null ? null : lname.substring(prefix.length());
                String[] valueParts = rname == null ? null : rname.split("\\.", 2);
                if (valueParts != null && valueParts.length > 0 && isInt(valueParts[0]) ) {
                    return new Selection(
                        getCertificate(context, Integer.parseInt(valueParts[0]), WSS_SIGN_CERT_VALUES_PREFIX.equals(prefix)),
                        valueParts.length > 1 ? valueParts[1] : null);
                } else if (strict) {
                    String msg = handler.handleBadVariable(name + " in " + context.getClass().getName());
                    throw new IllegalArgumentException(msg);
                } else {
                    return null;
                }
            }
        }
    };

    private static String getCertValPrefix(String lname) {
        return lname == null ? null :
               lname.startsWith(WSS_CERT_VALUES_PREFIX) ? WSS_CERT_VALUES_PREFIX :
               lname.startsWith(WSS_SIGN_CERT_VALUES_PREFIX) ? WSS_SIGN_CERT_VALUES_PREFIX :
               null;
    }

    private static boolean isInt(String maybeInt) {
        try {
            Integer.parseInt(maybeInt);
            return true;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    private static int getCertificateCount(Message message, boolean signingOnly) {
        int count = 0;
        for(SecurityToken token : message.getSecurityKnob().getProcessorResult().getXmlSecurityTokens()) {
            if ( token instanceof X509SecurityToken && (!signingOnly || ((SigningSecurityToken)token).isPossessionProved()))
                count++;
        }
        return count;
    }

    private static X509Certificate getCertificate(Message message, int index, boolean signingOnly) {
        ArrayList<X509SecurityToken> candidates = new ArrayList<X509SecurityToken>();
        for(SecurityToken token : message.getSecurityKnob().getProcessorResult().getXmlSecurityTokens()) {
            if(token instanceof X509SecurityToken && (!signingOnly || ((SigningSecurityToken)token).isPossessionProved()))
                candidates.add((X509SecurityToken) token);
        }
        return index < 1 || index > candidates.size() ? null : candidates.get(index - 1).getCertificate();
    }

    private static final HeaderSelector singleHeaderSelector = new HeaderSelector(HTTP_HEADER_PREFIX, false);
    private static final HeaderSelector multiHeaderSelector = new HeaderSelector(HTTP_HEADERVALUES_PREFIX, true);

    private static class HeaderSelector implements MessageAttributeSelector {
        String prefix;
        boolean multi;

        private HeaderSelector(String prefix, boolean multi) {
            this.prefix = prefix;
            this.multi = multi;
        }

        @Override
        public Selection select(Message context, String name, Syntax.SyntaxErrorHandler handler, boolean strict) {
            HasHeaders hrk = context.getKnob(HttpRequestKnob.class);
            if (hrk == null) hrk = context.getKnob(HttpResponseKnob.class);
            if (hrk == null) {
                String msg = handler.handleBadVariable(name + " in " + context.getClass().getName());
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }
            final String hname = name.substring(prefix.length());
            String[] vals = hrk.getHeaderValues(hname);
            if (vals == null || vals.length == 0) {
                String msg = handler.handleBadVariable(hname + " header was empty");
                if (strict) throw new IllegalArgumentException(msg);
                return null;
            }

            return new Selection(multi ? vals : vals[0]);
        }

    }


}
