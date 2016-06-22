package com.l7tech.external.assertions.amqpassertion.console;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.panels.*;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.amqpassertion.AMQPDestination;
import com.l7tech.external.assertions.amqpassertion.AmqpAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.jms.JmsAcknowledgementType;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Option;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.console.panels.CancelableOperationDialog.doWithDelayedCancelDialog;
import static com.l7tech.util.ExceptionUtils.getMessage;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.some;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 2/8/12
 * Time: 11:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class AMQPDestinationDialog extends JDialog {
    private static class ContentTypeComboBoxItem {
        private final ContentTypeHeader cType;

        private ContentTypeComboBoxItem(final ContentTypeHeader cType) {
            if (cType == null) throw new IllegalArgumentException("cType must not be null");
            this.cType = cType;
        }

        public ContentTypeHeader getContentType() {
            return this.cType;
        }

        @Override
        public String toString() {
            return this.cType.getMainValue();
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ContentTypeComboBoxItem that = (ContentTypeComboBoxItem) o;

            return cType.getFullValue().equals(that.cType.getFullValue());
        }

        public int hashCode() {
            return (cType != null ? cType.hashCode() : 0);
        }
    }

    private class ContentTypeComboBoxModel extends DefaultComboBoxModel {
        private ContentTypeComboBoxModel(ContentTypeComboBoxItem[] items) {
            super(items);
        }

        // implements javax.swing.MutableComboBoxModel
        @Override
        public void addElement(Object anObject) {
            if (anObject instanceof String) {
                String s = (String) anObject;
                try {
                    ContentTypeHeader cth = ContentTypeHeader.parseValue(s);
                    super.addElement(new ContentTypeComboBoxItem(cth));
                } catch (IOException e) {
                    logger.warning("Error parsing the content type " + s);
                }
            }
        }
    }

    private Logger logger = Logger.getLogger(AMQPDestinationDialog.class.getName());

    private JPanel mainPanel;
    private JTabbedPane tabbedPane;
    private JTextField destinationNameField;
    private JRadioButton outboundRadioButton;
    private JRadioButton inboundRadioButton;
    private JTextField virtualHostField;
    private JList addressesList;
    private DefaultListModel addressesListModel;
    private JButton addAddressButton;
    private JButton editAddressButton;
    private JButton removeAddressButton;
    private JCheckBox credentialsCheckBox;
    private JLabel usernameLabel;
    private JTextField usernameField;
    private JLabel passwordLabel;
    private SecurePasswordComboBox passwordComboBox;
    private JButton managePasswordsButton;
    private JCheckBox enableSSLCheckBox;
    private JLabel cipherSpecLabel;
    private JComboBox cipherSpecComboBox;
    private JCheckBox useClientAuthenticationCheckBox;
    private JLabel keystoreLabel;
    private PrivateKeysComboBox keystoreComboBox;
    private JTextField queueNameField;
    private JSpinner threadPoolSizeField;
    private JComboBox ackBehaviourComboBox;
    private JRadioButton automaticSendReplyIfRadioButton;
    private JRadioButton doNotSendRepliesRadioButton;
    private JRadioButton sendReplyToSpecifiedRadioButton;
    private JTextField inboundReplySpecifiedQueueField;
    private JPanel inboundCorrelationPanel;
    private JRadioButton inboundCorrelationIdRadioButton;
    private JRadioButton inboundMessageIdRadioButton;
    private JCheckBox associateQueueWithPublishedService;
    private JLabel serviceNameLabel;
    private JComboBox serviceNameCombo;
    private JCheckBox specifyContentTypeCheckBox;
    private JRadioButton specifyContentTypeFreeForm;
    private JComboBox contentTypeValues;
    private ContentTypeComboBoxModel contentTypeModel;
    private JRadioButton getContentTypeFromRadioButton;
    private JTextField getContentTypeFromProperty;
    private JCheckBox useQueueForFailedCheckBox;
    private JLabel failureQueueLabel;
    private JTextField failureQueueNameTextField;
    private JCheckBox disableListeningTheQueueCheckBox;
    private JRadioButton outboundReplyAutomaticRadioButton;
    private JRadioButton outboundReplyNoneRadioButton;
    private JRadioButton outboundReplySpecifiedQueueRadioButton;
    private JTextField outboundReplySpecifiedQueueField;
    private JTextField exchangeNameField;
    private JButton saveButton;
    private JButton cancelButton;
    private JPanel outboundCorrelationPanel;
    private JRadioButton outboundCorrelationIdRadioButton;
    private JRadioButton outboundMessageIdRadioButton;
    private JButton testButton;
    private boolean modified = false;

    private boolean confirmed = false;

    public AMQPDestinationDialog(Frame owner) {
        super(owner, "AMQP Destination", true);
        initializeComponents();
    }

    public AMQPDestinationDialog(Dialog owner) {
        super(owner, "AMQP Destination", true);
        initializeComponents();
    }

    private void initializeComponents() {
        DocumentListener amqpDocumentListener = new AMQPDocumentListener();

        outboundRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (outboundRadioButton.isSelected()) {
                    tabbedPane.setEnabledAt(1, false);
                    tabbedPane.setEnabledAt(2, true);
                    enableDisableOutboundComponents();
                } else {
                    tabbedPane.setEnabledAt(1, true);
                    tabbedPane.setEnabledAt(2, false);
                    enableDisableInboundComponents();
                }
            }
        });
        inboundRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (inboundRadioButton.isSelected()) {
                    tabbedPane.setEnabledAt(1, true);
                    tabbedPane.setEnabledAt(2, false);
                    enableDisableInboundComponents();
                } else {
                    tabbedPane.setEnabledAt(1, false);
                    tabbedPane.setEnabledAt(2, true);
                    enableDisableOutboundComponents();
                }
            }
        });

        addressesListModel = new DefaultListModel();
        addressesList.setModel(addressesListModel);
        addressesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        addressesList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableDisableAddressButtons();
            }
        });

        addAddressButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AddressDialog dialog = new AddressDialog(AMQPDestinationDialog.this);
                Utilities.centerOnParentWindow(dialog);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    addressesListModel.addElement(dialog.getData());
                    enableDisableSaveButton();
                }
            }
        });

        editAddressButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AddressDialog dialog = new AddressDialog(AMQPDestinationDialog.this);
                dialog.setData((String) addressesList.getSelectedValue());
                Utilities.centerOnParentWindow(dialog);
                dialog.setVisible(true);
                if (dialog.isConfirmed()) {
                    addressesListModel.setElementAt(dialog.getData(), addressesList.getSelectedIndex());
                }
            }
        });

        removeAddressButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] indices = addressesList.getSelectedIndices();
                for (int i = indices.length - 1; i >= 0; i--) {
                    addressesListModel.remove(indices[i]);
                }

                enableDisableSaveButton();
            }
        });

        credentialsCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableCredentialComponents();
            }
        });
        managePasswordsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SecurePasswordManagerWindow dialog = new SecurePasswordManagerWindow(TopComponents.getInstance().getTopParent());
                dialog.pack();
                Utilities.centerOnScreen(dialog);
                DialogDisplayer.display(dialog);
                passwordComboBox.reloadPasswordList();
            }
        });

        enableSSLCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableSslComponents();
            }
        });
        cipherSpecComboBox.setModel(new DefaultComboBoxModel(getCipherSuites()));
        useClientAuthenticationCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableSslComponents();
            }
        });

        threadPoolSizeField.setModel(new SpinnerNumberModel(10, 1, 500, 1));

        ackBehaviourComboBox.setModel(new DefaultComboBoxModel(JmsAcknowledgementType.values()));
        ackBehaviourComboBox.setRenderer(new TextListCellRenderer<Object>(new Functions.Unary<String, Object>() {
            @Override
            public String call(Object o) {
                JmsAcknowledgementType type = (JmsAcknowledgementType) o;
                String text;

                switch (type) {
                    case AUTOMATIC:
                        text = "On Take";
                        break;
                    case ON_COMPLETION:
                        text = "On Completion";
                        break;
                    default:
                        text = "Unknown";
                        break;
                }

                return text;
            }
        }));
        ackBehaviourComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableInboundComponents();
            }
        });

        automaticSendReplyIfRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableInboundComponents();
            }
        });
        doNotSendRepliesRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableInboundComponents();
            }
        });
        sendReplyToSpecifiedRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableInboundComponents();
            }
        });
        inboundReplySpecifiedQueueField.getDocument().addDocumentListener(amqpDocumentListener);

        associateQueueWithPublishedService.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableInboundComponents();
            }
        });

        serviceNameCombo.setRenderer(TextListCellRenderer.<ServiceComboItem>basicComboBoxRenderer());
        serviceNameCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                modified = true;
            }
        });

        specifyContentTypeCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableInboundComponents();
            }
        });
        specifyContentTypeFreeForm.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableInboundComponents();
            }
        });
        getContentTypeFromRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableInboundComponents();
            }
        });
        getContentTypeFromProperty.getDocument().addDocumentListener(amqpDocumentListener);

        useQueueForFailedCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableInboundComponents();
            }
        });

        outboundReplyAutomaticRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableOutboundComponents();
            }
        });
        outboundReplyNoneRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableOutboundComponents();
            }
        });
        outboundReplySpecifiedQueueRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableOutboundComponents();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.setDoubleClickAction(addressesList, editAddressButton);
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (destinationNameField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(AMQPDestinationDialog.this, "Destination name is required.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (addressesListModel.isEmpty()) {
                    JOptionPane.showMessageDialog(AMQPDestinationDialog.this, "At least one address must be specified.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (credentialsCheckBox.isSelected() && usernameField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(AMQPDestinationDialog.this, "Credentials are required, but the username field is empty.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (sendReplyToSpecifiedRadioButton.isSelected() && inboundReplySpecifiedQueueField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(AMQPDestinationDialog.this, "The reply queue name field is empty.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (associateQueueWithPublishedService.isSelected() && serviceNameCombo.getSelectedItem() == null) {
                    JOptionPane.showMessageDialog(AMQPDestinationDialog.this, "This queue is to be associated with a published service, but no published service was selected.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (specifyContentTypeCheckBox.isSelected()) {
                    if (specifyContentTypeFreeForm.isSelected()) {
                        if (contentTypeValues.getSelectedItem() == null && contentTypeValues.getEditor().getItem() == null) {
                            JOptionPane.showMessageDialog(AMQPDestinationDialog.this, "The value of the content type is required.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        } else if (contentTypeValues.getSelectedItem() instanceof String) {
                            try {
                                ContentTypeHeader.parseValue((String) contentTypeValues.getSelectedItem());
                            } catch (IOException ex) {
                                JOptionPane.showMessageDialog(AMQPDestinationDialog.this, "An invalid content type was specified.", "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }
                    } else if (getContentTypeFromRadioButton.isSelected() && getContentTypeFromProperty.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(AMQPDestinationDialog.this, "The name of the content type property is required.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                if (useQueueForFailedCheckBox.isSelected() && failureQueueNameTextField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(AMQPDestinationDialog.this, "The name of the failure queue is required.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (outboundReplySpecifiedQueueRadioButton.isSelected()) {
                    if (outboundReplySpecifiedQueueField.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(AMQPDestinationDialog.this, "The name of the reply queue is required.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                if (inboundRadioButton.isSelected()) {
                    if (queueNameField.getText().trim().isEmpty()) {
                        JOptionPane.showMessageDialog(AMQPDestinationDialog.this, "The queue name is required for inbound destinations.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                confirmed = true;
                setVisible(false);
            }
        });
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onTest();
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        destinationNameField.getDocument().addDocumentListener(amqpDocumentListener);
        exchangeNameField.getDocument().addDocumentListener(amqpDocumentListener);
        outboundReplySpecifiedQueueField.getDocument().addDocumentListener(amqpDocumentListener);
        queueNameField.getDocument().addDocumentListener(amqpDocumentListener);
        inboundReplySpecifiedQueueField.getDocument().addDocumentListener(amqpDocumentListener);
        failureQueueNameTextField.getDocument().addDocumentListener(amqpDocumentListener);
        usernameField.getDocument().addDocumentListener(amqpDocumentListener);
        setContentPane(mainPanel);
        pack();
    }

    private void onTest() {

        final AmqpAdmin admin = Registry.getDefault().getExtensionInterface(AmqpAdmin.class, null);
        final AMQPDestination destination = new AMQPDestination();
        updateModel(destination);
        try {
            final Option<? extends Exception> error = doWithDelayedCancelDialog(
                    new Callable<Option<? extends Exception>>() {
                        @Override
                        public Option<? extends Exception> call() {
                            Option<? extends Exception> result = none();
                            try {
                                boolean b = admin.testSettings(destination);
                                if (!b) {
                                    result = Option.some(new AmqpAdmin.AmqpTestException("Connection testing failed."));
                                }
                            } catch (AmqpAdmin.AmqpTestException e) {
                                result = some(e);
                            }
                            // ensure interrupted status is cleared
                            Thread.interrupted();

                            return result;
                        }
                    },
                    this,
                    "Testing AMQP destination settings",
                    "Testing AMQP destination settings, please wait ...",
                    5000L);
            if (error.isSome()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Unable to verify this AMQP Destination setting: " + getMessage(error.some()),
                        "AMQP Desination Test Failed",
                        JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "The Gateway has successfully verified this AMQP Destination setting.",
                        "AMQP Desination Test Successful",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (InterruptedException e) {
            //cancelled.
        } catch (InvocationTargetException e) {
            throw ExceptionUtils.wrap(e.getTargetException());
        }
    }

    private void loadContentTypesModel() {
        if (contentTypeModel == null) {
            java.util.List<ContentTypeComboBoxItem> items = new ArrayList<ContentTypeComboBoxItem>();
            items.add(new ContentTypeComboBoxItem(ContentTypeHeader.XML_DEFAULT));
            items.add(new ContentTypeComboBoxItem(ContentTypeHeader.SOAP_1_2_DEFAULT));
            items.add(new ContentTypeComboBoxItem(ContentTypeHeader.TEXT_DEFAULT));
            items.add(new ContentTypeComboBoxItem(ContentTypeHeader.APPLICATION_X_WWW_FORM_URLENCODED));
            items.add(new ContentTypeComboBoxItem(ContentTypeHeader.OCTET_STREAM_DEFAULT));
            try {
                items.add(new ContentTypeComboBoxItem(ContentTypeHeader.parseValue("application/fastinfoset")));
            } catch (IOException e) {
                logger.warning("Error trying to initialize content-type application/fastinfoset");
            }
            try {
                items.add(new ContentTypeComboBoxItem(ContentTypeHeader.parseValue("application/soap+fastinfoset")));
            } catch (IOException e) {
                logger.warning("Error trying to initialize content-type application/soap+fastinfoset");
            }

            contentTypeModel = new ContentTypeComboBoxModel(items.toArray(new ContentTypeComboBoxItem[items.size()]));
            contentTypeValues.setModel(contentTypeModel);
        }
    }

    private void enableDisableSaveButton() {
        boolean enableSaveButton = true;

        if (destinationNameField.getText().trim().isEmpty()) {
            enableSaveButton = false;
        }

        if (outboundRadioButton.isSelected()) {
            if (outboundReplySpecifiedQueueRadioButton.isSelected() && outboundReplySpecifiedQueueField.getText().trim().isEmpty()) {
                enableSaveButton = false;
            }
        } else if (inboundRadioButton.isSelected()) {
            if (queueNameField.getText().trim().isEmpty()) {
                enableSaveButton = false;
            }

            if (sendReplyToSpecifiedRadioButton.isSelected() && inboundReplySpecifiedQueueField.getText().trim().isEmpty()) {
                enableSaveButton = false;
            }
            Object o = serviceNameCombo.getSelectedItem();
            if (associateQueueWithPublishedService.isSelected() && (serviceNameCombo.getSelectedItem() == null)) {
                enableSaveButton = false;
            }

            if (specifyContentTypeCheckBox.isSelected()) {
                if (specifyContentTypeFreeForm.isSelected()) {
                    if ((contentTypeValues.getSelectedItem() == null) && (contentTypeValues.getEditor().getItem() == null)) {
                        enableSaveButton = false;
                    } else if (contentTypeValues.getSelectedItem() instanceof String) {
                        try {
                            ContentTypeHeader.parseValue((String) contentTypeValues.getSelectedItem());
                        } catch (IOException ex) {
                            enableSaveButton = false;
                        }
                    }
                } else if (getContentTypeFromRadioButton.isSelected() && (getContentTypeFromProperty.getText().trim() == "")) {
                    enableSaveButton = false;
                }
            }

            if (useQueueForFailedCheckBox.isSelected() && failureQueueNameTextField.getText().trim().isEmpty()) {
                enableSaveButton = false;
            }
        }

        if (addressesListModel.isEmpty()) {
            enableSaveButton = false;
        }

        if (credentialsCheckBox.isSelected() && (usernameField.getText().trim().isEmpty() || passwordComboBox.getSelectedItem() == null)) {
            enableSaveButton = false;
        }

        saveButton.setEnabled(enableSaveButton);
        testButton.setEnabled(enableSaveButton);
    }

    private void enableDisableCredentialComponents() {
        boolean allValid = true;

        usernameLabel.setEnabled(credentialsCheckBox.isSelected());
        usernameField.setEnabled(credentialsCheckBox.isSelected());
        passwordLabel.setEnabled(credentialsCheckBox.isSelected());
        passwordComboBox.setEnabled(credentialsCheckBox.isSelected());

        enableDisableSaveButton();
    }

    private void enableDisableSslComponents() {
        cipherSpecLabel.setEnabled(enableSSLCheckBox.isSelected());
        cipherSpecComboBox.setEnabled(enableSSLCheckBox.isSelected());
        useClientAuthenticationCheckBox.setEnabled(enableSSLCheckBox.isSelected());
        keystoreLabel.setEnabled(enableSSLCheckBox.isSelected() && useClientAuthenticationCheckBox.isSelected());
        keystoreComboBox.setEnabled(enableSSLCheckBox.isSelected() && useClientAuthenticationCheckBox.isSelected());

        enableDisableSaveButton();
    }

    private void enableDisableAddressButtons() {
        editAddressButton.setEnabled(addressesList.getSelectedIndices().length == 1);
        removeAddressButton.setEnabled(addressesList.getSelectedIndices().length > 0);
    }

    private void enableDisableOutboundComponents() {
        outboundReplySpecifiedQueueField.setEnabled(outboundReplySpecifiedQueueRadioButton.isSelected());

        outboundCorrelationIdRadioButton.setEnabled(outboundReplyAutomaticRadioButton.isSelected() || outboundReplySpecifiedQueueRadioButton.isSelected());
        outboundMessageIdRadioButton.setEnabled(outboundReplyAutomaticRadioButton.isSelected() || outboundReplySpecifiedQueueRadioButton.isSelected());

        enableDisableSaveButton();
    }

    private void enableDisableInboundComponents() {
        useQueueForFailedCheckBox.setEnabled(JmsAcknowledgementType.ON_COMPLETION == ackBehaviourComboBox.getSelectedItem());
        failureQueueLabel.setEnabled(JmsAcknowledgementType.ON_COMPLETION == ackBehaviourComboBox.getSelectedItem());
        failureQueueNameTextField.setEnabled(JmsAcknowledgementType.ON_COMPLETION == ackBehaviourComboBox.getSelectedItem());

        inboundReplySpecifiedQueueField.setEnabled(sendReplyToSpecifiedRadioButton.isSelected());

        inboundCorrelationIdRadioButton.setEnabled(automaticSendReplyIfRadioButton.isSelected() || sendReplyToSpecifiedRadioButton.isSelected());
        inboundMessageIdRadioButton.setEnabled(automaticSendReplyIfRadioButton.isSelected() || sendReplyToSpecifiedRadioButton.isSelected());

        serviceNameLabel.setEnabled(associateQueueWithPublishedService.isSelected());
        serviceNameCombo.setEnabled(associateQueueWithPublishedService.isSelected());

        specifyContentTypeFreeForm.setEnabled(specifyContentTypeCheckBox.isSelected());
        contentTypeValues.setEnabled(specifyContentTypeCheckBox.isSelected());
        getContentTypeFromRadioButton.setEnabled(specifyContentTypeCheckBox.isSelected());
        getContentTypeFromProperty.setEnabled(specifyContentTypeCheckBox.isSelected());

        enableDisableSaveButton();
    }

    public void updateView(AMQPDestination destination) {
        loadContentTypesModel();

        destinationNameField.setText(destination.getName() == null ? "" : destination.getName());
        outboundRadioButton.setSelected(!destination.isInbound());
        inboundRadioButton.setSelected(destination.isInbound());
        if (!destination.isInbound()) {
            tabbedPane.setEnabledAt(1, false);
            tabbedPane.setEnabledAt(2, true);
        } else {
            tabbedPane.setEnabledAt(1, true);
            tabbedPane.setEnabledAt(2, false);
        }

        virtualHostField.setText(destination.getVirtualHost() == null ? "" : destination.getVirtualHost());
        addressesListModel.clear();
        for (String address : destination.getAddresses()) {
            addressesListModel.addElement(address);
        }
        enableDisableAddressButtons();

        credentialsCheckBox.setSelected(destination.isCredentialsRequired());
        if (destination.isCredentialsRequired()) {
            usernameField.setText(destination.getUsername() == null ? "" : destination.getUsername());
            if (destination.getPasswordGoid() != null) {
                passwordComboBox.setSelectedSecurePassword(destination.getPasswordGoid());
            }
        }
        enableDisableCredentialComponents();

        enableSSLCheckBox.setSelected(destination.isUseSsl());
        if (destination.isUseSsl()) {
            if (destination.getCipherSpec() != null) {
                cipherSpecComboBox.setSelectedItem(destination.getCipherSpec());
            }

            useClientAuthenticationCheckBox.setSelected(destination.getSslClientKeyId() != null);
            if (destination.getSslClientKeyId() != null) {
                if (destination.getSslClientKeyId().equals("-1:")) {
                    keystoreComboBox.selectDefaultSsl();
                } else {
                    String[] parts = destination.getSslClientKeyId().split(":");
                    Goid keystoreGoid = new Goid(parts[0]);
                    String alias = parts[1];
                    keystoreComboBox.select(keystoreGoid, alias);
                }
            }
        }
        enableDisableSslComponents();

        automaticSendReplyIfRadioButton.setSelected(true);
        inboundCorrelationIdRadioButton.setSelected(true);
        specifyContentTypeCheckBox.setSelected(true);
        outboundReplyAutomaticRadioButton.setSelected(true);
        outboundCorrelationIdRadioButton.setSelected(true);

        // Populate the inbound service dropdown in case its needed.
        ServiceComboBox.populateAndSelect(serviceNameCombo, true, destination.getServiceGoid() == null ? new Goid(0, -1) : destination.getServiceGoid());

        if (destination.isInbound()) {
            queueNameField.setText(destination.getQueueName() == null ? "" : destination.getQueueName());
            threadPoolSizeField.setValue(destination.getThreadPoolSize());
            if (destination.getAcknowledgementType() != null) {
                ackBehaviourComboBox.setSelectedItem(destination.getAcknowledgementType());
            }

            automaticSendReplyIfRadioButton.setSelected(AMQPDestination.InboundReplyBehaviour.AUTOMATIC == destination.getInboundReplyBehaviour());
            doNotSendRepliesRadioButton.setSelected(AMQPDestination.InboundReplyBehaviour.ONE_WAY == destination.getInboundReplyBehaviour());
            sendReplyToSpecifiedRadioButton.setSelected(AMQPDestination.InboundReplyBehaviour.SPECIFIED_QUEUE == destination.getInboundReplyBehaviour());
            if (AMQPDestination.InboundReplyBehaviour.SPECIFIED_QUEUE == destination.getInboundReplyBehaviour()) {
                inboundReplySpecifiedQueueField.setText(destination.getInboundReplyQueue() == null ? "" : destination.getInboundReplyQueue());
            }

            inboundCorrelationIdRadioButton.setSelected(AMQPDestination.InboundCorrelationBehaviour.CORRELATION_ID == destination.getInboundCorrelationBehaviour());
            inboundMessageIdRadioButton.setSelected(AMQPDestination.InboundCorrelationBehaviour.MESSAGE_ID == destination.getInboundCorrelationBehaviour());

            associateQueueWithPublishedService.setSelected(destination.getServiceGoid() != null);

            specifyContentTypeCheckBox.setSelected(destination.getContentTypeValue() != null || destination.getContentTypePropertyName() != null);
            if (destination.getContentTypeValue() != null) {
                try {
                    //determine if the content type is a manually entered one, if so, we'll display this content type
                    //value in the editable box
                    ContentTypeHeader ctHeader = ContentTypeHeader.parseValue(destination.getContentTypeValue());
                    if (findContentTypeInList(ctHeader) != -1) {
                        contentTypeValues.setSelectedItem(new ContentTypeComboBoxItem(ctHeader));
                    } else {
                        contentTypeValues.setSelectedItem(null);
                        contentTypeValues.getEditor().setItem(new ContentTypeComboBoxItem(ctHeader));
                    }

                    specifyContentTypeFreeForm.setSelected(true);
                } catch (IOException e1) {
                    logger.log(Level.WARNING,
                            MessageFormat.format("Error while parsing the Content-Type for AMQP Destination {0}. Value was {1}", destination.getName(), destination.getContentTypeValue()),
                            ExceptionUtils.getMessage(e1));
                }
            } else if (destination.getContentTypePropertyName() != null) {
                getContentTypeFromRadioButton.setSelected(true);
                getContentTypeFromProperty.setText(destination.getContentTypePropertyName());
            }

            useQueueForFailedCheckBox.setSelected(destination.getFailureQueueName() != null);
            if (destination.getFailureQueueName() != null) {
                failureQueueNameTextField.setText(destination.getFailureQueueName());
            }

            disableListeningTheQueueCheckBox.setSelected(!destination.isEnabled());

            enableDisableInboundComponents();
        } else if (!destination.isInbound()) {
            exchangeNameField.setText(destination.getExchangeName() == null ? "" : destination.getExchangeName());

            outboundReplyAutomaticRadioButton.setSelected(AMQPDestination.OutboundReplyBehaviour.TEMPORARY_QUEUE == destination.getOutboundReplyBehaviour());
            outboundReplyNoneRadioButton.setSelected(AMQPDestination.OutboundReplyBehaviour.ONE_WAY == destination.getOutboundReplyBehaviour());
            outboundReplySpecifiedQueueRadioButton.setSelected(AMQPDestination.OutboundReplyBehaviour.SPECIFIED_QUEUE == destination.getOutboundReplyBehaviour());
            if (AMQPDestination.OutboundReplyBehaviour.SPECIFIED_QUEUE == destination.getOutboundReplyBehaviour()) {
                outboundReplySpecifiedQueueField.setText(destination.getResponseQueue() == null ? "" : destination.getResponseQueue());
            }

            if (AMQPDestination.OutboundReplyBehaviour.TEMPORARY_QUEUE == destination.getOutboundReplyBehaviour() ||
                    AMQPDestination.OutboundReplyBehaviour.SPECIFIED_QUEUE == destination.getOutboundReplyBehaviour()) {
                outboundCorrelationIdRadioButton.setSelected(AMQPDestination.OutboundCorrelationBehaviour.GENERATE_CORRELATION_ID == destination.getOutboundCorrelationBehaviour());
                outboundMessageIdRadioButton.setSelected(AMQPDestination.OutboundCorrelationBehaviour.USE_MESSAGE_ID == destination.getOutboundCorrelationBehaviour());
            }

            enableDisableOutboundComponents();
        }
    }

    /**
     * Finds the index of the ContentTypeComboBoxItem from the model that matches to the specified content type header
     *
     * @param ctHeader The content type header
     * @return -1 if not found in the list, otherwise the index location of the found match.
     */
    private int findContentTypeInList(ContentTypeHeader ctHeader) {
        for (int i = 0; i < contentTypeModel.getSize(); i++) {
            ContentTypeComboBoxItem contentTypeItem = (ContentTypeComboBoxItem) contentTypeModel.getElementAt(i);
            if (ctHeader.equals(contentTypeItem.getContentType())) {
                return i;
            }
        }
        return -1;
    }

    public void updateModel(AMQPDestination destination) {
        destination.setName(destinationNameField.getText().trim());

        if (outboundRadioButton.isSelected()) {
            destination.setInbound(false);

            destination.setExchangeName(exchangeNameField.getText().trim());

            if (outboundReplyAutomaticRadioButton.isSelected()) {
                destination.setOutboundReplyBehaviour(AMQPDestination.OutboundReplyBehaviour.TEMPORARY_QUEUE);
            } else if (outboundReplyNoneRadioButton.isSelected()) {
                destination.setOutboundReplyBehaviour(AMQPDestination.OutboundReplyBehaviour.ONE_WAY);
            } else if (outboundReplySpecifiedQueueRadioButton.isSelected()) {
                destination.setOutboundReplyBehaviour(AMQPDestination.OutboundReplyBehaviour.SPECIFIED_QUEUE);
                destination.setResponseQueue(outboundReplySpecifiedQueueField.getText().trim());
            }

            if (outboundReplyAutomaticRadioButton.isSelected() || outboundReplySpecifiedQueueRadioButton.isSelected()) {
                if (outboundCorrelationIdRadioButton.isSelected()) {
                    destination.setOutboundCorrelationBehaviour(AMQPDestination.OutboundCorrelationBehaviour.GENERATE_CORRELATION_ID);
                } else if (outboundMessageIdRadioButton.isSelected()) {
                    destination.setOutboundCorrelationBehaviour(AMQPDestination.OutboundCorrelationBehaviour.USE_MESSAGE_ID);
                }
            } else {
                destination.setOutboundCorrelationBehaviour(null);
            }
        } else if (inboundRadioButton.isSelected()) {
            destination.setInbound(true);

            destination.setQueueName(queueNameField.getText().trim());
            destination.setThreadPoolSize(((Number) threadPoolSizeField.getValue()).intValue());
            destination.setAcknowledgementType((JmsAcknowledgementType) ackBehaviourComboBox.getSelectedItem());

            if (automaticSendReplyIfRadioButton.isSelected()) {
                destination.setInboundReplyBehaviour(AMQPDestination.InboundReplyBehaviour.AUTOMATIC);
            } else if (doNotSendRepliesRadioButton.isSelected()) {
                destination.setInboundReplyBehaviour(AMQPDestination.InboundReplyBehaviour.ONE_WAY);
            } else if (sendReplyToSpecifiedRadioButton.isSelected()) {
                destination.setInboundReplyBehaviour(AMQPDestination.InboundReplyBehaviour.SPECIFIED_QUEUE);
                destination.setInboundReplyQueue(inboundReplySpecifiedQueueField.getText().trim());
            }

            if (inboundCorrelationIdRadioButton.isSelected()) {
                destination.setInboundCorrelationBehaviour(AMQPDestination.InboundCorrelationBehaviour.CORRELATION_ID);
            } else if (inboundMessageIdRadioButton.isSelected()) {
                destination.setInboundCorrelationBehaviour(AMQPDestination.InboundCorrelationBehaviour.MESSAGE_ID);
            }

            if (associateQueueWithPublishedService.isSelected()) {
                PublishedService svc = ServiceComboBox.getSelectedPublishedService(serviceNameCombo);
                if (svc != null) {
                    destination.setServiceGoid(svc.getGoid());
                } else {
                    destination.setServiceGoid(null);
                }
            } else {
                destination.setServiceGoid(null);
            }

            if (specifyContentTypeCheckBox.isSelected()) {
                if (specifyContentTypeCheckBox.isSelected()) {

                    try {
                        String contentTypeValue = contentTypeValues.getSelectedItem() == null ?
                                (ContentTypeHeader.parseValue((contentTypeValues.getEditor().getItem().toString()))).getFullValue() :
                                (ContentTypeHeader.parseValue((contentTypeValues.getSelectedItem().toString()))).getFullValue();
                        destination.setContentTypeValue(contentTypeValue);
                    } catch (IOException e) {
                        DialogDisplayer.showMessageDialog(this, null, e.getMessage(), null, null);
                        logger.warning(e.getMessage());
                    }
                    destination.setContentTypePropertyName(null);
                } else if (getContentTypeFromRadioButton.isSelected()) {
                    destination.setContentTypeValue(null);
                    destination.setContentTypePropertyName(getContentTypeFromProperty.getText().trim().isEmpty() ? null : getContentTypeFromProperty.getText().trim());
                }
            }

            if (useQueueForFailedCheckBox.isSelected()) {
                destination.setFailureQueueName(failureQueueNameTextField.getText().trim().isEmpty() ? null : failureQueueNameTextField.getText().trim());
            } else {
                destination.setFailureQueueName(null);
            }

            destination.setEnabled(!disableListeningTheQueueCheckBox.isSelected());
        }

        destination.setVirtualHost(virtualHostField.getText().trim().isEmpty() ? null : virtualHostField.getText().trim());
        String[] addresses = new String[addressesListModel.getSize()];
        for (int i = 0; i < addressesListModel.getSize(); i++) {
            addresses[i] = (String) addressesListModel.getElementAt(i);
        }
        destination.setAddresses(addresses);

        destination.setCredentialsRequired(credentialsCheckBox.isSelected());
        if (credentialsCheckBox.isSelected()) {
            destination.setUsername(usernameField.getText().trim().isEmpty() ? null : usernameField.getText().trim());
            destination.setPasswordGoid(passwordComboBox.getSelectedSecurePassword() == null ? null : passwordComboBox.getSelectedSecurePassword().getGoid());
        } else {
            destination.setUsername(null);
            destination.setPasswordGoid(null);
        }

        destination.setUseSsl(enableSSLCheckBox.isSelected());
        if (enableSSLCheckBox.isSelected()) {
            destination.setCipherSpec((String) cipherSpecComboBox.getSelectedItem());

            if (useClientAuthenticationCheckBox.isSelected() && keystoreComboBox.getSelectedItem() != null) {
                destination.setSslClientKeyId(keystoreComboBox.getSelectedKeystoreId() + ":" + (keystoreComboBox.getSelectedKeyAlias() == null ? "" : keystoreComboBox.getSelectedKeyAlias()));
            } else {
                destination.setSslClientKeyId(null);
            }
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private String[] getCipherSuites() {
        String[] suites = {"SSL_RSA_WITH_NULL_MD5", "SSL_RSA_WITH_NULL_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "SSL_RSA_WITH_RC4_128_MD5", "SSL_RSA_WITH_RC4_128_SHA", "SSL_RSA_EXPORT_WITH_RC2_CBC_40_MD5", "SSL_RSA_WITH_DES_CBC_SHA",
                "SSL_RSA_EXPORT1024_WITH_RC4_56_SHA", "SSL_RSA_EXPORT1024_WITH_DES_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
                "SSL_RSA_WITH_AES_128_CBC_SHA", "SSL_RSA_WITH_AES_256_CBC_SHA", "SSL_RSA_WITH_DES_CBC_SHA",
                "SSL_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_RSA_FIPS_WITH_DES_CBC_SHA", "SSL_RSA_FIPS_WITH_3DES_EDE_CBC_SHA"};

        return suites;
    }

    private class AMQPDocumentListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            enableDisableSaveButton();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            enableDisableSaveButton();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            enableDisableSaveButton();
        }
    }
}
