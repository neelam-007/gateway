package com.l7tech.console.action;

import com.l7tech.console.panels.SinkConfigurationPropertiesDialog;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.logging.Level;

/**
 * Action to create a logsink for an entityHeader
 */
public class CreateEntityLogSinkAction extends SecureAction {
    @NotNull private final EntityHeader entityHeader;

    public CreateEntityLogSinkAction( @NotNull EntityHeader entity ) {
        super(new AttemptedCreate(EntityType.LOG_SINK));
        this.entityHeader = entity;
    }

    @Override
    public String getName() {
        return "Create Log Sink";
    }

    @Override
    public String getDescription() {
        return "Create a new Log Sink";
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/interface.gif";
    }

    @Override
    protected void performAction() {
        final Frame mw = TopComponents.getInstance().getTopParent();
        final SinkConfiguration sink = new SinkConfiguration();

        sink.setCategories(SinkConfiguration.CATEGORY_AUDITS+','+SinkConfiguration.CATEGORY_GATEWAY_LOGS);

        Map<String, java.util.List<String>> filters = new HashMap<String, java.util.List<String>> (sink.getFilters());
        java.util.List<String> values = new ArrayList<String>();
        values.add(entityHeader.getStrId());
        filters.put(getHybridContextType(),  values);
        sink.setFilters(filters);

        String nameStr = filterName( entityHeader.getName() );
        sink.setName(nameStr);
        sink.setDescription(entityHeader.getName() + " Log");

        doEdit(mw, sink);
    }

    private String filterName( final String name ) {
        final StringBuilder nameBuilder = new StringBuilder();

        if ( name != null ) {
            final String valid = SinkConfigurationPropertiesDialog.VALID_NAME_CHARACTERS;
            for ( final char character : name.toCharArray() ) {
                if ( valid.indexOf( character ) > -1 ) {
                    nameBuilder.append( character );
                }
            }
        }

        return nameBuilder.toString();
    }

    private String getHybridContextType(){
        EntityType type = entityHeader.getType();
        switch (type){
            case POLICY:
                return GatewayDiagnosticContextKeys.POLICY_ID;
            case SERVICE:
                return GatewayDiagnosticContextKeys.SERVICE_ID;
            case FOLDER:
                return GatewayDiagnosticContextKeys.FOLDER_ID;
        }
        throw new IllegalStateException("Unsupported entityHeader type: " + type);
    }

     /** @return the LogSinkAdmin interface, or null if not connected or it's unavailable for some other reason */
    private LogSinkAdmin getLogSinkAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getLogSinkAdmin();
    }


    private void showErrorMessage(final Frame parent,String title, String msg, Throwable e, Runnable continuation) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(parent, msg, title, JOptionPane.ERROR_MESSAGE, continuation);
    }

    private void doEdit( final Frame parent, final SinkConfiguration sinkConfiguration ) {
        final SinkConfigurationPropertiesDialog dlg = new SinkConfigurationPropertiesDialog(parent, sinkConfiguration, false);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.selectNameField();
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.isConfirmed()) {
                    Runnable reedit = new Runnable() {
                        public void run() {
                            doEdit(parent, sinkConfiguration);
                        }
                    };

                    try {
                       getLogSinkAdmin().saveSinkConfiguration(sinkConfiguration);
                    } catch (SaveException e) {
                        showErrorMessage(parent,
                                "Save Failed",
                                "Failed to save log sink:" + " " + ExceptionUtils.getMessage(e),
                                ExceptionUtils.getDebugException(e),
                                reedit);
                    } catch (UpdateException e) {
                        showErrorMessage(parent,
                                "Save Failed",
                                "Failed to save log sink:" + " " + ExceptionUtils.getMessage(e),
                                ExceptionUtils.getDebugException(e),
                                reedit);
                    }
                }
            }
        });
    }
}
