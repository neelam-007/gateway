package com.l7tech.policy.exporter;

import org.w3c.dom.Element;
import org.w3c.dom.Text;

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

    public void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement("CustomAssertionReference");
        refEl.setAttribute(ExporterConstants.REF_TYPE_ATTRNAME, CustomAssertionReference.class.getName());
        referencesParentElement.appendChild(refEl);
        Element nameEl = referencesParentElement.getOwnerDocument().createElement("CustomAssertionName");
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
}
