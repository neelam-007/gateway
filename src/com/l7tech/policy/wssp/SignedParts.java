/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import org.apache.ws.policy.PrimitiveAssertion;

import javax.xml.namespace.QName;

/**
 * @author mike
 */
public class SignedParts extends PrimitiveAssertion {
    public static final String WSSP_NS = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512";

    public static final QName spBody = new QName(WSSP_NS, "Body");
    public static final QName spHeader = new QName(WSSP_NS, "Header");

    public static abstract class Part {
    }

    public static class BodyPart extends Part {
    }

    public static class HeaderPart extends Part {
        private final String ns;
        private final String name;

        public HeaderPart(String ns) {
            this.ns = ns;
            this.name = null;
        }

        public HeaderPart(String ns, String name) {
            this.ns = ns;
            this.name = name;
        }

        public HeaderPart(ElementCursor spHeaderEl) {
            this.ns = spHeaderEl.getAttributeValue("Namespace");
            String name = spHeaderEl.getAttributeValue("Name");
            this.name = name == null ? null : name;
        }

        public String getNs() {
            return ns;
        }

        public String getName() {
            return name;
        }
    }

    public SignedParts() {
        super(new QName(WSSP_NS, "SignedParts"));
    }

    public SignedParts(ElementCursor spSignedPartsEl) throws InvalidDocumentFormatException {
        super(new QName(WSSP_NS, "SignedParts"));
        spSignedPartsEl.visitChildElements(new ElementCursor.Visitor() {
            public void visit(ElementCursor ec) throws InvalidDocumentFormatException {

            }
        });
    }
}
