/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.policy.exporter;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.util.DomUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.PolicyReference;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.PolicyConflictException;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;

import java.io.StringReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Iterator;
import java.util.HashMap;
import java.util.UUID;

/**
 * @author alex
 */
public class IncludedPolicyReference extends ExternalReference {
    private static final String ATTR_OID = "oid";
    private static final String ATTR_GUID = "guid";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_SOAP = "soap";
    private static final String ATTR_INCLUDED = "included";
    private static final String ATTR_INTERNAL_TAG = "internalTag";

    public static enum UseType {
        IMPORT,
        USE_EXISTING,
        RENAME
    }

    private static final Logger logger = Logger.getLogger(IncludedPolicyReference.class.getName());

    private Long oid;
    private String guid;
    private String name;
    private PolicyType type;
    private Boolean soap;
    private String xml;
    private String internalTag;

    private boolean fromImport = false;
    private String oldName;

    private UseType useType;

    public IncludedPolicyReference(PolicyReference policyReference) {
        this.guid = policyReference.retrievePolicyGuid();

        if(policyReference instanceof Include) {
            this.name = ((Include)policyReference).getPolicyName();
        }
        
        try {
            Policy policy = Registry.getDefault().getPolicyAdmin().findPolicyByGuid(policyReference.retrievePolicyGuid());
            this.name = (name == null)? policy.getName() : name;
            this.type = policy.getType();
            this.soap = policy.isSoap();
            this.xml = policy.getXml();
            this.internalTag = policy.getInternalTag();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unable to resolve included policy #{0} --storing OID only", new Object[] { policyReference.retrievePolicyGuid() });
        }
    }

    void serializeToRefElement(Element referencesParentElement) {
        Element includeEl = referencesParentElement.getOwnerDocument().createElement(getClass().getSimpleName());
        includeEl.setAttribute(ATTR_GUID, guid);
        includeEl.setAttribute(ExporterConstants.REF_TYPE_ATTRNAME, getClass().getName());
        if (name != null) includeEl.setAttribute(ATTR_NAME, name);
        if (type != null) includeEl.setAttribute(ATTR_TYPE, type.name());
        if (soap != null) includeEl.setAttribute(ATTR_SOAP, soap.toString());
        if (internalTag != null) includeEl.setAttribute(ATTR_INTERNAL_TAG, internalTag);
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

    boolean verifyReference() throws InvalidPolicyStreamException {
        try {
            Policy policy = Registry.getDefault().getPolicyAdmin().findPolicyByGuid(guid);
            
            if (policy == null) {
                logger.log(Level.INFO, MessageFormat.format("Policy #{0} ({1}) does not exist on this system; importing", guid, name));
                useType = UseType.IMPORT;

                // Check to see there is a policy with the same name already
                try {
                    policy = Registry.getDefault().getPolicyAdmin().findPolicyByUniqueName(name);
                    if(policy != null) {
                        return false;
                    }
                } catch(Exception e) {
                }

                return true;
            }

            Policy tempPolicy = new Policy(type, name, xml, soap);
            String comparableXml = WspWriter.getPolicyXml(tempPolicy.getAssertion());
            if (policy.getType() == type && policy.isSoap() == soap &&
                    (internalTag == null && policy.getInternalTag() == null || internalTag != null && internalTag.equals(policy.getInternalTag())))
            {
                boolean matches = policy.getXml().equals(comparableXml);
                if(!matches && fromImport) {
                    updateIncludeAssertionOids(tempPolicy.getAssertion(), new HashMap<String, String>());
                    comparableXml = WspWriter.getPolicyXml(tempPolicy.getAssertion());
                    updateIncludeAssertionOids(policy.getAssertion(), new HashMap<String, String>());
                    String otherXml = WspWriter.getPolicyXml(policy.getAssertion());

                    matches = comparableXml.equals(otherXml);
                }

                if(matches) {
                    guid = policy.getGuid();
                    logger.log(Level.INFO, "Existing Policy #{0} ({1}) is essentially identical to the imported version, using existing version", new Object[] { guid, name });
                    useType = UseType.USE_EXISTING;
                    return true;
                }
            }

            throw new PolicyConflictException("The imported policy fragment " + name + " does not match the existing policy.", name, policy.getName(), guid);
        } catch(InvalidPolicyStreamException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to determine whether imported policy already present");
            return false;
        }
    }

    /**
     * Sets the policy OID for include assertions to dummy values in a systematic way. This is useful for
     * comparing two policies that may have been created on different systems.
     * @param rootAssertion The root assertion of the policy to update
     * @param nameGuidMap A map of policy OIDs. This is updated as this method is run.
     */
    private void updateIncludeAssertionOids(Assertion rootAssertion, HashMap<String, String> nameGuidMap) {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            for(Iterator it = compAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                updateIncludeAssertionOids(child, nameGuidMap);
            }
        } else if(rootAssertion instanceof Include) {
            Include includeAssertion = (Include)rootAssertion;
            if(!nameGuidMap.containsKey(includeAssertion.getPolicyName())) {
                nameGuidMap.put(includeAssertion.getPolicyName(), Long.toString(nameGuidMap.size() + 1L));
            }
            includeAssertion.setPolicyGuid(nameGuidMap.get(includeAssertion.getPolicyName()));
        }
    }

    boolean localizeAssertion(Assertion assertionToLocalize) {
        if (assertionToLocalize instanceof Include) {
            Include include = (Include) assertionToLocalize;
            if(include.getPolicyGuid() == null && include.getPolicyOid() != null && include.getPolicyOid().equals(oid)) {
                include.setPolicyOid(null);
                include.setPolicyGuid(guid);
            } else if(guid.equals(include.getPolicyGuid()) && useType == UseType.RENAME) {
                include.setPolicyName(name);
            }
        }
        return true;
    }

    public static IncludedPolicyReference parseFromElement(Element el) {
        String guid = el.getAttribute(ATTR_GUID);
        Long oid = null;
        if (guid == null || guid.length() <= 0) {
            String oidString = el.getAttribute(ATTR_OID);
            if(oidString == null || oidString.length() <= 0) {
                throw new IllegalArgumentException("No " + ATTR_GUID + " or " + ATTR_OID + " attribute found");
            }

            oid = new Long(oidString);
            String name = el.getAttribute(ATTR_NAME);
            String uuidString = oidString + "#" + name;
            guid = UUID.nameUUIDFromBytes(uuidString.getBytes()).toString();
        }

        String name = el.getAttribute(ATTR_NAME);
        IncludedPolicyReference ipr = new IncludedPolicyReference(new Include(guid, name));
        if(ipr.getName() == null) {
            ipr.setName(name);
        }
        if(oid != null) {
            ipr.setOid(oid);
        }
        ipr.soap = Boolean.TRUE.toString().equals(el.getAttribute(ATTR_SOAP));
        ipr.type = PolicyType.valueOf(el.getAttribute(ATTR_TYPE));
        Attr internalTagAttribute = el.getAttributeNode(ATTR_INTERNAL_TAG);
        if(internalTagAttribute != null) {
            ipr.internalTag = internalTagAttribute.getValue();
        }
        if (Boolean.TRUE.toString().equals(el.getAttribute(ATTR_INCLUDED))) {
            Element includedPolicy = DomUtils.findFirstChildElement(el);
            if (includedPolicy == null) throw new IllegalArgumentException("included=\"true\" but no child element");
            try {
                ipr.xml = XmlUtil.nodeToString(includedPolicy);
            } catch (IOException e) {
                throw new RuntimeException(e); // Can't happen
            }
        }
        return ipr;
    }

    public String getGuid() {
        return guid;
    }

    public Long getOid() {
        return oid;
    }
    public void setOid(Long oid) {
        this.oid = oid;
    }
    
    public PolicyType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getXml() {
        return xml;
    }

    public Boolean isSoap() {
        return soap;
    }

    public String getInternalTag() {
        return internalTag;
    }

    public void setInternalTag(String internalTag) {
        this.internalTag = internalTag;
    }

    public void setFromImport(boolean fromImport) {
        this.fromImport = fromImport;
    }

    public UseType getUseType() {
        return useType;
    }

    public void setUseType(UseType useType) {
        this.useType = useType;
    }

    public String getOldName() {
        if(useType != UseType.RENAME) {
            return null;
        } else {
            return oldName;
        }
    }

    public void setOldName(String oldName) {
        if(useType == UseType.RENAME) {
            this.oldName = oldName;
        }
    }
}
