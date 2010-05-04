package com.l7tech.policy.exporter;

import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.assertion.GlobalResourceInfo;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.objectmodel.FindException;
import com.l7tech.xml.DocumentReferenceProcessor;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

/**
 * A reference to an imported schema from a schema validation assertion
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Oct 19, 2005<br/>
 */
public class ExternalSchemaReference extends ExternalReference {
    private final Logger logger = Logger.getLogger(ExternalSchemaReference.class.getName());

    public ExternalSchemaReference( final ExternalReferenceFinder finder,
                                    final String name,
                                    final String tns ) {
        super(finder);
        this.name = name;
        this.tns = tns;
    }

    public static ExternalSchemaReference parseFromElement( final ExternalReferenceFinder finder,
                                                            final Element el ) throws InvalidDocumentFormatException {
        if (!el.getNodeName().equals(TOPEL_NAME)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + TOPEL_NAME);
        }
        String name = null;
        if (el.hasAttribute(LOC_ATTR_NAME)) {
            name = el.getAttribute(LOC_ATTR_NAME);
        }
        String tns = null;
        if (el.hasAttribute(TNS_ATTR_NAME)) {
            tns = el.getAttribute(TNS_ATTR_NAME);
        }
        return new ExternalSchemaReference(finder, name, tns);
    }

    public String getName() {
        return name;
    }

    public String getTns() {
        return tns;
    }

    @Override
    void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(TOPEL_NAME);
        setTypeAttribute( refEl );
        if (name != null) {
            refEl.setAttribute(LOC_ATTR_NAME, name);
        }
        if (tns != null) {
            refEl.setAttribute(TNS_ATTR_NAME, tns);
        }
        referencesParentElement.appendChild(refEl);
    }

    @Override
    boolean verifyReference() {
        // check that the schema is present on this target system
        try {
            if (name == null || getFinder().findSchemaByName(name).isEmpty()) {
                return tns != null && !getFinder().findSchemaByTNS(tns).isEmpty();
            }
        } catch (RuntimeException e) {
            logger.log(Level.SEVERE, "error using schema admin layer", e);
            throw new RuntimeException(e);
        } catch (FindException e) {
            logger.log(Level.SEVERE, "error using schema admin layer", e);
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    boolean localizeAssertion(Assertion assertionToLocalize) {
        if (assertionToLocalize instanceof SchemaValidation) {
            final SchemaValidation schemaVal = (SchemaValidation) assertionToLocalize;
            final AssertionResourceInfo schemaResource = schemaVal.getResourceInfo();

            if ( localizeType == LocalizeAction.DELETE ) {
                // check if this assertion indeed refers to this external schema
                if (schemaResource instanceof StaticResourceInfo) {
                    if ( name != null || tns != null ) {
                        try {
                            final Document schema = XmlUtil.stringToDocument(((StaticResourceInfo) schemaResource).getDocument());

                            // check schema imports, if any
                            for (ExternalSchemaReference.ListedImport listedImport : listImports(schema)) {
                                if (listedImport.name!=null && listedImport.name.equals(name)) return false;
                                if (listedImport.tns!=null && listedImport.tns.equals(tns)) return false;
                            }
                        } catch (SAXException e) {
                            logger.log(Level.SEVERE, "Cannot parse schema: " + ExceptionUtils.getMessage( e ));
                        }
                    }
                } else if (schemaResource instanceof GlobalResourceInfo && name != null) {
                    String globalSchemaName = ((GlobalResourceInfo) schemaResource).getId();
                    if (globalSchemaName.equals(name))
                        return false;
                }
            } else if ( localizeType == LocalizeAction.REPLACE && name != null) {
                if (schemaResource instanceof GlobalResourceInfo) {
                    final GlobalResourceInfo resourceInfo = (GlobalResourceInfo) schemaResource;
                    if (resourceInfo.getId()!=null && resourceInfo.getId().equals(name)) {
                        resourceInfo.setId( localName );       
                    }
                } else if (schemaResource instanceof StaticResourceInfo) {
                    StaticResourceInfo resourceInfo = (StaticResourceInfo) schemaResource;
                    try {
                        final Document schema = XmlUtil.stringToDocument(resourceInfo.getDocument());
                        final boolean[] updated = new boolean[]{false};
                        DocumentReferenceProcessor processor = DocumentReferenceProcessor.schemaProcessor();
                        processor.processDocumentReferences( schema, new DocumentReferenceProcessor.ReferenceCustomizer(){
                            @Override
                            public String customize( final Document document,
                                                     final Node node,
                                                     final String documentUrl,
                                                     final DocumentReferenceProcessor.ReferenceInfo referenceInfo ) {
                                String uri = null;
                                if ( name.equals( referenceInfo.getReferenceUrl() ) ) {
                                    updated[0] = true;
                                    uri = localName;
                                }
                                return uri;
                            }
                        } );
                        if ( updated[0] ) {
                            resourceInfo.setDocument( XmlUtil.nodeToString(schema) );
                        }
                    } catch (SAXException e) {
                        logger.log(Level.SEVERE, "Cannot parse schema: " + ExceptionUtils.getMessage( e ));
                    } catch ( IOException e) {
                        logger.log(Level.SEVERE, "Cannot update schema: " + ExceptionUtils.getMessage( e ));
                    }
                }
            }
        }

        return true;
    }

    static class ListedImport {
        ListedImport(final String name, final String tns) {
            this.name = name;
            this.tns = tns;
        }
        public String name;
        public String tns;
    }

    /**
     * @return An array list of ListedImport objects
     */
    static ArrayList<ListedImport> listImports(Document schemaDoc) {
        final ArrayList<ListedImport> output = new ArrayList<ListedImport>();
        final List<Element> dependencyElements = new ArrayList<Element>();
        final DocumentReferenceProcessor schemaReferenceProcessor = DocumentReferenceProcessor.schemaProcessor();
        schemaReferenceProcessor.processDocumentReferences( schemaDoc, new DocumentReferenceProcessor.ReferenceCustomizer(){
            @Override
            public String customize( final Document document,
                                     final Node node,
                                     final String documentUrl,
                                     final DocumentReferenceProcessor.ReferenceInfo referenceInfo ) {
                if ( node instanceof Element ) dependencyElements.add( (Element)node );
                return null;
            }
        } );

        for ( Element dependencyElement : dependencyElements ) {
            String schemaNamespace = "import".equals(dependencyElement.getLocalName())?
                    dependencyElement.getAttribute("namespace") : // we want empty, not null for a reference to a schema with no TNS
                    null;
            String schemaUrl = dependencyElement.hasAttribute("schemaLocation") ?
                    dependencyElement.getAttribute("schemaLocation") :
                    null;

            if ( schemaUrl != null || schemaNamespace != null ) {
                output.add(new ListedImport(schemaUrl, schemaNamespace));
            }
        }

        return output;
    }

    @SuppressWarnings({ "RedundantIfStatement" })
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ExternalSchemaReference that = (ExternalSchemaReference) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (tns != null ? !tns.equals(that.tns) : that.tns != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 29 * result + (tns != null ? tns.hashCode() : 0);
        return result;
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
    public boolean setLocalizeReplace( final String identifier ) {
        localizeType = LocalizeAction.REPLACE;
        localName = identifier;
        return true;
    }

    private String name;
    private String tns;
    private String localName;
    private LocalizeAction localizeType = LocalizeAction.IGNORE;
    private static final String TOPEL_NAME = "ExternalSchema";
    private static final String LOC_ATTR_NAME = "schemaLocation";
    private static final String TNS_ATTR_NAME = "targetNamespace";
}
