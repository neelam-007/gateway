/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.policy.exporter;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyType;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Include;
import org.w3c.dom.Element;

import java.io.StringReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class IncludedPolicyReference extends ExternalReference {
    private static final String ATTR_OID = "oid";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_SOAP = "soap";
    private static final String ATTR_INCLUDED = "included";

    private static enum UseType {
        IMPORT,
        USE_EXISTING,
        UPDATE
    }

    private static final Logger logger = Logger.getLogger(IncludedPolicyReference.class.getName());

    private final Long oid;
    private String name;
    private PolicyType type;
    private Boolean soap;
    private String xml;

    private UseType useType;

    public IncludedPolicyReference(Include includeAssertion) {
        this.oid = includeAssertion.getPolicyOid();
        this.name = includeAssertion.getPolicyName();
        try {
            Policy policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(includeAssertion.getPolicyOid());
            this.name = policy.getName();
            this.type = policy.getType();
            this.soap = policy.isSoap();
            this.xml = policy.getXml();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unable to resolve included policy #{0} ({1})--storing OID only", new Object[] { includeAssertion.getPolicyOid(), includeAssertion.getPolicyName() });
        }
    }

    void serializeToRefElement(Element referencesParentElement) {
        Element includeEl = referencesParentElement.getOwnerDocument().createElement(getClass().getSimpleName());
        includeEl.setAttribute(ATTR_OID, oid.toString());
        includeEl.setAttribute(ExporterConstants.REF_TYPE_ATTRNAME, getClass().getName());
        if (name != null) includeEl.setAttribute(ATTR_NAME, name);
        if (type != null) includeEl.setAttribute(ATTR_TYPE, type.name());
        if (soap != null) includeEl.setAttribute(ATTR_SOAP, soap.toString());
        if (xml != null) {
            try {
                Element child = XmlUtil.parse(new StringReader(xml), false).getDocumentElement();
                Element newChild = (Element) includeEl.getOwnerDocument().importNode(child, true);
                includeEl.appendChild(newChild);
                includeEl.setAttribute(ATTR_INCLUDED, Boolean.TRUE.toString());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Unable to parse included policy", e);
            }
        }
        referencesParentElement.appendChild(includeEl);
    }

    boolean verifyReference() {
        try {
            Policy policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(oid);
            if (policy == null) {
                logger.log(Level.INFO, MessageFormat.format("Policy #{0} ({1}) does not exist on this system; importing", oid, name));
                useType = UseType.IMPORT;
                return true;
            }

            if (policy.getType() == type && policy.getXml().equals(xml) && policy.isSoap() == soap) {
                logger.log(Level.INFO, "Existing Policy #{0} ({1}) is essentially identical to the imported version, using existing version", new Object[] { oid, name });
                useType = UseType.USE_EXISTING;
                return true;
            }

            useType = UseType.UPDATE;
            logger.log(Level.INFO, "Existing Policy #{0} ({1}) found, but not the same, will need to merge");
            return false;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to determine whether imported policy already present");
            return false;
        }
    }

    boolean localizeAssertion(Assertion assertionToLocalize) {
        if (assertionToLocalize instanceof Include) {
            Include include = (Include) assertionToLocalize;
            include.setPolicyOid(oid);
            include.setPolicyName(name);
        }
        return true;
    }

    public static IncludedPolicyReference parseFromElement(Element el) {
        String soid = el.getAttribute(ATTR_OID);
        if (soid == null || soid.length() <= 0) throw new IllegalArgumentException("No " + ATTR_OID + " attribute found");
        long oid = Long.parseLong(soid);

        String name = el.getAttribute(ATTR_NAME);
        IncludedPolicyReference ipr = new IncludedPolicyReference(new Include(oid, name));
        ipr.soap = Boolean.TRUE.toString().equals(el.getAttribute(ATTR_SOAP));
        ipr.type = PolicyType.valueOf(el.getAttribute(ATTR_TYPE));
        if (Boolean.TRUE.toString().equals(el.getAttribute(ATTR_INCLUDED))) {
            Element includedPolicy = XmlUtil.findFirstChildElement(el);
            if (includedPolicy == null) throw new IllegalArgumentException("included=\"true\" but no child element");
            try {
                ipr.xml = XmlUtil.nodeToString(includedPolicy);
            } catch (IOException e) {
                throw new RuntimeException(e); // Can't happen
            }
        }
        return ipr;
    }
}
