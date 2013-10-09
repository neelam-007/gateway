package com.l7tech.policy.exporter;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.*;
import org.xml.sax.EntityResolver;

import java.io.IOException;
import java.util.*;

/**
 * An external reference used by an exported policy.
 *
 * <p>Subclasses should implement equals and hashCode such that equivalent
 * references on the source system are equal.</p>
 */
public abstract class ExternalReference {

    //- PUBLIC

    /**
     * Get the (possibly composite) identifier for the reference.
     *
     * <p>If a reference does not support identifiers then null is returned.</p>
     *
     * @return An identifier for the referenced dependency or null
     */
    public String getRefId() {
        return null;
    }

    /**
     * Get a synthetic identifier for the reference.
     *
     * <p>When a natural identifier is not available the synthetic identifier
     * can be used to identify the reference target.</p>
     *
     * <p>The synthetic identifier is a (prefixed) GUID generated from the
     * serialized form of the reference. Two references with the same content
     * will therefore have the same identifier.</p>
     *
     * @return The synthetic identifier
     */
    public final String getSyntheticRefId() {
        try {
            Document doc = XmlUtil.createEmptyDocument( "reference", null, null );
            serializeToRefElement( doc.getDocumentElement() );
            return "syn:" + UUID.nameUUIDFromBytes( XmlUtil.toByteArray(doc) ).toString();
        } catch ( IOException ioe ) {
            throw ExceptionUtils.wrap( ioe ); // should not occur since writing to byte array
        }
    }

    /**
     * Get the type for this reference.
     *
     * @return The reference type.
     */
    public String getRefType() {
        return getReferenceType(getClass());
    }

    /**
     * Configure this reference to rename on import.
     *
     * <p>This applies when a dependency that will be created as the result of
     * an import conflict with the name of an existing entity.</p>
     *
     * <p>References that do not support this behaviour will always return
     * false.</p>
     *
     * @return true if successful
     * @see #localizeAssertion(Assertion)
     */
    public boolean setLocalizeRename( final String name ) {
        return false;
    }

    /**
     * Configure this reference to be replaced on import.
     *
     * <p>This applies when a dependency of the imported policy should be
     * mapped to an existing dependency.</p>
     *
     * <p>References that use <code>long</code> identifier should override
     * {@code localizeReplace(long)} rather than this method.</p>
     *
     * <p>References that do not support this behaviour will always return
     * false.</p>
     *
     * @param identifier The identifier for the existing dependency
     * @return true if successful
     * @see #localizeAssertion(Assertion)
     * @see #setLocalizeReplace(Goid)
     */
    public boolean setLocalizeReplace( final String identifier ) {
        boolean localized = false;
        try {
            localized = setLocalizeReplace( new Goid(identifier) );
        } catch ( IllegalArgumentException ile ) {
            // not localized
        }
        return localized;
    }

    /**
     * Configure this reference to be removed on import.
     *
     * <p>This applies when any assertion with this dependency should be
     * deleted from the policy.</p>
     *
     * <p>References that do not support this behaviour will always return
     * false.</p>
     *
     * @return true if successful
     * @see #localizeAssertion(Assertion)
     */
    public boolean setLocalizeDelete() {
        return false;
    }

    /**
     * Configure this reference to be unchanged on import.
     *
     * <p>This applies when any assertion with this dependency should be
     * left as-is, possibly resulting in an invalid policy.</p>
     *
     * <p>All references must support this behaviour.</p>
     *
     * @see #localizeAssertion(Assertion)
     */
    public abstract void setLocalizeIgnore();

    /**
     * Configure this reference to be replaced on import.
     *
     * <p>This applies when a dependency of the imported policy should be
     * mapped to an existing dependency.</p>
     *
     * <p>References that do not support this behaviour will always return
     * false.</p>
     *
     * @param identifier The identifier for the existing dependency
     * @return true if successful
     * @see #localizeAssertion(Assertion)
     */
    public boolean setLocalizeReplace( Goid identifier ) {
        return false;
    }

    //- PROTECTED

    protected ExternalReference( final ExternalReferenceFinder finder ) {
        this.finder = finder;
    }

    protected final ExternalReferenceFinder getFinder() {
        return finder;
    }

    /**
     * Adds a child element to the passed references element that contains the xml
     * form of this reference object. Used by the policy exporter when serializing
     * references to xml format.
     * @param referencesParentElement
     */
    protected abstract void serializeToRefElement(Element referencesParentElement);

    /**
     * Checks whether or not an external reference can be mapped on this local
     * system without administrator interaction.
     */
    protected abstract boolean verifyReference() throws InvalidPolicyStreamException;

    /**
     * Once an exported policy is loaded with it's references and the references are
     * verified, this method will apply the necessary changes to the assertion. If
     * the assertion type passed does not relate to the reference, it will be left
     * untouched.
     * Returns false if the assertion should be deleted from the tree.
     * @param assertionToLocalize will be fixed once this method returns.  If null, this method will take no action.
     */
    protected abstract boolean localizeAssertion(@Nullable Assertion assertionToLocalize);

    protected static String getParamFromEl(Element parent, String param) {
        NodeList nodeList = parent.getElementsByTagName(param);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element)nodeList.item(i);
            String val = DomUtils.getTextValue(node);
            if (val != null && val.length() > 0) return val;
        }
        return null;
    }

    protected static String getRequiredParamFromEl( final Element parent,
                                          final String param ) throws InvalidDocumentFormatException {
        final String value = getParamFromEl( parent, param );
        if ( value == null ) {
            throw new InvalidDocumentFormatException( parent.getLocalName() + " missing required element " + param );
        }
        return value;
    }

    protected static void addParamEl( final Element parent,
                                      final String param,
                                      final String value,
                                      final boolean alwaysAdd ) {
        if ( value != null || alwaysAdd ) {
            final Element paramElement = parent.getOwnerDocument().createElementNS( null, param );
            parent.appendChild( paramElement );

            if ( value != null ) {
                final Text textNode = DomUtils.createTextNode(parent, value);
                paramElement.appendChild( textNode );
            }
        }
    }

    protected final void setTypeAttribute( final Element refEl ) {
        refEl.setAttributeNS( null, ExporterConstants.REF_TYPE_ATTRNAME, getRefType() );
    }

    protected boolean permitMapping( final Goid importGoid, final Goid targetGoid ) {
        return permitMapping( importGoid==null?"":Goid.toString(importGoid),targetGoid==null?"":Goid.toString(targetGoid) );
    }

    @Deprecated
    protected boolean permitMapping( final long importOid, final long targetOid ) {
        return permitMapping( Long.toString( importOid ), Long.toString( targetOid ) );
    }

    protected boolean permitMapping( final String importId, final String targetId ) {
        boolean proceed = true;
        PolicyImporter.PolicyImporterAdvisor advisor = this.advisor;
        if ( !importId.equals( targetId ) && advisor != null ) {
            proceed = advisor.mapReference( getRefType(), importId, targetId );
        }
        return proceed;
    }

    protected enum LocalizeAction { DELETE, IGNORE, REPLACE }

    //- PACKAGE

    /**
     * Parse references from an exported policy's exp:References element.
     * @param refElements an ExporterConstants.EXPORTED_REFERENCES_ELNAME element
     */
    static Collection<ExternalReference> parseReferences(final ExternalReferenceFinder finder,
                                                         final EntityResolver entityResolver,
                                                         final Set<ExternalReferenceFactory> factories,
                                                         final Element refElements) throws InvalidDocumentFormatException {
        // Verify that the passed element is what is expected
        if (!refElements.getLocalName().equals(ExporterConstants.EXPORTED_REFERENCES_ELNAME)) {
            throw new InvalidDocumentFormatException("The passed element must be " +
                                                     ExporterConstants.EXPORTED_REFERENCES_ELNAME);
        }
        if (!refElements.getNamespaceURI().equals(ExporterConstants.EXPORTED_POL_NS)) {
            throw new InvalidDocumentFormatException("The passed element must have namespace " +
                                                     ExporterConstants.EXPORTED_POL_NS);
        }
        // Go through child elements and process them one by one
        Collection<ExternalReference> references = new ArrayList<ExternalReference>();
        NodeList children = refElements.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element refEl = (Element)child;
                // Get the type of reference
                String refType = refEl.getAttribute(ExporterConstants.REF_TYPE_ATTRNAME);

                // Parse modular-assertion-related external references handled by ExternalReferenceFactory
                boolean found = false;
                if (factories != null) {
                    for (ExternalReferenceFactory factory: factories) {
                        if (factory.matchByExternalReference(refType)) {
                            references.add((ExternalReference) factory.parseFromElement(finder, refEl));
                            found = true;
                            break;
                        }
                    }
                }
                if (found) continue;

                // Parse external references not handled by ExternalReferenceFactory
                if (refType.equals(getReferenceType(FederatedIdProviderReference.class))) {
                    references.add(FederatedIdProviderReference.parseFromElement(finder, refEl));
                } else if (refType.equals(getReferenceType(IdProviderReference.class))) {
                    references.add(IdProviderReference.parseFromElement(finder, refEl));
                } else if (refType.equals(getReferenceType(JMSEndpointReference.class))) {
                    references.add(JMSEndpointReference.parseFromElement(finder, refEl));
                } else if (refType.equals(getReferenceType(CustomAssertionReference.class))) {
                    references.add(CustomAssertionReference.parseFromElement(finder, refEl));
                } else if (refType.equals(getReferenceType(ExternalSchemaReference.class))) {
                    references.add(ExternalSchemaReference.parseFromElement(finder, entityResolver, refEl));
                } else if (refType.equals(getReferenceType(IncludedPolicyReference.class))) {
                    references.add(IncludedPolicyReference.parseFromElement(finder, refEl));
                } else if (refType.equals(getReferenceType(TrustedCertReference.class))) {
                    references.add(TrustedCertReference.parseFromElement(finder, refEl));
                } else if (refType.equals(getReferenceType(PrivateKeyReference.class))) {
                    references.add(PrivateKeyReference.parseFromElement(finder, refEl));
                } else if (refType.equals(getReferenceType(JdbcConnectionReference.class))) {
                    references.add(JdbcConnectionReference.parseFromElement(finder, refEl));
                } else if (refType.equals(getReferenceType(StoredPasswordReference.class))) {
                    references.add(StoredPasswordReference.parseFromElement(finder, refEl));
                } else if (refType.equals(getReferenceType( GlobalResourceReference.class))) {
                    references.add( GlobalResourceReference.parseFromElement(finder, entityResolver, refEl));
                }
            }
        }
        return references;
    }

    static String getReferenceType( final Class<? extends ExternalReference> referenceClass ) {
        String type = referenceClass.getName();

        if ( repackagedReferences.contains( referenceClass )) {
            type = type.replaceFirst( "com.l7tech.policy.exporter.", "com.l7tech.console.policy.exporter." );
        }

        return type;
    }

    void warning( final String title, final String message ) {
        final ExternalReferenceErrorListener errorListener = this.errorListener;
        if ( errorListener != null ) {
            errorListener.warning( title, message );
        }
    }

    void setExternalReferenceErrorListener( final ExternalReferenceErrorListener errorListener ) {
        this.errorListener = errorListener;
    }

    void setPolicyImporterAdvisor( final PolicyImporter.PolicyImporterAdvisor advisor ) {
        this.advisor = advisor;        
    }

    //- PRIVATE

    private final ExternalReferenceFinder finder;
    private ExternalReferenceErrorListener errorListener;
    private PolicyImporter.PolicyImporterAdvisor advisor;
    private static final Set<Class<? extends ExternalReference>> repackagedReferences = Collections.unmodifiableSet( new HashSet<Class<? extends ExternalReference>>( Arrays.asList(
        FederatedIdProviderReference.class,
        IdProviderReference.class,
        JMSEndpointReference.class,
        CustomAssertionReference.class,
        ExternalSchemaReference.class,
        IncludedPolicyReference.class,
        TrustedCertReference.class,
        PrivateKeyReference.class,
        JdbcConnectionReference.class,
        EncapsulatedAssertionReference.class,
        StoredPasswordReference.class
    )) );
}