package com.l7tech.policy.exporter;

import org.w3c.dom.Element;
import org.w3c.dom.Text;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UnknownAssertion;
import com.l7tech.console.util.Registry;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.rmi.RemoteException;

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
        if (!el.getNodeName().equals(REF_EL_NAME)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + REF_EL_NAME);
        }
        CustomAssertionReference output = new CustomAssertionReference();
        output.customAssertionName = getParamFromEl(el, ASSNAME_EL_NAME);
        return output;
    }

    public String getCustomAssertionName() {
        return customAssertionName;
    }

    public void setLocalizeDelete() {
        localizeType = LocaliseAction.DELETE;
    }

    public void setLocalizeIgnore() {
        localizeType = LocaliseAction.IGNORE;
    }

    private CustomAssertionReference() {
        super();
    }

    public static class LocaliseAction {
        public static final LocaliseAction IGNORE = new LocaliseAction(1);
        public static final LocaliseAction DELETE = new LocaliseAction(2);
        private LocaliseAction(int val) {
            this.val = val;
        }
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LocaliseAction)) return false;

            final LocaliseAction localiseAction = (LocaliseAction) o;

            if (val != localiseAction.val) return false;

            return true;
        }
        public int hashCode() {
            return val;
        }
        private int val = 0;
    }

    void serializeToRefElement(Element referencesParentElement) {
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

    /**
     * Checks whether or not an external reference can be mapped on this local
     * system without administrator interaction.
     *
     * LOGIC:
     * Load all registered remote assertion through CustomAssertionsRegistrar
     * and look for one with same name as the one from this reference.
     */
    boolean verifyReference() {
        final CustomAssertionsRegistrar cr = Registry.getDefault().getCustomAssertionsRegistrar();
        Collection assertins = null;
        try {
            assertins = cr.getAssertions();
            for (Iterator iterator = assertins.iterator(); iterator.hasNext();) {
                CustomAssertionHolder cah = (CustomAssertionHolder)iterator.next();
                String thisname = cah.getCustomAssertion().getName();
                if (thisname != null && thisname.equals(customAssertionName)) {
                    // WE HAVE A MATCH!
                    logger.fine("Custom assertion " + customAssertionName + " found on local system.");
                    localizeType = LocaliseAction.IGNORE;
                    return true;
                }
            }
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "Cannot get remote assertions", e);
        }
        logger.warning("the custom assertion " + customAssertionName + " does not seem to exist on this system.");
        return false;
    }

    boolean localizeAssertion(Assertion assertionToLocalize) {
        if (localizeType == LocaliseAction.IGNORE) return true;
        // we need to instruct deletion for assertions that refer to this
        if (assertionToLocalize instanceof CustomAssertionHolder) {
            CustomAssertionHolder cahAss = (CustomAssertionHolder)assertionToLocalize;
            if (customAssertionName.equals(cahAss.getCustomAssertion().getName())) {
                return false;
            }
        } else if (assertionToLocalize instanceof UnknownAssertion) {
            return false;
        }
        return true;
    }

    private final Logger logger = Logger.getLogger(CustomAssertionReference.class.getName());

    private String customAssertionName;
    public static final String REF_EL_NAME = "CustomAssertionReference";
    public static final String ASSNAME_EL_NAME = "CustomAssertionName";

    private LocaliseAction localizeType = null;
}
