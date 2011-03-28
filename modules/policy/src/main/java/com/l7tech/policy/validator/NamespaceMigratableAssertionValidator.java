package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.xml.NamespaceMigratable;
import com.l7tech.xml.soap.SoapVersion;

import java.util.Set;

/**
 * A validator that checks for NamespaceMigratable assertions that are using an incorrect SOAP envelope
 * namespace and, if so, offers to fix it.
 */
public class NamespaceMigratableAssertionValidator implements AssertionValidator {
    private static final String SOAP_NS_REMEDIAL_ACTION_CLASSNAME = "com.l7tech.console.action.NamespaceMigratableSoapVersionMigrator";

    private final NamespaceMigratable migratable;
    private SoapVersion unwantedSoapVersion = null;
    private SoapVersion serviceSoapVersion = null;

    public NamespaceMigratableAssertionValidator(NamespaceMigratable migratable) {
        if (!(migratable instanceof Assertion))
            throw new ClassCastException("migratable mus be an Assertion");
        this.migratable = migratable;
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        if (serviceSoapVersion == null) {
            // Lazily check for SOAP version and mismatching URI
            // Can't do this in c'tor because we need the service's SOAP version first
            SoapVersion soapVersion = pvc.getSoapVersion();
            if (soapVersion != null && soapVersion.getNamespaceUri() != null) {
                unwantedSoapVersion = checkForUnwantedSoapVersion(migratable, soapVersion);
            }
            serviceSoapVersion = soapVersion;
        }

        if (unwantedSoapVersion != null && !SoapVersion.UNKNOWN.equals(unwantedSoapVersion)) {
            result.addWarning(new PolicyValidatorResult.Warning((Assertion)migratable,
                    String.format("This assertion contains an XPath that uses the %s envelope namespace URI, but the service is configured as using only %s.  The XPath will always fail.",
                            unwantedSoapVersion.getLabel(), serviceSoapVersion.getLabel()),
                    null, SOAP_NS_REMEDIAL_ACTION_CLASSNAME));
        }
    }

    /**
     * Check if the specified NamespaceMigratable is using any recognized SOAP namespace URIs other than the one expected
     * by the provided SoapVersion.
     *
     * @param assertion  the migratable to examine.  Required.
     * @param expectedSoapVersion a SoapVersion that the migratable is permitted to use.  Required.
     * @return the first SoapVersion found used by the migratable that is not permitted; or, null if the migratable uses no SoapVersions other than the permitted version.
     */
    public static SoapVersion checkForUnwantedSoapVersion(NamespaceMigratable assertion, SoapVersion expectedSoapVersion) {
        // Look for all SOAP namespace URIs other than the one the service is configured to use
        Set<String> usedUris = assertion.findNamespaceUrisUsed();
        Set<String> unwantedUris = expectedSoapVersion.getOtherNamespaceUris();
        usedUris.retainAll(unwantedUris);
        return usedUris.isEmpty() ? null : SoapVersion.namespaceToSoapVersion(usedUris.iterator().next());
    }
}
