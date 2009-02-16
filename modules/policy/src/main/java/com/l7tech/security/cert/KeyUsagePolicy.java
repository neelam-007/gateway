package com.l7tech.security.cert;

import com.l7tech.common.io.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.util.*;

/**
 * Represents a policy for key usage enforcement.
 */
public class KeyUsagePolicy {
    public static final String NAMESPACE_URI = "http://www.layer7tech.com/ws/keyusage";

    public static final Map<String, Integer> KEY_USAGE_BITS_BY_NAME = Collections.unmodifiableMap(new HashMap<String, Integer>() {{
        put("encipherOnly", 1);
        put("cRLSign", 2);
        put("keyCertSign", 4);
        put("keyAgreement", 8);
        put("dataEncipherment", 16);
        put("keyEncipherment", 32);
        put("nonRepudiation", 64);
        put("digitalSignature", 128);
        put("decipherOnly", 32768);
    }});

    public static final Map<String, String> KEY_PURPOSE_IDS_BY_NAME = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("any", "2.5.29.37.0");
        put("anyExtendedKeyUsage", "2.5.29.37.0");
        put("id-kp-emailProtection", "1.3.6.1.5.5.7.3.4");
        put("id-kp-serverAuth", "1.3.6.1.5.5.7.3.1");
        put("id-kp-clientAuth", "1.3.6.1.5.5.7.3.2");
        put("id-kp-timeStamping", "1.3.6.1.5.5.7.3.8");
        put("id-kp-smartcardlogon", "1.3.6.1.4.1.311.20.2.2");
        put("id-kp-OCSPSigning", "1.3.6.1.5.5.7.3.9");
        put("id-kp-codeSigning", "1.3.6.1.5.5.7.3.3");
        put("id-kp-ipsecTunnel", "1.3.6.1.5.5.7.3.6");
        put("id-kp-ipsecUser", "1.3.6.1.5.5.7.3.7");
        put("id-kp-ipsecEndSystem", "1.3.6.1.5.5.7.3.5");
        put("id-pkix-ocsp-nocheck", "1.3.6.1.5.5.7.48.1.5");
    }});

    /** for rules that specify that a given activity is always permitted. */
    private final Set<KeyUsageActivity> activityBlanketPermits;

    /** for rules that require at least one keyUsage bit. */
    private final Map<KeyUsageActivity, List<PermitRule>> activityKuPermits;

    /** for rules that do not require any keyUsage bits but require at least one extended key usage OID. */
    private final Map<KeyUsageActivity, List<PermitRule>> activityEkuPermits;

    private KeyUsagePolicy(Set<KeyUsageActivity> activityBlanketPermits, Map<KeyUsageActivity, List<PermitRule>> activityKuPermits, Map<KeyUsageActivity, List<PermitRule>> activityEkuPermits) {
        this.activityBlanketPermits = Collections.unmodifiableSet(activityBlanketPermits.isEmpty()
                                                                            ? EnumSet.noneOf(KeyUsageActivity.class)
                                                                            : EnumSet.copyOf(activityBlanketPermits));
        this.activityKuPermits = unmodifiableEnumMapOfLists(activityKuPermits);
        this.activityEkuPermits = unmodifiableEnumMapOfLists(activityEkuPermits);
    }

    private static <KT extends Enum<KT>, VT> Map<KT, List<VT>> unmodifiableEnumMapOfLists(Map<KT, List<VT>> map) {
        for (Map.Entry<KT, List<VT>> entry : map.entrySet())
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        Map<KT, List<VT>> ret = map.isEmpty()
                ? Collections.<KT, List<VT>>emptyMap()
                : new EnumMap<KT, List<VT>>(map);
        return Collections.unmodifiableMap(ret);
    }

    public static KeyUsagePolicy fromXml(String xml) throws SAXException {
        Document doc = XmlUtil.stringToDocument(xml);
        final Element root = doc.getDocumentElement();
        if (!"keyusagepolicy".equals(root.getLocalName()))
            throw new SAXException("Root element not keyusagepolicy");
        ns(root);

        final Set<KeyUsageActivity> blankets = new HashSet<KeyUsageActivity>();
        final Map<KeyUsageActivity, List<PermitRule>> kuPermits = new HashMap<KeyUsageActivity, List<PermitRule>>();
        final Map<KeyUsageActivity, List<PermitRule>> ekuPermits = new HashMap<KeyUsageActivity, List<PermitRule>>();

        List<Element> permits = XmlUtil.findChildElementsByName(root, NAMESPACE_URI, "permit");
        for (Element permit : permits) {
            PermitRule rule = PermitRule.valueOf(permit);
            KeyUsageActivity activity = rule.getActivity();

            if (activity != null) {
                collectRuleForActivity(blankets, kuPermits, ekuPermits, activity, rule);
            } else {
                // Add rule for all activities
                for (KeyUsageActivity act : KeyUsageActivity.values())
                    collectRuleForActivity(blankets, kuPermits, ekuPermits, act, rule);
            }
        }

        return new KeyUsagePolicy(blankets, kuPermits, ekuPermits);
    }

    // File away the specified rule into the appropriate collection
    private static void collectRuleForActivity(Set<KeyUsageActivity> blankets,
                                               Map<KeyUsageActivity, List<PermitRule>> kuPermits,
                                               Map<KeyUsageActivity, List<PermitRule>> ekuPermits,
                                               KeyUsageActivity activity,
                                               PermitRule rule)
    {
        if (rule.isBlanket()) {
            blankets.add(activity);
        } else if (rule.isKeyUsage()) {
            if (!kuPermits.containsKey(activity))
                kuPermits.put(activity, new ArrayList<PermitRule>());
            kuPermits.get(activity).add(rule);
        } else {
            if (!ekuPermits.containsKey(activity))
                ekuPermits.put(activity, new ArrayList<PermitRule>());
            ekuPermits.get(activity).add(rule);
        }
    }

    /**
     * Check if this key usage enforcement policy will permit the specified activity to be performed using
     * a certificate with a critical Key Usage extension containing the specified bits.
     * <p/>
     * This method should only be called if the cert has a key usage extension that is marked critical.
     * <p/>
     * If the cert has an Extended Key Usage extension that is also marked critical, you must call
     * {@link #isExtendedKeyUsagePermittedForActivity(KeyUsageActivity, java.util.List)} as well.
     *
     * @param activity the activity to be performed with this cert.  Required.
     * @param certKeyUsageBits the value of the key usage extension from this cert.
     * @return true if there is a blanket permit rule for this activity, or if at least one permit rule succeeds
     *         given the specified key usage bits.
     */
    public boolean isKeyUsagePermittedForActivity(KeyUsageActivity activity, int certKeyUsageBits) {
        if (activityBlanketPermits.contains(activity))
            return true;

        final List<PermitRule> permitRules = activityKuPermits.get(activity);
        if (permitRules == null)
            return false;

        for (PermitRule rule : permitRules) {
            if (rule.isKeyUsagePermitted(certKeyUsageBits))
                return true;
        }

        return false;
    }

    /**
     * Check if this key usage enforcement policy will permit the specified activity to be perfomed
     * using a certificate with a critical Extended Key Usage extension provided the specified key purpose OID strings.
     * <p/>
     * This method should only be called if the cert has an Extended Key Usage extension that is marked critical.
     * <p/>
     * If the cert has a Key Usage extension that is also marked critical, you must call
     * {@link #isKeyUsagePermittedForActivity(KeyUsageActivity, int)} as well.
     *
     * @param activity the activity to be performed with this cert.  Required.
     * @param certKeyPurposeOidStrings the key purpose IDs contained in the Extended Key Usage extension in the cert,
     *                                 expressed as dotted-decimal strings.
     * @return true if there is a blanket permit rule for this activity, or if one of the key purpose IDs from the cert
     *              matches
     */
    public boolean isExtendedKeyUsagePermittedForActivity(KeyUsageActivity activity, List<String> certKeyPurposeOidStrings) {
        if (activityBlanketPermits.contains(activity))
            return true;

        final List<PermitRule> permitRules = activityEkuPermits.get(activity);
        if (permitRules == null)
            return false;

        for (PermitRule rule : permitRules) {
            if (rule.isAllowAnyKeyPurpose() || rule.isAllExtendedKeyUsagesPermitted(certKeyPurposeOidStrings))
                return true;
        }

        return false;
    }

    private static void ns(Element element) throws SAXException {
        if (!NAMESPACE_URI.equals(element.getNamespaceURI()))
            throw new SAXException("Element " + element.getNodeName() + " not in namespace " + NAMESPACE_URI);
    }

    static class PermitRule {
        private final KeyUsageActivity activity;
        private final int requiredKeyUsageBits;
        private final List<String> requiredKeyPurposeOidStrings;
        private final boolean allowAnyKeyPurpose;
        private final boolean blanket;

        PermitRule(KeyUsageActivity activity, int requiredKeyUsageBits, List<String> requiredKeyPurposeOidStrings) {
            this.activity = activity;
            this.requiredKeyUsageBits = requiredKeyUsageBits;
            this.requiredKeyPurposeOidStrings = Collections.unmodifiableList(requiredKeyPurposeOidStrings);
            this.allowAnyKeyPurpose = requiredKeyPurposeOidStrings.isEmpty();
            this.blanket = requiredKeyUsageBits == 0 && allowAnyKeyPurpose;
        }

        static PermitRule valueOf(Element permitElement) throws SAXException {
            final String activityName = permitElement.getAttribute("action");
            KeyUsageActivity activity = null;
            try {
                if (activityName != null && activityName.length() > 0)
                    activity = KeyUsageActivity.valueOf(activityName);
                int kubits = 0;
                List<String> purposeOidStrings = new ArrayList<String>();

                List<Element> requirements = XmlUtil.findChildElementsByName(permitElement, NAMESPACE_URI, "req");
                for (Element requirement : requirements) {
                    String str = XmlUtil.getTextValue(requirement);

                    // Try to parse as key usage name
                    Integer kubit = KEY_USAGE_BITS_BY_NAME.get(str);
                    if (kubit != null) {
                        kubits |= kubit;
                        continue;
                    }

                    // Try to parse as well-known extended key usage name
                    String kpid = KEY_PURPOSE_IDS_BY_NAME.get(str);
                    if (kpid != null) {
                        purposeOidStrings.add(kpid);
                        continue;
                    }

                    // Try to parse as dotted decimal OID for extended key usage
                    purposeOidStrings.add(str);
                }

                return new PermitRule(activity, kubits, purposeOidStrings);
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
}
