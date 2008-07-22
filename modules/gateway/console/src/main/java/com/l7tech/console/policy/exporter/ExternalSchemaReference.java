package com.l7tech.console.policy.exporter;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.DomUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

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

    public ExternalSchemaReference(String name, String tns) {
        this.name = name;
        this.tns = tns;
    }

    public static ExternalSchemaReference parseFromElement(Element el) throws InvalidDocumentFormatException {
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
        return new ExternalSchemaReference(name, tns);
    }

    public String getName() {
        return name;
    }

    public String getTns() {
        return tns;
    }

    void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(TOPEL_NAME);
        refEl.setAttribute(ExporterConstants.REF_TYPE_ATTRNAME, ExternalSchemaReference.class.getName());
        if (name != null) {
            refEl.setAttribute(LOC_ATTR_NAME, name);
        }
        if (tns != null) {
            refEl.setAttribute(TNS_ATTR_NAME, tns);
        }
        referencesParentElement.appendChild(refEl);
    }

    boolean verifyReference() {
        // check that the schema is present on this target system
        Registry reg = Registry.getDefault();
        if (reg == null || reg.getSchemaAdmin() == null) {
            throw new RuntimeException("No access to registry. Cannot check for external reference.");
        }
        try {
            if (name == null || reg.getSchemaAdmin().findByName(name).isEmpty()) {
                return false;
                /* dont allow resolution from ns
                if (tns == null || reg.getSchemaAdmin().findByTNS(tns).isEmpty()) {
                    return false;
                }
                */
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

    boolean localizeAssertion(Assertion assertionToLocalize) {
        if (removeRefferees) {
            // check if this assertion indeed refers to this external schema
            if (assertionToLocalize instanceof SchemaValidation) {
                SchemaValidation ass = (SchemaValidation)assertionToLocalize;
                if (ass.getResourceInfo() instanceof StaticResourceInfo) {
                    StaticResourceInfo sri = (StaticResourceInfo)ass.getResourceInfo();
                    final ArrayList imports;
                    try {
                        imports = listImports( XmlUtil.stringToDocument(sri.getDocument()));
                    } catch (SAXException e) {
                        logger.log(Level.SEVERE, "cannot parse schema");
                        // nothing more to do since assertion has no valid xml, nothing to localize
                        return true;
                    }
                    for (Iterator iterator = imports.iterator(); iterator.hasNext();) {
                        ListedImport listedImport = (ListedImport) iterator.next();
                        if (listedImport.name.equals(name)) return false;
                        if (listedImport.tns.equals(tns)) return false;
                    }
                }
            }
        }
        // nothing to do here. the assertion does not need to be localized once the external schema has been added
        // since the link is obvious through tns or schemalocation attribute
        return true;
    }

    static class ListedImport {
        public ListedImport(String name, String tns) {
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
        Element schemael = schemaDoc.getDocumentElement();
        List<Element> listofimports = DomUtils.findChildElementsByName(schemael, schemael.getNamespaceURI(), "import");
        ArrayList output = new ArrayList();
        if (listofimports.isEmpty()) return output;

        for (Element importEl : listofimports) {
            String importns = importEl.getAttribute("namespace");
            String importloc = importEl.getAttribute("schemaLocation");
            output.add(new ListedImport(importloc, importns));
        }
        return output;
    }

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

    public void setRemoveRefferees(boolean removeRefferees) {
        this.removeRefferees = removeRefferees;
    }

    private String name;
    private String tns;
    private boolean removeRefferees = false;
    private static final String TOPEL_NAME = "ExternalSchema";
    private static final String LOC_ATTR_NAME = "schemaLocation";
    private static final String TNS_ATTR_NAME = "targetNamespace";
}
