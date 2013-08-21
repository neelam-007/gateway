package com.l7tech.external.assertions.siteminder;

import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.InvalidDocumentFormatException;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.logging.Logger;

public class SiteMinderExternalReference extends ExternalReference {
    private final Logger logger = Logger.getLogger(SiteMinderExternalReference.class.getName());

    public static final String ELMT_NAME_REF = "SiteMinderConfigurationReference";
    private LocalizeAction localizeType;
    private Goid identifier;

    private SiteMinderConfiguration siteMinderConfiguration;

    protected SiteMinderExternalReference(final ExternalReferenceFinder finder) {
        super(finder);
    }

    protected SiteMinderExternalReference(final ExternalReferenceFinder finder, final SiteMinderConfiguration config) {
        super(finder);
        siteMinderConfiguration = config;
    }

    protected SiteMinderExternalReference(final ExternalReferenceFinder finder, String name) {
        super(finder);
        try {
            siteMinderConfiguration = getFinder().findSiteMinderConfigurationByName(name);
        } catch (FindException e) {
            logger.warning("Cannot find the SiteMinder Configuration entity (name = " + name + ").");
        }
    }

    public SiteMinderConfiguration getSiteMinderConfiguration() {
        return siteMinderConfiguration;
    }

    @Override
    public boolean setLocalizeDelete() {
        localizeType = LocalizeAction.DELETE;
        return true;
    }

    @Override
    public void setLocalizeIgnore() {
        localizeType = LocalizeAction.IGNORE;
    }

    @Override
    public boolean setLocalizeReplace(Goid identifier) {
        this.identifier = identifier;
        localizeType = LocalizeAction.REPLACE;
        return true;
    }

    @Override
    protected void serializeToRefElement(Element referencesParentElement) {
        final Document doc = referencesParentElement.getOwnerDocument();
        Element referenceElement = doc.createElement(ELMT_NAME_REF);
        setTypeAttribute(referenceElement);

        try {
            JAXBContext context = JAXBContext.newInstance(SiteMinderConfiguration.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            SiteMinderConfiguration copy = new SiteMinderConfiguration();
            copy.copyFrom(siteMinderConfiguration);
            copy.setSecret(null);
            copy.setPasswordGoid(null);
            copy.setGoid(PersistentEntity.DEFAULT_GOID);
            marshaller.marshal(copy, referenceElement);

        } catch (JAXBException e) {
            throw new IllegalArgumentException("Unable to save SiteMinder configuration reference.");
        }
        referencesParentElement.appendChild(referenceElement);
    }

    @Override
    protected boolean verifyReference() throws InvalidPolicyStreamException {
        if (siteMinderConfiguration != null) {
            try {
                SiteMinderConfiguration localSiteMinderConfig = getFinder().findSiteMinderConfigurationByName(siteMinderConfiguration.getName());
                if (localSiteMinderConfig != null) {
                    if (localSiteMinderConfig.equalsConfiguration(siteMinderConfiguration)) {
                        return true;
                    }
                }
            } catch (FindException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    protected boolean localizeAssertion(@Nullable Assertion assertionToLocalize) {
        if (localizeType != LocalizeAction.IGNORE) {
            if (assertionToLocalize instanceof SiteMinderCheckProtectedAssertion) {
                final SiteMinderCheckProtectedAssertion assertion = (SiteMinderCheckProtectedAssertion) assertionToLocalize;
                if (assertion.getAgentID().equalsIgnoreCase(siteMinderConfiguration.getName())) { // The purpose of "equals" is to find the right assertion and update it using localized value.
                    if (localizeType == LocalizeAction.REPLACE) {
                        try {
                            SiteMinderConfiguration config = getFinder().findSiteMinderConfigurationByID(identifier);
                            assertion.setAgentID(config.getName());
                        } catch (FindException e) {
                            logger.info("Unable to find SiteMinder Configuration.");
                        }
                    } else if (localizeType == LocalizeAction.DELETE) {
                        logger.info("Deleted this assertion from the tree.");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static ExternalReference parseFromElement(ExternalReferenceFinder finder, Element el) throws InvalidDocumentFormatException {

        // make sure passed element has correct connectorName
        if (!el.getNodeName().equals(ELMT_NAME_REF)) {
            throw new InvalidDocumentFormatException("Expecting element of SiteMinder configuration " + ELMT_NAME_REF);
        }
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                try {
                    JAXBContext context = JAXBContext.newInstance(SiteMinderConfiguration.class);
                    Unmarshaller unmarshaller = context.createUnmarshaller();
                    SiteMinderConfiguration siteMinderConfiguration = (SiteMinderConfiguration) unmarshaller.unmarshal(child);
                    siteMinderConfiguration.setGoid(PersistentEntity.DEFAULT_GOID);
                    siteMinderConfiguration.setPasswordGoid(null);
                    return new SiteMinderExternalReference(finder, siteMinderConfiguration);

                } catch (JAXBException e) {
                }
            }
        }
        throw new InvalidDocumentFormatException("Unable to load SiteMinder configuration reference.");
    }
}
