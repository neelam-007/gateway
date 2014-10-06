package com.l7tech.external.assertions.siteminder;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.exporter.ExternalReference;
import com.l7tech.policy.exporter.ExternalReferenceFinder;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SiteMinderExternalReference extends ExternalReference {
    private static final Logger logger = Logger.getLogger(SiteMinderExternalReference.class.getName());

    public static final String ELMT_NAME_REF = "SiteMinderConfigurationReference";
    public static final String GOID_EL_NAME = "GOID";
    private LocalizeAction localizeType;
    private Goid identifier;
    private Goid locallyMatchingIdentifier;

    private SiteMinderConfiguration siteMinderConfiguration;

    protected SiteMinderExternalReference(final ExternalReferenceFinder finder) {
        super(finder);
    }

    protected SiteMinderExternalReference(final ExternalReferenceFinder finder, final SiteMinderConfiguration config) {
        super(finder);
        siteMinderConfiguration = config;
        if (config != null) {
            identifier = siteMinderConfiguration.getGoid();
        }
    }

    protected SiteMinderExternalReference(final ExternalReferenceFinder finder, Goid agentGoid) {
        super(finder);
        try {
            siteMinderConfiguration = getFinder().findSiteMinderConfigurationByID(agentGoid);
        } catch (final FindException e) {
            logger.warning("Cannot find the SiteMinder Configuration entity (Goid = " + agentGoid + ").");
        }
        identifier = siteMinderConfiguration == null ? agentGoid : siteMinderConfiguration.getGoid();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SiteMinderExternalReference that = (SiteMinderExternalReference) o;

        if (identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return identifier != null ? identifier.hashCode() : 0;
    }

    @Nullable
    public SiteMinderConfiguration getSiteMinderConfiguration() {
        return siteMinderConfiguration;
    }

    @Override
    public String getRefId() {
        String id = null;

        if ( identifier != null && !identifier.equals(SiteMinderConfiguration.DEFAULT_GOID) ) {
            id = identifier.toString();
        }

        return id;
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
        locallyMatchingIdentifier = identifier;
        localizeType = LocalizeAction.REPLACE;
        return true;
    }

    @Override
    protected void serializeToRefElement(Element referencesParentElement) {
        final Document doc = referencesParentElement.getOwnerDocument();
        Element referenceElement = doc.createElement(ELMT_NAME_REF);
        setTypeAttribute(referenceElement);
        addElement(referenceElement, GOID_EL_NAME, identifier==null?Goid.DEFAULT_GOID.toString():identifier.toString());

        try {
            JAXBContext context = JAXBContext.newInstance(SiteMinderConfiguration.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            SiteMinderConfiguration copy = new SiteMinderConfiguration();
            if (siteMinderConfiguration != null) {
                copy.copyFrom(siteMinderConfiguration);
                copy.setSecret(null);
                copy.setPasswordGoid(null);
                marshaller.marshal(copy, referenceElement);
            }

        } catch (final JAXBException e) {
            throw new IllegalArgumentException("Unable to save CA Single Sign-On configuration reference.");
        }
        referencesParentElement.appendChild(referenceElement);
    }

    private void addElement( final Element parent,
                             final String childElementName,
                             final String text ) {
        Element childElement = parent.getOwnerDocument().createElement( childElementName );
        parent.appendChild(childElement);

        if ( text != null ) {
            Text textNode = DomUtils.createTextNode(parent, text);
            childElement.appendChild( textNode );
        }
    }

    @Override
    protected boolean verifyReference() throws InvalidPolicyStreamException {
        if (siteMinderConfiguration != null) {
            try {
                SiteMinderConfiguration localSiteMinderConfig = getFinder().findSiteMinderConfigurationByID(siteMinderConfiguration.getGoid());
                if (localSiteMinderConfig != null) {
                    if (localSiteMinderConfig.getGoid().equals(siteMinderConfiguration.getGoid())) {
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
                final Goid agentGoid = assertion.getAgentGoid();
                if ((siteMinderConfiguration != null && agentGoid.equals(siteMinderConfiguration.getGoid())) || (identifier != null && agentGoid.equals(identifier))) { // The purpose of "equals" is to find the right assertion and update it using localized value.
                    if (localizeType == LocalizeAction.REPLACE) {
                        if(!locallyMatchingIdentifier.equals(identifier)){
                            try {
                                SiteMinderConfiguration config = getFinder().findSiteMinderConfigurationByID(locallyMatchingIdentifier);
                                logger.info("The goid of the imported SiteMinderConfiguration has been changed from " + identifier + " to " + locallyMatchingIdentifier );
                                assertion.setAgentGoid(config.getGoid());
                                assertion.setAgentId(config.getName());
                            } catch (FindException e) {
                                logger.info("Unable to find CA Single Sign-On Configuration.");
                            }
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
            throw new InvalidDocumentFormatException("Expecting element of CA Single Sign-On configuration " + ELMT_NAME_REF);
        }
        SiteMinderExternalReference ref = null;
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                try {
                    JAXBContext context = JAXBContext.newInstance(SiteMinderConfiguration.class);
                    Unmarshaller unmarshaller = context.createUnmarshaller();
                    SiteMinderConfiguration siteMinderConfiguration = (SiteMinderConfiguration) unmarshaller.unmarshal(child);
                    siteMinderConfiguration.setPasswordGoid(null);
                    ref = new SiteMinderExternalReference(finder, siteMinderConfiguration);

                } catch (final JAXBException e) {
                    logger.log(Level.WARNING, "Unable to unmarshal SiteMinderConfiguration: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        }
        if (ref == null) {
            // try to parse the GOID
            Goid parsedGoid = null;
            try {
                final Element goidElem = XmlUtil.findExactlyOneChildElementByName(el, GOID_EL_NAME);
                if (goidElem != null && goidElem.getTextContent() != null) {
                    parsedGoid = Goid.parseGoid(goidElem.getTextContent());
                }
            } catch (final InvalidDocumentFormatException | IllegalArgumentException e) {
                logger.log(Level.WARNING, "Unable to parse SiteMinderConfiguration goid: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
            if (parsedGoid != null) {
                return new SiteMinderExternalReference(finder, parsedGoid);
            } else {
                throw new InvalidDocumentFormatException("Unable to load CA Single Sign-On configuration reference.");
            }
        } else {
            return ref;
        }
    }
}
