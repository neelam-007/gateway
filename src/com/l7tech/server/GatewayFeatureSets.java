/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.alert.SnmpTrapAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.identity.MappingAssertion;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.server.identity.cert.CSRHandler;
import com.l7tech.server.policy.PolicyServlet;
import com.l7tech.common.Feature;
import com.l7tech.common.License;
import com.l7tech.proxy.BridgeServlet;

import javax.servlet.http.HttpServlet;
import java.util.*;
import java.util.logging.Logger;

/**
 * Master list of Feature Sets for the SSG, hard-baked into the code so it will be obfuscated.
 */
public class GatewayFeatureSets {
    private static final Logger logger = Logger.getLogger(GatewayFeatureSets.class.getName());

    /** All pre-configured FeatureSets. */
    private static final Map<String, FeatureSet> sets = new LinkedHashMap<String, FeatureSet>();

    /** Only the root-level FeatureSets. */
    private static final Map<String, FeatureSet> rootSets = new LinkedHashMap<String, FeatureSet>();

    /** Only the root-level Product Profile feature sets. */
    private static final Map<String, FeatureSet> profileSets = new LinkedHashMap<String, FeatureSet>();

    /** The ultimate Product Profile that enables every possible feature. */
    private static final FeatureSet PROFILE_ALL;

    /** Feature set to use for (usually old) licenses that, while valid, do not explicitly name any feature sets. */
    private static final FeatureSet PROFILE_LICENSE_NAMES_NO_FEATURES;

    // Top-level services queried for by enforcement points throughout the code
    public static final FeatureSet SERVICE_MESSAGEPROCESSOR = misc("service:MessageProcessor", "Core Gateway message processing module");
    public static final FeatureSet SERVICE_HTTP_MESSAGE_INPUT = srv(SoapMessageProcessingServlet.class, "Accept incoming messages over HTTP", "service:HttpMessageInput");
    public static final FeatureSet SERVICE_JMS_MESSAGE_INPUT = misc("service:JmsMessageInput", "Accept incoming messages over JMS");
    public static final FeatureSet SERVICE_ADMIN = misc("service:Admin", "All admin APIs, over all admin API transports");
    public static final FeatureSet SERVICE_POLICYDISCO = srv(PolicyServlet.class, "Policy discovery service");
    public static final FeatureSet SERVICE_STS = srv(TokenServiceServlet.class, "Security token service");
    public static final FeatureSet SERVICE_CSRHANDLER = srv(CSRHandler.class, "Certificate signer (CA) service");
    public static final FeatureSet SERVICE_PASSWD = srv(PasswdServlet.class, "Internal user password change service");
    public static final FeatureSet SERVICE_WSDLPROXY = srv(WsdlProxyServlet.class, "WSDL proxy service");
    public static final FeatureSet SERVICE_SNMPQUERY = srv(SnmpQueryServlet.class, "HTTP SNMP query service");
    public static final FeatureSet SERVICE_BRIDGE = srv(BridgeServlet.class, "Experimental SSB service (standalone, non-BRA, present-but-disabled)"); // experimental bridge servlet, disabled by default (neither SSB nor BRA!)

    static {
        // Declare all baked-in feature sets

        //
        // Declare "twig" feature sets
        // (feature sets that don't include other feature sets, totally useless on their own, but not
        //  a "leaf" feature set like a single assertion or servlet)
        // Naming convention: set:all:lowercase
        //
        FeatureSet core =
        fsr("set:core", "Core features, without which nothing else will work",
            "Always needed",
            SERVICE_MESSAGEPROCESSOR,
            ass(AllAssertion.class),
            ass(UnknownAssertion.class));

        FeatureSet admin =
        fsr("set:admin", "All admin APIs, over all admin API transports",
            "Everything that used to be enabled by the catchall Feature.ADMIN",
            SERVICE_ADMIN);

        FeatureSet branching =
        fsr("set:policy:branching", "Support for branching policies",
            ass(AllAssertion.class),
            ass(ExactlyOneAssertion.class),
            ass(OneOrMoreAssertion.class));

        FeatureSet wssc =
        fsr("set:wssc", "WS-SecureConversation support",
            "Requires enabling the STS",
            SERVICE_STS,
            ass(SecureConversation.class));

        FeatureSet httpFront =
        fsr("set:http:front", "Allow incoming HTTP messages",
            SERVICE_HTTP_MESSAGE_INPUT);

        FeatureSet httpBack =
        fsr("set:http:back", "Allow outgoing HTTP messages",
            ass(HttpRoutingAssertion.class));

        FeatureSet jmsFront =
        fsr("set:jms:front", "Allow incoming JMS messages",
            SERVICE_JMS_MESSAGE_INPUT);

        FeatureSet jmsBack =
        fsr("set:jms:back", "Allow outgoing JMS messages",
            ass(JmsRoutingAssertion.class));

        FeatureSet ssb =
        fsr("set:ssb", "Features needed for best use of the SecureSpan Bridge",
            SERVICE_CSRHANDLER,
            SERVICE_PASSWD,
            SERVICE_POLICYDISCO,
            SERVICE_WSDLPROXY);

        FeatureSet snmp =
        fsr("set:snmp", "SNMP features",
            SERVICE_SNMPQUERY,
            ass(SnmpTrapAssertion.class));

        FeatureSet experimental =
        fsr("set:experimental", "Enable experimental features",
            "Enables features that are only present during development, and that will be moved or renamed before shipping.",
            SERVICE_BRIDGE,
            ass(WsspAssertion.class));

        //
        // Declare "building block" feature sets
        // (feature sets built out of "twig" feature sets, and which may include other building block feature sets,
        //  but which on their own may still be useless or of little value as a product.)
        // Naming convention:  set:CamelCaseCategory:LevelName
        //

        // Access control
        FeatureSet accessAccel =
        fsr("set:AccessControl:Accel", "SecureSpan Accelerator access control",
            "User and group auth, and basic HTTP, SSL, and XML credential sources.  No WSS message-level security.",
            ass(SpecificUser.class),
            ass(MemberOfGroup.class),
            ass(HttpBasic.class),
            ass(HttpDigest.class),
            ass(SslAssertion.class), // TODO omit client cert support from this grant (when it is possible to do so)
            ass(XpathCredentialSource.class));

        FeatureSet accessFw =
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

        FeatureSet accessGateway =
        fsr("set:AccessControl:Gateway", "SecureSpan Gateway access control",
            "Adds SSL client certs (although currently this comes with our SSL support and can't be disabled)",
            fs(accessFw),
            ass(SslAssertion.class)); // TODO enable client cert here exclusively (when it is possible to do so)

        // XML Security
        FeatureSet xmlsecAccel =
        fsr("set:XmlSec:Accel", "SecureSpan Accelerator XML security",
            "Element signature and encryption using WSS",
            ass(RequestWssIntegrity.class),
            ass(RequestWssConfidentiality.class),
            ass(ResponseWssIntegrity.class),
            ass(ResponseWssConfidentiality.class),
            ass(ResponseWssTimestamp.class));

        FeatureSet xmlsecFw =
        fsr("set:XmlSec:Firewall", "SecureSpan Firewall XML security",
            "Adds timestamp and token manipulation",
            fs(xmlsecAccel),
            ass(RequestWssReplayProtection.class),
            ass(RequestWssTimestamp.class),
            ass(WssTimestamp.class),
            ass(ResponseWssSecurityToken.class));

        // Message Validation/Transform
        FeatureSet validationAccel =
        fsr("set:Validation:Accel", "SecureSpan Accelerator message validation and transformation",
            "XPath, Schema, XSLT, and SOAP operation detector",
            ass(RequestXpathAssertion.class),
            ass(ResponseXpathAssertion.class),
            ass(Operation.class),
            ass(SchemaValidation.class),
            ass(XslTransformation.class));

        FeatureSet validationFw =
        fsr("set:Validation:Firewall", "SecureSpan Firewall message validation and transformation",
            "Adds regex and attachments",
            fs(validationAccel),
            ass(RequestSwAAssertion.class),
            ass(Regex.class),
            ass(WsiBspAssertion.class),
            ass(WsiSamlAssertion.class));

        FeatureSet validationGateway =
        fsr("set:Validation:Gateway", "SecureSpan Gateway message validation and transformation",
            "Adds HTTP form/MIME conversion assertions",
            fs(validationFw),
            ass(HttpFormPost.class),
            ass(InverseHttpFormPost.class));

        // Message routing
        FeatureSet routingAccel =
        fsr("set:Routing:Accel", "SecureSpan Accelerator message routing",
            "HTTP and hardcoded responses.",
            fs(httpFront),
            fs(httpBack),
            ass(HardcodedResponseAssertion.class),
            ass(EchoRoutingAssertion.class));

        FeatureSet routingGateway =
        fsr("set:Routing:Gateway", "SecureSpan Gateway message routing",
            "Adds BRA and JMS routing.",
            fs(routingAccel),
            fs(jmsFront),
            fs(jmsBack),
            ass(BridgeRoutingAssertion.class));

        // Service availability
        FeatureSet availabilityAccel =
        fsr("set:Availability:Accel", "SecureSpan Accelerator service availability",
            "Time/Day, IP range, and throughput qutoa",
            ass(TimeRange.class),
            ass(RemoteIpRange.class),
            ass(ThroughputQuota.class));

        // Logging/auditing and alerts
        FeatureSet auditAccel =
        fsr("set:Audit:Accel", "SecureSpan Accelerator logging/auditing and alerts",
            "Auditing, email and SNMP traps",
            fs(snmp),
            ass(AuditAssertion.class),
            ass(AuditDetailAssertion.class),
            ass(EmailAlertAssertion.class));

        // Policy logic
        FeatureSet policyAccel =
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

        FeatureSet threatIps =
        fsr("set:Threats:IPS", "SecureSpan XML IPS threat protection",
            "Stealth fault, size limits, document structure threats, schema validation, and XTM (when it's done)",
            ass(FaultLevel.class),
            ass(OversizedTextAssertion.class),
            ass(RequestSizeLimit.class),
            ass(SchemaValidation.class)); // TODO Assertion for XTM Signature goes here, as soon as it exists

        FeatureSet threatAccel =
        fsr("set:Threats:Accel", "SecureSpan Accelerator threat protection",
            "Just document structure threats and schema validation",
            ass(OversizedTextAssertion.class),
            ass(SchemaValidation.class));

        FeatureSet threatFw =
        fsr("set:Threats:Firewall", "SecureSpan Firewall threat protection",
            "Both of the above, plus adds regex, SQL attacks, and cluster-wide replay protection",
            fs(threatIps),
            fs(threatAccel),
            ass(Regex.class),
            ass(SqlAttackAssertion.class),
            ass(RequestWssReplayProtection.class));

        // Custom assertions
        FeatureSet customFw =
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

        FeatureSet profileGateway =
        fsp("set:Profile:Gateway", "SecureSpan Gateway",
            "All features enabled.",
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

        PROFILE_ALL =
        fsp("set:Profile:Federal", "SecureSpan Federal",
            "Exactly the same features as SecureSpan Gateway, but Bridge software is not bundled.",
            fs(profileGateway));

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
    public static Map<String, FeatureSet> getAllFeatureSets() {
        return Collections.unmodifiableMap(sets);
    }


    /** @return all root-level FeatureSets, including all product profiles, building blocks, and twig features. */
    public static Map<String, FeatureSet> getRootFeatureSets() {
        return Collections.unmodifiableMap(rootSets);
    }


    /** @return all Product Profile FeatureSets. */
    public static Map<String, FeatureSet> getProductProfiles() {
        return Collections.unmodifiableMap(profileSets);
    }


    /** @return the product profile that has all features enabled. */
    public static FeatureSet getBestProductProfile() {
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
                    FeatureSet fs = sets.get(topName);
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

    static class FeatureSet implements Feature {
        final String name;
        final String desc;
        final String note;
        final FeatureSet[] sets;

        public FeatureSet(String name, String desc, String note, FeatureSet[] sets) {
            if (name == null || name.trim().length() < 1) throw new IllegalArgumentException("Invalid set name: " + name);
            this.name = name;
            this.desc = desc;
            this.note = note;
            this.sets = sets;
        }

        public FeatureSet(String name, String desc) {
            this(name, desc, null, new FeatureSet[0]);
        }

        public String getNote() {
            return note == null ? "" : note;
        }

        /** @return true if the specified feature set is containing in this or any subset.  This does a deep search and may be slow.  */
        public boolean contains(String name) {
            if (name.equals(this.name)) return true;
            for (FeatureSet featureSet : sets)
                if (featureSet.contains(name)) return true;
            return false;
        }

        /** Collect the names of this feature set and any sub sets it enables into the specified set. */
        public void collectAllFeatureNames(Set<String> collector) {
            for (FeatureSet subset : sets)
                subset.collectAllFeatureNames(collector);
            collector.add(name);
        }

        public String getName() {
            return name;
        }
    }

    /** Find already-registered FeatureSet by FeatureSet.  (Basically just asserts that a featureset is registered already.) */
    private static FeatureSet fs(FeatureSet fs)  throws IllegalArgumentException {
        return fs(fs.name);
    }

    /** Find already-registered FeatureSet by name. */
    private static FeatureSet fs(String name) throws IllegalArgumentException {
        FeatureSet got = sets.get(name);
        if (got == null || name == null) throw new IllegalArgumentException("Unknown feature set name: " + name);
        return got;
    }

    /** Create and register a new root-level FeatureSet with no note. */
    private static FeatureSet fsr(String name, String desc, FeatureSet... deps) {
        return fsr(name, desc, null, deps);
    }

    /** Create and register a new root-level FeatureSet. */
    private static FeatureSet fsr(String name, String desc, String note, FeatureSet... deps) throws IllegalArgumentException {
        if (!name.startsWith("set:")) throw new IllegalArgumentException("Non-leaf feature set name must start with \"set:\": " + name);
        FeatureSet newset = new FeatureSet(name, desc, note, deps);
        FeatureSet old = sets.put(name, newset);
        if (old != null) throw new IllegalArgumentException("Duplicate feature set name: " + name);
        rootSets.put(name, newset);
        return newset;
    }

    /** Create and register a new root-level "product profile" FeatureSet. */
    private static FeatureSet fsp(String name, String desc, String note, FeatureSet... deps) throws IllegalArgumentException {
        FeatureSet got = fsr(name, desc, note, deps);
        profileSets.put(name, got);
        return got;
    }

    /** @return the feature name to use for the specified Assertion class name. */
    static String getFeatureSetNameForAssertion(Class<? extends Assertion> assertion) {
        String classname = assertion.getName();
        String pass = "com.l7tech.policy.assertion.";
        if (!classname.startsWith(pass))
            throw new IllegalArgumentException("Assertion concrete classname must start with " + pass);
        String rest = classname.substring(pass.length());
        rest = stripSuffix(rest, "Assertion");
        return "assertion:" + rest;
    }

    /** Create (and register, if new) a feature set for the specified policy assertion and return it. */
    private static FeatureSet ass(Class<? extends Assertion> ass) {
        String name = getFeatureSetNameForAssertion(ass);
        String classname = ass.getName();
        String desc = "Policy assertion: " + classname;

        return getOrMakeFeatureSet(name, desc);
    }

    private static FeatureSet getOrMakeFeatureSet(String name, String desc) {
        FeatureSet got = sets.get(name);
        if (got != null) {
            if (!desc.equals(got.desc)) throw new IllegalArgumentException("Already have different feature set named: " + name);
            return got;
        }

        got = new FeatureSet(name, desc);
        sets.put(name, got);
        return got;
    }

    /** Remove a suffix from a string, ie changing "FooAssertion" into "Foo" or "BlahFooberServlet" into "BlahFoober". */
    private static String stripSuffix(String rest, String suffix) {
        if (rest.endsWith(suffix))
            rest = rest.substring(0, rest.length() - suffix.length());
        return rest;
    }

    /** Create (and register, if new) a new miscellaneous FeatureSet with the specified name and description. */
    private static FeatureSet misc(String name, String desc) {
        FeatureSet got = sets.get(name);
        if (got != null) {
            if (!desc.equals(got.desc))
                throw new IllegalArgumentException("Feature set name already in use with different description: " + name);
            return got;
        }

        got = new FeatureSet(name, desc);
        sets.put(name, got);
        return got;
    }

    /** Create (and register, if new) a new FeatureSet for the specified HttpServlet and return it. */
    private static FeatureSet srv(Class<? extends HttpServlet> srv, String desc) {
        return srv(srv, desc, null);
    }

    /**
     * Create (and register, if new) a new FeatureSet for the specified HttpServlet and return it,
     * but using the specified name instead of the default.
     */
    private static FeatureSet srv(Class<? extends HttpServlet> srv, String desc, String preferredName) {
        String classname = srv.getName();
        int lastdot = classname.lastIndexOf('.');
        String rest = classname.substring(lastdot + 1);
        rest = stripSuffix(rest, "Servlet");
        String name = "service:" + rest;

        if (preferredName != null) {
            if (!preferredName.startsWith("service:"))
                throw new IllegalArgumentException("Preferred feature name for service must start with \"service:\": " + preferredName);
            if (sets.containsKey(name))
                throw new IllegalArgumentException("Service with preferred feature name " + preferredName + " has already been registered as default name " + name);
            name = preferredName;
        }

        return getOrMakeFeatureSet(name, desc);
    }

}
