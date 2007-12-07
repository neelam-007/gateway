/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.common.audit.Audit;
import com.l7tech.common.message.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.HexUtils;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.policy.variable.Syntax;

import java.io.IOException;

/**
 * Extracts values by name from {@link Message}s.
 * @author alex
*/
class MessageSelector implements ExpandVariables.Selector {
    private static final String HTTP_HEADER_PREFIX = "http.header.";
    private static final String HTTP_HEADERVALUES_PREFIX = "http.headerValues.";
    private static final String STATUS_NAME = "http.status";
    private static final String MAINPART_NAME = "mainPart";
    // TODO parts?

    public Class getContextObjectClass() {
        return Message.class;
    }

    public Object select(Object context, String name, Audit audit, boolean strict) {
        Message message;
        if (context instanceof Message) {
            message = (Message) context;
        } else {
            throw new IllegalArgumentException();
        }

        final MessageAttributeSelector selector;

        if (name.startsWith(HTTP_HEADER_PREFIX))
            selector = singleHeaderSelector;
        else if (name.startsWith(HTTP_HEADERVALUES_PREFIX))
            selector = multiHeaderSelector;
        else if (STATUS_NAME.equals(name)) {
            selector = statusSelector;
        } else if (MAINPART_NAME.equals(name)) {
            selector = mainPartSelector;
        } else {
            // TODO other Message attributes
            Syntax.badVariable(name + " in "  + context.getClass().getName(), strict, audit);
            return null;
        }

        return selector.select(message, name, audit, strict);
    }

    private static interface MessageAttributeSelector {
        Object select(Message context, String name, Audit audit, boolean strict);
    }

    private static final MessageAttributeSelector statusSelector = new MessageAttributeSelector() {
        public Object select(Message context, String name, Audit audit, boolean strict) {
            HttpResponseKnob hrk = (HttpResponseKnob) context.getKnob(HttpResponseKnob.class);
            if (hrk == null) {
                Syntax.badVariable(name, strict, audit);
                return null;
            }
            return hrk.getStatus();
        }
    };

    private final MessageAttributeSelector mainPartSelector = new MessageAttributeSelector() {
        public Object select(Message context, String name, Audit audit, boolean strict) {
            try {
                final MimeKnob mk = context.getMimeKnob();
                ContentTypeHeader cth = mk.getFirstPart().getContentType();
                if (cth.isText() || cth.isXml()) {
                    // TODO maximum size? This could be huge and OOM
                    byte[] bytes = HexUtils.slurpStreamLocalBuffer(mk.getFirstPart().getInputStream(false));
                    return new String(bytes, cth.getEncoding());
                } else {
                    Syntax.badVariable("Message is not text", strict, audit);
                    return null;
                }
            } catch (IOException e) {
                throw new RuntimeException(e); // Can't happen
            } catch (NoSuchPartException e) {
                throw new RuntimeException(e); // Can't happen
            }
        }
    };

    private static final HeaderSelector singleHeaderSelector = new HeaderSelector(HTTP_HEADER_PREFIX, false);
    private static final HeaderSelector multiHeaderSelector = new HeaderSelector(HTTP_HEADERVALUES_PREFIX, true);

    private static class HeaderSelector implements MessageAttributeSelector {
        String prefix;
        boolean multi;

        private HeaderSelector(String prefix, boolean multi) {
            this.prefix = prefix;
            this.multi = multi;
        }

        public Object select(Message context, String name, Audit audit, boolean strict) {
            HasHeaders hrk = (HasHeaders) context.getKnob(HttpRequestKnob.class);
            if (hrk == null) hrk = (HasHeaders) context.getKnob(HttpResponseKnob.class);
            if (hrk == null) {
                Syntax.badVariable(name + " in " + context.getClass().getName(), strict, audit);
                return null;
            }
            final String hname = name.substring(prefix.length());
            String[] vals = hrk.getHeaderValues(hname);
            if (vals == null || vals.length == 0) {
                Syntax.badVariable(hname + " header was empty", strict, audit);
                return null;
            }

            return multi ? vals : vals[0];
        }

    }


}
