package com.l7tech.policy.exporter;

import com.l7tech.objectmodel.GuidEntityHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.util.DomUtils;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * This is a reference to an EncapsulatedAssertionConfig, it is only used to add the encass external reference to the
 * exported policy xml. It is not used in import.
 *
 * @author Victor Kazakov
 */
public class EncapsulatedAssertionReference extends ExternalReference {
    public static final String REF_EL_NAME = "EncapsulatedAssertionReference";
    public static final String ENCASS_NAME_EL_NAME = "Name";
    public static final String ENCASS_GUID_EL_NAME = "Guid";

    private String name;
    private String guid;

    protected EncapsulatedAssertionReference(final ExternalReferenceFinder finder) {
        super(finder);
    }

    public EncapsulatedAssertionReference(final ExternalReferenceFinder finder,
                                          final GuidEntityHeader header) {
        this(finder);
        name = header.getName();
        guid = header.getGuid();
    }

    @Override
    protected void serializeToRefElement(Element referencesParentElement) {
        Element refEl = referencesParentElement.getOwnerDocument().createElement(REF_EL_NAME);
        setTypeAttribute(refEl);
        referencesParentElement.appendChild(refEl);

        addElement(refEl, ENCASS_NAME_EL_NAME, name);
        addElement(refEl, ENCASS_GUID_EL_NAME, guid);
    }

    private void addElement(final Element parent,
                            final String childElementName,
                            final String text) {
        Element childElement = parent.getOwnerDocument().createElement(childElementName);
        parent.appendChild(childElement);

        if (text != null) {
            Text textNode = DomUtils.createTextNode(parent, text);
            childElement.appendChild(textNode);
        }
    }

    /**
     * EncapsulatedAssertionReference is only used to add the encass external reference to the exported policy xml. It
     * is not used in import and this method will never be called.
     *
     * @throws InvalidPolicyStreamException
     */
    @Override
    public void setLocalizeIgnore() {
        throw new UnsupportedOperationException("Cannot setLocalizeIgnore on EncapsulatedAssertionReference");
    }

    /**
     * EncapsulatedAssertionReference is only used to add the encass external reference to the exported policy xml. It
     * is not used in import and this method will never be called.
     *
     * @throws InvalidPolicyStreamException
     */
    @Override
    protected boolean verifyReference() throws InvalidPolicyStreamException {
        throw new UnsupportedOperationException("Cannot verifyReference on EncapsulatedAssertionReference");
    }

    /**
     * EncapsulatedAssertionReference is only used to add the encass external reference to the exported policy xml. It
     * is not used in import and this method will never be called.
     *
     * @throws InvalidPolicyStreamException
     */
    @Override
    protected boolean localizeAssertion(@Nullable Assertion assertionToLocalize) {
        throw new UnsupportedOperationException("Cannot localizeAssertion on EncapsulatedAssertionReference");
    }

    /**
     * Two EncapsulatedAssertionReference's are equal if their name and guid's are the same.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EncapsulatedAssertionReference)) return false;

        EncapsulatedAssertionReference that = (EncapsulatedAssertionReference) o;

        if (guid != null ? !guid.equals(that.guid) : that.guid != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (guid != null ? guid.hashCode() : 0);
        return result;
    }
}
