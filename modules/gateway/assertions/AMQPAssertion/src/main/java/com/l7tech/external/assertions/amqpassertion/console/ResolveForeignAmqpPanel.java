package com.l7tech.external.assertions.amqpassertion.console;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.amqpassertion.AMQPDestination;
import com.l7tech.external.assertions.amqpassertion.AmqpExternalReference;
import com.l7tech.external.assertions.amqpassertion.AmqpSsgActiveConnector;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.gui.util.DialogDisplayer.showMessageDialog;
import static com.l7tech.util.ExceptionUtils.getMessage;
import static com.l7tech.util.Functions.grep;
import static com.l7tech.util.Functions.negate;
import static com.l7tech.util.TextUtils.truncStringMiddleExact;
import static java.util.Collections.emptyList;

/**
 * @author ashah
 */
public class ResolveForeignAmqpPanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(ResolveForeignAmqpPanel.class.getName());

    private JPanel mainPanel;
    private JTextField hostTextField;
    private JTextField exchangeNameTextField;
    private JTextField channelNameTextField;
    private JTextField queueNameTextField;
    private JButton createQueueButton;
    private JRadioButton changeRadioButton;
    private JRadioButton removeRadioButton;
    private JRadioButton ignoreRadioButton;
    private JComboBox queueSelectorComboBox;

    private AmqpExternalReference foreignRef;

    public ResolveForeignAmqpPanel(WizardStepPanel next, AmqpExternalReference foreignRef) {
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
        return "Unresolved AMQP Queue " + foreignRef.getSsgActiveConnector().getName();
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
        String[] addresses = AMQPDestinationHelper.stringRepresentationToObject(foreignRef.getSsgActiveConnector().getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_ADDRESSES));
        String finalAddress = "";
        for (String str : addresses) {
            finalAddress += str + ",";
        }
        hostTextField.setText(finalAddress);
        exchangeNameTextField.setText(foreignRef.getSsgActiveConnector().getProperty(AmqpSsgActiveConnector.PROPERTY_KEY_AMQP_EXCHANGE_NAME));

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
            public String call(final SsgActiveConnector ssgActiveConnector) {
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
        final SsgActiveConnector newConnector = foreignRef.getSsgActiveConnector();
        EntityUtils.resetIdentity(newConnector);
        editAndSave(newConnector);
    }

    private void editAndSave(final SsgActiveConnector connector) {
        AMQPDestination destination = AMQPDestinationHelper.ssgConnectorToAmqpDestination(connector);
        AMQPDestinationDialog dialog = new AMQPDestinationDialog(this.getOwner());
        dialog.updateView(destination);
        Utilities.centerOnParentWindow(dialog);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            dialog.updateModel(destination);
            try {
                Goid goid = AMQPDestinationHelper.addAMQPDestination(destination);
                populateConnectorComboBox();
                changeRadioButton.setEnabled(true);
                changeRadioButton.setSelected(true);
                queueSelectorComboBox.setEnabled(true);
                int itemCount = queueSelectorComboBox.getItemCount();
                for (int i = 0; i < itemCount; i++) {
                    SsgActiveConnector o = (SsgActiveConnector) queueSelectorComboBox.getItemAt(i);
                    if (o.getGoid() == goid) {
                        queueSelectorComboBox.setSelectedItem(o);
                    }
                }
            } catch (SaveException t) {
                showMessageDialog(this, "Cannot save AMQP Queue: " + getMessage(t), "Error Saving AMQP Queue", JOptionPane.ERROR_MESSAGE, null);
            } catch (UpdateException t) {
                showMessageDialog(this, "Cannot save AMQP Queue: " + getMessage(t), "Error Saving AMQP Queue", JOptionPane.ERROR_MESSAGE, null);
            }
        }
    }

    private void populateConnectorComboBox() {
        TransportAdmin admin = getTransportAdmin();
        if (admin == null) return;

        final Object selectedItem = queueSelectorComboBox.getSelectedItem();
        final Collection<SsgActiveConnector> connectors = findAllOutboundQueues();

        // Sort connectors by combination name
        Collections.sort((List<SsgActiveConnector>) connectors, new Comparator<SsgActiveConnector>() {
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
            return grep(transportAdmin.findSsgActiveConnectorsByType(AmqpSsgActiveConnector.ACTIVE_CONNECTOR_TYPE_AMQP),
                    negate(booleanProperty(PROPERTIES_KEY_IS_INBOUND)));
        } catch (IllegalStateException e) {
            // no admin context available
            logger.info("Unable to access queues from server.");
        } catch (FindException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Error loading queues");
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
        changeRadioButton.setEnabled(enableSelection);

        if (!changeRadioButton.isEnabled() && changeRadioButton.isSelected()) {
            removeRadioButton.setSelected(true);
        }
    }

    /**
     * This method is the same as info(final SsgActiveConnector connector) in MqNativeRoutingAssertionDialog.
     */
    private String getConnectorInfo(final SsgActiveConnector connector) {
        final StringBuilder builder = new StringBuilder();
        builder.append(truncStringMiddleExact(connector.getName(), 48));
        builder.append(" [");
        if (connector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_TEMPLATE_QUEUE)) {
            builder.append("<template>");
        } else {
            builder.append(truncStringMiddleExact(connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME, ""), 32));
        }
        builder.append(" on ");
        builder.append(truncStringMiddleExact(connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, ""), 32));
        builder.append(':');
        builder.append(connector.getProperty(PROPERTIES_KEY_MQ_NATIVE_PORT, ""));
        builder.append("]");
        return builder.toString();
    }
}