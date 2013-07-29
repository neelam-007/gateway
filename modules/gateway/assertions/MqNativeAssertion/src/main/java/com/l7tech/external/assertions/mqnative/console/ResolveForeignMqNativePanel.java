package com.l7tech.external.assertions.mqnative.console;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.mqnative.MqNativeExternalReference;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME;
import static com.l7tech.util.Functions.grep;
import static com.l7tech.util.Functions.negate;
import static com.l7tech.util.TextUtils.truncStringMiddleExact;
import static java.util.Collections.emptyList;

/**
 * @author ghuang
 */
public class ResolveForeignMqNativePanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(ResolveForeignMqNativePanel.class.getName());

    private JPanel mainPanel;
    private JTextField hostTextField;
    private JTextField portTextField;
    private JTextField queueMngrNameTextField;
    private JTextField channelNameTextField;
    private JTextField queueNameTextField;
    private JButton createQueueButton;
    private JRadioButton changeRadioButton;
    private JRadioButton removeRadioButton;
    private JRadioButton ignoreRadioButton;
    private JComboBox queueSelectorComboBox;

    private MqNativeExternalReference foreignRef;

    public ResolveForeignMqNativePanel(WizardStepPanel next, MqNativeExternalReference foreignRef) {
        super(next);
        this.foreignRef = foreignRef;
        initialize();
    }

    @Override
    public String getDescription() {
        return getStepLabel();
    }

    @Override
    public boolean canFinish() {
        return !hasNextPanel();
    }

    @Override
    public String getStepLabel() {
        return "Unresolved MQ Native Queue " + foreignRef.getConnectorName();
    }

    @Override
    public boolean onNextButton() {
        if (changeRadioButton.isSelected()) {
            if (queueSelectorComboBox.getSelectedIndex() < 0) return false;

            final SsgActiveConnector connector = (SsgActiveConnector) queueSelectorComboBox.getSelectedItem();
            foreignRef.setLocalizeReplace(connector.getGoid());
        } else if (removeRadioButton.isSelected()) {
            foreignRef.setLocalizeDelete();
        } else if (ignoreRadioButton.isSelected()) {
            foreignRef.setLocalizeIgnore();
        }
        return true;
    }

    @Override
    public void notifyActive() {
        populateConnectorComboBox();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        hostTextField.setText(foreignRef.getHost());
        portTextField.setText(String.valueOf(foreignRef.getPort()));
        queueMngrNameTextField.setText(foreignRef.getQueueManagerName());
        channelNameTextField.setText(foreignRef.getChannelName());
        queueNameTextField.setText(foreignRef.getQueueName());

        // default is delete
        removeRadioButton.setSelected(true);
        queueSelectorComboBox.setEnabled(false);

        // enable/disable provider selector as per action type selected
        changeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                queueSelectorComboBox.setEnabled(true);
            }
        });
        removeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                queueSelectorComboBox.setEnabled(false);
            }
        });
        ignoreRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                queueSelectorComboBox.setEnabled(false);
            }
        });

        queueSelectorComboBox.setRenderer(new TextListCellRenderer<SsgActiveConnector>(new Functions.Unary<String, SsgActiveConnector>() {
            @Override
            public String call( final SsgActiveConnector ssgActiveConnector ) {
                return getConnectorInfo(ssgActiveConnector);
            }
        }));

        createQueueButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                createQueue();
            }
        });

        populateConnectorComboBox();
        enableAndDisableComponents();
    }

    private void createQueue() {
        final SsgActiveConnector newConnector = new SsgActiveConnector();
        newConnector.setEnabled(true);
        newConnector.setName(foreignRef.getConnectorName());
        newConnector.setProperty(PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, foreignRef.getHost());
        newConnector.setProperty(PROPERTIES_KEY_MQ_NATIVE_PORT, String.valueOf(foreignRef.getPort()));
        newConnector.setProperty(PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME, foreignRef.getQueueManagerName());
        newConnector.setProperty(PROPERTIES_KEY_MQ_NATIVE_CHANNEL, foreignRef.getChannelName());
        newConnector.setProperty(PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME, foreignRef.getQueueName());

        EntityUtils.resetIdentity(newConnector);
        editAndSave(newConnector);
    }

    private void editAndSave(final SsgActiveConnector connector){
        final MqNativePropertiesDialog mqQueuePropertiesDialog = MqNativePropertiesDialog.createInstance(
            TopComponents.getInstance().getTopParent(), connector, true, true);

        mqQueuePropertiesDialog.pack();
        Utilities.centerOnScreen(mqQueuePropertiesDialog);

        DialogDisplayer.display(mqQueuePropertiesDialog, new Runnable() {
            @Override
            public void run() {
                if (!mqQueuePropertiesDialog.isCanceled()) {
                    SsgActiveConnector newConnector = mqQueuePropertiesDialog.getTheMqResource();
                    queueSelectorComboBox.setSelectedItem(newConnector);

                    // refresh the list of connectors
                    populateConnectorComboBox();

                    // Set the selected item as the new connector just created.
                    for (int idx = 0; idx < queueSelectorComboBox.getModel().getSize(); idx++) {
                        SsgActiveConnector item = (SsgActiveConnector) queueSelectorComboBox.getItemAt(idx);
                        if (item.getName().equals(newConnector.getName())) {
                            queueSelectorComboBox.setSelectedItem(item);
                            break;
                        }
                    }
                    changeRadioButton.setEnabled(true);
                    changeRadioButton.setSelected(true);
                    queueSelectorComboBox.setEnabled(true);
                }
            }
        });
    }

    private void populateConnectorComboBox() {
        TransportAdmin admin = getTransportAdmin();
        if (admin == null) return;

        final Object selectedItem = queueSelectorComboBox.getSelectedItem();
        final Collection<SsgActiveConnector> connectors = findAllOutboundQueues();

        // Sort connectors by combination name
        Collections.sort((List<SsgActiveConnector>)connectors, new Comparator<SsgActiveConnector>() {
            @Override
            public int compare(SsgActiveConnector o1, SsgActiveConnector o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        // Add all items into the combo box.
        queueSelectorComboBox.setModel(Utilities.comboBoxModel(connectors));

        if (selectedItem != null && queueSelectorComboBox.getModel().getSize() > 0) {
            queueSelectorComboBox.setSelectedItem(selectedItem);
            if (queueSelectorComboBox.getSelectedIndex() == -1) {
                queueSelectorComboBox.setSelectedIndex(0);
            }
        }
    }

    private List<SsgActiveConnector> findAllOutboundQueues() {
        try {
            final TransportAdmin transportAdmin = Registry.getDefault().getTransportAdmin();
            return grep( transportAdmin.findSsgActiveConnectorsByType( ACTIVE_CONNECTOR_TYPE_MQ_NATIVE ),
                negate( booleanProperty( PROPERTIES_KEY_IS_INBOUND ) ) );
        } catch ( IllegalStateException e ) {
            // no admin context available
            logger.info( "Unable to access queues from server." );
        } catch ( FindException e ) {
            ErrorManager.getDefault().notify( Level.WARNING, e, "Error loading queues" );
        }
        return emptyList();
    }

    private TransportAdmin getTransportAdmin() {
        Registry registry = Registry.getDefault();
        if (!registry.isAdminContextPresent()) {
            logger.warning("Cannot get Transport Admin due to no Admin Context present.");
            return null;
        }
        return registry.getTransportAdmin();
    }

    private void enableAndDisableComponents() {
        final boolean enableSelection = queueSelectorComboBox.getModel().getSize() > 0;
        changeRadioButton.setEnabled( enableSelection );

        if ( !changeRadioButton.isEnabled() && changeRadioButton.isSelected() ) {
            removeRadioButton.setSelected( true );
        }
    }

    /**
     * This method is the same as info(final SsgActiveConnector connector) in MqNativeRoutingAssertionDialog.
     */
    private String getConnectorInfo( final SsgActiveConnector connector ) {
        final StringBuilder builder = new StringBuilder();
        builder.append( truncStringMiddleExact( connector.getName(), 48 ) );
        builder.append( " [" );
        if ( connector.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_TEMPLATE_QUEUE ) ) {
            builder.append( "<template>" );
        } else {
            builder.append( truncStringMiddleExact( connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME, "" ), 32 ) );
        }
        builder.append( " on " );
        builder.append( truncStringMiddleExact( connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, "" ), 32 ) );
        builder.append( ':' );
        builder.append( connector.getProperty( PROPERTIES_KEY_MQ_NATIVE_PORT, "" ) );
        builder.append( "]" );
        return builder.toString();
    }
}