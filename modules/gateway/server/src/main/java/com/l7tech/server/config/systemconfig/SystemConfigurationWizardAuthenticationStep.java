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
import static com.l7tech.server.config.systemconfig.AuthenticationConfigurationBean.AuthType.LOCAL;
import static com.l7tech.server.config.systemconfig.AuthenticationConfigurationBean.AuthType.values;

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

        doAuthTypeSpecificPrompts(authType);
    }

    private void doAuthTypeSpecificPrompts(AuthenticationConfigurationBean.AuthType authType) throws IOException, WizardNavigationException {
        List<AuthenticationConfigurationBean.AuthTypeDescriptor> prompts = authType.getPrompts();


        for (AuthenticationConfigurationBean.AuthTypeDescriptor prompt : prompts) {
            Pattern allowedPattern = prompt.getAllowedPattern() == null?null:Pattern.compile(prompt.getAllowedPattern());
            String val = getData(
                new String[] {prompt.getPrompt() + ": "},
                "",
                allowedPattern,
                "*** Invalid Entry: Please enter a valid address for the " + prompt.getDescription() + "***"
            );
            configBean.setAuthData(prompt,val);
        }
        configBean.setAuthType(authType);
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

        promptList.add("Please make a selection [1] : ");
        printText("Select the authentication method you wish to configure." + EOL);

        String[] allowedEntries = new String[x-1];
        for (int index=1; index < x; ++index) {
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

    @Override
    public boolean isShowNavigation() {
        return false;
    }

    private static final Logger logger = Logger.getLogger(SystemConfigWizardNetworkingStep.class.getName());
    private static final String TITLE = "Configure User Authentication";
    private static final String STEP_INFO = "This step lets you configure the authentication method for users on this machine" + EOL;
}