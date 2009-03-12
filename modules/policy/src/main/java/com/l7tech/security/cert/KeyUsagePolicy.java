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

    /** for rules that specify that a given activity is always permitted. */
    private final Set<KeyUsageActivity> activityBlanketPermits;

    /** for rules that require at least one keyUsage bit. */
    private final Map<KeyUsageActivity, List<KeyUsagePermitRule>> activityKuPermits;

    /** for rules that do not require any keyUsage bits but require at least one extended key usage OID. */
    private final Map<KeyUsageActivity, List<KeyUsagePermitRule>> activityEkuPermits;

    private KeyUsagePolicy(Set<KeyUsageActivity> activityBlanketPermits, Map<KeyUsageActivity, List<KeyUsagePermitRule>> activityKuPermits, Map<KeyUsageActivity, List<KeyUsagePermitRule>> activityEkuPermits) {
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

    /**
     * Create a new KeyUsagePolicy from the specified rules.
     *
     * @param blanketPermits  activites which should always be permitted regardless of critical key usage or ext. key usage, or null
     * @param keyUsagePermits permit rules pertaining to key usage, or null.  Any of these rules that have ext. key usage requirements will never be satisfied.
     * @param extKeyUsagePermits permit rules pertatining to ext key usage, or null.  Any of these rules that have key usage requirements will never be satisfied.
     * @return a KeyUsagePolicy that will enforce the specified rules.  Never null.
     */
    public static KeyUsagePolicy fromRules(Set<KeyUsageActivity> blanketPermits,
                                           Map<KeyUsageActivity, List<KeyUsagePermitRule>> keyUsagePermits,
                                           Map<KeyUsageActivity, List<KeyUsagePermitRule>> extKeyUsagePermits)
    {
        if (blanketPermits == null) blanketPermits = Collections.emptySet();
        if (keyUsagePermits == null) keyUsagePermits = Collections.emptyMap();
        if (extKeyUsagePermits == null) extKeyUsagePermits = Collections.emptyMap();
        return new KeyUsagePolicy(blanketPermits, keyUsagePermits, extKeyUsagePermits);
    }

    /**
     * Create a new KeyUsagePolicy from the specified policy XML.
     *
     * @param xml the policy XML to parse.  Required.
     * @return a KeyUsagePolicy instance that will enforce the specified policy.  Never null.
     * @throws SAXException if the policy cannot be parsed or is invalid.
     */
    public static KeyUsagePolicy fromXml(String xml) throws SAXException {
        Document doc = XmlUtil.stringToDocument(xml);
        final Element root = doc.getDocumentElement();
        if (!"keyusagepolicy".equals(root.getLocalName()))
            throw new SAXException("Root element not keyusagepolicy");
        ns(root);

        final Set<KeyUsageActivity> blankets = new HashSet<KeyUsageActivity>();
        final Map<KeyUsageActivity, List<KeyUsagePermitRule>> kuPermits = new HashMap<KeyUsageActivity, List<KeyUsagePermitRule>>();
        final Map<KeyUsageActivity, List<KeyUsagePermitRule>> ekuPermits = new HashMap<KeyUsageActivity, List<KeyUsagePermitRule>>();

        List<Element> permits = XmlUtil.findChildElementsByName(root, NAMESPACE_URI, "permit");
        for (Element permit : permits) {
            KeyUsagePermitRule rule = KeyUsagePermitRule.valueOf(permit);
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
                                               Map<KeyUsageActivity, List<KeyUsagePermitRule>> kuPermits,
                                               Map<KeyUsageActivity, List<KeyUsagePermitRule>> ekuPermits,
                                               KeyUsageActivity activity,
                                               KeyUsagePermitRule rule)
    {
        if (rule.isBlanket()) {
            blankets.add(activity);
        } else if (rule.isKeyUsage()) {
            if (!kuPermits.containsKey(activity))
                kuPermits.put(activity, new ArrayList<KeyUsagePermitRule>());
            kuPermits.get(activity).add(rule);
        } else {
            if (!ekuPermits.containsKey(activity))
                ekuPermits.put(activity, new ArrayList<KeyUsagePermitRule>());
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

        final List<KeyUsagePermitRule> permitRules = activityKuPermits.get(activity);
        if (permitRules == null)
            return false;

        for (KeyUsagePermitRule rule : permitRules) {
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

        final List<KeyUsagePermitRule> permitRules = activityEkuPermits.get(activity);
        if (permitRules == null)
            return false;

        for (KeyUsagePermitRule rule : permitRules) {
            if (rule.isAllowAnyKeyPurpose() || rule.isAllExtendedKeyUsagesPermitted(certKeyPurposeOidStrings))
                return true;
        }

        return false;
    }

    private static void ns(Element element) throws SAXException {
        if (!NAMESPACE_URI.equals(element.getNamespaceURI()))
            throw new SAXException("Element " + element.getNodeName() + " not in namespace " + NAMESPACE_URI);
    }
}
