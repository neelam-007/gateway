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
                manager.findByPrimaryKey(providerId);
            } catch (FindException e) {
                logger.log(Level.WARNING, "error finding id provider config", e);
            }
        }
        if (config == null) {
            logger.severe("This policy is referring to an Id provider that cannot be retrieved.");
        } else {
            try {
                setIdProviderConfProps(config.getSerializedProps());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error getting properties from id provider", e);
            }
        }
        setProviderId(providerId);
    }

    public static IdProviderReference parseFromElement(Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!el.getLocalName().equals(REF_EL_NAME)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + REF_EL_NAME);
        }
        IdProviderReference output = new IdProviderReference();
        String val = getParamFromEl(el, OID_EL_NAME);
        if (val != null) {
            output.providerId = Long.parseLong(val);
        }
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
        Element propsEl = referencesParentElement.getOwnerDocument().createElement(PROPS_EL_NAME);
        if (idProviderConfProps != null) {
            txt = referencesParentElement.getOwnerDocument().createTextNode(idProviderConfProps);
            propsEl.appendChild(txt);
        }
        refEl.appendChild(propsEl);
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

    private long providerId;
    private String idProviderConfProps;
    private final Logger logger = Logger.getLogger(IdProviderReference.class.getName());

    public static final String REF_EL_NAME = "IDProviderReference";
    public static final String PROPS_EL_NAME = "Props";
    public static final String OID_EL_NAME = "OID";
}
