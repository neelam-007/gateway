package com.l7tech.external.assertions.xmppassertion.console;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.xmppassertion.XMPPConnectionEntity;
import com.l7tech.external.assertions.xmppassertion.XMPPConnectionEntityAdmin;
import com.l7tech.external.assertions.xmppassertion.XMPPConnectionReference;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Collections.emptyList;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 20/03/12
 * Time: 4:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResolveForeignXMPPConnectionPanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(ResolveForeignXMPPConnectionPanel.class.getName());

    private JPanel mainPanel;
    private JTextField nameField;
    private JComboBox directionComboBox;
    private JLabel threadpoolSizeLabel;
    private JSpinner threadpoolSizeField;
    private JLabel bindAddressLabel;
    private JComboBox bindAddressComboBox;
    private JLabel hostnameLabel;
    private JTextField hostnameField;
    private JSpinner portField;
    private JLabel enabledLabel;
    private JPanel enabledPanel;
    private JComboBox enabledComboBox;
    private JComboBox messageReceivedServiceComboBox;
    private JComboBox sessionTerminatedServiceComboBox;
    private JTextField contentTypeField;
    private JRadioButton changeRadioButton;
    private JComboBox updateAssertionsXMPPConnectionComboBox;
    private JRadioButton removeRadioButton;
    private JRadioButton ignoreRadioButton;
    private JButton createXMPPConnectionButton;

    private XMPPConnectionReference foreignRef;

    public ResolveForeignXMPPConnectionPanel(WizardStepPanel next, XMPPConnectionReference foreignRef) {
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
        return "Unresolved XMPP Connection " + foreignRef.getName();
    }

    @Override
    public boolean onNextButton() {
        if (changeRadioButton.isSelected()) {
            if (updateAssertionsXMPPConnectionComboBox.getSelectedIndex() < 0) return false;

            final XMPPConnectionEntity connector = (XMPPConnectionEntity)updateAssertionsXMPPConnectionComboBox.getSelectedItem();
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
        populateConnectionComboBox();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        nameField.setText(foreignRef.getName());
        
        directionComboBox.setModel(new DefaultComboBoxModel(new String[] {"Inbound", "Outbound"}));
        directionComboBox.setSelectedIndex(foreignRef.isInbound() ? 0 : 1);

        threadpoolSizeLabel.setVisible(directionComboBox.getSelectedIndex() == 0);
        threadpoolSizeField.setVisible(directionComboBox.getSelectedIndex() == 0);
        bindAddressLabel.setVisible(directionComboBox.getSelectedIndex() == 0);
        bindAddressComboBox.setVisible(directionComboBox.getSelectedIndex() == 0);
        hostnameLabel.setVisible(directionComboBox.getSelectedIndex() == 1);
        hostnameField.setVisible(directionComboBox.getSelectedIndex() == 1);
        enabledLabel.setVisible(directionComboBox.getSelectedIndex() == 0);
        enabledComboBox.setVisible(directionComboBox.getSelectedIndex() == 0);
        enabledPanel.setVisible(directionComboBox.getSelectedIndex() == 0);
        
        threadpoolSizeField.setModel(new SpinnerNumberModel(
                (foreignRef.getThreadPoolSize() > 0 ? foreignRef.getThreadPoolSize() : 1), 1, 100, 1));

        InetAddress[] addrs = Registry.getDefault().getTransportAdmin().getAvailableBindAddresses();
        java.util.List<String> entries = new ArrayList<String>();
        entries.add("0.0.0.0");
        for (InetAddress addr : addrs) {
            entries.add(addr.getHostAddress());
        }
        bindAddressComboBox.setModel(new DefaultComboBoxModel(entries.toArray(new String[entries.size()])));
        bindAddressComboBox.setSelectedIndex(0);
        for(int i = 0;i < bindAddressComboBox.getItemCount();i++) {
            String hostname = (String)bindAddressComboBox.getItemAt(i);
            if(hostname.equals(foreignRef.getBindAddress())) {
                bindAddressComboBox.setSelectedIndex(i);
                break;
            }
        }

        hostnameField.setText(foreignRef.getHostname() == null ? "" : foreignRef.getHostname());

        // @bug 12925: Removes comma from bindPortField.
        // ALSO USED IN XMPPConnectionEntityDialog
        portField.setModel(new SpinnerNumberModel((foreignRef.getPort() >= 1024 ? foreignRef.getPort() : 1024), 1024, 65535, 1));
        portField.setEditor(new JSpinner.NumberEditor(portField, "#"));

        enabledComboBox.setModel(new DefaultComboBoxModel(new String[] {"Yes", "No"}));
        enabledComboBox.setSelectedIndex(foreignRef.isEnabled() ? 0 : 1);

        try {
            // Related to @bug 12902.
            // This list of ServiceHeader is not sorted either.
            // Using the bugfix from XMPPConnectionEntityDialog.java to sort the list.

            // @bug 12902
            // The ServiceHeader entries need to be sorted for display on the UI.
            // However, for one of the UI elements, "NONE" needs to be inserted at the top of the sorted list.
            // This will sort the serviceHeaders and return them in alphabetical order without implementing
            // Comparible in the ServiceHeader, and allows for a NONE insertion afterwards.
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
            sessionTerminatedServiceComboBox.setModel(new DefaultComboBoxModel(sortedServices.toArray()));
            messageReceivedServiceComboBox.insertItemAt("NONE", 0);
            sessionTerminatedServiceComboBox.insertItemAt("NONE", 0);
        } catch(FindException e) {
        }
        messageReceivedServiceComboBox.setSelectedIndex(0);
        for(int i = 1;i < messageReceivedServiceComboBox.getItemCount();i++) {
            ServiceHeader serviceHeader = (ServiceHeader)messageReceivedServiceComboBox.getItemAt(i);
            if(serviceHeader.getGoid().equals(foreignRef.getMessageReceivedServiceGoid())) {
                messageReceivedServiceComboBox.setSelectedIndex(i);
                break;
            }
        }
        sessionTerminatedServiceComboBox.setSelectedIndex(0);
        for(int i = 1;i < sessionTerminatedServiceComboBox.getItemCount();i++) {
            ServiceHeader serviceHeader = (ServiceHeader)sessionTerminatedServiceComboBox.getItemAt(i);
            if(serviceHeader.getGoid().equals(foreignRef.getSessionTerminatedServiceGoid())) {
                sessionTerminatedServiceComboBox.setSelectedIndex(i);
                break;
            }
        }
        
        contentTypeField.setText(foreignRef.getContentType() == null ? "" : foreignRef.getContentType());

        directionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                threadpoolSizeLabel.setVisible(directionComboBox.getSelectedIndex() == 0);
                threadpoolSizeField.setVisible(directionComboBox.getSelectedIndex() == 0);
                bindAddressLabel.setVisible(directionComboBox.getSelectedIndex() == 0);
                bindAddressComboBox.setVisible(directionComboBox.getSelectedIndex() == 0);
                hostnameLabel.setVisible(directionComboBox.getSelectedIndex() == 1);
                hostnameField.setVisible(directionComboBox.getSelectedIndex() == 1);
                enabledLabel.setVisible(directionComboBox.getSelectedIndex() == 0);
                enabledComboBox.setVisible(directionComboBox.getSelectedIndex() == 0);
                enabledPanel.setVisible(directionComboBox.getSelectedIndex() == 0);
            }
        });
        directionComboBox.setEnabled(false); // Possibly in the future there will be assertions that depend on inbound connections

        // default is delete
        removeRadioButton.setSelected(true);
        updateAssertionsXMPPConnectionComboBox.setEnabled(false);

        // enable/disable provider selector as per action type selected
        changeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAssertionsXMPPConnectionComboBox.setEnabled(true);
            }
        });
        removeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAssertionsXMPPConnectionComboBox.setEnabled(false);
            }
        });
        ignoreRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAssertionsXMPPConnectionComboBox.setEnabled(false);
            }
        });

        updateAssertionsXMPPConnectionComboBox.setRenderer(new TextListCellRenderer<XMPPConnectionEntity>(new Functions.Unary<String, XMPPConnectionEntity>() {
            @Override
            public String call( final XMPPConnectionEntity ssgActiveConnector ) {
                return getConnectionInfo(ssgActiveConnector);
            }
        }));

        createXMPPConnectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                createXMPPConnection();
            }
        });

        populateConnectionComboBox();
        enableAndDisableComponents();
    }

    private void createXMPPConnection() {
        final XMPPConnectionEntity newConnection = new XMPPConnectionEntity();
        newConnection.setName(foreignRef.getName());
        newConnection.setInbound(foreignRef.isInbound());
        newConnection.setThreadpoolSize(foreignRef.getThreadPoolSize());
        newConnection.setBindAddress(foreignRef.getBindAddress());
        newConnection.setEnabled(foreignRef.isEnabled());
        newConnection.setHostname(foreignRef.getHostname());
        newConnection.setPort(foreignRef.getPort());
        newConnection.setMessageReceivedServiceOid(foreignRef.getMessageReceivedServiceGoid());
        newConnection.setSessionTerminatedServiceOid(foreignRef.getSessionTerminatedServiceGoid());
        newConnection.setContentType(foreignRef.getContentType());

        EntityUtils.resetIdentity(newConnection);
        editAndSave(newConnection);
    }

    private void editAndSave(final XMPPConnectionEntity connection) {
        final XMPPConnectionEntityDialog dialog = new XMPPConnectionEntityDialog(TopComponents.getInstance().getTopParent(), Collections.<XMPPConnectionEntity>emptyList(), connection);
        Utilities.centerOnScreen(dialog);

        dialog.setVisible(true);
        if(dialog.isConfirmed()) {
            try {
                dialog.updateModel(connection);
                getEntityManager().save(connection);
                
                if(connection.isInbound()) {
                    return; // They changed it to inbound, which cannot be referenced from an assertion
                }
                        
                populateConnectionComboBox();

                for(int i = 0;i < updateAssertionsXMPPConnectionComboBox.getItemCount();i++) {
                    XMPPConnectionEntity entity = (XMPPConnectionEntity)updateAssertionsXMPPConnectionComboBox.getItemAt(i);
                    if(connection.getGoid().equals(entity.getGoid())) {
                        updateAssertionsXMPPConnectionComboBox.setSelectedIndex(i);
                        break;
                    }
                }

                changeRadioButton.setEnabled(true);
                changeRadioButton.setSelected(true);
                updateAssertionsXMPPConnectionComboBox.setEnabled(true);
            } catch(SaveException e) {
                logger.log(Level.INFO, "Failed to save the new XMPP connection.", e);
            } catch(UpdateException e) {
                logger.log(Level.INFO, "Failed to save the new XMPP connection.", e);
            }
        }
    }

    private static XMPPConnectionEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(XMPPConnectionEntityAdmin.class, null);
    }

    private void populateConnectionComboBox() {
        XMPPConnectionEntityAdmin admin = getEntityManager();
        if (admin == null) return;

        final Object selectedItem = updateAssertionsXMPPConnectionComboBox.getSelectedItem();
        final Collection<XMPPConnectionEntity> connections = findAllOutboundXMPPConnections();

        // Sort connectors by combination name
        Collections.sort((java.util.List<XMPPConnectionEntity>) connections, new Comparator<XMPPConnectionEntity>() {
            @Override
            public int compare(XMPPConnectionEntity o1, XMPPConnectionEntity o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        // Add all items into the combo box.
        updateAssertionsXMPPConnectionComboBox.setModel(Utilities.comboBoxModel(connections));

        if (selectedItem != null && updateAssertionsXMPPConnectionComboBox.getModel().getSize() > 0) {
            updateAssertionsXMPPConnectionComboBox.setSelectedItem(selectedItem);
            if (updateAssertionsXMPPConnectionComboBox.getSelectedIndex() == -1) {
                updateAssertionsXMPPConnectionComboBox.setSelectedIndex(0);
            }
        }
    }

    private java.util.List<XMPPConnectionEntity> findAllOutboundXMPPConnections() {
        try {
            final XMPPConnectionEntityAdmin admin = getEntityManager();
            ArrayList<XMPPConnectionEntity> entities = new ArrayList<XMPPConnectionEntity>();
            for(XMPPConnectionEntity entity : admin.findAll()) {
                if(!entity.isInbound()) {
                    entities.add(entity);
                }
            }

            return entities;
        } catch ( IllegalStateException e ) {
            // no admin context available
            logger.info( "Unable to access queues from server." );
        } catch ( FindException e ) {
            ErrorManager.getDefault().notify( Level.WARNING, e, "Error loading queues" );
        }
        return emptyList();
    }

    private void enableAndDisableComponents() {
        final boolean enableSelection = updateAssertionsXMPPConnectionComboBox.getModel().getSize() > 0;
        changeRadioButton.setEnabled( enableSelection );

        if ( !changeRadioButton.isEnabled() && changeRadioButton.isSelected() ) {
            removeRadioButton.setSelected( true );
        }
    }

    private String getConnectionInfo( final XMPPConnectionEntity connection ) {
        final StringBuilder builder = new StringBuilder();
        builder.append(connection.getName());
        builder.append(" [");
        builder.append(connection.getHostname());
        builder.append(':');
        builder.append(connection.getPort());
        builder.append(']');
        return builder.toString();
    }
}
