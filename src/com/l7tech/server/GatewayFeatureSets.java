/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.common.License;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.alert.SnmpTrapAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.identity.MappingAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;

import javax.servlet.http.HttpServlet;
import java.util.*;
import java.util.logging.Logger;

/**
 * Master list of Feature Sets for the SSG, hard-baked into the code so it will be obfuscated.
 */
public class GatewayFeatureSets {
    private static final Logger logger = Logger.getLogger(GatewayFeatureSets.class.getName());

    /** All pre-configured FeatureSets. */
    private static final Map<String, GatewayFeatureSet> sets = new LinkedHashMap<String, GatewayFeatureSet>();

    /** Only the root-level FeatureSets. */
    private static final Map<String, GatewayFeatureSet> rootSets = new LinkedHashMap<String, GatewayFeatureSet>();

    /** Only the root-level Product Profile feature sets. */
    private static final Map<String, GatewayFeatureSet> profileSets = new LinkedHashMap<String, GatewayFeatureSet>();

    /** The ultimate Product Profile that enables every possible feature. */
    public static final GatewayFeatureSet PROFILE_ALL;

    /** Feature set to use for (usually old) licenses that, while valid, do not explicitly name any feature sets. */
    public static final GatewayFeatureSet PROFILE_LICENSE_NAMES_NO_FEATURES;

    // Constants for service names
    public static final String SERVICE_MESSAGEPROCESSOR = "service:MessageProcessor";
    public static final String SERVICE_HTTP_MESSAGE_INPUT = "service:HttpMessageInput";
    public static final String SERVICE_JMS_MESSAGE_INPUT = "service:JmsMessageInput";
    public static final String SERVICE_ADMIN = "service:Admin";
    public static final String SERVICE_POLICYDISCO = "service:Policy";
    public static final String SERVICE_STS = "service:TokenService";
    public static final String SERVICE_CSRHANDLER = "service:CSRHandler";
    public static final String SERVICE_PASSWD = "service:Passwd";
    public static final String SERVICE_WSDLPROXY = "service:WsdlProxy";
    public static final String SERVICE_SNMPQUERY = "service:SnmpQuery";
    public static final String SERVICE_BRIDGE = "service:Bridge";

    static {
        // Declare all baked-in feature sets

        //
        // Declare "twig" feature sets
        // (feature sets that don't include other feature sets, totally useless on their own, but not
        //  a "leaf" feature set like a single assertion or servlet)
        // Naming convention: set:all:lowercase
        //
        GatewayFeatureSet core =
        fsr("set:core", "Core features, without which nothing else will work",
            "Always needed",
            misc(SERVICE_MESSAGEPROCESSOR, "Core Gateway message processing module", null),
            ass(AllAssertion.class),
            ass(UnknownAssertion.class));

        GatewayFeatureSet admin =
        fsr("set:admin", "All admin APIs, over all admin API transports",
            "Everything that used to be enabled by the catchall Feature.ADMIN",
            misc(SERVICE_ADMIN, "All admin APIs, over all admin API transports", null));

        GatewayFeatureSet branching =
        fsr("set:policy:branching", "Support for branching policies",
            ass(AllAssertion.class),
            ass(ExactlyOneAssertion.class),
            ass(OneOrMoreAssertion.class));

        GatewayFeatureSet wssc =
        fsr("set:wssc", "WS-SecureConversation support",
            "Requires enabling the STS",
            srv(SERVICE_STS, "Security token service"),
            ass(SecureConversation.class));

        GatewayFeatureSet httpFront =
        fsr("set:http:front", "Allow incoming HTTP messages",
            srv(SERVICE_HTTP_MESSAGE_INPUT, "Accept incoming messages over HTTP"));

        GatewayFeatureSet httpBack =
        fsr("set:http:back", "Allow outgoing HTTP messages",
            ass(HttpRoutingAssertion.class));

        GatewayFeatureSet srvJms = misc(SERVICE_JMS_MESSAGE_INPUT, "Accept incoming messages over JMS", null);
        GatewayFeatureSet jmsFront =
        fsr("set:jms:front", "Allow incoming JMS messages",
            srvJms);

        GatewayFeatureSet jmsBack =
        fsr("set:jms:back", "Allow outgoing JMS messages",
            "Current requires allowing the JMS front end as well",
            srvJms,
            ass(JmsRoutingAssertion.class));

        GatewayFeatureSet ssb =
        fsr("set:ssb", "Features needed for best use of the SecureSpan Bridge",
            srv(SERVICE_CSRHANDLER, "Certificate signer (CA) service"),
            srv(SERVICE_PASSWD, "Internal user password change service"),
            srv(SERVICE_POLICYDISCO, "Policy discovery service"),
            srv(SERVICE_WSDLPROXY, "WSDL proxy service"));

        GatewayFeatureSet snmp =
        fsr("set:snmp", "SNMP features",
            srv(SERVICE_SNMPQUERY, "HTTP SNMP query service"),
            ass(SnmpTrapAssertion.class));

        GatewayFeatureSet experimental =
        fsr("set:experimental", "Enable experimental features",
            "Enables features that are only present during development, and that will be moved or renamed before shipping.",
            srv(SERVICE_BRIDGE, "Experimental SSB service (standalone, non-BRA, present-but-disabled)"),
            ass(WsspAssertion.class));

        //
        // Declare "building block" feature sets
        // (feature sets built out of "twig" feature sets, and which may include other building block feature sets,
        //  but which on their own may still be useless or of little value as a product.)
        // Naming convention:  set:CamelCaseCategory:LevelName
        //

        // Access control
        GatewayFeatureSet accessAccel =
        fsr("set:AccessControl:Accel", "SecureSpan Accelerator access control",
            "User and group auth, and basic HTTP, SSL, and XML credential sources.  No WSS message-level security.",
            ass(SpecificUser.class),
            ass(MemberOfGroup.class),
            ass(HttpBasic.class),
            ass(HttpDigest.class),
            ass(SslAssertion.class), // TODO omit client cert support from this grant (when it is possible to do so)
            ass(XpathCredentialSource.class));

        GatewayFeatureSet accessFw =
        fsr("set:AccessControl:Firewall", "SecureSpan Firewall access control",
            "Adds WSS message-level security (including WS-SecureConversation) and identity mapping",
            fs(accessAccel),
            fs(wssc),
            ass(MappingAssertion.class),
            ass(WssBasic.class),
            ass(RequestWssX509Cert.class),
            ass(RequestWssSaml.class),
            ass(RequestWssSaml2.class),
            ass(EncryptedUsernameTokenAssertion.class),
            ass(WsTrustCredentialExchange.class),
            ass(SamlBrowserArtifact.class));

        GatewayFeatureSet accessGateway =
        fsr("set:AccessControl:Gateway", "SecureSpan Gateway access control",
            "Adds SSL client certs (although currently this comes with our SSL support and can't be disabled)",
            fs(accessFw),
            ass(SslAssertion.class)); // TODO enable client cert here exclusively (when it is possible to do so)

        // XML Security
        GatewayFeatureSet xmlsecAccel =
        fsr("set:XmlSec:Accel", "SecureSpan Accelerator XML security",
            "Element signature and encryption using WSS",
            ass(RequestWssIntegrity.class),
            ass(RequestWssConfidentiality.class),
            ass(ResponseWssIntegrity.class),
            ass(ResponseWssConfidentiality.class),
            ass(ResponseWssTimestamp.class));

        GatewayFeatureSet xmlsecFw =
        fsr("set:XmlSec:Firewall", "SecureSpan Firewall XML security",
            "Adds timestamp and token manipulation",
            fs(xmlsecAccel),
            ass(RequestWssReplayProtection.class),
            ass(RequestWssTimestamp.class),
            ass(WssTimestamp.class),
            ass(ResponseWssSecurityToken.class));

        // Message Validation/Transform
        GatewayFeatureSet validationAccel =
        fsr("set:Validation:Accel", "SecureSpan Accelerator message validation and transformation",
            "XPath, Schema, XSLT, and SOAP operation detector",
            ass(RequestXpathAssertion.class),
            ass(ResponseXpathAssertion.class),
            ass(Operation.class),
            ass(SchemaValidation.class),
            ass(XslTransformation.class));

        GatewayFeatureSet validationFw =
        fsr("set:Validation:Firewall", "SecureSpan Firewall message validation and transformation",
            "Adds regex and attachments",
            fs(validationAccel),
            ass(RequestSwAAssertion.class),
            ass(Regex.class),
            ass(WsiBspAssertion.class),
            ass(WsiSamlAssertion.class));

        GatewayFeatureSet validationGateway =
        fsr("set:Validation:Gateway", "SecureSpan Gateway message validation and transformation",
            "Adds HTTP form/MIME conversion assertions",
            fs(validationFw),
            ass(HttpFormPost.class),
            ass(InverseHttpFormPost.class));

        // Message routing
        GatewayFeatureSet routingAccel =
        fsr("set:Routing:Accel", "SecureSpan Accelerator message routing",
            "HTTP and hardcoded responses.",
            fs(httpFront),
            fs(httpBack),
            ass(HardcodedResponseAssertion.class),
            ass(EchoRoutingAssertion.class));

        GatewayFeatureSet routingGateway =
        fsr("set:Routing:Gateway", "SecureSpan Gateway message routing",
            "Adds BRA and JMS routing.",
            fs(routingAccel),
            fs(jmsFront),
            fs(jmsBack),
            ass(BridgeRoutingAssertion.class));

        // Service availability
        GatewayFeatureSet availabilityAccel =
        fsr("set:Availability:Accel", "SecureSpan Accelerator service availability",
            "Time/Day, IP range, and throughput qutoa",
            ass(TimeRange.class),
            ass(RemoteIpRange.class),
            ass(ThroughputQuota.class));

        // Logging/auditing and alerts
        GatewayFeatureSet auditAccel =
        fsr("set:Audit:Accel", "SecureSpan Accelerator logging/auditing and alerts",
            "Auditing, email and SNMP traps",
            fs(snmp),
            ass(AuditAssertion.class),
            ass(AuditDetailAssertion.class),
            ass(EmailAlertAssertion.class));

        // Policy logic
        GatewayFeatureSet policyAccel =
        fsr("set:Policy:Accel", "SecureSpan Accelerator complex policy logic",
            "Branches, comments, comparisons, variables, and echoing",
            fs(core),
            fs(branching),
            ass(CommentAssertion.class),
            ass(ComparisonAssertion.class),
            ass(TrueAssertion.class),
            ass(FalseAssertion.class),
            ass(SetVariableAssertion.class),
            ass(EchoRoutingAssertion.class),
            ass(HardcodedResponseAssertion.class));

        GatewayFeatureSet threatIps =
        fsr("set:Threats:IPS", "SecureSpan XML IPS threat protection",
            "Stealth fault, size limits, document structure threats, schema validation, and XTM (when it's done)",
            ass(FaultLevel.class),
            ass(OversizedTextAssertion.class),
            ass(RequestSizeLimit.class),
            ass(SchemaValidation.class)); // TODO Assertion for XTM Signature goes here, as soon as it exists

        GatewayFeatureSet threatAccel =
        fsr("set:Threats:Accel", "SecureSpan Accelerator threat protection",
            "Just document structure threats and schema validation",
            ass(OversizedTextAssertion.class),
            ass(SchemaValidation.class));

        GatewayFeatureSet threatFw =
        fsr("set:Threats:Firewall", "SecureSpan Firewall threat protection",
            "Both of the above, plus adds regex, SQL attacks, and cluster-wide replay protection",
            fs(threatIps),
            fs(threatAccel),
            ass(Regex.class),
            ass(SqlAttackAssertion.class),
            ass(RequestWssReplayProtection.class));

        // Custom assertions
        GatewayFeatureSet customFw =
        fsr("set:Custom:Firewall", "SecureSpan Firewall custom assertions",
            "Synamtec, TAM, SM, TM, CT, ADFS, OAM",
            ass(CustomAssertionHolder.class),
            ass(HttpNegotiate.class),
            ass(WsFederationPassiveTokenExchange.class),
            ass(WsFederationPassiveTokenRequest.class),
            ass(RequestWssKerberos.class));


        //
        // Declare "product profile" feature sets
        // (feature sets built out of "building block" feature sets, and which each constitutes a useable,
        //  complete product in its own right.)
        // Naming convention:   set:Profile:ProfileName
        //
        fsp("set:Profile:IPS", "SecureSpan XML IPS",
            "Threat protection features only.  (No routing assertions?  Not even hardcoded response?)",
            fs(core),
            fs(admin),
            fs(httpFront),
            fs(threatIps));

        fsp("set:Profile:Accel", "SecureSpan Accelerator",
            "XML acceleration features with basic authentication",
            fs(core),
            fs(admin),
            fs(accessAccel),
            fs(xmlsecAccel),
            fs(validationAccel),
            fs(routingAccel),
            fs(availabilityAccel),
            fs(auditAccel),
            fs(policyAccel),
            fs(threatAccel));

        fsp("set:Profile:Firewall", "SecureSpan Firewall",
            "XML firewall with custom assertions.  No BRA, no JMS, no special Bridge support",
            fs(core),
            fs(admin),
            fs(accessFw),
            fs(xmlsecFw),
            fs(validationFw),
            fs(routingAccel),
            fs(availabilityAccel),
            fs(auditAccel),
            fs(policyAccel),
            fs(threatFw),
            fs(customFw));

        PROFILE_ALL =
        fsp("set:Profile:Gateway", "SecureSpan Gateway",
            "All features enabled.",
            misc("bundle:Bridge", "Bundled SecureSpan Bridge", "No effect on Gateway license code -- only purpose is to distinguish two otherwise-identical feature sets"),
            fs(core),
            fs(admin),
            fs(accessGateway),
            fs(xmlsecFw),
            fs(validationGateway),
            fs(routingGateway),
            fs(availabilityAccel),
            fs(auditAccel),
            fs(policyAccel),
            fs(threatFw),
            fs(customFw),
            fs(ssb),
            fs(experimental));

        fsp("set:Profile:Federal", "SecureSpan Federal",
            "Exactly the same features as SecureSpan Gateway, but Bridge software is not bundled.",
            fs(core),
            fs(admin),
            fs(accessGateway),
            fs(xmlsecFw),
            fs(validationGateway),
            fs(routingGateway),
            fs(availabilityAccel),
            fs(auditAccel),
            fs(policyAccel),
            fs(threatFw),
            fs(customFw),
            fs(ssb),
            fs(experimental));

        // For now, if a license names no features explicitly, we will enable all features.
        // TODO in the future, we should enable only those features that existed in 3.5.
        PROFILE_LICENSE_NAMES_NO_FEATURES =
        fsp("set:Profile:Compat:Pre36License", "Profile for old license files that don't name any feature sets",
            "Backward compatibility with license files that lack featureset elements, but would otherwise be perfectly valid. " +
            "Such licenses were intended to allow upgrades (within their version and date constraints) and should enable " +
            "at least all features that were enabled by a valid 3.5 license.",
            fs(PROFILE_ALL));
    }

    /** @return All registered FeatureSets, including product profiles, building blocks, and twig and leaf features. */
    public static Map<String, GatewayFeatureSet> getAllFeatureSets() {
        return Collections.unmodifiableMap(sets);
    }


    /** @return all root-level FeatureSets, including all product profiles, building blocks, and twig features. */
    public static Map<String, GatewayFeatureSet> getRootFeatureSets() {
        return Collections.unmodifiableMap(rootSets);
    }


    /** @return all Product Profile FeatureSets. */
    public static Map<String, GatewayFeatureSet> getProductProfiles() {
        return Collections.unmodifiableMap(profileSets);
    }


    /** @return the product profile that has all features enabled. */
    public static GatewayFeatureSet getBestProductProfile() {
        return PROFILE_ALL;
    }

    /** @return the FeatureSetExpander to use when parsing License files. */
    public static License.FeatureSetExpander getFeatureSetExpander() {
        return new License.FeatureSetExpander() {
            public Set getAllEnabledFeatures(Set inputSet) {
                //noinspection unchecked
                Set<String> ret = new HashSet<String>((Set<String>)inputSet);

                if (ret.isEmpty()) {
                    // Backwards compatibility mode for licenses that never contained any featureset elements
                    PROFILE_LICENSE_NAMES_NO_FEATURES.collectAllFeatureNames(ret);
                    return ret;
                }

                for (Iterator i = inputSet.iterator(); i.hasNext();) {
                    String topName = (String)i.next();
                    GatewayFeatureSet fs = sets.get(topName);
                    if (fs == null) {
                        logger.fine("Ignoring unrecognized feature set name: " + topName);
                        continue;
                    }
                    fs.collectAllFeatureNames(ret);
                }

                return ret;
            }
        };
    }

    /** Find already-registered GatewayFeatureSet by GatewayFeatureSet.  (Basically just asserts that a featureset is registered already.) */
    private static GatewayFeatureSet fs(GatewayFeatureSet fs)  throws IllegalArgumentException {
        return fs(fs.name);
    }

    /** Find already-registered GatewayFeatureSet by name. */
    private static GatewayFeatureSet fs(String name) throws IllegalArgumentException {
        GatewayFeatureSet got = sets.get(name);
        if (got == null || name == null) throw new IllegalArgumentException("Unknown feature set name: " + name);
        return got;
    }

    /** Create and register a new root-level GatewayFeatureSet with no note. */
    private static GatewayFeatureSet fsr(String name, String desc, GatewayFeatureSet... deps) {
        return fsr(name, desc, null, deps);
    }

    /** Create and register a new root-level GatewayFeatureSet. */
    private static GatewayFeatureSet fsr(String name, String desc, String note, GatewayFeatureSet... deps) throws IllegalArgumentException {
        if (!name.startsWith("set:")) throw new IllegalArgumentException("Non-leaf feature set name must start with \"set:\": " + name);
        GatewayFeatureSet newset = new GatewayFeatureSet(name, desc, note, deps);
        GatewayFeatureSet old = sets.put(name, newset);
        if (old != null) throw new IllegalArgumentException("Duplicate feature set name: " + name);
        rootSets.put(name, newset);
        return newset;
    }

    /** Create and register a new root-level "product profile" GatewayFeatureSet. */
    private static GatewayFeatureSet fsp(String name, String desc, String note, GatewayFeatureSet... deps) throws IllegalArgumentException {
        GatewayFeatureSet got = fsr(name, desc, note, deps);
        profileSets.put(name, got);
        return got;
    }

    /** @return the feature name to use for the specified Assertion class. */
    static String getFeatureSetNameForAssertion(Class<? extends Assertion> assertionClass) {
        return Assertion.getFeatureSetName(assertionClass.getName());
    }

    /** @return the feature name to use for the specified HttpServlet class. */
    static String getFeatureSetNameForServlet(Class<? extends HttpServlet> servletClass) {
        String classname = servletClass.getName();
        int lastdot = classname.lastIndexOf('.');
        String rest = classname.substring(lastdot + 1);
        rest = stripSuffix(rest, "Servlet");
        return "service:" + rest;
    }

    /** Create (and register, if new) a feature set for the specified policy assertion and return it. */
    private static GatewayFeatureSet ass(Class<? extends Assertion> ass) {
        String name = getFeatureSetNameForAssertion(ass);
        String classname = ass.getName();
        String desc = "Policy assertion: " + classname;

        return getOrMakeFeatureSet(name, desc);
    }

    private static GatewayFeatureSet getOrMakeFeatureSet(String name, String desc) {
        GatewayFeatureSet got = sets.get(name);
        if (got != null) {
            if (!desc.equals(got.desc)) throw new IllegalArgumentException("Already have different feature set named: " + name);
            return got;
        }

        got = new GatewayFeatureSet(name, desc);
        sets.put(name, got);
        return got;
    }

    /** Remove a suffix from a string, ie changing "FooAssertion" into "Foo" or "BlahFooberServlet" into "BlahFoober". */
    private static String stripSuffix(String rest, String suffix) {
        if (rest.endsWith(suffix))
            rest = rest.substring(0, rest.length() - suffix.length());
        return rest;
    }

    /** Create (and register, if new) a new miscellaneous GatewayFeatureSet with the specified name and description. */
    private static GatewayFeatureSet misc(String name, String desc, String note) {
        GatewayFeatureSet got = sets.get(name);
        if (got != null) {
            if (!desc.equals(got.desc))
                throw new IllegalArgumentException("Feature set name already in use with different description: " + name);
            return got;
        }

        got = new GatewayFeatureSet(name, desc, note, null);
        sets.put(name, got);
        return got;
    }


    /**
     * Create (and register, if new) a new GatewayFeatureSet for the specified HttpServlet and return it,
     * but using the specified name instead of the default.
     */
    private static GatewayFeatureSet srv(String name, String desc) {
        if (!name.startsWith("service:"))
            throw new IllegalArgumentException("Preferred feature name for service must start with \"service:\": " + name);
        return getOrMakeFeatureSet(name, desc);
    }

}
