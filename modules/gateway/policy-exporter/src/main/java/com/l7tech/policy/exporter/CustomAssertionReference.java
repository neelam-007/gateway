package com.l7tech.policy.exporter;

import org.w3c.dom.Element;
import org.w3c.dom.Text;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.DomUtils;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UnknownAssertion;

import java.util.Collection;
import java.util.regex.Pattern;
import java.util.logging.Logger;

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
 */
public class CustomAssertionReference extends ExternalReference {
    public CustomAssertionReference( final ExternalReferenceFinder finder,
                                     final String customAssertionName ) {
        this(finder);
        this.customAssertionName = customAssertionName;
    }

    public static CustomAssertionReference parseFromElement(final ExternalReferenceFinder finder,
                                                            final Element el) throws InvalidDocumentFormatException {
        // make sure passed element has correct name
        if (!el.getNodeName().equals(REF_EL_NAME)) {
            throw new InvalidDocumentFormatException("Expecting element of name " + REF_EL_NAME);
        }
        CustomAssertionReference output = new CustomAssertionReference(finder);
        output.customAssertionName = fixWhitespace(getParamFromEl(el, ASSNAME_EL_NAME));
        return output;
    }

    private static final Pattern FIX_WS = Pattern.compile("\\s+");

    /**
     * Fold repeated whitespace in s into a single space character.
     * Works around extra spaces introduced when the exported policy XML is reformatted (Bug #2916)
     *
     * @param s  the string to fold.  Must not be null.
     * @return the folded string.  Never null.
     */
    private static String fixWhitespace(String s) {
        return FIX_WS.matcher(s).replaceAll(" ");        
    }

    public String getCustomAssertionName() {
        return customAssertionName;
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

    private CustomAssertionReference(final ExternalReferenceFinder finder) {
        super(finder);
    }

    @Override
    protected void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(REF_EL_NAME);
        setTypeAttribute( refEl );
        referencesParentElement.appendChild(refEl);
        Element nameEl = referencesParentElement.getOwnerDocument().createElement(ASSNAME_EL_NAME);
        if (customAssertionName != null) {
            Text txt = DomUtils.createTextNode(referencesParentElement, customAssertionName);
            nameEl.appendChild(txt);
        }
        refEl.appendChild(nameEl);
    }

    @SuppressWarnings({ "RedundantIfStatement" })
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
    @Override
    protected boolean verifyReference() {
        final Collection assertions = getFinder().getAssertions();
        for ( final Object assertion : assertions ) {
            if ( assertion instanceof CustomAssertionHolder ) {
                final CustomAssertionHolder cah = (CustomAssertionHolder) assertion;
                final String name = cah.getCustomAssertion().getName();
                if ( name != null && name.equals( customAssertionName ) ) {
                    // WE HAVE A MATCH!
                    logger.fine( "Custom assertion " + customAssertionName + " found on local system." );
                    localizeType = LocalizeAction.IGNORE;
                    return true;
                }
            }
        }
        logger.warning("the custom assertion " + customAssertionName + " does not seem to exist on this system.");
        return false;
    }

    @Override
    protected boolean localizeAssertion(Assertion assertionToLocalize) {
        if (localizeType == LocalizeAction.IGNORE) return true;
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

    private LocalizeAction localizeType = null;
}