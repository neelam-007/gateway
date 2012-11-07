package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.wizard.BaseConsoleStep;
import com.l7tech.util.Functions;
import com.l7tech.util.ValidationUtils;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.l7tech.server.config.beans.BaseConfigurationBean.EOL;
import static com.l7tech.server.config.systemconfig.AuthenticationConfigurationBean.AuthType.LOCAL;
import static com.l7tech.server.config.systemconfig.AuthenticationConfigurationBean.AuthType.values;

/**
 * author: megery
 */
public class SystemConfigurationWizardAuthenticationStep extends BaseConsoleStep<AuthenticationConfigurationBean, AuthenticationConfigurationCommand> {

    private static final Pattern DN_PATTERN = Pattern.compile("[\\S\\s]+");

    public SystemConfigurationWizardAuthenticationStep(SystemConfigurationWizard authWizard) {
        super(authWizard);
        configBean = new AuthenticationConfigurationBean("Authentication Configuration", "");
        configCommand = new AuthenticationConfigurationCommand(configBean);
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public void doUserInterview(boolean validated) throws WizardNavigationException {
        printText(STEP_INFO + EOL);
        try {
            doAuthMethodPrompts();
            storeInput();
        } catch (IOException ioex) {
            logger.severe("Exception caught: " + ioex.getMessage());
        }
    }

    private void doAuthMethodPrompts() throws IOException, WizardNavigationException {
        AuthenticationConfigurationBean.AuthType authType = doSelectAuthTypePrompts();
        doAuthTypeSpecificPrompts(authType);
    }

    private void doAuthTypeSpecificPrompts(AuthenticationConfigurationBean.AuthType authType) throws IOException, WizardNavigationException {
        switch (authType) {
            case RADIUS:
                doRadiusPrompts();
                break;
            case LDAP:
                doLdapPrompts(true);
                break;
            case LDAP_RADIUS:
                doRadiusPrompts();
                doLdapPrompts(false);
                break;
            case LOCAL:
                break;
        }
        configBean.setAuthType(authType);

    }

    private void doRadiusPrompts() throws IOException, WizardNavigationException {
        String srvIp = getData(
                new String[]{"Enter the address of the RADIUS server : "},
                "",
                Pattern.compile("\\S+"),
                "*** Invalid Entry: Please enter a valid RADIUS server address ***"
        );

        String sharedSecret = getData(
                new String[] {"Enter the RADIUS shared secret : "},
                "",
                (Pattern)null,
                "",
                true
        );

        String radiusTimeout = getData(
                new String[] {"Enter the RADIUS reply timeout (in seconds) : [3] "},
                "3",
                Pattern.compile("\\d+"),
                "*** Invalid Entry: Please enter a valid timeout in seconds ***"
        );

        RadiusAuthTypeSettings radiusSettings = new RadiusAuthTypeSettings();
        radiusSettings.setRadiusServer(srvIp);
        radiusSettings.setRadiusSecret(sharedSecret);
        radiusSettings.setRadiusTimeout(radiusTimeout);
        configBean.addAuthTypeView(radiusSettings);
    }

    private void doLdapPrompts(boolean isLdapOnly) throws IOException, WizardNavigationException {

        LdapAuthTypeSettings ldapView = new LdapAuthTypeSettings();

        doLdapServerPrompts(ldapView);
        doLdapBindPrompts(ldapView);
        doLdapAccessControlPrompts(ldapView, isLdapOnly);

        if (ldapView.isLdapSecure()) {
            doLdapSecurePrompts(ldapView);
        }

        doLdapPamOptionsPrompts(ldapView);

//        doCustomizeAdvancedLdapPrompts(ldapView);


        //store bean
        configBean.addAuthTypeView(ldapView);
    }

        private void doLdapPamOptionsPrompts(LdapAuthTypeSettings ldapView) throws IOException, WizardNavigationException {
        String defaultPamAttr = "uid";
        String pamLoginAttr = defaultPamAttr;
        if (getConfirmationFromUser("Do you want to customize the PAM login attribute name?","n")) {
            pamLoginAttr = getData(
                new String[]{"Specify the PAM login attribute name : [" + defaultPamAttr + "] "},
                defaultPamAttr,
                Pattern.compile("\\S+"),
                "*** Invalid attribute name, please re-enter ***"
            );
        }
        ldapView.setPamLoginAttr(pamLoginAttr);
    }

    private void doLdapSecurePrompts(LdapAuthTypeSettings ldapView) throws IOException, WizardNavigationException {
        printText(EOL + "You have chosen to use LDAPS." + EOL);
        printText("The following configuration items are needed in order to enable LDAPS" + EOL + EOL);

        printText("Determine how to locate the CA certificate for the LDAP server" + EOL);


        String caCertUrl = "";
        String caCertFile = "";
        boolean isCaCertAtUrl = getConfirmationFromUser("Specify the URL to a PEM containing the certificate?", "n");
        if (isCaCertAtUrl) {
            final Functions.UnaryVoidThrows<String,Exception> verifier = new Functions.UnaryVoidThrows<String,Exception>(){
                @Override
                public void call( final String input ) throws Exception {
                    if (!ValidationUtils.isValidUrl(input)) {
                        throw new IllegalArgumentException("Invalid URL");
                    }
                }
            };
            caCertUrl = getData(
                    new String[] {"Specify the URL where the PEM formatted CA certificate can be located : "},
                    "",
                    verifier,
                    "*** Invalid URL: please provide a valid URL ***");

        } else {
            caCertFile = getData(
                    new String[] {"Specify the File containing the PEM formatted CA certificate : "},
                    "",
                    Pattern.compile("\\S+"),
                    "*** Invalid File: please provide a valid file ***");
        }
        ldapView.setLdapCaCertFile(caCertFile);
        ldapView.setLdapCaCertURL(caCertUrl);

        LdapAuthTypeSettings.CertAction ldapTlsReqCert = LdapAuthTypeSettings.CertAction.CERT_NEVER;

        boolean isldapTlsClientAuth = false;
        String ldapTlsClientCertFile = "";
        String ldapTlsClientKeyFile = "";
        boolean ldapTlsAdvanced = false;
        String ldapTlsCiphers = "HIGH:MEDIUM:+SSLv2";
        boolean isldapTlsCheckPeer = true;

        String nssBindPolicy = "soft";
        String nssTimeLimit = "10";
        String nssBindTimeLimit = "30";
        String nssIdleTimeLimit = "600";

        boolean doTlsOptions = getConfirmationFromUser("Configure LDAPS TLS options?","n");
        if (doTlsOptions) {
            ldapTlsReqCert = maybeDoTlsReqCertPrompts(ldapTlsReqCert);
            isldapTlsClientAuth = doIsClientAuthPrompts();
            if (isldapTlsClientAuth) {
                ldapTlsClientCertFile = doTlsClientCertFilePrompt();
                ldapTlsClientKeyFile = doTlsClientKeyFilePrompt();
            }

            ldapTlsAdvanced = maybeDoTlsAdvanced(ldapTlsAdvanced);
            //never true right now
            if (ldapTlsAdvanced) {
               ldapTlsCiphers = maybeDoTlsCiphersPrompts(ldapTlsCiphers);
               isldapTlsCheckPeer = doIsTlsCheckPeerPrompts(isldapTlsCheckPeer);
            }
        }

        if (shouldDoNssOptions()) {
            //TODO: collect the NSS Options. Using defaults for now.
        }

        //set all properties - may be defaults
        ldapView.setLdapTlsReqCert(ldapTlsReqCert);
        ldapView.setIsLdapTlsClientAuth(isldapTlsClientAuth);
        ldapView.setLdapTlsClientCertFile(ldapTlsClientCertFile);
        ldapView.setLdapTlsClientKeyFile(ldapTlsClientKeyFile);
        ldapView.setIsLdapTlsAdvanced(ldapTlsAdvanced);
        ldapView.setLdapTlsCiphers(ldapTlsCiphers);
        ldapView.setIsLdapTlsCheckPeer(isldapTlsCheckPeer);

        ldapView.setNssBindPolicy(nssBindPolicy);
        ldapView.setNssTimeLimit(nssTimeLimit);
        ldapView.setNssBindTimeLimit(nssBindTimeLimit);
        ldapView.setNssIdleTimeLimit(nssIdleTimeLimit);
    }

    private boolean shouldDoNssOptions() {
        //TODO we wont be doing NSS Options here, just using the defaults;
        return false;
    }

    private boolean maybeDoTlsAdvanced(boolean ldapTlsAdvanced) {
        //TODO we won't be doing TLS Advanced settings right now
        return ldapTlsAdvanced;
    }

    private boolean doIsTlsCheckPeerPrompts(boolean isldapTlsCheckPeer) {
        return isldapTlsCheckPeer;
    }

    private String maybeDoTlsCiphersPrompts(String ldapTlsCiphers) {
        return ldapTlsCiphers;
    }

    private String doTlsClientKeyFilePrompt() throws IOException, WizardNavigationException {
        return getData(
                new String[] {"Enter the path to the client key file : "},
                "",
                (String[])null,
                "");
    }

    private String doTlsClientCertFilePrompt() throws IOException, WizardNavigationException {
        return  getData(
                new String[] {"Enter the path to the PEM formatted client certificate file : "},
                "",
                (String[])null,
                "");
    }

    private boolean doIsClientAuthPrompts() throws IOException, WizardNavigationException {
        return getConfirmationFromUser("Configure Client Authentication?","n");
    }

    private LdapAuthTypeSettings.CertAction maybeDoTlsReqCertPrompts(LdapAuthTypeSettings.CertAction ldapTlsReqCert) throws IOException, WizardNavigationException {

        if (getConfirmationFromUser("Specify client handling of server certificates?" , "n")) {

            Map<String, LdapAuthTypeSettings.CertAction> actionMap = new TreeMap<String, LdapAuthTypeSettings.CertAction>();

            int index = 1;
            for (LdapAuthTypeSettings.CertAction certAction : LdapAuthTypeSettings.CertAction.values()) {
                actionMap.put(String.valueOf(index++), certAction);
            }

            List<String> promptList = new ArrayList<String>();
            for (Map.Entry<String, LdapAuthTypeSettings.CertAction> entry : actionMap.entrySet()) {
                promptList.add(entry.getKey() + ") " + entry.getValue().getDescription() + EOL);
            }

            String defaultChoice = "1";
            promptList.add("Make a selection : " + "[" + defaultChoice + "] ");
            List<String> allowedEntries = new ArrayList<String>(actionMap.keySet());

            printText(EOL + "Choose an option for how the client will handle the server's certificate" + EOL);
            String whichChoice = getData(
                    promptList.toArray(new String[promptList.size()]),
                    defaultChoice,
                    allowedEntries.toArray(new String[allowedEntries.size()]),
                    "");

            if (actionMap.get(whichChoice) != null) {
                ldapTlsReqCert = actionMap.get(whichChoice);
            }
        }
        return ldapTlsReqCert;
    }

    private void doLdapAccessControlPrompts(LdapAuthTypeSettings ldapView, boolean isLdapOnly) throws IOException, WizardNavigationException {

        printText(EOL);
        String defaultGroup = "ssgconfig_ldap";
        String ldapGroupName = getData(
            new String[]{"What is the name of the group in LDAP which contains the users granted access to the Gateway? : " + "[" + defaultGroup + "] "},
                defaultGroup,
            DN_PATTERN,
            "*** Invalid Entry: Please enter a valid group name***"
        );
        printText(EOL);
        ldapView.setLdapGroupName(ldapGroupName);

        String ldapGid = getData(
            new String[] {"What is the group ID (gid) for the group that should be granted access to the Gateway? : "},
            "",
            Pattern.compile("\\d+"),
            "*** Invalid GID, please retry ***"
        );
        printText(EOL);
        ldapView.setPamFilter(ldapView.createPamFilterFromGids(new String[]{ldapGid}));

        if (isLdapOnly) {
            String nssBasePasswd = getData(
                new String[]{"Which object in the LDAP will be used to find the password for users : "},
                    "",
                DN_PATTERN,
                "*** Invalid Entry: Please enter a valid object name***"
            );

            // Since they may have chosen a new base Object other than the default, use it
            String nssBaseGroup = getData(
                new String[]{"Which object in the LDAP will be used to find the groups for users : "},
                    "",
                DN_PATTERN,
                "*** Invalid Entry: Please enter a valid object name***"
            );

            String nssBaseShadow = getData(
                new String[]{"Which object in the LDAP will be used to find the shadow entries for users : "},
                    "",
                DN_PATTERN,
                "*** Invalid Entry: Please enter a valid object name***"
            );

            ldapView.setNssBasePasswdObj(nssBasePasswd);
            ldapView.setNssBaseGroupObj(nssBaseGroup);
            ldapView.setNssBaseShadowObj(nssBaseShadow);
        }
    }

    private void doLdapServerPrompts(LdapAuthTypeSettings ldapView) throws IOException, WizardNavigationException {

        boolean isAD = getConfirmationFromUser("Is the directory service to be used an Active Directory?","n");

        boolean isLdapSecure = getConfirmationFromUser("Do you want to use LDAPS (secure)?", "n");
        String ldapServer = getData(
                new String[] {"Enter the address of the LDAP server: "},
                "",
                Pattern.compile("\\S+"),
                "*** Invalid Entry: Please enter a valid LDAP server address ***"
        );

        String defPort = isLdapSecure?"636":"389";
                String ldapPort = getData(
                new String[] {"Enter the LDAP server port : " + "[" + defPort + "] "},
                defPort,
                Pattern.compile("\\d+"),
                "*** Invalid Entry: Please enter a valid LDAP port***"
        );

        String ldapBaseDn = getData(
                new String[] {"Enter the LDAP base DN: "},
                "",
                DN_PATTERN,
                "*** Invalid Entry: Please enter a valid LDAP base DN ***"
        );
        ldapView.setIsActiveDirectory(isAD);
        ldapView.setLdapSecure(isLdapSecure);
        ldapView.setLdapServer(ldapServer);
        ldapView.setLdapPort(ldapPort);
        ldapView.setLdapBaseDn(ldapBaseDn);
    }

    private void doLdapBindPrompts(LdapAuthTypeSettings ldapView) throws IOException, WizardNavigationException {
        boolean isAnonBind = getConfirmationFromUser("Do you want to enable LDAP Anonymous Bind?", "n");
        ldapView.setLdapAnonBind(isAnonBind);

        if (!ldapView.isAnonBind()) {
            String ldapBindDn = getData(
                    new String[] {"Enter the LDAP bind DN : "},
                    "",
                    DN_PATTERN,
                    "*** Invalid Entry: Please enter a valid LDAP bind DN ***"
            );

            String ldapBindPasswd = getData(
                    new String[] {"Enter the LDAP bind password: "},
                    "",
                    (Pattern) null,
                    "",
                    true
            );
            ldapView.setLdapBindDn(ldapBindDn);
            ldapView.setLdapBindPassword(ldapBindPasswd);
        }
    }

    private AuthenticationConfigurationBean.AuthType doSelectAuthTypePrompts() throws IOException, WizardNavigationException {
        List<AuthenticationConfigurationBean.AuthType> allAuthTypes = new ArrayList<AuthenticationConfigurationBean.AuthType>();
        allAuthTypes.addAll(Arrays.asList(values()));

        List<String> promptList = new ArrayList<String>();

        int x = 1;
        for (AuthenticationConfigurationBean.AuthType at: allAuthTypes) {
            String indexStr = String.valueOf(x++);
            promptList.add(indexStr + ") " + at.getNiceName() + EOL);
        }

        promptList.add("Please make a selection : [1] ");
        printText("Select the authentication method you wish to configure." + EOL);

        String[] allowedEntries = new String[x-1];
        for (int index=1; index < x; ++index) {
            allowedEntries[index-1] = String.valueOf(index);
        }
        String whichChoice = getData(
                promptList.toArray(new String[promptList.size()]),
                "1",
                allowedEntries,
                "");

        int choiceAsNum = Integer.parseInt(whichChoice);

        AuthenticationConfigurationBean.AuthType whichType;
        if (choiceAsNum < 1 || choiceAsNum > allAuthTypes.size()) {
            whichType = LOCAL;
        } else {
            whichType = allAuthTypes.get(choiceAsNum -1);
        }

        configBean.setAuthType(whichType);
        return whichType;
    }

    @Override
    public boolean validateStep() {
        return true;
    }

    @Override
    public boolean isShowNavigation() {
        return false;
    }

    private static final Logger logger = Logger.getLogger(SystemConfigWizardNetworkingStep.class.getName());
    private static final String TITLE = "Configure User Authentication";
    private static final String STEP_INFO = "This step lets you configure the authentication method for users on this machine" + EOL;
}
