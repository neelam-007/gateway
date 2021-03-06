package com.l7tech.server;

import com.l7tech.gateway.common.licensing.FeatureSetExpander;
import com.l7tech.policy.AllAssertions;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.*;
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
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.transport.PreemptiveCompression;
import com.l7tech.policy.assertion.transport.RemoteDomainIdentityInjection;
import com.l7tech.policy.assertion.xml.RemoveElement;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServlet;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * Master list of Feature Sets for the SSG, hard-baked into the code so it will be obfuscated.
 * @noinspection JavaDoc,StaticMethodNamingConvention,OverloadedMethodsWithSameNumberOfParameters,OverloadedVarargsMethod,OverlyCoupledClass
 */
public class GatewayFeatureSets {
    private static final Logger logger = Logger.getLogger(GatewayFeatureSets.class.getName());

    /** All pre-configured FeatureSets. */
    private static final Map<String, GatewayFeatureSet> sets = new LinkedHashMap<>();

    /** Only the root-level FeatureSets. */
    private static final Map<String, GatewayFeatureSet> rootSets = new LinkedHashMap<>();

    /** Only the root-level Product Profile feature sets. */
    private static final Map<String, GatewayFeatureSet> profileSets = new LinkedHashMap<>();

    /** Feature sets that won't work unless SERVICE_MODULELOADER is also available. */
    private static final Set<String> optionalModules = new HashSet<>();

    /** The ultimate Product Profile that enables every possible feature. */
    public static final GatewayFeatureSet PROFILE_ALL;

    // Constants for service names
    public static final String SERVICE_MESSAGEPROCESSOR = "service:MessageProcessor";
    public static final String SERVICE_FTP_MESSAGE_INPUT = "service:FtpMessageInput";
    public static final String SERVICE_SSH_MESSAGE_INPUT = "service:Ssh2MessageInput";
    public static final String SERVICE_MQTT_MESSAGE_INPUT = "service:MqttMessageInput";
    public static final String SERVICE_HTTP_MESSAGE_INPUT = "service:HttpMessageInput";
    public static final String SERVICE_JMS_MESSAGE_INPUT = "service:JmsMessageInput";
    public static final String SERVICE_MQNATIVE_MESSAGE_INPUT = "service:MqNativeMessageInput";
    public static final String SERVICE_EMAIL_MESSAGE_INPUT = "service:EmailMessageInput";
    public static final String SERVICE_L7RAWTCP_MESSAGE_INPUT = "service:L7RawTcpMessageInput";
    public static final String SERVICE_WEBSOCKET_MESSAGE_INPUT = "service:WebSocketMessageInput";
    public static final String SERVICE_ADMIN = "service:Admin";
    public static final String SERVICE_REMOTE_MANAGEMENT = "service:RemoteManagement";
    public static final String SERVICE_POLICYDISCO = "service:Policy";
    public static final String SERVICE_STS = "service:TokenService";
    public static final String SERVICE_CSRHANDLER = "service:CSRHandler";
    public static final String SERVICE_PASSWD = "service:Passwd";
    public static final String SERVICE_WSDLPROXY = "service:WsdlProxy";
    public static final String SERVICE_SNMPQUERY = "service:SnmpQuery";
    public static final String SERVICE_BRIDGE = "service:Bridge";
    public static final String SERVICE_TRUSTSTORE = "service:TrustStore"; // Ability to configure Trusted Certs
    public static final String SERVICE_KEYSTORE = "service:KeyStore"; // Ability to configure Private Keys
    public static final String SERVICE_SECURE_PASSWORD = "service:SecurePassword"; // Ability to manage secure passwords
    public static final String SERVICE_MODULELOADER = "service:ModuleLoader"; // Ability to load jars from /ssg/modules/assertions
    public static final String SERVICE_EMS = "service:EnterpriseManageable"; // Ability to be managed remotely by an Enterprise Manager Server
    public static final String SERVICE_ENCAPSULATED_ASSERTION = "service:EncapsulatedAssertion"; // Ability to use encapsulated assertions
    public static final String SERVICE_CASSANDRA = "service:Cassandra"; // Ability to configure Cassandra connections

    // Constants for flag names
    public static final String FLAG_PERMAFIPS = "flag:FipsModeAlways";

    public static final String UI_PUBLISH_SERVICE_WIZARD = "ui:PublishServiceWizard";
    public static final String UI_PUBLISH_XML_WIZARD = "ui:PublishXmlWizard";
    public static final String UI_PUBLISH_INTERNAL_WIZARD = "ui:PublishInternalWizard";
    public static final String UI_PUBLISH_WSDL_QUERY_HANDLER_WIZARD = "ui:PublishWsdlQueryHandlerWizard";
    public static final String UI_WSDL_CREATE_WIZARD = "ui:WsdlCreateWizard";
    public static final String UI_AUDIT_WINDOW = "ui:AuditWindow";
    public static final String UI_RBAC_ROLE_EDITOR = "ui:RbacRoleEditor";
    public static final String UI_DASHBOARD_WINDOW = "ui:DashboardWindow";
    public static final String UI_MANAGE_LOG_SINKS = "ui:ManageLogSinks";
    public static final String UI_MANAGE_AUDIT_SINK = "ui:ManageAuditSink";
    public static final String UI_MANAGE_EMAIL_LISTENERS = "ui:ManageEmailListeners";
    public static final String UI_MANAGE_ENCAPSULATED_ASSERTIONS = "ui:ManageEncapsulatedAssertions";
    public static final String UI_MANAGE_POLICY_BACKED_SERVICES = "ui:ManagePolicyBackedServices";
    public static final String UI_MANAGE_SECURITY_ZONES = "ui:ManageSecurityZones";
    public static final String UI_MANAGE_CASSANDRA_CONNECTIONS = "ui:ManageCassandraConnections";

    public static final String FEATURE_SIGNED_ATTACHMENTS = "feature:SignedAttachments";

    private static final String SET_MODULAR_ASSERTIONS = "set:modularAssertions";

    public static final String PROFILE_DATASCREEN = "set:Profile:Datascreen";

    public static final String PROFILE_ACCELERATOR = "set:Profile:Accel";

    public static final String PROFILE_FIREWALL = "set:Profile:Firewall";

    public static final String PROFILE_CLOUD_CONNECT = "set:Profile:CloudConnect";

    public static final String PROFILE_CLOUD_PROTECT = "set:Profile:CloudProtect";

    public static final String PROFILE_GATEWAY = "set:Profile:Gateway";

    public static final String PROFILE_GATEWAY_ENTERPRISE = "set:Profile:EnterpriseGateway";

    public static final String PROFILE_GATEWAY_ESSENTIALS = "set:Profile:GatewayEssentials";

    public static final String PROFILE_CLOUD_CONTROL = "set:Profile:CloudControl";

    public static final String PROFILE_POLICY_INTEGRATION_POINT = "set:Profile:PolicyIntegrationPoint";

    public static final String SET_PROFILE_GATEWAY_US = "set:Profile:US";

    public static final String PROFILE_FEDERAL = "set:Profile:Federal";

    public static final String PROFILE_API_PROXY = "set:Profile:Api";

    public static final String PROFILE_SALESFORCE_EXTENSION = "set:Profile:Salesforce";

    public static final String PROFILE_NCES_EXTENSION = "set:Profile:NCES";

    public static final String PROFILE_MOBILE_EXTENSION = "set:Profile:Mobile";

    //Mobile App Services Extension
    public static final String PROFILE_MAS_EXTENSION = "set:Profile:MAS";

    public static final String PROFILE_MICROSERVICES = "set:Profile:Microservices";

    public static final String PROFILE_DEVELOPMENT = "set:Profile:Development";
    public static final String FS_WEBSOCKETS = "set:WebSocket:Assertions";
    public static final String FS_CSRSIGNER = "set:CsrSigner:Assertions";

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
            ass(UnknownAssertion.class),
            ass(ResolveServiceAssertion.class));

        GatewayFeatureSet admin =
        fsr("set:admin", "All admin APIs, over all admin API transports",
            "Everything that used to be enabled by the catchall Feature.ADMIN",
            misc(SERVICE_ADMIN, "All admin APIs, over all admin API transports", null),
            misc(SERVICE_REMOTE_MANAGEMENT, "Gateway Remote Management", null));

        GatewayFeatureSet branching =
        fsr("set:policy:branching", "Support for branching policies",
            ass(AllAssertion.class),
            ass(ExactlyOneAssertion.class),
            ass(OneOrMoreAssertion.class),
            ass(ForEachLoopAssertion.class),
            ass(HandleErrorsAssertion.class),
            ass(RaiseErrorAssertion.class)
        );

        GatewayFeatureSet wssc =
        fsr("set:wssc", "WS-SecureConversation support",
            "Requires enabling the STS",
            srv(SERVICE_STS, "Security token service"),
            ass(SecureConversation.class));

        GatewayFeatureSet httpFront =
        fsr("set:http:front", "Allow incoming HTTP messages",
            srv(SERVICE_HTTP_MESSAGE_INPUT, "Accept incoming messages over HTTP"),
            ass(AddHeaderAssertion.class),
            mass("assertion:ManageCookie"),
            mass("assertion:ReplaceTagContent"));

        GatewayFeatureSet httpBack =
        fsr("set:http:back", "Allow outgoing HTTP messages",
            ass(HttpRoutingAssertion.class),
            ass(AddHeaderAssertion.class),
            mass("assertion:ManageCookie"),
            mass("assertion:ReplaceTagContent"));

        GatewayFeatureSet ftpFront =
        fsr("set:ftp:front", "Allow incoming FTP messages",
            srv(SERVICE_FTP_MESSAGE_INPUT, "Accept incoming messages over FTP"),
            mass("assertion:FtpCredential"));

        GatewayFeatureSet sshFront =
        fsr("set:SSH2:front", "Allow incoming SSH2 messages",
            srv(SERVICE_SSH_MESSAGE_INPUT, "Accept incoming messages over SSH2"),
            mass("assertion:SshCredential"));

        GatewayFeatureSet mqttFront =
        fsr("set:MQTT:front", "Allow incoming MQTT messages",
            srv(SERVICE_MQTT_MESSAGE_INPUT, "Accept incoming messages over MQTT"),
            mass("assertion:MQTTCredential"));

        GatewayFeatureSet srvRawTcp = misc(SERVICE_L7RAWTCP_MESSAGE_INPUT, "Accept incoming messages over l7.raw.tcp", null);
        GatewayFeatureSet rawTcpFront =
        fsr("set:l7.raw.tcp:front", "Allow incoming l7.raw.tcp requests",
            srvRawTcp);

        GatewayFeatureSet rawTcpBack =
        fsr("set:l7.raw.tcp:back", "Allow outgoing l7.raw.tcp requests",
            mass("assertion:SimpleRawTransport"));

        GatewayFeatureSet srvJms = misc(SERVICE_JMS_MESSAGE_INPUT, "Accept incoming messages over JMS", null);
        GatewayFeatureSet jmsFront =
        fsr("set:jms:front", "Allow incoming JMS messages",
            srvJms);

        GatewayFeatureSet jmsBack =
        fsr("set:jms:back", "Allow outgoing JMS messages",
            "Current requires allowing the JMS front end as well",
            srvJms,
            ass(JmsRoutingAssertion.class));

        GatewayFeatureSet srvMqNative = misc(SERVICE_MQNATIVE_MESSAGE_INPUT, "Accept incoming messages over MQ Native", null);
        GatewayFeatureSet mqNativeFront =
        fsr("set:mqNative:front", "Allow incoming MQ Native messages",
            srvMqNative);

        GatewayFeatureSet mqNativeBack =
        fsr("set:mqNative:back", "Allow outgoing MQ Native messages",
                mass("assertion:MqNativeRouting"));

        GatewayFeatureSet srvEmail = misc(SERVICE_EMAIL_MESSAGE_INPUT, "Accept incoming messages via email", null);
        GatewayFeatureSet emailFront =
        fsr("set:email:front", "Allow incoming email messages",
            srvEmail);

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

        GatewayFeatureSet securePassword =
        fsr("set:securePassword", "Ability to configure Secure Passwords",
                srv(SERVICE_SECURE_PASSWORD, "Ability to configure Secure Passwords"));

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

        GatewayFeatureSet encass =
        fsr("set:encass", "Enable Encapsulated Assertion support",
            "Enables Gateway to create and use encapsulated assertions.",
            ass(EncapsulatedAssertion.class),
            ui(UI_MANAGE_ENCAPSULATED_ASSERTIONS, "Ability to use Manage Encapsulated Assertions GUI"),
            srv(SERVICE_ENCAPSULATED_ASSERTION, "Ability to use encapsulated assertions"));

        GatewayFeatureSet polback =
        fsr("set:polback", "Enable Policy-Backed Services support",
            "Enables Gateway to create and use policy-backed services such as key-value stores.",
            ui(UI_MANAGE_POLICY_BACKED_SERVICES, "Ability to use Manage Policy-Backed Services GUI"));

        GatewayFeatureSet seczones =
        fsr("set:seczones", "Enable Security Zones support",
            "Enables Gateway to create and use RBAC security zones.",
            ui(UI_MANAGE_SECURITY_ZONES, "Ability to use Manage Security Zones GUI"));

        GatewayFeatureSet experimental =
        fsr("set:experimental", "Enable experimental features",
            "Enables features that are only present during development, and that will be moved or renamed before shipping.",
            srv(SERVICE_BRIDGE, "Experimental SSB service (standalone, non-BRA, present-but-disabled)"),
            mass("assertion:Script"));  // ScriptAssertion is for experimental/tactical use only

        GatewayFeatureSet wssp =
        fsr("set:wssp", "WS-SecurityPolicy assertion",
            ass(WsspAssertion.class));

        GatewayFeatureSet nonSoapXmlSigning =
        fsr("set:nonSoapXmlSigning", "Non-SOAP-specific XML signatures and verification",
                mass("assertion:NonSoapSignElement"),
                mass("assertion:NonSoapVerifyElement"),
                mass("assertion:NonSoapCheckVerifyResults"),
                mass("assertion:IndexLookupByItem"),
                mass("assertion:ItemLookupByIndex"),
                mass("assertion:SelectElement"),
                mass("assertion:VariableCredentialSource"));

        GatewayFeatureSet nonSoapXmlEncryption =
        fsr("set:nonSoapXmlEncryption", "Non-SOAP-specific XML encryption and decryption",
                mass("assertion:NonSoapEncryptElement"),
                mass("assertion:NonSoapDecryptElement"),
                mass("assertion:IndexLookupByItem"),
                mass("assertion:ItemLookupByIndex"),
                mass("assertion:SelectElement"));

        GatewayFeatureSet uiPublishServiceWizard = ui(UI_PUBLISH_SERVICE_WIZARD, "Enable the SSM Publish SOAP Service Wizard");
        GatewayFeatureSet uiPublishXmlWizard = ui(UI_PUBLISH_XML_WIZARD, "Enable the SSM Publish XML Service Wizard");
        GatewayFeatureSet uiPublishInternalWizard = ui(UI_PUBLISH_INTERNAL_WIZARD, "Enable the SSM Publish Internal Service Wizard");
        GatewayFeatureSet uiPublishWsdlQueryHandlerWizard = ui(UI_PUBLISH_WSDL_QUERY_HANDLER_WIZARD, "Enable the SSM Publish Wsdl Query Handler Service Wizard");
        GatewayFeatureSet uiWsdlCreateWizard = ui(UI_WSDL_CREATE_WIZARD, "Enable the SSM WSDL Create Wizard");
        GatewayFeatureSet uiAuditWindow = ui(UI_AUDIT_WINDOW, "Enable the SSM Audit Window");
        GatewayFeatureSet uiRbacRoleEditor = ui(UI_RBAC_ROLE_EDITOR, "Enable the SSM RBAC Role Editor");
        GatewayFeatureSet uiDashboardWindow = ui(UI_DASHBOARD_WINDOW, "Enable the SSM Dashboard Window");
        GatewayFeatureSet uiLogSinksDialog = ui(UI_MANAGE_LOG_SINKS, "Enable the SSM Log Sinks Dialog");
        GatewayFeatureSet uiAuditSinkDialog = ui(UI_MANAGE_AUDIT_SINK, "Enable the SSM Audit Sink Dialog");
        GatewayFeatureSet uiEmailListenersDialog = ui(UI_MANAGE_EMAIL_LISTENERS, "Enable the SSM Email Listeners Dialog");

        GatewayFeatureSet cassandraConnections = fsr("set:Cassandra",
                "Enable Cassandra support",
                "Enables Gateway to create and use Cassandra connections.",
                ui(UI_MANAGE_CASSANDRA_CONNECTIONS, "Enable the Manage Cassandra Connections Dialog"),
                srv(SERVICE_CASSANDRA, "Ability to manage Cassandra connections"));

        GatewayFeatureSet flagPermaFips = flag(FLAG_PERMAFIPS, "FIPS mode is always required.");

        GatewayFeatureSet identityAttributesAssertion = fsr("set:IdentityAttributes:Assertions",
                "The necessary assertions to enable Identity Attributes extraction functionality",
                mass("assertion:IdentityAttributes"));

        GatewayFeatureSet ldapWriteAssertion = fsr("set:LdapWrite:Assertions",
                "The necessary assertions to write to the LDAP Provider",
                mass("assertion:LdapWrite"));

        //
        // Declare "building block" feature sets
        // (feature sets built out of "twig" feature sets, and which may include other building block feature sets,
        //  but which on their own may still be useless or of little value as a product.)
        // Naming convention:  set:CamelCaseCategory:LevelName
        //

        GatewayFeatureSet adminAndEms =
        fsr("set:adminAndEms", "All admin APIs, including enterprise management.",
            "Everything that used to be enabled by the catchall Feature.ADMIN plus enterprise management",
            admin,
            ems,
            mass("assertion:GatewayManagement"),
            mass("assertion:RESTGatewayManagement"));

        GatewayFeatureSet uiAccel =
        fsr("set:UI:Accel", "SecureSpan Accelerator UI features",
            uiPublishXmlWizard,
            uiPublishServiceWizard,
            uiAuditWindow,
            uiDashboardWindow,
            uiWsdlCreateWizard,
            uiLogSinksDialog,
            uiAuditSinkDialog);

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
            fs(nonSoapXmlSigning),
            ass(SpecificUser.class),
            ass(MemberOfGroup.class),
            ass(AuthenticationAssertion.class),
            ass(HttpBasic.class),
            ass(HttpDigest.class),
            ass(SslAssertion.class), // TODO omit client cert support from this grant (when it is possible to do so)
            ass(XpathCredentialSource.class),
            ass(RequireWssX509Cert.class),
            ass(WssBasic.class),
            ass(WssDigest.class),
            ass(RequireWssSaml.class),
            ass(RequireWssSaml2.class),
            ass(EncryptedUsernameTokenAssertion.class),
            ass(WsTrustCredentialExchange.class),
            ass(SamlBrowserArtifact.class),
            ass(PreemptiveCompression.class),
            ass(RemoteDomainIdentityInjection.class),
            mass("assertion:SamlIssuer"),
            mass("assertion:LDAPQuery"),
            fs(ldapWriteAssertion),
            fs(identityAttributesAssertion),
            mass("assertion:CertificateAttributes"),
            mass("assertion:ValidateNonSoapSamlToken"));

        GatewayFeatureSet accessGateway =
        fsr("set:AccessControl:Gateway", "SecureSpan Gateway access control",
            "Adds SSL client certs (although currently this comes with our SSL support and can't be disabled)",
            fs(accessFw),
            ass(SslAssertion.class)); // TODO enable client cert here exclusively (when it is possible to do so)

        // XML Security
        GatewayFeatureSet xmlsecAccel =
        fsr("set:XmlSec:Accel", "SecureSpan Accelerator XML security",
            "Element signature and encryption using WSS",
            ass(AddWssTimestamp.class));

        GatewayFeatureSet xmlsecFw =
        fsr("set:XmlSec:Firewall", "SecureSpan Firewall XML security",
            "Adds timestamp and token manipulation",
            fs(xmlsecAccel),
            fs(nonSoapXmlSigning),
            fs(nonSoapXmlEncryption),
            ass(RequireWssSignedElement.class),
            ass(RequireWssEncryptedElement.class),
            ass(WssSignElement.class),
            ass(WssEncryptElement.class),
            ass(WssReplayProtection.class),
            ass(RequireWssTimestamp.class),
            ass(AddWssSecurityToken.class),
            ass(WsSecurity.class),
            ass(CreateSecurityContextToken.class),
            ass(CancelSecurityContext.class),
            ass(LookupOutboundSecureConversationSession.class),
            ass(BuildRstSoapRequest.class),
            ass(BuildRstrSoapResponse.class),
            ass(ProcessRstrSoapResponse.class),
            ass(EstablishOutboundSecureConversation.class),
            ass(WssVersionAssertion.class),
            ass(WssConfigurationAssertion.class),
            ass(AddWssUsernameToken.class),
            ass(LookupTrustedCertificateAssertion.class),
            feat(FEATURE_SIGNED_ATTACHMENTS, "Signed SOAP attachments."));

        // Message Validation/Transform
        GatewayFeatureSet validationAccel =
        fsr("set:Validation:Accel", "SecureSpan Accelerator message validation and transformation",
            "XPath, Schema, XSLT, and SOAP operation detector",
            ass(RequestXpathAssertion.class),
            ass(ResponseXpathAssertion.class),
            ass(SchemaValidation.class),
            ass(XslTransformation.class),
            ass(RemoveElement.class),
            mass("assertion:EncodeDecode"));

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
            mass("assertion:WsAddressing"),
            mass("assertion:AddWsAddressing"));

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
            fs(emailFront),
            fs(trustStore),
            fs(keyStore),
            fs(securePassword));

        GatewayFeatureSet routingAccel =
        fsr("set:Routing:Accel", "SecureSpan Accelerator message routing",
            "HTTP and hardcoded responses.",
            fs(httpFront),
            fs(httpBack),
            fs(keyStore),
            fs(securePassword),
            mass("assertion:HardcodedResponse"),
            mass("assertion:EchoRouting"));

        GatewayFeatureSet routingFw =
        fsr("set:Routing:Firewall", "SecureSpan Firewall message routing",
            "Adds abiltity to configure Trusted Certificates",
            fs(routingAccel),
            fs(trustStore),
            fs(securePassword),
            fs(keyStore));

        GatewayFeatureSet routingGateway =
        fsr("set:Routing:Gateway", "SecureSpan Gateway message routing",
            "Adds BRA, JMS, FTP, MQ Native and SSH routing.",
            fs(routingFw),
            fs(ftpFront),
            fs(sshFront),
            fs(jmsFront),
            fs(jmsBack),
            fs(emailFront),
            fs(mqNativeFront),
            fs(mqNativeBack),
            fs(rawTcpFront),
            fs(rawTcpBack),
            fs(uiEmailListenersDialog),
            mass("assertion:FtpRouting"),
            mass("assertion:SshRoute"));

        GatewayFeatureSet mqtt =
                fsr("set:Mbaas:Mqtt", "CA API Gateway MQTT listen port and assertions.",
                        "Adds MQTT features.",
                        fs(mqttFront),
                        mass("assertion:MQTTConnection"),
                        mass("assertion:MQTTPublish"),
                        mass("assertion:MQTTSubscribe"));

        // Service availability
        GatewayFeatureSet availabilityAccel =
        fsr("set:Availability:Accel", "SecureSpan Accelerator service availability",
            "Time/Day and IP range",
            ass(TimeRange.class),
            ass(RemoteIpRange.class),
            mass("assertion:CacheLookup"),
            mass("assertion:CacheStorage"),
            mass("assertion:RateLimit"),
            mass("assertion:RateLimitQuery"));

        GatewayFeatureSet availabilityFw =
        fsr("set:Availability:Firewall", "SecureSpan Firewall service availability",
            "Adds throughput qutoa",
            fs(availabilityAccel),
            mass("assertion:ThroughputQuota"),
            mass("assertion:ThroughputQuotaQuery"));

        // Logging/auditing and alerts
        GatewayFeatureSet auditAccel =
        fsr("set:Audit:Accel", "SecureSpan Accelerator logging/auditing and alerts",
            "Auditing, email and SNMP traps",
            fs(snmp),
            ass(AuditAssertion.class),
            ass(AuditDetailAssertion.class),
            mass("assertion:Email"),
            ass(AuditRecordToXmlAssertion.class),
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
            ass(ExportVariablesAssertion.class),
            mass("assertion:EchoRouting"),
            mass("assertion:HardcodedResponse"),
            mass("assertion:UUIDGenerator"),
            mass("assertion:ManipulateMultiValuedVariable"),
            ass(MapValueAssertion.class));

        GatewayFeatureSet threatIps =
        fsr("set:Threats:IPS", "SecureSpan XML IPS threat protection",
            "Stealth fault, size limits, document structure threats, schema validation, and XTM (when it's done)",
            ass(FaultLevel.class),
            ass(CustomizeErrorResponseAssertion.class),
            ass(OversizedTextAssertion.class),
            ass(RequestSizeLimit.class),
            ass(MessageBufferingAssertion.class),
            mass("assertion:BufferData"),
            ass(ContentTypeAssertion.class),
            ass(SqlAttackAssertion.class),
            ass(SchemaValidation.class)); // TODO Assertion for XTM Signature goes here, as soon as it exists

        GatewayFeatureSet threatAccel =
        fsr("set:Threats:Accel", "SecureSpan Accelerator threat protection",
            "Just document structure threats and schema validation",
            ass(SchemaValidation.class),
            ass(FaultLevel.class),
            ass(CustomizeErrorResponseAssertion.class));

        GatewayFeatureSet threatFw =
        fsr("set:Threats:Firewall", "SecureSpan Firewall threat protection",
            "Both of the above, plus adds regex, SQL attacks, and cluster-wide replay protection",
            fs(threatIps),
            fs(threatAccel),
            ass(Regex.class),
            ass(SqlAttackAssertion.class),
            ass(WssReplayProtection.class));

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

        /**
         * Outbound in the sense that these assertions allow for the Gateway to initiate a SAMLP exchange and to
         * process a response.
         */
        GatewayFeatureSet samlpAssertions =
        fsr("set:SAMLP:Assertions", "The necessary assertions to enable outbound SAMLP functionality",
            mass("assertion:SamlpRequestBuilder"),
            mass("assertion:SamlpResponseEvaluation"));

        /**
         * Inbound in the sense that these assertions allow for an incoming request to be processed and a samlp
         * response to be sent in reply.
         */
        GatewayFeatureSet samlpInboundAssertions =
        fsr("set:SAMLP_Inbound:Assertions", "The necessary assertions to enable inbound SAMLP functionality",
            mass("assertion:ProcessSamlAttributeQueryRequest"),
            mass("assertion:SetSamlStatus"),
            mass("assertion:ProcessSamlAuthnRequest"),
            mass("assertion:SamlpResponseBuilder"));

        /**
         * Set of SAMLP assertions needed to support SAMLP Web SSO
         */
        GatewayFeatureSet samlpSsoAssertions =
        fsr("set:SAMLPSSO:Assertions", "The necessary assertions to enable SAMLP SSO functionality",
            mass("assertion:SetSamlStatus"),
            mass("assertion:ProcessSamlAuthnRequest"),
            mass("assertion:SamlpResponseBuilder"));

        GatewayFeatureSet xacmlAssertions =
        fsr("set:XACML:Assertions", "The necessary assertions to enable XACML functionality",
            mass("assertion:XacmlRequestBuilder"),
            mass("assertion:XacmlPdp"));

        GatewayFeatureSet jdbcQueryAssertions =
        fsr("set:JdbcQuery:Assertions", "The necessary assertions to enable JDBC Query functionality",
            mass("assertion:JdbcQuery"),
            mass("assertion:composite.Transaction"));

        GatewayFeatureSet bulkJdbcInsertAssertions =
        fsr("set:BulkJdbcInsert:Assertions", "The necessary assertions to enable Bulk JDBC insert functionality",
             mass("assertion:BulkJdbcInsert"));

        GatewayFeatureSet icapAntivirusScannerAssertions =
        fsr("set:IcapAntivirusScanner:Assertions", "The necessary assertions to enable ICAP Antivirus scanning functionality",
            mass("assertion:IcapAntivirusScanner"));

        GatewayFeatureSet uddiNotificationAssertions =
        fsr("set:UDDINotification:Assertions", "The necessary assertions to enable UDDI Notification functionality",
            mass("assertion:UDDINotification"));

        GatewayFeatureSet mtomDecodeAssertions =
        fsr("set:MtomDecode:Assertions", "The necessary assertions to enable MTOM Decode functionality",
            mass("assertion:MtomDecode"));

        GatewayFeatureSet mtomEncodeAssertions =
        fsr("set:MtomEncode:Assertions", "The necessary assertions to enable MTOM Encode functionality",
            mass("assertion:MtomEncode"));

        GatewayFeatureSet mtomValidateAssertions =
        fsr("set:MtomValidate:Assertions", "The necessary assertions to enable MTOM Validate functionality",
            mass("assertion:MtomValidate"));

        GatewayFeatureSet api3scaleAssertions =
        fsr("set:api3scaleAssertions:Assertions", "The necessary assertions to enable MTOM Validate functionality",
            mass("assertion:Api3ScaleAuthorize"),
            mass("assertion:Api3ScaleReport"));

        // Saml2AttributeQueryAssertion.aar
        GatewayFeatureSet saml2AttributeQueryAssertions =
        fsr("set:Saml2AttributeQuery:Assertions", "BAE Saml2AttributeQueryAssertion.aar",
            mass("assertion:DecryptElement"),
            mass("assertion:EncryptNameID"),
            mass("assertion:EncryptSamlAssertion", true),
            mass("assertion:Saml2AttributeQuery"),
            mass("assertion:SignResponseElement"),
            mass("assertion:ValidateSignature"));

        GatewayFeatureSet jsonTransformationAssertion =
        fsr("set:JsonTransformation:Assertions", "The necessary assertions to enable JSON transformation functionality",
            mass("assertion:JsonTransformation"));

        GatewayFeatureSet generateSecurityHashAssertion =
                fsr("set:GenerateSecurityHashAssertion:Assertions", "The necessary assertions to enable the Generate Security Hash functionality",
                        mass("assertion:GenerateSecurityHash"));

        GatewayFeatureSet evaluateJsonPathExpression =
                fsr("set:EvaluateJsonPathExpressionAssertion:Assertions", "The necessary assertions to enable the Evaluate Json Path Expression functionality",
                        mass("assertion:EvaluateJsonPathExpression"));

        GatewayFeatureSet evaluateJsonPathExpressionV2 =
                fsr("set:EvaluateJsonPathExpressionV2Assertion:Assertions", "The necessary assertions to enable the Evaluate Json Path Expression V2 functionality",
                        mass("assertion:EvaluateJsonPathExpressionV2"));

        GatewayFeatureSet lookupDynamicContextVariables = fsr("set:LookupDynamicContextVariablesAssertion:Assertions",
                "The necessary assertions to enable the Look Up Dynamic Context Variables functionality",
                mass("assertion:LookupDynamicContextVariables"));

        GatewayFeatureSet generateOAuthSignatureBaseString = fsr("set:GenerateOAuthSignatureBaseStringAssertion:Assertions",
                "The necessary assertions to enable generate of an OAuth 1.0 A Signature Base String",
                mass("assertion:GenerateOAuthSignatureBaseString"));

        GatewayFeatureSet ntlmAuthenticationAssertion =
                fsr("set:NtlmAuthenticationAssertion:Assertions",
                     "The necessary assertions to enable NTLM inbound authentication functionality",
                     mass("assertion:NtlmAuthentication"));

        GatewayFeatureSet kerberosAuthenticationAssertion =
                fsr("set:KerberosAuthenticationAssertion:Assertions",
                 "The necessary assertions to enable Kerberos authentication and constrained delegation functionality",
                 mass("assertion:KerberosAuthentication"));

        GatewayFeatureSet qstAssertion =
                fsr("set:QuickStartTemplateAssertion:Assertions",
                        "The necessary assertions to enable Quick Start Template functionality",
                        mass("assertion:QuickStartTemplate"),
                        mass("assertion:QuickStartDocumentation"));

        /**
         * This assertion requires the policy bundle installer assertion so it cannot be added to a license without
         * the policy bundle installer module also being added.
         */
        GatewayFeatureSet oAuthInstaller = fsr("set:OAuthInstallerAssertion:Assertions",
                "The necessary assertions to install the OAuth Toolkit",
                mass("assertion:OAuthInstaller"));

        GatewayFeatureSet oAuthValidationAssertions = fsr("set:OAuthAssertions:Assertions",
                "The necessary assertions to perform OAuth validation",
                mass("assertion:OAuthValidation"));

        GatewayFeatureSet policyBundleInstaller = fsr("set:PolicyBundleInstallerAssertion:Assertions",
                "The necessary assertions to install policy bundles",
                mass("assertion:PolicyBundleInstaller"));

        GatewayFeatureSet splitJoinAssertions =
        fsr("set:SplitJoin:Assertions", "The necessary assertions to enable split and join variable functionality",
            mass("assertion:Split"),
            mass("assertion:Join"));

        GatewayFeatureSet adaptiveLoadBalancingAssertions = fsr("set:AdaptiveLoadBalancing:Assertions",
                "The necessary assertions to enable Adaptive Load Balancing functionality",
                mass("assertion:CreateRoutingStrategy"),
                mass("assertion:ProcessRoutingStrategyResult"),
                mass("assertion:ExecuteRoutingStrategy"));

        GatewayFeatureSet caWsdmAssertions = fsr("set:CaWsdm:Assertions",
                "The necessary assertions to enable CA WSDM Observer functionality",
                mass("assertion:CaWsdm"));

        GatewayFeatureSet concurrentAllAssertion = fsr("set:ConcurrentAll:Assertions",
                "The necessary assertions to enable Concurrent All Assertion functionality",
                mass("assertion:ConcurrentAll"));

        GatewayFeatureSet genericIdentityManagementServiceAssertion = fsr("set:GenericIdentityManagementService:Assertions",
                "The necessary assertions to enable Generic Identity Management Service functionality",
                mass("assertion:GenericIdentityManagementService"));

        GatewayFeatureSet jsonSchemaAssertion = fsr("set:JSONSchema:Assertions",
                "The necessary assertions to enable JSON Schema validation functionality",
                mass("assertion:JSONSchema"));

        GatewayFeatureSet jsonDocumentStructureAssertion = fsr("set:JsonDocumentStructure:Assertions",
                "The necessary assertions to protect against JSON document structure threats functionality",
                mass("assertion:JsonDocumentStructure"));

        GatewayFeatureSet oDataValidationAssertion = fsr("set:OdataValidation:Assertions",
                "The necessary assertions to validate and protect OData messages",
                mass("assertion:OdataValidation"));

        GatewayFeatureSet swaggerAssertion = fsr("set:Swagger:Assertions",
                "The necessary assertions to validate messages against Swagger document",
                mass("assertion:Swagger"));

        GatewayFeatureSet circuitBreakerAssertion = fsr("set:CircuitBreaker:Assertions",
                "The necessary assertions to enable Circuit Breaker pattern functionality",
                mass("assertion:CircuitBreaker"));

        GatewayFeatureSet corsAssertion = fsr("set:CORS:Assertions",
                "The necessary assertions to enable CORS functionality",
                mass("assertion:CORS"));

        GatewayFeatureSet apiPortalIntegration = fsr("set:ApiPortalIntegration:Assertions",
                "The necessary assertions to enable API Portal integration features",
                mass("assertion:UpgradePortal"),
                mass("assertion:ApiPortalIntegration"),
                mass("assertion:ManagePortalResource"),
                mass("assertion:LookupApiKey"),
                mass("assertion:ManageApiKey"));

        GatewayFeatureSet siteMinderAssertions = fsr("set:SiteMinder:Assertions",
                "The necessary assertions to enable CA Single Sign-On integration",
                mass("assertion:SiteMinderCheckProtected"),
                mass("assertion:SiteMinderAuthenticate"),
                mass("assertion:SiteMinderAuthorize"));

        GatewayFeatureSet csrfProtectionAssertion = fsr("set:CsrfProtection:Assertions",
                "Cross Site Request Forgery Protection Assertion",
                mass("assertion:CsrfProtection"));

        GatewayFeatureSet ncesAssertions = fsr("set:NCES:Assertions",
                "NCES decoration and validation assertions",
                mass("assertion:NcesDecorator"),
                mass("assertion:NcesValidator"));

        GatewayFeatureSet sophosAssertions = fsr("set:Sophos:Assertions",
                "Sophos AntiMalware Assertion",
                mass("assertion:Sophos"));

        GatewayFeatureSet radiusAssertions =
                fsr("set:Radius:Assertions",
                        "The necessary assertions to enable Radius authentication functionality",
                        mass("assertion:Radius"),
                        mass("assertion:RadiusAuthenticate"));

        GatewayFeatureSet csrSignerAssertions =
                fsr(FS_CSRSIGNER,
                        "Assertions to enable CSR Signing functionality",
                        mass("assertion:CsrSigner"));

        GatewayFeatureSet jsonWebTokenAssertions =
                fsr("set:JsonWebToken:Assertions",
                        "Assertions to enable JSON Web Token functionality.",
                        mass("assertion:JwtDecode"),
                        mass("assertion:JwtEncode"));

        GatewayFeatureSet jwtAssertion =
                fsr("set:Jwt:Assertions",
                        "Assertions to enable JWT functionality.",
                        mass("assertion:EncodeJsonWebToken"),
                        mass("assertion:DecodeJsonWebToken"),
                        mass("assertion:CreateJsonWebKey")
                );

        GatewayFeatureSet openIDConnectAssertions =
                fsr("set:OpenIDConnect:Assertions",
                        "Assertions to enable OpenIDConnect functionality, including policy dependencies",
                        mass("assertion:IDTokenGeneration"),
                        mass("assertion:IDTokenDecode"),
                        mass("assertion:OpenIDConnectInstaller"),
                        csrSignerAssertions,
                        jsonWebTokenAssertions);

        GatewayFeatureSet applePushNotificationAssertions =
                fsr("set:ApplePushNotification:Assertions",
                        "Assertions to enable Apple Push Notification functionality.",
                        mass("assertion:ApplePushNotification"),
                        mass("assertion:AppleFeedbackService"));

        GatewayFeatureSet webSocketAssertions =
                fsr(FS_WEBSOCKETS,
                        "Assertions to enable WebSocket functionality.",
                        mass("assertion:WebSocket"),
                        mass("assertion:WebSocketEntityManager"),
                        mass("assertion:WebSocketValidation"),
                        mass("assertion:WebSocketMessageInjection"),
                        mass("assertion:WebSocketTransport"),
                        srv(SERVICE_WEBSOCKET_MESSAGE_INPUT, "Accept incoming messages over WebSocket"));

        GatewayFeatureSet xmppAssertions =
                fsr("set:XMPP:Assertions",
                        "Assertions to enable XMPP functionality.",
                        mass("assertion:XMPPGetRemoteCertificate"),
                        mass("assertion:XMPPStartTLS"),
                        mass("assertion:XMPPCloseSession"),
                        mass("assertion:XMPPAssociateSessions"),
                        mass("assertion:XMPPGetAssociatedSessionId"),
                        mass("assertion:XMPPGetSessionAttribute"),
                        mass("assertion:XMPPSendToRemoteHost"),
                        mass("assertion:XMPPSetSessionAttribute"),
                        mass("assertion:XMPPOpenServerSession"));

        GatewayFeatureSet retrieveServiceWsdlAssertion = fsr("set:RetrieveServiceWsdl:Assertions",
                "Assertions to enable service WSDL retrieval functionality.",
                mass("assertion:RetrieveServiceWsdl"),
                uiPublishWsdlQueryHandlerWizard);

        GatewayFeatureSet cassandraAssertions = fsr("set:Cassandra:Assertions",
                "The necessary assertions to support Cassandra functionality",
                mass("assertion:CassandraQuery"));

        // US (NCES)
        GatewayFeatureSet usAssertions =
        fsr("set:US:Assertions", "US decoration and validation assertions",
            fs(ncesAssertions),
            esmAssertions,
            uiPublishInternalWizard);

        // Formerly a profile set, now present only for backward compatibility
        fsr("set:Profile:IPS", "SecureSpan XML IPS",
            "DEPRECATED -- present for license compat only",
            fs(core),
            fs(adminAndEms),
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
        GatewayFeatureSet dataScreen =
        fsp(PROFILE_DATASCREEN, "SecureSpan Data Screen",
            "HTTP/HTML/AJAX/JSON/XML gateway",
            fs(core),
            fs(adminAndEms),
            fs(routingAccel),
            fs(threatIps),
            fs(availabilityFw),
            fs(validationDs),
            fs(auditAccel),
            fs(policyAccel),
            fs(uiDs),
            fs(customDs),
            fs(encass),
            fs(polback),
            fs(seczones),
            fs(uddiNotificationAssertions),
            fs(esmAssertions),
            fs(generateSecurityHashAssertion),
            fs(evaluateJsonPathExpression),
            fs(evaluateJsonPathExpressionV2),
            fs(lookupDynamicContextVariables),
            fs(generateOAuthSignatureBaseString),
            fs(splitJoinAssertions),
            fs(jsonTransformationAssertion),
            ass(SslAssertion.class),
            srv(SERVICE_WSDLPROXY, "WSDL proxy service")); // TODO omit client cert support from this grant (when it is possible to do so)

        fsp(PROFILE_ACCELERATOR, "SecureSpan Accelerator",
            "XML acceleration features",
            fs(core),
            fs(adminAndEms),
            fs(xmlsecAccel),
            fs(validationAccel),
            fs(routingAccel),
            fs(availabilityAccel),
            fs(auditAccel),
            fs(policyAccel),
            fs(threatAccel),
            fs(uiAccel),
            fs(moduleLoader),
            fs(uddiNotificationAssertions),
            fs(esmAssertions),
            fs(evaluateJsonPathExpression),
            fs(evaluateJsonPathExpressionV2),
            fs(lookupDynamicContextVariables),
            fs(generateOAuthSignatureBaseString),
            fs(splitJoinAssertions),
            ass(SslAssertion.class),
            srv(SERVICE_WSDLPROXY, "WSDL proxy service")); // TODO omit client cert support from this grant (when it is possible to do so)

        GatewayFeatureSet profileFirewall =
        fsp(PROFILE_FIREWALL, "SecureSpan Firewall",
            "XML firewall with custom assertions.  No BRA, no JMS, no special XML VPN Client support",
            fs(core),
            fs(adminAndEms),
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
            fs(jdbcQueryAssertions),
            fs(uddiNotificationAssertions),
            fs(mtomDecodeAssertions),
            fs(mtomEncodeAssertions),
            fs(mtomValidateAssertions),
            fs(customFw),
            fs(encass),
            fs(polback),
            fs(seczones),
            fs(esmAssertions),
            fs(samlpAssertions),
            fs(samlpSsoAssertions),
            fs(samlpInboundAssertions),
            fs(generateSecurityHashAssertion),
            fs(evaluateJsonPathExpression),
            fs(evaluateJsonPathExpressionV2),
            fs(lookupDynamicContextVariables),
            fs(generateOAuthSignatureBaseString),
            fs(ntlmAuthenticationAssertion),
            fs(kerberosAuthenticationAssertion),
            fs(oAuthInstaller),
            fs(oAuthValidationAssertions),
            fs(jsonSchemaAssertion),
            fs(jsonDocumentStructureAssertion),
            fs(cassandraConnections),
            fs(cassandraAssertions),
            fs(oDataValidationAssertion),
            fs(swaggerAssertion),
            fs(circuitBreakerAssertion),
            fs(concurrentAllAssertion),
            fs(caWsdmAssertions),
            fs(adaptiveLoadBalancingAssertions),
            fs(genericIdentityManagementServiceAssertion),
            fs(icapAntivirusScannerAssertions),
            fs(policyBundleInstaller),
            fs(splitJoinAssertions),
            fs(jsonTransformationAssertion),
            fs(siteMinderAssertions),
            fs(sophosAssertions),
            fs(corsAssertion),
            fs(bulkJdbcInsertAssertions),
            fs(modularAssertions),
            fs(csrfProtectionAssertion),
            fs(retrieveServiceWsdlAssertion),
            fs(jwtAssertion),
            mass("assertion:ValidateCertificate"));
            mass("assertion:PortalBootstrap");

        fsp(PROFILE_CLOUD_CONNECT, "CloudSpan CloudConnect",
            "Same features as XML Firewall for now.",
            fs(profileFirewall));

        fsp(PROFILE_CLOUD_PROTECT, "CloudSpan CloudProtect",
            "Same features as XML Firewall for now.",
            fs(profileFirewall));

        GatewayFeatureSet profileGateway =
        fsp(PROFILE_GATEWAY, "SecureSpan Gateway",
            "All features enabled.",
            misc("bundle:Bridge", "Bundled SecureSpan XML VPN Client", "No effect on Gateway license code -- only purpose is to distinguish two otherwise-identical feature sets"),
            fs(core),
            fs(adminAndEms),
            fs(accessGateway),
            fs(xmlsecFw),
            fs(validationGateway),
            fs(routingGateway),
            fs(availabilityFw),
            fs(auditAccel),
            fs(policyAccel),
            fs(threatFw),
            fs(customFw),
            fs(encass),
            fs(polback),
            fs(seczones),
            fs(ssb),
            fs(modularAssertions),
            fs(samlpAssertions),
            fs(samlpSsoAssertions),
            fs(samlpInboundAssertions),
            fs(xacmlAssertions),
            fs(jdbcQueryAssertions),
            fs(bulkJdbcInsertAssertions),
            fs(uddiNotificationAssertions),
            fs(mtomDecodeAssertions),
            fs(mtomEncodeAssertions),
            fs(mtomValidateAssertions),
            fs(uiFw),
            fs(api3scaleAssertions),
            fs(esmAssertions),
            fs(wssp),
            fs(icapAntivirusScannerAssertions),
            fs(jsonTransformationAssertion),
            fs(generateSecurityHashAssertion),
            fs(evaluateJsonPathExpression),
            fs(evaluateJsonPathExpressionV2),
            fs(lookupDynamicContextVariables),
            fs(generateOAuthSignatureBaseString),
            fs(ntlmAuthenticationAssertion),
            fs(kerberosAuthenticationAssertion),
            fs(oAuthInstaller),
            fs(oAuthValidationAssertions),
            fs(jsonSchemaAssertion),
            fs(jsonDocumentStructureAssertion),
            fs(cassandraConnections),
            fs(cassandraAssertions),
            fs(oDataValidationAssertion),
            fs(swaggerAssertion),
            fs(circuitBreakerAssertion),
            fs(concurrentAllAssertion),
            fs(caWsdmAssertions),
            fs(adaptiveLoadBalancingAssertions),
            fs(genericIdentityManagementServiceAssertion),
            fs(apiPortalIntegration),
            fs(policyBundleInstaller),
            fs(splitJoinAssertions),
            fs(siteMinderAssertions),
            fs(sophosAssertions),
            fs(csrfProtectionAssertion),
            fs(radiusAssertions),
            fs(retrieveServiceWsdlAssertion),
            fs(jwtAssertion),
            fs(corsAssertion),
            mass("assertion:ValidateCertificate"));
            mass("assertion:PortalBootstrap");

        fsp(PROFILE_CLOUD_CONTROL, "CloudSpan CloudControl",
            "Same features as Gateway for now.",
            fs(profileGateway));

        fsp(PROFILE_POLICY_INTEGRATION_POINT, "SecureSpan Policy Integration Point",
            "Same as SecureSpan Gateway.",
            fs(profileGateway));

        GatewayFeatureSet profileUs =
        fsp(SET_PROFILE_GATEWAY_US, "SecureSpan Gateway US",
            "Adds US features.",
            fs(profileGateway),
            fs(saml2AttributeQueryAssertions),
            fs(usAssertions));

        GatewayFeatureSet profileGatewayEnterprise =
        fsp(PROFILE_GATEWAY_ENTERPRISE, "Enterprise API Gateway",
                "Gateway feature set including non mobile specific MAG assertions.",
                fs(profileGateway),
                xmppAssertions,
                webSocketAssertions,
                csrSignerAssertions);

        GatewayFeatureSet profileFederal =
        fsp(PROFILE_FEDERAL, "SecureSpan Federal",
            "Exactly the same features as SecureSpan Gateway, but XML VPN Client software is not bundled.",
            fs(core),
            fs(adminAndEms),
            fs(accessGateway),
            fs(xmlsecFw),
            fs(validationGateway),
            fs(routingGateway),
            fs(availabilityFw),
            fs(auditAccel),
            fs(policyAccel),
            fs(threatFw),
            fs(customFw),
            fs(encass),
            fs(polback),
            fs(seczones),
            fs(ssb),
            fs(modularAssertions),
            fs(samlpSsoAssertions),
            fs(samlpInboundAssertions),
            fs(samlpAssertions),
            fs(wssp),
            fs(jdbcQueryAssertions),
            fs(bulkJdbcInsertAssertions),
            fs(uddiNotificationAssertions),
            fs(mtomDecodeAssertions),
            fs(mtomEncodeAssertions),
            fs(mtomValidateAssertions),
            fs(esmAssertions),
            fs(uiFw),
            fs(generateSecurityHashAssertion),
            fs(evaluateJsonPathExpression),
            fs(evaluateJsonPathExpressionV2),
            fs(lookupDynamicContextVariables),
            fs(generateOAuthSignatureBaseString),
            fs(ntlmAuthenticationAssertion),
            fs(kerberosAuthenticationAssertion),
            fs(oAuthInstaller),
            fs(oAuthValidationAssertions),
            fs(jsonSchemaAssertion),
            fs(jsonDocumentStructureAssertion),
            fs(cassandraConnections),
            fs(cassandraAssertions),
            fs(oDataValidationAssertion),
            fs(swaggerAssertion),
            fs(circuitBreakerAssertion),
            fs(concurrentAllAssertion),
            fs(caWsdmAssertions),
            fs(adaptiveLoadBalancingAssertions),
            fs(genericIdentityManagementServiceAssertion),
            fs(apiPortalIntegration),
            fs(policyBundleInstaller),
            fs(splitJoinAssertions),
            fs(jsonTransformationAssertion),
            fs(siteMinderAssertions),
            fs(sophosAssertions),
            fs(csrfProtectionAssertion),
            fs(corsAssertion),
            fs(retrieveServiceWsdlAssertion),
            mass("assertion:ValidateCertificate"));

        GatewayFeatureSet profileApi =
        fsp(PROFILE_API_PROXY, "Layer 7 API Proxy",
            "Same as Data Screen with some additional features",
                fs(dataScreen),
                fs(uiRbacRoleEditor),
                fs(mtomDecodeAssertions),
                fs(mtomEncodeAssertions),
                fs(mtomValidateAssertions),
                fs(jdbcQueryAssertions),
                fs(bulkJdbcInsertAssertions),
                fs(xmlsecAccel),
                ass(RequireWssTimestamp.class),
                ass(SslAssertion.class),
                ass(SpecificUser.class),
                ass(MemberOfGroup.class),
                ass(AuthenticationAssertion.class),
                ass(HttpBasic.class),
                ass(HttpDigest.class),
                ass(XpathCredentialSource.class),
                ass(SamlBrowserArtifact.class),
                mass("assertion:LDAPQuery"),
                fs(ldapWriteAssertion),
                mass("assertion:CertificateAttributes"),
                fs(nonSoapXmlSigning),
                fs(nonSoapXmlEncryption),
                ass(RequireWssEncryptedElement.class),
                ass(RequireWssSignedElement.class),
                ass(WssSignElement.class),
                mass("assertion:ProcessSamlAuthnRequest"),
                mass("assertion:SetSamlStatus"),
                mass("assertion:ValidateCertificate"),
                fs(encass),
                fs(polback),
                fs(seczones),
                fs(modularAssertions),
                fs(oAuthInstaller),
                fs(oAuthValidationAssertions),
                fs(jsonSchemaAssertion),
                fs(jsonDocumentStructureAssertion),
                fs(cassandraConnections),
                fs(cassandraAssertions),
                fs(oDataValidationAssertion),
                fs(swaggerAssertion),
                fs(circuitBreakerAssertion),
                fs(corsAssertion),
                fs(concurrentAllAssertion),
                fs(caWsdmAssertions),
                fs(adaptiveLoadBalancingAssertions),
                fs(genericIdentityManagementServiceAssertion),
                fs(identityAttributesAssertion),
                fs(apiPortalIntegration),
                fs(icapAntivirusScannerAssertions),
                fs(policyBundleInstaller),
                ass(CookieCredentialSourceAssertion.class),
                ass(WssReplayProtection.class),
                mass("assertion:SamlIssuer"),
                mass("assertion:ValidateNonSoapSamlToken"),
                fs(trustStore),
                fs(siteMinderAssertions),
                fs(sophosAssertions),
                fs(csrfProtectionAssertion),
                fs(retrieveServiceWsdlAssertion),
                fs(jwtAssertion));
                mass("assertion:PortalBootstrap");

        GatewayFeatureSet profileGatewayEssentials =
                fsp(PROFILE_GATEWAY_ESSENTIALS, "API Gateway Essentials",
                        "Same features as Layer 7 API Proxy",
                        fs(profileApi));

        /**
         * ### FEATURE PACK DEFINITIONS BEGIN ###
         *
         * IMPORTANT:
         *
         * -Feature packs containing modular assertions *must* include the Module Loader Service feature set.
         * -All feature pack profiles should be included in the Development profile (set:Profile:Development).
         */

        /**
         * Salesforce Connector
         */
        GatewayFeatureSet salesforceFeaturePack = fsp(PROFILE_SALESFORCE_EXTENSION,
                "Salesforce Connector",
                "Includes assertions that provide access to Salesforce data",
                mass("assertion:Salesforce"),
                mass("assertion:SalesforceInstaller"),
                cass("assertion:SalesforceOperation"),
                fs(moduleLoader));

        /**
         * NCES Feature Pack
         */
        GatewayFeatureSet ncesFeaturePack = fsp(PROFILE_NCES_EXTENSION,
                "NCES Feature Pack",
                "Includes NCES and SAML specific assertions",
                fs(ncesAssertions),
                fs(saml2AttributeQueryAssertions),
                fs(moduleLoader));

        /**
         * Mobile Access Gateway
         *
         * Applicable as of MAG 2.2.
         */
        GatewayFeatureSet mobileFeaturePack = fsp(PROFILE_MOBILE_EXTENSION,
                "Mobile Access Gateway",
                "Includes series of assertions required to support existing and future Mobile Access Gateway " +
                        "functionality (requires SOA Gateway license as base)",
                mass("assertion:MAGInstaller"),
                csrSignerAssertions,
                jsonWebTokenAssertions,
                openIDConnectAssertions,
                applePushNotificationAssertions,
                webSocketAssertions,
                xmppAssertions,
                fs(moduleLoader));

        /**
         * Mobile App Services
         *
         * The Mobile App Services feature set
         */
        GatewayFeatureSet masFeaturePack = fsp(PROFILE_MAS_EXTENSION,
                "Mobile App Services",
                "Includes series of assertions required to support existing and future Mobile App Services " +
                        "functionality.",
                mqtt,
                fs(moduleLoader));

        /**
         * Microservice Gateway
         *
         * The Microservices feature set
         */
        GatewayFeatureSet microserviceFeaturePack = fsp(PROFILE_MICROSERVICES,
                "Microservice Gateway",
                "Includes series of assertions required to support the Microservices gateway functionality.",
                fs(core),
                fs(branching),
                fs(admin),
                fs(encass),
                fs(polback),
                fs(seczones),
                fs(trustStore),
                fs(securePassword),
                fs(keyStore),
                ass(AuthenticationAssertion.class),
                ass(MemberOfGroup.class),
                fs(jdbcQueryAssertions),
                fs(corsAssertion),
                ass(HttpBasic.class),
                ass(SslAssertion.class),
                fs(jwtAssertion),
                // decode id token  (custom)
                mass("assertion:EncodeDecode"),
                fs(evaluateJsonPathExpression),
                ass(Regex.class),
                ass(RequestXpathAssertion.class),
                ass(ResponseXpathAssertion.class),
                // generate id token  (custom)
                ass(AddHeaderAssertion.class),
                ass(HttpRoutingAssertion.class),
                mass("assertion:RateLimitQuery"),
                mass("assertion:RateLimit"),
                ass(RemoteIpRange.class),
                mass("assertion:CacheLookup"),
                mass("assertion:CacheStorage"),
                ass(AuditDetailAssertion.class),
                ass(AuditAssertion.class),
                ass(CustomizeErrorResponseAssertion.class),
                ass(CommentAssertion.class),
                mass("assertion:Comparison"),
                ass(TrueAssertion.class),
                ass(FalseAssertion.class),
                ass(SetVariableAssertion.class),
                ass(Include.class),
                ass(ExportVariablesAssertion.class),
                mass("assertion:HardcodedResponse"),
                mass("assertion:UUIDGenerator"),
                mass("assertion:ManipulateMultiValuedVariable"),
                ass(MapValueAssertion.class),
                fs(adaptiveLoadBalancingAssertions),
                fs(splitJoinAssertions),
                mass("assertion:IndexLookupByItem"),
                mass("assertion:ItemLookupByIndex"),
                fs(lookupDynamicContextVariables),
                fs(concurrentAllAssertion),
                ass(RequestSizeLimit.class),
                ass(CodeInjectionProtectionAssertion.class),
                fs(circuitBreakerAssertion),
                ass(XpathCredentialSource.class),
                ass(CustomAssertionHolder.class),
                mass("assertion:ValidateCertificate"),
                fs(uiPublishXmlWizard),
                fs(uiPublishServiceWizard),
                fs(uiAuditWindow),
                fs(uiDashboardWindow),
                fs(uiWsdlCreateWizard),
                fs(uiLogSinksDialog),
                fs(uiAuditSinkDialog),
                srv(SERVICE_HTTP_MESSAGE_INPUT, "Accept incoming messages over HTTP"),
                fs(generateSecurityHashAssertion),
                ass(XslTransformation.class),
                fs(swaggerAssertion),
                fs(jsonSchemaAssertion),
                ass(ContentTypeAssertion.class),
                ass(SchemaValidation.class),
                ass(MessageBufferingAssertion.class),
                fs(csrfProtectionAssertion),
                fs(jsonDocumentStructureAssertion),
                ass(SqlAttackAssertion.class),
                fs(jsonSchemaAssertion),
                ass(ContentTypeAssertion.class),
                fs(qstAssertion),
                mass("assertion:RESTGatewayManagement"),
                mass("assertion:LDAPQuery"),
                mass("assertion:JsonJolt"),
                ass(HtmlFormDataAssertion.class),
                ass(RemoveElement.class),
                fs(moduleLoader));


        /**
         * ### FEATURE PACK DEFINITIONS END ###
         */

        PROFILE_ALL = fsp(PROFILE_DEVELOPMENT, "Development Mode",
                "Everything everywhere, including extension packs and experimental features.",
                fs(profileGateway),
                fs(profileFederal),
                fs(profileGatewayEnterprise),
                fs(profileGatewayEssentials),
                fs(profileUs),
                fs(experimental),
                fs(flagPermaFips),
                fs(profileApi),
                fs(salesforceFeaturePack),
                fs(ncesFeaturePack),
                fs(mobileFeaturePack),
                fs(masFeaturePack),
                fs(microserviceFeaturePack));
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
    public static FeatureSetExpander getFeatureSetExpander() {
        return new FeatureSetExpander() {
            @Override
            public Set<String> getAllEnabledFeatures(Set<String> inputSet) {
                Set<String> ret = new HashSet<>(inputSet);

                for (String topName : inputSet) {
                    GatewayFeatureSet fs = sets.get(topName);

                    /**
                     * If the feature set name is unrecognized, add it directly - it is likely
                     * for a post-release feature, such as a Tactical modular assertion or new feature pack.
                     */
                    if (fs == null) {
                        logger.fine("Included unrecognized feature set name: " + topName);
                        ret.add(topName);
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
    private static GatewayFeatureSet fsr(String name, String desc, @Nullable String note, GatewayFeatureSet... deps) throws IllegalArgumentException {
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
        return mass(fsName, false);
    }

    /** Create (and register, if new) a feature set for the specified optional custom assertion, and return it. */
    private static GatewayFeatureSet cass(String fsName) {
        return mass(fsName);
    }

    /**
     * Create (and register, if new) a feature set for the specified optional modular assertion, and return it.
     *
     * @param fsName  feature set name.
     * @param allowAssertionSuffix  if false, a sanity check will be performed to forbid feature set names that include the "Assertion" suffix.
     *                              if true, a feature set name ending in "Assertion" will be allowed.
     * @return
     */
    private static GatewayFeatureSet mass(String fsName, boolean allowAssertionSuffix) {
        String prefix = "assertion:";
        if (!fsName.startsWith(prefix))
            throw new IllegalArgumentException("Optional modular assertion feature set name doesn't start with \"assertion:\" :" + fsName);
        String rest = fsName.substring(prefix.length());
        if (rest.length() < 1)
            throw new IllegalArgumentException("Optional modular assertion feature set local name is empty:" + fsName);
        if (rest.endsWith("Assertion") && !allowAssertionSuffix)
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
    private static GatewayFeatureSet misc(String name, String desc, @Nullable String note) {
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

    private static GatewayFeatureSet feat(String name, String desc) {
        if (!name.startsWith("feature:"))
            throw new IllegalArgumentException("Preferred feature name for feature must start with \"feature:\": " + name);
        return getOrMakeFeatureSet(name, desc);
    }

    private static GatewayFeatureSet ui(String name, String desc) {
        if (!name.startsWith("ui:"))
            throw new IllegalArgumentException("Preferred feature name for ui feature must start with \"ui:\": " + name);
        return getOrMakeFeatureSet(name, desc);
    }

    private static GatewayFeatureSet flag(String name, String desc) {
        if (!name.startsWith("flag:"))
            throw new IllegalArgumentException("Preferred feature name for flag must start with \"flag:\": " + name);
        return getOrMakeFeatureSet(name, desc);
    }
}
