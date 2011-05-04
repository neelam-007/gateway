package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.commands.BaseConfigurationCommand;

import java.util.logging.Logger;

/**
 * @author: megery
 */
public class AuthenticationConfigurationCommand extends BaseConfigurationCommand<AuthenticationConfigurationBean> {
    private static final Logger logger = Logger.getLogger(AuthenticationConfigurationCommand.class.getName());
    private static final String CONF_FILE = "radius_ldap_setup.conf";

    protected AuthenticationConfigurationCommand(AuthenticationConfigurationBean bean) {
        super(bean);
    }

    @Override
    public boolean execute() {
        boolean success;
        success = writeConfigFile(CONF_FILE,configBean.asConfigFile());
        return success;
    }
}
