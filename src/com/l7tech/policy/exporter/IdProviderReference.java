package com.l7tech.policy.exporter;

import org.w3c.dom.Element;
import org.w3c.dom.Text;
import com.l7tech.common.util.Locator;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.FindException;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;
import java.util.Iterator;
import java.io.IOException;

/**
 * A reference to an id provider.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 16, 2004<br/>
 * $Id$<br/>
 */
public class IdProviderReference extends ExternalReference {
    /**
     * Constructor meant to be used by the export process.
     * This will actually retreive the id provider and remember its
     * settings.
     */
    public IdProviderReference(long providerId) {
        // try to retrieve this id provider to remember its settings
        IdentityProviderConfigManager manager = (IdentityProviderConfigManager)
                                                  Locator.getDefault().lookup(IdentityProviderConfigManager.class);
        IdentityProviderConfig config = null;
        if (manager != null) {
            try {
                config = manager.findByPrimaryKey(providerId);
            } catch (FindException e) {
                logger.log(Level.WARNING, "error finding id provider config", e);
            }
        }
        if (config == null) {
            logger.severe("This policy is referring to an Id provider that cannot be retrieved.");
        } else {
            try {
                setIdProviderConfProps(config.getSerializedProps());
                setProviderName(config.getName());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error getting properties from id provider", e);
            }
        }
        setProviderId(providerId);
    }

    public static IdProviderReference parseFromElement(Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!el.getNodeName().equals(REF_EL_NAME)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + REF_EL_NAME);
        }
        IdProviderReference output = new IdProviderReference();
        String val = getParamFromEl(el, OID_EL_NAME);
        if (val != null) {
            output.providerId = Long.parseLong(val);
        }
        output.providerName = getParamFromEl(el, NAME_EL_NAME);
        output.idProviderConfProps = getParamFromEl(el, PROPS_EL_NAME);
        return output;
    }

    private IdProviderReference() {
        super();
    }

    public void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(REF_EL_NAME);
        refEl.setAttribute(ExporterConstants.REF_TYPE_ATTRNAME, IdProviderReference.class.getName());
        referencesParentElement.appendChild(refEl);
        Element oidEl = referencesParentElement.getOwnerDocument().createElement(OID_EL_NAME);
        Text txt = referencesParentElement.getOwnerDocument().createTextNode(Long.toString(providerId));
        oidEl.appendChild(txt);
        refEl.appendChild(oidEl);
        Element nameEl = referencesParentElement.getOwnerDocument().createElement(NAME_EL_NAME);
        txt = referencesParentElement.getOwnerDocument().createTextNode(providerName);
        nameEl.appendChild(txt);
        refEl.appendChild(nameEl);
        Element propsEl = referencesParentElement.getOwnerDocument().createElement(PROPS_EL_NAME);
        if (idProviderConfProps != null) {
            txt = referencesParentElement.getOwnerDocument().createTextNode(idProviderConfProps);
            propsEl.appendChild(txt);
        }
        refEl.appendChild(propsEl);
    }

    /**
     * Checks whether or not an external reference can be mapped on this local
     * system without administrator interaction.
     *
     * LOGIC:
     * 1. Look for same oid and name. If that exists, => record perfect match.
     * 2. Look for same properties. If that exists, => record corresponding match.
     * 3. Otherwise => this reference if 'not verified'.
     */
    public boolean verifyReference() {
        // 1. Look for same oid and name. If that exists, => record perfect match.
        IdentityProviderConfigManager manager = (IdentityProviderConfigManager)
                                                  Locator.getDefault().lookup(IdentityProviderConfigManager.class);
        // We can't do anything without the id provider config manager.
        if (manager == null) {
            logger.severe("Cannot get an IdentityProviderConfigManager");
            return false;
        }
        IdentityProviderConfig configOnThisSystem = null;
        try {
            configOnThisSystem = manager.findByPrimaryKey(getProviderId());
        } catch (FindException e) {
            logger.log(Level.WARNING, "error getting id provider config", e);
        }
        if (configOnThisSystem != null && configOnThisSystem.getName().equals(getProviderName())) {
            // PERFECT MATCH!
            logger.fine("The id provider reference found the same provider locally.");
            locallyMatchingProviderId = getProviderId();
            return true;
        }
        // 2. Look for same properties. If that exists, => record corresponding match.
        Collection allConfigs = null;
        try {
            allConfigs = manager.findAll();
        } catch (FindException e) {
            logger.log(Level.WARNING, "error getting all id provider config", e);
            return false;
        }
        if (allConfigs != null) {
            for (Iterator i = allConfigs.iterator(); i.hasNext();) {
                configOnThisSystem = (IdentityProviderConfig)i.next();
                String localProps = null;
                try {
                    localProps = configOnThisSystem.getSerializedProps();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Cannot get serialized properties");
                    return false;
                }
                if (localProps != null && localProps.equals(this.getIdProviderConfProps())) {
                    // WE GOT A MATCH!
                    locallyMatchingProviderId = configOnThisSystem.getOid();
                    logger.fine("the provider was matched using the config's properties.");
                    return true;
                }
            }
        }
        // 3. Otherwise => this reference if 'not verified' and will require manual resolution.
        return false;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdProviderReference)) return false;
        final IdProviderReference idProviderReference = (IdProviderReference)o;
        if (providerId != idProviderReference.providerId) return false;
        return true;
    }

    public int hashCode() {
        return (int) (providerId ^ (providerId >>> 32));
    }

    public long getProviderId() {
        return providerId;
    }

    public void setProviderId(long providerId) {
        this.providerId = providerId;
    }

    public String getIdProviderConfProps() {
        return idProviderConfProps;
    }

    public void setIdProviderConfProps(String idProviderConfProps) {
        this.idProviderConfProps = idProviderConfProps;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    private long providerId;
    private long locallyMatchingProviderId;
    private String providerName;
    private String idProviderConfProps;
    private final Logger logger = Logger.getLogger(IdProviderReference.class.getName());

    public static final String REF_EL_NAME = "IDProviderReference";
    public static final String PROPS_EL_NAME = "Props";
    public static final String OID_EL_NAME = "OID";
    public static final String NAME_EL_NAME = "Name";
}
