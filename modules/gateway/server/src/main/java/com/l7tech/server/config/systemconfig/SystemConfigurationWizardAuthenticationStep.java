package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.wizard.BaseConsoleStep;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.l7tech.server.config.beans.BaseConfigurationBean.EOL;
import static com.l7tech.server.config.systemconfig.AuthenticationConfigurationBean.AuthType.*;

/**
 * @author: megery
 */
public class SystemConfigurationWizardAuthenticationStep extends BaseConsoleStep<AuthenticationConfigurationBean, AuthenticationConfigurationCommand> {

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
        switch (authType) {
            case LOCAL:
                    doLocalConfigPrompts();
                    break;
            case LDAP:
                    doLdapConfigPrompts();
                    break;
            case RADIUS:
                    doRadiusConfigPrompts();
                    break;
            case RADIUS_LDAP:
                    doRadiusWithLdapConfigPrompts();
                    break;
        }
    }

    private void doRadiusWithLdapConfigPrompts() throws IOException, WizardNavigationException {
        doRadiusConfigPrompts();
        doLdapConfigPrompts();
    }

    private void doRadiusConfigPrompts() throws IOException, WizardNavigationException {
        String radiusAddress = getData(
                new String[] {GET_RADIUS_ADDRESS},
                "",
                Pattern.compile("\\S+"),
                "*** Invalid Entry: Please enter a valid address for the RADIUS server ***"
        );

        configBean.setRadiusAddress(radiusAddress);

        //TODO mask this
        String radiusSecret = getData(
                new String[] {GET_RADIUS_SECRET},
                "",
                (String[])null,
                null
        );

        configBean.setRadiusSecret(radiusSecret);

        //TODO make sure this is ONLY a number
        String radiusTimeout = getData(
                new String[] {GET_RADIUS_TIMEOUT},
                "",
                Pattern.compile("\\d+"),
                "*** Invalid Entry: Please enter the number of seconds to wait for a reply from the RADIUS server ***"
        );

        configBean.setRadiusTimeout(radiusTimeout);
    }

    private void doLdapConfigPrompts() throws IOException, WizardNavigationException {
        String ldapAddress = getData(
                new String[] {GET_LDAP_ADDRESS},
                "",
                Pattern.compile("\\S+"),
                "*** Invalid Entry: Please enter a valid address for the LDAP server ***"
        );

        configBean.setLdapAddress(ldapAddress);

        //TODO mask this
        String ldapBase = getData(
                new String[] {GET_LDAP_BASE},
                "",
                (String[])null,
                null
        );

        configBean.setLdapBase(ldapBase);
    }

    private void doLocalConfigPrompts() {
        //nothing to do since this amounts to disabling the other methods
        configBean.setAuthType(LOCAL);
    }

    private AuthenticationConfigurationBean.AuthType doSelectAuthTypePrompts() throws IOException, WizardNavigationException {
        List<AuthenticationConfigurationBean.AuthType> allAuthTypes = new ArrayList<AuthenticationConfigurationBean.AuthType>();
        allAuthTypes.addAll(Arrays.asList(values()));

        List<String> promptList = new ArrayList<String>();

        int x = 1;
        for (AuthenticationConfigurationBean.AuthType at: allAuthTypes) {
            String indexStr = String.valueOf(x++);
            promptList.add(indexStr + ") " + at.describe() + EOL);
        }

        promptList.add("Please make a selection [1] : ");
        printText("Select the authentication method you wish to configure." + EOL);

        String[] allowedEntries = new String[x];
        for (int index=1; index <= x; ++index) {
            allowedEntries[index-1] = String.valueOf(index);
        }
        String whichChoice = getData(promptList.toArray(new String[promptList.size()]), "1", allowedEntries,null);

        int choiceAsNum = Integer.parseInt(whichChoice);

        AuthenticationConfigurationBean.AuthType whichType = null;
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

    private static final Logger logger = Logger.getLogger(SystemConfigWizardNetworkingStep.class.getName());
    private static final String TITLE = "Configure User Authentication";
    private static final String STEP_INFO = "This step lets you configure the authentication method for users on this machine" + EOL;

    private static final String GET_RADIUS_ADDRESS = EOL + "Enter the address of the RADIUS server: ";
    private static final String GET_RADIUS_SECRET = EOL + "Enter the RADIUS shared secret: ";
    private static final String GET_RADIUS_TIMEOUT = EOL + "Enter the RADIUS timeout (in seconds): ";
    private static final String GET_LDAP_ADDRESS = EOL + "Enter the address of the LDAP server: ";
    private static final String GET_LDAP_BASE = EOL + "Enter the LDAP base DN: ";

}
