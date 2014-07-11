package com.l7tech.external.assertions.xmppassertion.console;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.xmppassertion.XMPPConnectionEntity;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 07/03/12
 * Time: 10:53 AM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPConnectionEntityDialog extends JDialog {

    protected static final Logger logger = Logger.getLogger(XMPPConnectionEntityDialog.class.getName());

    private JPanel mainPanel;
    private JTextField nameField;
    private JComboBox directionComboBox;
    private JPanel inboundSettingsPanel;
    private JSpinner inboundThreadpoolSizeField;
    private JComboBox bindAddressComboBox;
    private SquigglyTextField bindPortField;
    private JComboBox messageReceivedServiceComboBox;
    private JComboBox sessionTerminatedServiceComboBox;
    private JTextField contentTypeField;
    private JCheckBox enabledCheckBox;
    private JPanel outboundSettingsPanel;
    private JTextField hostnameField;
    private JSpinner portField;
    private JButton okButton;
    private JButton cancelButton;
    private JLabel contentTypeStatusLabel;
    private JCheckBox useLegacySSLCheckBox;

    private List<XMPPConnectionEntity> existingEntities;

    private boolean confirmed = false;

    private final ImageIcon OK_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Check16.png"));
    private final ImageIcon WARNING_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Warning16.png"));

    public XMPPConnectionEntityDialog(Frame owner, List<XMPPConnectionEntity> existingEntities, XMPPConnectionEntity entity) {
        super(owner, "XMPP Connection Properties", true);

        if (entity.getName() != null) {
            // This is editing of an existing entity. Remove current entity selected for edit
            // from the existingEntities list.
            //
            this.existingEntities = new LinkedList<XMPPConnectionEntity>();
            for (XMPPConnectionEntity existingEntity : existingEntities) {
                if (!existingEntity.getName().equals(entity.getName())) {
                    this.existingEntities.add(existingEntity);
                }
            }
        } else {
            // This is adding a new entity.
            //
            this.existingEntities = existingEntities;
        }

        initializeComponents();
        updateView(entity);
    }

    public XMPPConnectionEntityDialog(Dialog owner, List<XMPPConnectionEntity> existingEntities, XMPPConnectionEntity entity) {
        super(owner, "XMPP Connection Properties", true);

        if (entity.getName() != null) {
            // This is editing of an existing entity. Remove current entity selected for edit
            // from the existingEntities list.
            //
            this.existingEntities = new LinkedList<XMPPConnectionEntity>();
            for (XMPPConnectionEntity existingEntity : existingEntities) {
                if (!existingEntity.getName().equals(entity.getName())) {
                    this.existingEntities.add(existingEntity);
                }
            }
        } else {
            // This is adding a new entity.
            //
            this.existingEntities = existingEntities;
        }

        initializeComponents();
        updateView(entity);
    }

    private void initializeComponents() {
        InputValidator inputValidator = new InputValidator(this, "XMPP Connection Properties");
        inputValidator.attachToButton(okButton, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        nameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                enableDisableOKButton();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                enableDisableOKButton();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                enableDisableOKButton();
            }
        });

        directionComboBox.setModel(new DefaultComboBoxModel(new String[] {"Inbound", "Outbound"}));
        directionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if("Inbound".equals(directionComboBox.getSelectedItem())) {
                    inboundSettingsPanel.setVisible(true);
                    outboundSettingsPanel.setVisible(false);
                } else {
                    inboundSettingsPanel.setVisible(false);
                    outboundSettingsPanel.setVisible(true);
                }
                enableDisableOKButton();
            }
        });

        try {
            // @bug 12902
            // The ServiceHeader entries need to be sorted for display on the UI.
            // However, for one of the UI elements, "NONE" needs to be inserted at the top of the sorted list.
            // This will sort the serviceHeaders and return them in alphabetical order without implementing
            // Comparible in the ServiceHeader, and allows for a NONE insertion afterwards.
            // *** NOTE: This fix is used in ResolveForeignXMPPConnectionPanel.java to sort its services as well. ***
            ServiceHeader[] serviceHeaders = Registry.getDefault().getServiceManager().findAllPublishedServices();
            // Create a TreeMap that sorts automatically by keys.
            // Use the serviceHeader getDisplayName() as the key, with the ServiceHeader as it's object.
            TreeMap<String, ServiceHeader> sortedServiceNames = new TreeMap<String, ServiceHeader>();
            for (ServiceHeader currentService : serviceHeaders) {
                sortedServiceNames.put(currentService.getDisplayName(), currentService);
            }

            ArrayList<String> tempServiceList = new ArrayList<String>(sortedServiceNames.keySet());
            // Now that it's all sorted, create an ArrayList containing the Headers in CASE INSENSITIVE alphabetical order.
            Collections.sort(tempServiceList, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o1.compareToIgnoreCase(o2);
                }
            });

            ArrayList<ServiceHeader> sortedServices = new ArrayList<ServiceHeader>(sortedServiceNames.size()+1);
            for (String key : tempServiceList) {
                sortedServices.add(sortedServiceNames.get(key));
            }


            messageReceivedServiceComboBox.setModel(new DefaultComboBoxModel(sortedServices.toArray()));
            // Sort the serviceHeaders in the ComboBox
            sessionTerminatedServiceComboBox.setModel(new DefaultComboBoxModel(sortedServices.toArray()));
            sessionTerminatedServiceComboBox.insertItemAt("NONE", 0);
        } catch(FindException e) {
        }
        messageReceivedServiceComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableOKButton();
            }
        });
        sessionTerminatedServiceComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableDisableOKButton();
            }
        });

        contentTypeField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                enableDisableOKButton();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                enableDisableOKButton();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                enableDisableOKButton();
            }
        });

        inboundThreadpoolSizeField.setModel(new SpinnerNumberModel(10, 10, 100, 1));

        InetAddress[] addrs = Registry.getDefault().getTransportAdmin().getAvailableBindAddresses();
        java.util.List<String> entries = new ArrayList<String>();
        entries.add("0.0.0.0");
        for (InetAddress addr : addrs) {
            entries.add(addr.getHostAddress());
        }
        bindAddressComboBox.setModel(new DefaultComboBoxModel(entries.toArray(new String[entries.size()])));
        bindAddressComboBox.setSelectedIndex(0);

        // @bug 12925: Removes comma from bindPortField.
        // ALSO USED IN ResolveForeignXMPPConnectionPanel
        int maxlen = Math.max(Long.toString(1025L).length(), Long.toString(65535L).length());
        bindPortField.setDocument(new NumberField(maxlen + 1));
        inputValidator.validateWhenDocumentChanges(bindPortField);
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return isBindPortValid();
            }
        });

        hostnameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                enableDisableOKButton();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                enableDisableOKButton();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                enableDisableOKButton();
            }
        });

        // @bug 12925: Removes comma from portField, as above.
        portField.setModel(new SpinnerNumberModel(5222, 1, 65535, 1));
        portField.setEditor(new JSpinner.NumberEditor(portField, "#"));

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        directionComboBox.setSelectedItem("Inbound");
        inboundSettingsPanel.setVisible(true);
        outboundSettingsPanel.setVisible(false);
        
        setContentPane(mainPanel);
        getRootPane().setDefaultButton(okButton);
        pack();

        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.setEnterAction(this, okButton);
    }

    private void enableDisableOKButton() {
        boolean enableOKButton = true;
        
        if(nameField.getText().trim().isEmpty()) {
            enableOKButton = false;
        }

        if(messageReceivedServiceComboBox.getSelectedItem() == null) {
            enableOKButton = false;
        }
        if(sessionTerminatedServiceComboBox.getSelectedItem() == null) {
            enableOKButton = false;
        }
        try {
            ContentTypeHeader.parseValue(contentTypeField.getText().trim());
            contentTypeStatusLabel.setIcon(OK_ICON);
            contentTypeStatusLabel.setText("OK");
        } catch(IOException e) {
            contentTypeStatusLabel.setIcon(WARNING_ICON);
            contentTypeStatusLabel.setText("Incorrect syntax");
            enableOKButton = false;
        }

        if("Outbound".equals(directionComboBox.getSelectedItem())) {
            if(hostnameField.getText().trim().isEmpty()) {
                enableOKButton = false;
            }
        }

        okButton.setEnabled(enableOKButton);
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void updateView(XMPPConnectionEntity entity) {
        nameField.setText(entity.getName() == null ? "" : entity.getName());
        directionComboBox.setSelectedItem(entity.isInbound() ? "Inbound" : "Outbound");

        if(entity.getMessageReceivedServiceOid() != null) {
            for(int i = 0;i < messageReceivedServiceComboBox.getItemCount();i++) {
                ServiceHeader header = (ServiceHeader) messageReceivedServiceComboBox.getItemAt(i);
                if(entity.getMessageReceivedServiceOid().equals(header.getGoid())) {
                    messageReceivedServiceComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
        if(entity.getSessionTerminatedServiceOid() != null) {
            for(int i = 0;i < sessionTerminatedServiceComboBox.getItemCount();i++) {
                if(sessionTerminatedServiceComboBox.getItemAt(i).equals("NONE")){
                    continue;
                }
                ServiceHeader header = (ServiceHeader) sessionTerminatedServiceComboBox.getItemAt(i);
                if(entity.getSessionTerminatedServiceOid().equals(header.getGoid())) {
                    sessionTerminatedServiceComboBox.setSelectedIndex(i);
                    break;
                }
            }
        } else {
            sessionTerminatedServiceComboBox.setSelectedItem("NONE");
        }

        if(entity.getContentType() != null) {
            contentTypeField.setText(entity.getContentType());
        }

        useLegacySSLCheckBox.setSelected(entity.isLegacySsl());

        if(entity.isInbound()) {
            inboundThreadpoolSizeField.setValue(entity.getThreadpoolSize());
            if(entity.getBindAddress() != null) {
                for(int i = 0;i < bindAddressComboBox.getItemCount();i++) {
                    if(entity.getBindAddress().equals(bindAddressComboBox.getItemAt(i))) {
                        bindAddressComboBox.setSelectedIndex(i);
                        break;
                    }
                }
            }

            // @bug 12925: Remove commas from port field JSpinners.
            bindPortField.setText(Integer.toString(entity.getPort()));
            enabledCheckBox.setSelected(entity.isEnabled());
        } else {
            hostnameField.setText(entity.getHostname() == null ? "" : entity.getHostname());
            // @bug 12925: Remove commas from port field JSpinners.
            portField.getModel().setValue(entity.getPort());
        }
    }

    public void updateModel(XMPPConnectionEntity entity) {
        entity.setName(nameField.getText().trim());
        entity.setInbound("Inbound".equals(directionComboBox.getSelectedItem()));
        entity.setMessageReceivedServiceOid(((ServiceHeader) messageReceivedServiceComboBox.getSelectedItem()).getGoid());
        entity.setLegacySsl(useLegacySSLCheckBox.isSelected());
        
        if("NONE".equals(sessionTerminatedServiceComboBox.getSelectedItem())) {
            entity.setSessionTerminatedServiceOid(null);
        } else {
            entity.setSessionTerminatedServiceOid(((ServiceHeader) sessionTerminatedServiceComboBox.getSelectedItem()).getGoid());
        }

        entity.setContentType(contentTypeField.getText().trim());

        if(entity.isInbound()) {
            entity.setThreadpoolSize(((Number) inboundThreadpoolSizeField.getValue()).intValue());
            entity.setBindAddress((String) bindAddressComboBox.getSelectedItem());
            entity.setPort(Integer.parseInt(bindPortField.getText()));
            entity.setEnabled(enabledCheckBox.isSelected());

            entity.setHostname(null);
        } else {
            entity.setHostname(hostnameField.getText().trim());
            entity.setPort(((Number) portField.getValue()).intValue());

            entity.setThreadpoolSize(10);
            entity.setBindAddress(null);
            entity.setEnabled(false);
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Checks if bind port is valid.
     *
     * @return null, if port in valid. Error message is port is invalid.
     */
    private String isBindPortValid() {
        if ("Outbound".equals(directionComboBox.getSelectedItem())) {
            // The connection is Outbound. Skip validation.
            //
            return null;
        }

        if(bindPortField.getText().length() == 0) {
            return "The Bind Port cannot be empty.";
        }

        int port = Integer.parseInt(bindPortField.getText());

        if(port < 1025 || port > 65535) {
            return "The Bind Port must be a number between 1025 and 65535.";
        }

        // No need to check that port is not system reserved port. This is enforced by the Spinner.
        //

        // Check that port is not already in use by existing XMPP inbound listeners.
        //
        for (XMPPConnectionEntity existingEntity : existingEntities) {
            if (existingEntity.isEnabled() &&
                existingEntity.isInbound() &&
                existingEntity.getPort() == port) {
                return "The Bind Port is already in use.";
            }
        }

        // Check that port is not already in use by the SSG.
        //
        try {
            Collection<SsgConnector> connectors = Registry.getDefault().getTransportAdmin().findAllSsgConnectors();
            for (SsgConnector ssgConnector : connectors) {
                if (ssgConnector.getPort() == port) {
                    return "The Bind Port is already in use.";
                }
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, "SsgConnectors are unavailable", e);
            return null;
        }

        // The port in valid.
        //
        return null;
    }
}