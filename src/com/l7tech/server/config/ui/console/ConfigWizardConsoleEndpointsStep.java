package com.l7tech.server.config.ui.console;

import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.server.config.EndpointActions;
import com.l7tech.server.config.PartitionActions;
import com.l7tech.server.config.SharedWizardInfo;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.config.beans.EndpointConfigBean;
import com.l7tech.server.config.commands.EndpointConfigCommand;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.partition.FirewallRules;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * User: megery
 * Date: Nov 1, 2007
 * Time: 12:08:57 PM
 */
public class ConfigWizardConsoleEndpointsStep extends BaseConsoleStep {
    private static final Logger logger = Logger.getLogger(ConfigWizardConsoleEndpointsStep.class.getName());

    private static final String HEADER_CONFIGURE_ENDPOINTS= "-- Configure Endpoints for the \"{0}\" Partition --" + getEolChar();
    public static final String TITLE = "Configure Endpoints";
    private List<SsgConnector> endpointsToAdd;
//    private EndpointConfigBean endpointBean;

    private static enum EndpointEnabledState {
        ENABLED("enabled"),

        DISABLED("disabled"),

        WILL_BE_ENABLED("will be enabled"),
        ;

        private String description;
        EndpointEnabledState (String description) {this.description = description;}
        public String toString() {return description;}
    }

    private class ConnectorAdapter {

        private SsgConnector myConnector;
        private PartitionInformation pinfo;
        private DBInformation dbinfo;

        public ConnectorAdapter(SsgConnector theConnector, PartitionInformation pinfo, DBInformation dbinfo) {
            myConnector = theConnector;
            this.pinfo = pinfo;
            this.dbinfo = dbinfo;
        }

        public String toString() {
            return myConnector.getName() + "(" + myConnector.getPort() + ") " +  "[" + getEnabledState() + "]";
        }

        public EndpointEnabledState getEnabledState() {
            if (myConnector.isEnabled()) {
                return EndpointEnabledState.ENABLED;
            } else {
                if (dbinfo.isNew() && pinfo.getPartitionId().equals(PartitionInformation.DEFAULT_PARTITION_NAME)) {
                    return EndpointEnabledState.WILL_BE_ENABLED;
                } else
                    return EndpointEnabledState.DISABLED;
            }
        }

        public SsgConnector getConnector() {
            return myConnector;
        }

        public boolean asBoolean() {
            switch (getEnabledState()) {
                case ENABLED:
                case WILL_BE_ENABLED:
                    return true;
                case DISABLED:
                default:
                    return false;
            }
        }
    }

    public ConfigWizardConsoleEndpointsStep(ConfigurationWizard parent_) {
        super(parent_);
        init();
    }

    private void init() {
        osFunctions = parent.getOsFunctions();
        configBean = new EndpointConfigBean();
//        endpointBean = (EndpointConfigBean) configBean;
        configCommand = new EndpointConfigCommand(configBean);
        endpointsToAdd = new ArrayList<SsgConnector>();
    }

    //each step must implement these.
    public boolean validateStep() {
        return ensureAtLeastOneEndpoint();
    }

    private boolean ensureAtLeastOneEndpoint() {
        PartitionInformation pinfo = PartitionManager.getInstance().getActivePartition();
        DBInformation dbinfo = SharedWizardInfo.getInstance().getDbinfo();

        //existing endpoints are tricky since they may be enabled at config application time if this is a new db and
        // the default_ partition.
        Collection<SsgConnector> adminEndpoints = EndpointActions.getExistingAdminEndpoints(pinfo);
        if (adminEndpoints != null && !adminEndpoints.isEmpty()) {
            for (SsgConnector adminEndpoint : adminEndpoints) {
                if (new ConnectorAdapter(adminEndpoint, pinfo,dbinfo).asBoolean())
                    return true;
            }
        }

        if (endpointsToAdd != null && !endpointsToAdd.isEmpty()) {
            for (SsgConnector ssgConnector : endpointsToAdd) {
                if (ssgConnector.isEnabled())
                    return true;
            }
        }

        printText(getEolChar() +
                  "*** At least one adminsitrative endpoint must be present and enabled in order to use the SecureSpan Gateway ***" + getEolChar() +
                  "Please create at least one adminsitrative endpoint." + getEolChar() +
                  getEolChar());
        return false;
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {
        PartitionInformation pinfo = PartitionManager.getInstance().getActivePartition();
        try {
            doConfigureEndpointsPrompts(pinfo, validated);
            EndpointConfigBean endpointBean = (EndpointConfigBean) configBean;
            endpointBean.setEndpointsToAdd(endpointsToAdd);
            endpointBean.setPartitionInfo(pinfo);
        } catch (IOException e) {
            logger.severe(ExceptionUtils.getMessage(e));
        }
        storeInput();
    }

    public String getTitle() {
        return TITLE;
    }

    private void doConfigureEndpointsPrompts(PartitionInformation pinfo, boolean shouldShowMessage) throws IOException, WizardNavigationException {

        List<String> promptList = new ArrayList<String>();
        DBInformation dbinfo = SharedWizardInfo.getInstance().getDbinfo();
        boolean finishedEndpointConfig;
        EndpointConfigBean endpointBean = (EndpointConfigBean) configBean;
        endpointBean.setLegacyEndpoints(EndpointActions.getLegacyEndpoints(pinfo));

        do {
            promptList.clear();
            boolean hasExisting = false;
            Collection<SsgConnector> adminEndpoints = EndpointActions.getExistingAdminEndpoints(pinfo);

            if (adminEndpoints != null && !adminEndpoints.isEmpty()) {
                hasExisting = true;
                printText("You have the following administrative (secure) endpoints already configured." + getEolChar());
                for (SsgConnector adminEndpoint : adminEndpoints) {
                  printText("\t" + new ConnectorAdapter(adminEndpoint, pinfo, dbinfo) + getEolChar());
                }
                printText(getEolChar());
            } else {
                if (shouldShowMessage) printText("At least one administration (secure) endpoint needs to be configured" + getEolChar() + "in order to administer the SecureSpan Gateway." + getEolChar() + getEolChar());    
            }

            printText(MessageFormat.format(HEADER_CONFIGURE_ENDPOINTS, pinfo.getPartitionId()));
            String defaultValue = (hasExisting?"2":"1");
            promptList.add("1) Create " + (hasExisting?"another":"an") + " administrative endpoint" + getEolChar());
            promptList.add("2) Continue" + getEolChar());
            promptList.add("Please make a selection: [" + defaultValue + "]");

            String selection = getData(promptList.toArray(new String[promptList.size()]), defaultValue, new String[]{"1", "2"}, null);
            int whichEndpointIndex = Integer.parseInt(selection);

            if (whichEndpointIndex == 2) { //if the select was the last in the list then it's the "continue" option
                if (!PartitionActions.validateAllPartitionEndpoints(pinfo, false)) {
                    printText(getEolChar() + "*** Some of the partition endpoints have errors. Please correct the indicated errors ***" + getEolChar() + getEolChar());
                    finishedEndpointConfig = false;
                } else {
                    finishedEndpointConfig = true;
                }
            } else {
                SsgConnector endpoint = new SsgConnector();
                doCollectEndpointInfo(endpoint);
                FirewallRules.PortInfo portInfo = FirewallRules.getAllInfo();
                if (portInfo.isPortUsed(endpoint.getPort(), false, null)) {
                    printText(endpoint.getPort() + " is already in use. Please select another port." + getEolChar() + getEolChar());
                    finishedEndpointConfig =false;
                } else {
                    finishedEndpointConfig = true;
                    endpointsToAdd.add(endpoint);
                }
            }
        } while (!finishedEndpointConfig);
    }

    private void doCollectEndpointInfo(SsgConnector connector) throws IOException, WizardNavigationException {
        Pattern portPattern = Pattern.compile("\\d{1,5}+");

        String defaultPort= String.valueOf(9443);

        boolean hadErrors = false;
        String input;
        do {
        input = getData(
                new String[]{"Please enter the port for the new administrative endpoint: [" + defaultPort + "]"},
                defaultPort,
                portPattern,
                "The port you have entered is invalid. Please re-enter");

            int intPort = Integer.valueOf(input);
            if ( intPort < 1024) {
                printText("*** The SecureSpan Gateway cannot use ports less than 1024 ***" + getEolChar());
                hadErrors = true;
            } else if (intPort > 65535) {
                printText("*** The maximum port allowed is 65535 ***" + getEolChar());
                hadErrors = true;
            } else {
                hadErrors = false;
            }
        }while (hadErrors);

        connector.setPort(Integer.parseInt(input));

        String defaultName = "Default Administration Endpoint";
        input = getData(
                new String[]{"Enter a name for this new connector: [" + defaultName + "]"},
                defaultName,
                (String[]) null,
                null);
        connector.setName(input);
        connector.setScheme(SsgConnector.SCHEME_HTTPS);
        connector.setEndpoints(SsgConnector.Endpoint.asCommaList(EnumSet.of(SsgConnector.Endpoint.ADMIN_APPLET, SsgConnector.Endpoint.ADMIN_REMOTE)));
        connector.setClientAuth(SsgConnector.CLIENT_AUTH_NEVER);
        connector.setEnabled(true);
        connector.setSecure(true);
    }
}
