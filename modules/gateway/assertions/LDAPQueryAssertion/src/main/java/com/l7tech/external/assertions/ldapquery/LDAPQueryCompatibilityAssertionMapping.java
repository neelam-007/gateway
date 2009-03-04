package com.l7tech.external.assertions.ldapquery;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.*;
import com.l7tech.util.BeanUtils;
import com.l7tech.util.DomUtils;
import org.w3c.dom.Element;

import java.lang.reflect.InvocationTargetException;

/**
 * Compatibility mapping for reading the old pre-5.0 LDAPQuery format.
 */
public class LDAPQueryCompatibilityAssertionMapping extends CompatibilityAssertionMapping  {
    private static final WspVisitor QUIET_VISITOR = new PermissiveWspVisitor(null) {
        public void unknownProperty(Element originalObject, Element problematicParameter, Object deserializedObject, String parameterName, TypedReference parameterValue, Throwable problemEncountered) throws InvalidPolicyStreamException {
        }
    };

    public LDAPQueryCompatibilityAssertionMapping(Assertion a, String externalName) {
        super(a, externalName);
    }

    protected void configureAssertion(Assertion assertion, Element source, WspVisitor visitor) throws InvalidPolicyStreamException {
        LDAPQueryAssertion ass = (LDAPQueryAssertion)assertion;

        // First parse the easy stuff that's in exactly the same format as before
        copyUnchangedProperties(source, ass,
                "cachePeriod",
                "enableCache",
                "failIfNoResults",
                "ldapProviderOid",
                "searchFilter");

        // Now find the old String and Boolean arrays and parse em into a single array of mapping instances
        ass.setQueryMappings(extractQueryAttributeMappings(source));
    }

    private static QueryAttributeMapping[] extractQueryAttributeMappings(Element source) throws InvalidPolicyStreamException {
        String[] attrNames = extractStringArray(source, "AttrNames");
        String[] varNames = extractStringArray(source, "VarNames");
        Boolean[] multivalued = extractBooleanArray(source, "Multivalued");
        int maxLen = Math.max(multivalued.length, Math.max(attrNames.length, varNames.length));
        QueryAttributeMapping[] mappings = new QueryAttributeMapping[maxLen];
        for (int i = 0; i < maxLen; i++) {
            mappings[i] = new QueryAttributeMapping();
            if (i < attrNames.length)
                mappings[i].setAttributeName(attrNames[i]);
            if (i < varNames.length)
                mappings[i].setMatchingContextVariableName(varNames[i]);
            if (i < multivalued.length)
                mappings[i].setMultivalued(multivalued[i]);
        }
        return mappings;
    }

    private static String[] extractStringArray(Element source, String childElementName) throws InvalidPolicyStreamException {
        final ArrayTypeMapping stringArrayMapping = new ArrayTypeMapping(new String[0], "stringArrayValue");
        final Element childElement = childElement(source, childElementName);
        return childElement == null ? new String[0] : (String[])stringArrayMapping.thaw(childElement, new PermissiveWspVisitor(null)).target;
    }

    private static Boolean[] extractBooleanArray(Element source, String childElementName) throws InvalidPolicyStreamException {
        final ArrayTypeMapping booleanArrayMapping = new ArrayTypeMapping(new Boolean[0], "bools");
        final Element childElement = childElement(source, childElementName);
        return childElement == null ? new Boolean[0] : (Boolean[])booleanArrayMapping.thaw(childElement, new PermissiveWspVisitor(null)).target;
    }

    private static Element childElement(Element source, String childElementName) {
        return DomUtils.findFirstChildElementByName(source, (String)null, childElementName);
    }

    private static void copyUnchangedProperties(Element source, LDAPQueryAssertion dest, String... names) throws InvalidPolicyStreamException {
        try {
            final AssertionMapping currentMapping = new AssertionMapping(new LDAPQueryAssertion(), "LDAPQuery");
            TypedReference tr = currentMapping.thaw(source, QUIET_VISITOR);
            LDAPQueryAssertion ass2 = (LDAPQueryAssertion)tr.target;
            BeanUtils.copyProperties(ass2, dest, BeanUtils.includeProperties(BeanUtils.getProperties(LDAPQueryAssertion.class), names));
        } catch (IllegalAccessException e) {
            throw new InvalidPolicyStreamException(e);
        } catch (InvocationTargetException e) {
            throw new InvalidPolicyStreamException(e);
        }
    }
}
