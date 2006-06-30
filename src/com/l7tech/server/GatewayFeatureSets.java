/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.alert.SnmpTrapAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.server.identity.cert.CSRHandler;
import com.l7tech.server.policy.PolicyServlet;

import javax.servlet.http.HttpServlet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Master list of Feature Sets
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


    static {
        // Declare all baked-in feature sets

        //
        // Declare "twig" feature sets
        // (feature sets that don't include other feature sets, totally useless on their own, but not
        //  a "leaf" feature set like a single assertion or servlet)
        //
        FeatureSet core =
        fsr("set:Gateway:Core", "Core features, without which nothing else will work", "Always needed",
            ass(AllAssertion.class),
            ass(UnknownAssertion.class));

        FeatureSet branching =
        fsr("set:Gateway:Policy:Branching", "Support for branching policies",
            ass(AllAssertion.class),
            ass(ExactlyOneAssertion.class),
            ass(OneOrMoreAssertion.class));

        FeatureSet threatProtection =
        fsr("set:Gateway:ThreatProtection", "All threat protection",
            ass(Regex.class),
            ass(SqlAttackAssertion.class),
            ass(OversizedTextAssertion.class),
            ass(RequestSizeLimit.class),
            ass(RequestWssReplayProtection.class),
            ass(SchemaValidation.class));

        FeatureSet validationBasic =
        fsr("set:Gateway:Validation:Basic", "Basic message validation features",
            ass(RequestSizeLimit.class),
            ass(OversizedTextAssertion.class));

        FeatureSet validationAll =
        fsr("set:Gateway:Validation:All", "All message validation features",
            fs(validationBasic),
            ass(WsiBspAssertion.class),
            ass(WsiSamlAssertion.class));

        FeatureSet audit =
        fsr("set:Gateway:Audit", "Auditing features",
            ass(AuditAssertion.class),
            ass(AuditDetailAssertion.class));

        FeatureSet snmp =
        fsr("set:Gateway:SNMP", "SNMP features",
            srv(SnmpQueryServlet.class),
            ass(SnmpTrapAssertion.class));

        FeatureSet email =
        fsr("set:Gateway:Email", "Email features",
            ass(EmailAlertAssertion.class));

        FeatureSet availability =
        fsr("set:Gateway:Availability", "Service availability features",
            ass(TimeRange.class),
            ass(RemoteIpRange.class),
            ass(ThroughputQuota.class));

        FeatureSet xpath =
        fsr("set:Gateway:XML:XPath", "XPath features",
            ass(RequestXpathAssertion.class),
            ass(ResponseXpathAssertion.class));

        FeatureSet xslt =
        fsr("set:Gateway:XML:XSLT", "XSLT features",
            ass(XslTransformation.class));

        FeatureSet schema =
        fsr("set:Gateway:XML:Schema", "Schema validation features",
            ass(SchemaValidation.class));

        FeatureSet xml =
        fsr("set:Gateway:XML", "All XML features",
            fs(xpath),
            fs(xslt),
            fs(schema));

        FeatureSet bra =
        fsr("set:Gateway:BRA", "Bridge Routing Assertion",
            ass(BridgeRoutingAssertion.class));

        FeatureSet httpFront =
        fsr("set:Gateway:HTTP:Front", "Allow incoming HTTP messages",
            srv(SoapMessageProcessingServlet.class, "service:HttpMessageProcessor"));

        FeatureSet httpBack =
        fsr("set:Gateway:HTTP:Back", "Allow outgoing HTTP messages",
            ass(HttpRoutingAssertion.class));

        FeatureSet jmsFront =
        fsr("set:Gateway:JMS:Front", "Allow incoming JMS messages",
            misc("service:JmsMessageProcessor", "JMS message processor"));

        FeatureSet jmsBack =
        fsr("set:Gateway:JMS:Back", "Allow outgoing JMS messages",
            ass(JmsRoutingAssertion.class));

        FeatureSet httpCredsSimple =
        fsr("set:Gateway:Creds:HTTP:Simple", "Allow simple HTTP-based credential sources (not including SSL or SPNEGO)",
            ass(HttpBasic.class),
            ass(HttpDigest.class));

        FeatureSet httpCredsAll =
        fsr("set:Gateway:Creds:HTTP:All", "Allow all HTTP-based credential sources (including SSL and SPNEGO)",
            fs(httpCredsSimple),
            ass(HttpNegotiate.class),
            ass(SslAssertion.class));

        FeatureSet wssBasicCreds =
        fsr("set:Gateway:Creds:WSS:Basic", "Allow simple message-level credential sources",
            ass(WssBasic.class));

        FeatureSet wssSimple =
        fsr("set:Gateway:WSS:Intermediate", "Allow simple WSS 1.0 message-level security assertions, not including SAML",
            fs(wssBasicCreds),
            ass(RequestWssX509Cert.class),
            ass(RequestWssIntegrity.class),
            ass(RequestWssConfidentiality.class),
            ass(ResponseWssIntegrity.class),
            ass(ResponseWssConfidentiality.class),
            ass(WssTimestamp.class));

        FeatureSet wss =
        fsr("set:Gateway:WSS:All", "Allow all WSS message-level-security assertions including SAML and EncryptedUsernameToken",
            fs(wssSimple),
            ass(RequestWssSaml.class),
            ass(RequestWssSaml2.class),
            ass(EncryptedUsernameTokenAssertion.class),
            ass(RequestWssReplayProtection.class),
            ass(RequestWssTimestamp.class),
            ass(ResponseWssTimestamp.class),
            ass(RequestWssKerberos.class));

        FeatureSet attachments =
        fsr("set:Gateway:SwA", "Allow SOAP-with-attachments assertion",
            ass(RequestSwAAssertion.class));

        FeatureSet authUsers =
        fsr("set:Gateway:Auth:Users", "Allow SpecificUser authentication",
            ass(SpecificUser.class));

        FeatureSet authAll =
        fsr("set:Gateway:Auth:All", "Allow all user and group authentication",
            fs(authUsers),
            ass(MemberOfGroup.class));

        FeatureSet customAss =
        fsr("set:Gateway:CustomAssertion", "Allow CustomAssertionHolder to appear in a policy",
            ass(CustomAssertionHolder.class));

        FeatureSet sts =
        fsr("set:Gateway:STS", "Enable built-in Security Token Service",
            srv(TokenServiceServlet.class),
            ass(SecureConversation.class));

        FeatureSet wsTrust =
        fsr("set:Gateway:WsTrust", "Enable WS-Trust-specific assertions",
            ass(WsTrustCredentialExchange.class),
            ass(WsFederationPassiveTokenExchange.class),
            ass(WsFederationPassiveTokenRequest.class));

        FeatureSet csrSrv =
        fsr("set:Gateway:CertAuthority", "Enable Certificate Authority service (CSRHandler)",
            srv(CSRHandler.class));

        FeatureSet passwdSrv =
        fsr("set:Gateway:PasswdChange", "Enable password-change service",
            srv(PasswdServlet.class));

        FeatureSet policySrv =
        fsr("set:Gateway:PolicyService", "Enable policy-discovery service",
            srv(PolicyServlet.class));

        FeatureSet wsdlProxy =
        fsr("set:Gateway:WsdlProxy", "Enable WSDL proxy service",
            srv(WsdlProxyServlet.class));

        FeatureSet experimental =
        fsr("set:Gateway:Experimental", "Enable experimental features",
            "Enables features that are only present during developement, and that will be moved or renamed before shipping.",
            ass(WsspAssertion.class));


        //
        // Declare "building block" feature sets
        // (feature sets built out of "twig" feature sets, and which may include other building block feature sets,
        //  but which on their own may still be useless or of little value as a product.)
        //
        FeatureSet policy =
        fsr("set:Gateway:Policy", "Support for complex policy logic",
            fs(core),
            fs(branching),
            ass(CommentAssertion.class),
            ass(ComparisonAssertion.class),
            ass(TrueAssertion.class),
            ass(FalseAssertion.class),
            ass(SetVariableAssertion.class),
            ass(EchoRoutingAssertion.class),
            ass(HardcodedResponseAssertion.class));


        FeatureSet http =
        fsr("set:Gateway:HTTP", "Basic HTTP appliance", "HTTP routing + credential sources.  No authentication on its own.",
            fs(policy),
            fs(httpFront),
            fs(httpBack),
            fs(httpCredsAll));

        FeatureSet jms =
        fsr("set:Gateway:JMS", "Basic JMS appliance", "JMS routing + credential sources.  No authentication on its own.",
            fs(policy),
            fs(jmsFront),
            fs(jmsBack),
            fs(wssBasicCreds));




        //
        // Declare "product profile" feature sets
        // (feature sets built out of "building block" feature sets, and which each constitutes a useable,
        //  complete product in its own right.)
        //
        FeatureSet xmlFirewallBronze =
        fsp("set:Profile:Gateway:XmlFirewall:Bronze", "Basic \"Bronze\" HTTP XML firewall",
            "HTTP-based XML firewall with authentication and message-level security.",
            fs(http),
            fs(wssSimple),
            fs(authUsers),
            fs(xml),
            fs(audit));

        FeatureSet xmlFirewallSilver =
        fsp("set:Profile:Gateway:XmlFirewall:Silver", "Intermediate \"Silver\" HTTP XML firewall",
            "HTTP-based XML firewall with authentication, message-level security, auditing, and attachments.",
            fs(xmlFirewallBronze),
            fs(authAll),
            fs(audit),
            fs(attachments),
            fs(wsdlProxy));

        FeatureSet xmlFirewallGold =
        fsp("set:Profile:Gateway:XmlFirewall:Gold", "Advanced \"Gold\" HTTP and JMS XML firewall",
            "HTTP/JMS XML firewall with authentication, message-level security, auditing, Bridge support, and more.",
            fs(xmlFirewallSilver),
            fs(jms),
            fs(xml),
            fs(wss),
            fs(threatProtection),
            fs(validationAll),
            fs(sts),
            fs(wsTrust),
            fs(bra),
            fs(email),
            fs(snmp),
            fs(policySrv),
            fs(csrSrv),
            fs(passwdSrv));

        PROFILE_ALL =
        fsp("set:Profile:Gateway:Platinum", "Complete, full-featured \"Platinum\" SecureSpan Gateway",
            "The ultimate SecureSpan Gateway, with all possible features enabled",
            fs(xmlFirewallGold),
            fs(http),
            fs(jms),
            fs(wss),
            fs(xml),
            fs(authAll),
            fs(bra),
            fs(attachments),
            fs(audit),
            fs(email),
            fs(snmp),
            fs(customAss),
            fs(threatProtection),
            fs(validationAll),
            fs(availability),
            fs(sts),
            fs(wsTrust),
            fs(experimental));

    }


    static class FeatureSet {
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

        return getOrMakeFeatureSet(name, desc, classname);
    }

    private static FeatureSet getOrMakeFeatureSet(String name, String desc, String classname) {
        FeatureSet got = sets.get(name);
        if (got != null) {
            if (!desc.equals(got.desc)) throw new IllegalArgumentException("Already have different feature set named: " + name);
            return got;
        }

        logger.info("Registered FeatureSet for " + classname + " named " + name); // TODO remove this line after testing

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
    private static FeatureSet srv(Class<? extends HttpServlet> srv) {
        return srv(srv, null);
    }

    /**
     * Create (and register, if new) a new FeatureSet for the specified HttpServlet and return it,
     * but using the specified name instead of the default.
     */
    private static FeatureSet srv(Class<? extends HttpServlet> srv, String preferredName) {
        String classname = srv.getName();
        int lastdot = classname.lastIndexOf('.');
        String rest = classname.substring(lastdot + 1);
        rest = stripSuffix(rest, "Servlet");
        String name = "service:" + rest;
        String desc = "Servlet: " + classname;

        if (preferredName != null) {
            if (!preferredName.startsWith("service:"))
                throw new IllegalArgumentException("Preferred feature name for service must start with \"service:\": " + preferredName);
            if (sets.containsKey(name))
                throw new IllegalArgumentException("Service with preferred feature name " + preferredName + " has already been registered as default name " + name);
            name = preferredName;
        }

        return getOrMakeFeatureSet(name, desc, classname);
    }

}
