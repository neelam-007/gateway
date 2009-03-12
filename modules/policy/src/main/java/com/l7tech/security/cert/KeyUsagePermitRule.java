package com.l7tech.security.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a rule allowing an activity to be performed iff. a given set of key purposes OIDs or key usage bits are present.
 */
public class KeyUsagePermitRule {
    private final KeyUsageActivity activity;
    private final int requiredKeyUsageBits;
    private final List<String> requiredKeyPurposeOidStrings;
    private final boolean allowAnyKeyPurpose;
    private final boolean blanket;

    /**
     * Create a permit rule that will permit the specified activity if all the specified key usage bits are set.
     *
     * @param activity the activity to permit.  Required.
     * @param requiredKeyUsageBits  the required key usage bits, bitwise-ORed together, or 0 if this rule should always succeed regardless of key usage.
     *                               See {@link CertUtils#KEY_USAGE_BITS_BY_NAME} for the complete list of recognized key usage bits.
     */
    public KeyUsagePermitRule(KeyUsageActivity activity, int requiredKeyUsageBits) {
        this(activity, requiredKeyUsageBits, Collections.<String>emptyList());
    }

    /**
     * Create a permit rule that will permit the specified activity if all the specified key purpose OIDs are present.
     *
     * @param activity the activity to permit.  Required.
     * @param requiredKeyPurposeOidStrings  the required key purpose OIDs, or empty if this rule should always succeed regardless of extended key usage.
     *                                      See {@link CertUtils#KEY_PURPOSE_IDS_BY_NAME} for some well known key purpose OIDs.
     */
    public KeyUsagePermitRule(KeyUsageActivity activity, List<String> requiredKeyPurposeOidStrings) {
        this(activity, 0, requiredKeyPurposeOidStrings);
    }

    // Package private since it allows configuring a rule which requires both KU and EKU and hence can never be satisfied
    KeyUsagePermitRule(KeyUsageActivity activity, int requiredKeyUsageBits, List<String> requiredKeyPurposeOidStrings) {
        if (requiredKeyPurposeOidStrings == null)
            requiredKeyPurposeOidStrings = Collections.emptyList();
        this.activity = activity;
        this.requiredKeyUsageBits = requiredKeyUsageBits;
        this.requiredKeyPurposeOidStrings = Collections.unmodifiableList(requiredKeyPurposeOidStrings);
        this.allowAnyKeyPurpose = requiredKeyPurposeOidStrings.isEmpty();
        this.blanket = requiredKeyUsageBits == 0 && allowAnyKeyPurpose;
    }

    static KeyUsagePermitRule valueOf(Element permitElement) throws SAXException {
        final String activityName = permitElement.getAttribute("action");
        KeyUsageActivity activity = null;
        try {
            if (activityName != null && activityName.length() > 0)
                activity = KeyUsageActivity.valueOf(activityName);
            int kubits = 0;
            List<String> purposeOidStrings = new ArrayList<String>();

            List<Element> requirements = XmlUtil.findChildElementsByName(permitElement, KeyUsagePolicy.NAMESPACE_URI, "req");
            for (Element requirement : requirements) {
                String str = XmlUtil.getTextValue(requirement);

                // Try to parse as key usage name
                Integer kubit = CertUtils.KEY_USAGE_BITS_BY_NAME.get(str);
                if (kubit != null) {
                    kubits |= kubit;
                    continue;
                }

                // Try to parse as well-known extended key usage name
                String kpid = CertUtils.KEY_PURPOSE_IDS_BY_NAME.get(str);
                if (kpid != null) {
                    purposeOidStrings.add(kpid);
                    continue;
                }

                // Try to parse as dotted decimal OID for extended key usage
                purposeOidStrings.add(str);
            }

            return new KeyUsagePermitRule(activity, kubits, purposeOidStrings);
        } catch (IllegalArgumentException e) {
            throw new SAXException("Unrecognized activity name in action attribute: " + activityName, e);
        }
    }

    public boolean isBlanket() {
        return blanket;
    }

    public boolean isKeyUsage() {
        return requiredKeyUsageBits != 0;
    }

    public boolean isAllowAnyKeyPurpose() {
        return allowAnyKeyPurpose;
    }

    /** @return the activity this rule pertains to, or null if it pertains to all activities. */
    public KeyUsageActivity getActivity() {
        return activity;
    }

    /**
     * Check if the all key usage bits required by this permit rule are enabled in the specified
     * certificate key usage bits.
     * <p/>
     * Before calling this method caller should ensure this isn't a blanket permit rule by
     * checking {@link #isBlanket()}.
     *
     * @param certKeyUsageBits the key usage bits from the certificate.
     * @return true iff. all key usage bits required by this permit rule are enabled by this certificate.
     */
    public boolean isKeyUsagePermitted(int certKeyUsageBits) {
        return (requiredKeyUsageBits & certKeyUsageBits) == requiredKeyUsageBits;
    }

    /**
     * Check if all the extended key usage OIDs required by this permit rule are enabled in the specified
     * certificate key usage OID list.
     * <p/>
     * Before calling this method caller should ensure this isn't a blanket permit rule
     * by checking {@link #isBlanket}, and should also ensure that this isn't an "allow any key purpose"
     * rule by calling {@link #isAllowAnyKeyPurpose()}.
     *
     * @param certKeyUsageOidStrings the extended key usage OIDs enabled by the certificate.
     * @return true if all key purpose OIDs required by this pertmit rule are provided by the certificate, OR
     *              if the certificate provides the "any" extended key usage.
     */
    public boolean isAllExtendedKeyUsagesPermitted(Collection<String> certKeyUsageOidStrings) {
        for (String reqkp : requiredKeyPurposeOidStrings) {
            if (!certKeyUsageOidStrings.contains(reqkp))
                return false;
        }
        return true;
    }
}
