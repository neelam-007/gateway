package com.l7tech.policy.exporter;

import org.w3c.dom.Element;
import org.w3c.dom.Text;
import com.l7tech.common.xml.InvalidDocumentFormatException;

/**
 * A reference to a CustomAssertion type. This reference is exported alongside
 * a policy when a custom assertion of that type exists in the policy. At import
 * time, the importer will verify that this type of custom assertion is installed
 * before importing the policy.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 20, 2004<br/>
 * $Id$<br/>
 */
public class CustomAssertionReference extends ExternalReference {
    public CustomAssertionReference(String customAssertionName) {
        this.customAssertionName = customAssertionName;
    }

    public static CustomAssertionReference parseFromElement(Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!el.getLocalName().equals(REF_EL_NAME)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + REF_EL_NAME);
        }
        CustomAssertionReference output = new CustomAssertionReference();
        output.customAssertionName = getParamFromEl(el, ASSNAME_EL_NAME);
        return output;
    }

    private CustomAssertionReference() {
        super();
    }

    public void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(REF_EL_NAME);
        refEl.setAttribute(ExporterConstants.REF_TYPE_ATTRNAME, CustomAssertionReference.class.getName());
        referencesParentElement.appendChild(refEl);
        Element nameEl = referencesParentElement.getOwnerDocument().createElement(ASSNAME_EL_NAME);
        if (customAssertionName != null) {
            Text txt = referencesParentElement.getOwnerDocument().createTextNode(customAssertionName);
            nameEl.appendChild(txt);
        }
        refEl.appendChild(nameEl);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomAssertionReference)) return false;

        final CustomAssertionReference customAssertionReference = (CustomAssertionReference) o;

        if (customAssertionName != null ? !customAssertionName.equals(customAssertionReference.customAssertionName) : customAssertionReference.customAssertionName != null) return false;

        return true;
    }

    public int hashCode() {
        return (customAssertionName != null ? customAssertionName.hashCode() : 0);
    }

    private String customAssertionName;
    public static final String REF_EL_NAME = "CustomAssertionReference";
    public static final String ASSNAME_EL_NAME = "CustomAssertionName";
}
