/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.gateway.common.License;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.transport.PreemptiveCompression;
import com.l7tech.policy.assertion.transport.RemoteDomainIdentityInjection;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.policy.AllAssertions;

import javax.servlet.http.HttpServlet;
import java.util.*;
import java.util.logging.Logger;
import java.text.MessageFormat;

/**
 * Master list of Feature Sets for the SSG, hard-baked into the code so it will be obfuscated.
 * @noinspection JavaDoc,StaticMethodNamingConvention,OverloadedMethodsWithSameNumberOfParameters,OverloadedVarargsMethod,OverlyCoupledClass
 */
public class GatewayFeatureSets {
    private static final Logger logger = Logger.getLogger(GatewayFeatureSets.class.getName());

    /** All pre-configured FeatureSets. */
    private static final Map<String, GatewayFeatureSet> sets = new LinkedHashMap<String, GatewayFeatureSet>();

    /** Only the root-level FeatureSets. */
    private static final Map<String, GatewayFeatureSet> rootSets = new LinkedHashMap<String, GatewayFeatureSet>();

    /** Only the root-level Product Profile feature sets. */
    private static final Map<String, GatewayFeatureSet> profileSets = new LinkedHashMap<String, GatewayFeatureSet>();

    /** Feature sets that won't work unless SERVICE_MODULELOADER is also available. */
    private static final Set<String> optionalModules = new HashSet<String>();

    /** The ultimate Product Profile that enables every possible feature. */
    public static final GatewayFeatureSet PROFILE_ALL;

    /** Feature set to use for (usually old) licenses that, while valid, do not explicitly name any feature sets. */
    public static final GatewayFeatureSet PROFILE_LICENSE_NAMES_NO_FEATURES;

    // Constants for service names
    public static final String SERVICE_MESSAGEPROCESSOR = "service:MessageProcessor";
    public static final String SERVICE_FTP_MESSAGE_INPUT = "service:FtpMessageInput";
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
    public static final String SERVICE_TRUSTSTORE = "service:TrustStore"; // Ability to configure Trusted Certs
    public static final String SERVICE_KEYSTORE = "service:KeyStore"; // Ability to configure Private Keys
    public static final String SERVICE_MODULELOADER = "service:ModuleLoader"; // Ability to load jars from /ssg/modules/assertions
    public static final String SERVICE_EMS = "service:EnterpriseManageable"; // Ability to be managed remotely by an Enterprise Manager Server

    public static final String UI_PUBLISH_SERVICE_WIZARD = "ui:PublishServiceWizard";
    public static final String UI_PUBLISH_XML_WIZARD = "ui:PublishXmlWizard";
    public static final String UI_PUBLISH_INTERNAL_WIZARD = "ui:PublishInternalWizard";
    public static final String UI_WSDL_CREATE_WIZARD = "ui:WsdlCreateWizard";
    public static final String UI_AUDIT_WINDOW = "ui:AuditWindow";
    public static final String UI_RBAC_ROLE_EDITOR = "ui:RbacRoleEditor";
    public static final String UI_DASHBOARD_WINDOW = "ui:DashboardWindow";
    public static final String UI_MANAGE_LOG_SINKS = "ui:ManageLogSinks";
    public static final String UI_MANAGE_EMAIL_LISTENERS = "ui:ManageEmailListeners";

    public static final String FEATURE_SIGNED_ATTACHMENTS = "feature:SignedAttachments";

    private static final String SET_MODULAR_ASSERTIONS = "set:modularAssertions";

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

        GatewayFeatureSet ftpFront =
        fsr("set:ftp:front", "Allow incoming FTP messages",
            srv(SERVICE_FTP_MESSAGE_INPUT, "Accept incoming messages over FTP"),
            mass("assertion:FtpCredential"));

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
        fsr("set:ssb", "Features needed for best use of the SecureSpan XML VPN Client",
            srv(SERVICE_CSRHANDLER, "Certificate signer (CA) service"),
            srv(SERVICE_PASSWD, "Internal user password change service"),
            srv(SERVICE_POLICYDISCO, "Policy discovery service"),
            srv(SERVICE_WSDLPROXY, "WSDL proxy service"));

        GatewayFeatureSet snmp =
        fsr("set:snmp", "SNMP features",
            srv(SERVICE_SNMPQUERY, "HTTP SNMP query service"),
            mass("assertion:SnmpTrap"));

        GatewayFeatureSet trustStore =
        fsr("set:trustStore", "Ability to configure Trusted Certificates",
            srv(SERVICE_TRUSTSTORE, "Ability to configure Trusted Certificates"));

        GatewayFeatureSet keyStore =
        fsr("set:keyStore", "Ability to configure Private Keys",
            srv(SERVICE_KEYSTORE, "Ability to configure Private Keys"));

        GatewayFeatureSet moduleLoader = srv(SERVICE_MODULELOADER, "Enables the assertion module loader (.AAR files)",
                "Note: This feature set IS REQUIRED in order to use bundled and optional modular assertions, as well as post-release modular assertions.");

        GatewayFeatureSet modularAssertions =
        fsr(SET_MODULAR_ASSERTIONS, "Ability to use post-release modular assertions",
            "This is any Assertion that was not listed in AllAssertions or GatewayFeatureSets when this version of the SecureSpan code was built.  " +
              "Note: This feature set is NOT required in order to use optional modular assertions that WERE listed in GatewayFeatureSets when this " +
              "version of the SecureSpan code was built.  Enabling this implies also enabling the assertion module loader.",
            moduleLoader);

        GatewayFeatureSet ems =
        fsr("set:ems", "Enable Enterprise Manager support",
            "Enables Gateway to be managed by an Enterprise Manager Server.",
            srv(SERVICE_EMS, "Ability to be managed remotely by an Enterprise Manager Server",
                "Includes the ability to view and modify Trusted EMS registrations and EMS user mappings, both in the SSM and using the bootstrap web page."));

        GatewayFeatureSet experimental =
        fsr("set:experimental", "Enable experimental features",
            "Enables features that are only present during development, and that will be moved or renamed before shipping.",
            srv(SERVICE_BRIDGE, "Experimental SSB service (standalone, non-BRA, present-but-disabled)"),
            ass(WsspAssertion.class),
            mass("assertion:CacheLookup"),
            mass("assertion:CacheStorage"));

        GatewayFeatureSet uiPublishServiceWizard = ui(UI_PUBLISH_SERVICE_WIZARD, "Enable the SSM Publish SOAP Service Wizard");
        GatewayFeatureSet uiPublishXmlWizard = ui(UI_PUBLISH_XML_WIZARD, "Enable the SSM Publish XML Service Wizard");
        GatewayFeatureSet uiPublishInternalWizard = ui(UI_PUBLISH_INTERNAL_WIZARD, "Enable the SSM Publish Internal Service Wizard");
        GatewayFeatureSet uiWsdlCreateWizard = ui(UI_WSDL_CREATE_WIZARD, "Enable the SSM WSDL Create Wizard");
        GatewayFeatureSet uiAuditWindow = ui(UI_AUDIT_WINDOW, "Enable the SSM Audit Window");
        GatewayFeatureSet uiRbacRoleEditor = ui(UI_RBAC_ROLE_EDITOR, "Enable the SSM RBAC Role Editor");
        GatewayFeatureSet uiDashboardWindow = ui(UI_DASHBOARD_WINDOW, "Enable the SSM Dashboard Window");
        GatewayFeatureSet uiLogSinksDialog = ui(UI_MANAGE_LOG_SINKS, "Enable the SSM Log Sinks Dialog");
        GatewayFeatureSet uiEmailListenersDialog = ui(UI_MANAGE_EMAIL_LISTENERS, "Enable the SSM Email Listeners Dialog");

        //
        // Declare "building block" feature sets
        // (feature sets built out of "twig" feature sets, and which may include other building block feature sets,
        //  but which on their own may still be useless or of little value as a product.)
        // Naming convention:  set:CamelCaseCategory:LevelName
        //

        GatewayFeatureSet uiAccel =
        fsr("set:UI:Accel", "SecureSpan Accelerator UI features",
            uiPublishXmlWizard,
            uiPublishServiceWizard,
            uiAuditWindow,
            uiDashboardWindow,
            uiWsdlCreateWizard,
            uiLogSinksDialog);

        GatewayFeatureSet uiDs =
        fsr("set:UI:Datascreen", "SecureSpan Datascreen UI features",
            "Adds Publish SOAP Web Service Wizard and Create WSDL Wizard",
            fs(uiAccel));

        GatewayFeatureSet uiFw =
        fsr("set:UI:Firewall", "SecureSpan Firewall UI features",
            "Adds RBAC role editing",
            fs(uiDs),
            uiRbacRoleEditor);

        GatewayFeatureSet accessFw =
        fsr("set:AccessControl:Firewall", "SecureSpan Firewall access control",
            "Adds access control WSS message-level security (including WS-SecureConversation) and identity mapping",
            fs(wssc),
            ass(SpecificUser.class),
            ass(MemberOfGroup.class),
            ass(AuthenticationAssertion.class),
            ass(SamlIssuerAssertion.class),
            ass(HttpBasic.class),
            ass(HttpDigest.class),
            ass(SslAssertion.class), // TODO omit client cert support from this grant (when it is possible to do so)
            ass(XpathCredentialSource.class),
            ass(RequestWssX509Cert.class),
            ass(WssBasic.class),
            ass(RequestWssSaml.class),
            ass(RequestWssSaml2.class),
            ass(EncryptedUsernameTokenAssertion.class),
            ass(WsTrustCredentialExchange.class),
            ass(SamlBrowserArtifact.class),
            ass(PreemptiveCompression.class),
            ass(RemoteDomainIdentityInjection.class),
            mass("assertion:LDAPQuery"),
            mass("assertion:IdentityAttributes"));

        GatewayFeatureSet accessGateway =
        fsr("set:AccessControl:Gateway", "SecureSpan Gateway access control",
            "Adds SSL client certs (although currently this comes with our SSL support and can't be disabled)",
            fs(accessFw),
            ass(SslAssertion.class)); // TODO enable client cert here exclusively (when it is possible to do so)

        // XML Security
        GatewayFeatureSet xmlsecAccel =
        fsr("set:XmlSec:Accel", "SecureSpan Accelerator XML security",
            "Element signature and encryption using WSS",
            ass(ResponseWssTimestamp.class));

        GatewayFeatureSet xmlsecFw =
        fsr("set:XmlSec:Firewall", "SecureSpan Firewall XML security",
            "Adds timestamp and token manipulation",
            fs(xmlsecAccel),
            ass(RequestWssIntegrity.class),
            ass(RequestWssConfidentiality.class),
            ass(ResponseWssIntegrity.class),
            ass(ResponseWssConfidentiality.class),
            ass(RequestWssReplayProtection.class),
            ass(RequestWssTimestamp.class),
            ass(ResponseWssSecurityToken.class),
            ass(WssVersionAssertion.class),
            misc(FEATURE_SIGNED_ATTACHMENTS, "Signed SOAP attachments.", null));

        // Message Validation/Transform
        GatewayFeatureSet validationAccel =
        fsr("set:Validation:Accel", "SecureSpan Accelerator message validation and transformation",
            "XPath, Schema, XSLT, and SOAP operation detector",
            ass(RequestXpathAssertion.class),
            ass(ResponseXpathAssertion.class),
            ass(SchemaValidation.class),
            ass(XslTransformation.class));

        GatewayFeatureSet validationDs =
        fsr("set:Validation:Datascreen", "SecureSpan Datascreen message validation and transformation",
            "Adds regex and attachments",
            fs(validationAccel),
            ass(RequestSwAAssertion.class),
            ass(Regex.class),
            ass(HtmlFormDataAssertion.class),
            ass(CodeInjectionProtectionAssertion.class));

        GatewayFeatureSet validationFw =
        fsr("set:Validation:Firewall", "SecureSpan Firewall message validation and transformation",
            "Adds regex, attachments, WSDL operation, and WS-I BSP and SAML validation",
            fs(validationAccel),
            ass(Operation.class),
            ass(RequestSwAAssertion.class),
            ass(Regex.class),
            ass(WsiBspAssertion.class),
            ass(WsiSamlAssertion.class),
            ass(WsspAssertion.class),
            ass(HtmlFormDataAssertion.class),
            ass(CodeInjectionProtectionAssertion.class),
            mass("assertion:WsAddressing"));

        GatewayFeatureSet validationGateway =
        fsr("set:Validation:Gateway", "SecureSpan Gateway message validation and transformation",
            "Adds HTTP form/MIME conversion assertions",
            fs(validationFw),
            ass(HttpFormPost.class),
            ass(InverseHttpFormPost.class));

        // Message routing
        GatewayFeatureSet routingIps =
        fsr("set:Routing:IPS", "SecureSpan IPS message routing",
            "HTTP and JMS routing",
            fs(httpFront),
            fs(httpBack),
            fs(jmsFront),
            fs(jmsBack),
            fs(trustStore),
            fs(keyStore));

        GatewayFeatureSet routingAccel =
        fsr("set:Routing:Accel", "SecureSpan Accelerator message routing",
            "HTTP and hardcoded responses.",
            fs(httpFront),
            fs(httpBack),
            mass("assertion:HardcodedResponse"),
            mass("assertion:EchoRouting"));

        GatewayFeatureSet routingFw =
        fsr("set:Routing:Firewall", "SecureSpan Firewall message routing",
            "Adds abiltity to configure Trusted Certificates",
            fs(routingAccel),
            fs(trustStore),
            fs(keyStore));

        GatewayFeatureSet routingGateway =
        fsr("set:Routing:Gateway", "SecureSpan Gateway message routing",
            "Adds BRA, JMS and FTP routing.",
            fs(routingFw),
            fs(ftpFront),
            fs(jmsFront),
            fs(jmsBack),
            fs(uiEmailListenersDialog),
            ass(BridgeRoutingAssertion.class),
            mass("assertion:FtpRouting"));

        // Service availability
        GatewayFeatureSet availabilityAccel =
        fsr("set:Availability:Accel", "SecureSpan Accelerator service availability",
            "Time/Day and IP range",
            ass(TimeRange.class),
            ass(RemoteIpRange.class),
            mass("assertion:RateLimit"));

        GatewayFeatureSet availabilityFw =
        fsr("set:Availability:Firewall", "SecureSpan Firewall service availability",
            "Adds throughput qutoa",
            fs(availabilityAccel),
            ass(ThroughputQuota.class));

        // Logging/auditing and alerts
        GatewayFeatureSet auditAccel =
        fsr("set:Audit:Accel", "SecureSpan Accelerator logging/auditing and alerts",
            "Auditing, email and SNMP traps",
            fs(snmp),
            ass(AuditAssertion.class),
            ass(AuditDetailAssertion.class),
            ass(EmailAlertAssertion.class),
            mass("assertion:MessageContext"));

        // Policy logic
        GatewayFeatureSet policyAccel =
        fsr("set:Policy:Accel", "SecureSpan Accelerator complex policy logic",
            "Branches, comments, comparisons, variables, includes and echoing",
            fs(core),
            fs(branching),
            ass(CommentAssertion.class),
            mass("assertion:Comparison"),
            ass(TrueAssertion.class),
            ass(FalseAssertion.class),
            ass(SetVariableAssertion.class),
            ass(Include.class),
            mass("assertion:EchoRouting"),
            mass("assertion:HardcodedResponse"));

        GatewayFeatureSet threatIps =
        fsr("set:Threats:IPS", "SecureSpan XML IPS threat protection",
            "Stealth fault, size limits, document structure threats, schema validation, and XTM (when it's done)",
            ass(FaultLevel.class),
            ass(OversizedTextAssertion.class),
            ass(RequestSizeLimit.class),
            ass(SqlAttackAssertion.class),
            ass(SchemaValidation.class)); // TODO Assertion for XTM Signature goes here, as soon as it exists

        GatewayFeatureSet threatAccel =
        fsr("set:Threats:Accel", "SecureSpan Accelerator threat protection",
            "Just document structure threats and schema validation",
            ass(SchemaValidation.class),
            ass(FaultLevel.class));

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
            ass(RequestWssKerberos.class),
            ass(CookieCredentialSourceAssertion.class),
            moduleLoader);

        GatewayFeatureSet customDs =
        fsr("set:Custom:Datascreen", "SecureSpan Datascreen custom assertions",
            "Symantec only",
            ass(CustomAssertionHolder.class),
            moduleLoader);

        GatewayFeatureSet esmAssertions =
        fsr("set:ESM:Assertions", "The necessary assertions to enable ESM functionality",
            mass("assertion:EsmMetrics"),
            mass("assertion:EsmSubscription"));

        // US (NCES)
        GatewayFeatureSet usAssertions =
        fsr("set:US:Assertions", "US decoration and validation assertions",
            mass("assertion:NcesDecorator"),
            mass("assertion:NcesValidator"),
            esmAssertions,
            uiPublishInternalWizard);

        // Formerly a profile set, now present only for backward compatibility
        fsr("set:Profile:IPS", "SecureSpan XML IPS",
            "DEPRECATED -- present for license compat only",
            fs(core),
            fs(admin),
            fs(routingIps),
            fs(threatIps),
            fs(availabilityFw),
            fs(validationAccel),
            fs(auditAccel),
            fs(policyAccel));


        //
        // Declare "product profile" feature sets
        // (feature sets built out of "building block" feature sets, and which each constitutes a useable,
        //  complete product in its own right.)
        // Naming convention:   set:Profile:ProfileName
        //
        fsp("set:Profile:Datascreen", "SecureSpan Data Screen",
            "HTTP/HTML/AJAX/JSON/XML gateway",
            fs(core),
            fs(admin),
            fs(routingAccel),
            fs(threatIps),
            fs(availabilityFw),
            fs(validationDs),
            fs(auditAccel),
            fs(policyAccel),
            fs(uiDs),
            fs(customDs),
            ass(SslAssertion.class),
            srv(SERVICE_WSDLPROXY, "WSDL proxy service")); // TODO omit client cert support from this grant (when it is possible to do so)

        fsp("set:Profile:Accel", "SecureSpan Accelerator",
            "XML acceleration features",
            fs(core),
            fs(admin),
            fs(xmlsecAccel),
            fs(validationAccel),
            fs(routingAccel),
            fs(availabilityAccel),
            fs(auditAccel),
            fs(policyAccel),
            fs(threatAccel),
            fs(uiAccel),
            fs(moduleLoader),
            ass(SslAssertion.class),
            srv(SERVICE_WSDLPROXY, "WSDL proxy service")); // TODO omit client cert support from this grant (when it is possible to do so)

            fsp("set:Profile:Firewall", "SecureSpan Firewall",
            "XML firewall with custom assertions.  No BRA, no JMS, no special XML VPN Client support",
            fs(core),
            fs(admin),
            fs(accessFw),
            fs(xmlsecFw),
            fs(validationFw),
            fs(routingFw),
            fs(availabilityFw),
            fs(auditAccel),
            fs(policyAccel),
            fs(threatFw),
            fs(uiFw),
            fs(ssb),
            fs(customFw));

        GatewayFeatureSet profileGateway =
        fsp("set:Profile:Gateway", "SecureSpan Gateway",
            "All features enabled.",
            misc("bundle:Bridge", "Bundled SecureSpan XML VPN Client", "No effect on Gateway license code -- only purpose is to distinguish two otherwise-identical feature sets"),
            fs(core),
            fs(admin),
            fs(accessGateway),
            fs(xmlsecFw),
            fs(validationGateway),
            fs(routingGateway),
            fs(availabilityFw),
            fs(auditAccel),
            fs(policyAccel),
            fs(threatFw),
            fs(customFw),
            fs(ssb),
            fs(modularAssertions),
            fs(uiFw),
            fs(ems),
            fs(experimental));

        fsp("set:Profile:PolicyIntegrationPoint", "SecureSpan Policy Integration Point",
            "Same as SecureSpan Gateway.",
            fs(profileGateway));

        PROFILE_ALL =
        fsp("set:Profile:US", "SecureSpan Gateway US",
            "Adds US features.",
            fs(profileGateway),
            fs(usAssertions));

        fsp("set:Profile:Federal", "SecureSpan Federal",
            "Exactly the same features as SecureSpan Gateway, but XML VPN Client software is not bundled.",
            fs(core),
            fs(admin),
            fs(accessGateway),
            fs(xmlsecFw),
            fs(validationGateway),
            fs(routingGateway),
            fs(availabilityFw),
            fs(auditAccel),
            fs(policyAccel),
            fs(threatFw),
            fs(customFw),
            fs(ssb),
            fs(modularAssertions),
            fs(uiFw),
            fs(experimental));

        // For now, if a license names no features explicitly, we will enable all features.
        // TODO we should enable only those features that existed in 3.5.
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

                //noinspection unchecked
                for (String topName : (Iterable<String>)inputSet) {
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

    /**
     * Check if the specified assertion, identified by its feature set name, is marked as an optional module
     * that requires the module loading capability in order to work.
     *
     * @param fsName  the feature set name, ie "assertion:RateLimit".  Required.
     * @return true if this feature set is marked as requiring the module loader.
     */
    public static boolean isOptionalModularAssertion(String fsName) {
        return optionalModules.contains(fsName);
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

    /** Create and register a new non-leaf GatewayFeatureSet. */
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
        return Assertion.getFeatureSetName(assertionClass);
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
        if (SET_MODULAR_ASSERTIONS.equals(name))
            throw new IllegalArgumentException(MessageFormat.format("{0} is a built-in assertion, but is using the {1} feature set name (possibly missing from {2})", ass.getClass().getSimpleName(), SET_MODULAR_ASSERTIONS, AllAssertions.class.getSimpleName()));
        String classname = ass.getName();
        String desc = "Policy assertion: " + classname;

        return getOrMakeFeatureSet(name, desc);
    }

    /** Create (and register, if new) a feature set for the specified optional modular assertion, and return it. */
    private static GatewayFeatureSet mass(String fsName) {
        String prefix = "assertion:";
        if (!fsName.startsWith(prefix))
            throw new IllegalArgumentException("Optional modular assertion feature set name doesn't start with \"assertion:\" :" + fsName);
        String rest = fsName.substring(prefix.length());
        if (rest.length() < 1)
            throw new IllegalArgumentException("Optional modular assertion feature set local name is empty:" + fsName);
        if (rest.endsWith("Assertion"))
            throw new IllegalArgumentException("Optional modular assertion feature set name should not end with \"Assertion\"");
        String desc = "Optional modular policy assertion: " + rest;
        optionalModules.add(fsName);
        return getOrMakeFeatureSet(fsName, desc);
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
     * Create (and register, if new) a new GatewayFeatureSet for the specified HttpServlet or Gateway server component and return it,
     * but using the specified name instead of the default.
     */
    private static GatewayFeatureSet srv(String name, String desc) {
        if (!name.startsWith("service:"))
            throw new IllegalArgumentException("Preferred feature name for service must start with \"service:\": " + name);
        return getOrMakeFeatureSet(name, desc);
    }

    private static GatewayFeatureSet srv(String name, String desc, String note) {
        if (!name.startsWith("service:"))
            throw new IllegalArgumentException("Preferred feature name for service must start with \"service:\": " + name);
        return misc(name, desc, note);
    }

    private static GatewayFeatureSet ui(String name, String desc) {
        if (!name.startsWith("ui:"))
            throw new IllegalArgumentException("Preferred feature name for ui feature must start with \"ui:\": " + name);
        return getOrMakeFeatureSet(name, desc);
    }
}
