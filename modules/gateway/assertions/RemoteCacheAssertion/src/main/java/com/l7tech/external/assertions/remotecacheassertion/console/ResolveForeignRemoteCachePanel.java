package com.l7tech.external.assertions.remotecacheassertion.console;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntityAdmin;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheReference;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheTypes;
import com.l7tech.external.assertions.remotecacheassertion.server.*;
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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
public class ResolveForeignRemoteCachePanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(ResolveForeignRemoteCachePanel.class.getName());

    private JPanel mainPanel;
    private JTextField nameField;
    private JLabel hostnameLabel;
    private JTextField hostnameField;
    private JComboBox cacheTypeComboBox;
    private JRadioButton changeRadioButton;
    private JComboBox updateAssertionsRemoteCacheConnectionComboBox;
    private JRadioButton removeRadioButton;
    private JRadioButton ignoreRadioButton;
    private JButton createRemoteCacheConnectionButton;

    private RemoteCacheReference foreignRef;

    public ResolveForeignRemoteCachePanel(WizardStepPanel next, RemoteCacheReference foreignRef) {
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
        return "Unresolved Remote Cache Connection " + foreignRef.getName();
    }

    @Override
    public boolean onNextButton() {
        if (changeRadioButton.isSelected()) {
            if (updateAssertionsRemoteCacheConnectionComboBox.getSelectedIndex() < 0) return false;

            final RemoteCacheEntity connector = (RemoteCacheEntity) updateAssertionsRemoteCacheConnectionComboBox.getSelectedItem();
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

        InetAddress[] addrs = Registry.getDefault().getTransportAdmin().getAvailableBindAddresses();
        java.util.List<String> entries = new ArrayList<String>();
        entries.add("0.0.0.0");
        for (InetAddress addr : addrs) {
            entries.add(addr.getHostAddress());
        }

        RemoteCacheTypes cacheType = RemoteCacheTypes.getEntityEnumType(foreignRef.getType());
        switch (cacheType) {
            case Memcached:
                hostnameField.setText(foreignRef.getProperties().get(MemcachedRemoteCache.PROP_SERVERPORTS));
                break;
            case Terracotta:
                hostnameField.setText(foreignRef.getProperties().get(TerracottaRemoteCache.PROPERTY_URLS));
                break;
            case Coherence:
                hostnameField.setText(foreignRef.getProperties().get(CoherenceRemoteCache.PROP_SERVERS));
                break;
            case GemFire:
                hostnameField.setText(foreignRef.getProperties().get(GemfireRemoteCache.PROPERTY_SERVERS));
                break;
            case Redis:
                hostnameField.setText(foreignRef.getProperties().get(RedisRemoteCache.PROPERTY_SERVERS));
                break;

            default:
                break;
        }

        String[] cacheTypesArray = RemoteCacheTypes.getEntityTypes();
        cacheTypeComboBox.setModel(new DefaultComboBoxModel(cacheTypesArray));


        cacheTypeComboBox.setSelectedItem(foreignRef.getType());


        // default is delete
        removeRadioButton.setSelected(true);
        updateAssertionsRemoteCacheConnectionComboBox.setEnabled(false);

        // enable/disable provider selector as per action type selected
        changeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAssertionsRemoteCacheConnectionComboBox.setEnabled(true);
            }
        });
        removeRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAssertionsRemoteCacheConnectionComboBox.setEnabled(false);
            }
        });
        ignoreRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateAssertionsRemoteCacheConnectionComboBox.setEnabled(false);
            }
        });

        updateAssertionsRemoteCacheConnectionComboBox.setRenderer(new TextListCellRenderer<RemoteCacheEntity>(new Functions.Unary<String, RemoteCacheEntity>() {
            @Override
            public String call(final RemoteCacheEntity ssgActiveConnector) {
                return getConnectionInfo(ssgActiveConnector);
            }
        }));

        createRemoteCacheConnectionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                createRemoteCacheConnection();
            }
        });

        populateConnectionComboBox();
        enableAndDisableComponents();
    }

    private void createRemoteCacheConnection() {
        final RemoteCacheEntity newConnection = new RemoteCacheEntity();
        newConnection.setName(foreignRef.getName());
        newConnection.setProperties(foreignRef.getProperties());
        newConnection.setType(foreignRef.getType());
        newConnection.setTimeout(foreignRef.getTimeout());
        newConnection.setEnabled(foreignRef.isEnabled());
        EntityUtils.resetIdentity(newConnection);
        editAndSave(newConnection);
    }

    private void editAndSave(final RemoteCacheEntity connection) {

        final CacheServerDialog dialog = new CacheServerDialog(this.getOwner(), connection);
        dialog.setVisible(true);
        Utilities.centerOnParent(dialog);
        if (dialog.isConfirmed()) {
            try {
                dialog.getData(connection);
                Goid savedGoid = getEntityManager().save(connection);
                populateConnectionComboBox();

                for (int i = 0; i < updateAssertionsRemoteCacheConnectionComboBox.getItemCount(); i++) {
                    RemoteCacheEntity entity = (RemoteCacheEntity) updateAssertionsRemoteCacheConnectionComboBox.getItemAt(i);
                    if (savedGoid.equals(entity.getGoid())) {
                        updateAssertionsRemoteCacheConnectionComboBox.setSelectedIndex(i);
                        break;
                    }
                }
                changeRadioButton.setEnabled(true);
                changeRadioButton.setSelected(true);
                updateAssertionsRemoteCacheConnectionComboBox.setEnabled(true);
            } catch (SaveException e) {
                logger.log(Level.INFO, "Failed to save the new Remote Cache connection.", e);
            } catch (UpdateException e) {
                logger.log(Level.INFO, "Failed to update the Remote Cache connection.", e);
            }
        }
    }

    private static RemoteCacheEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(RemoteCacheEntityAdmin.class, null);
    }

    private void populateConnectionComboBox() {
        RemoteCacheEntityAdmin admin = getEntityManager();
        if (admin == null) return;

        final Object selectedItem = updateAssertionsRemoteCacheConnectionComboBox.getSelectedItem();
        final Collection<RemoteCacheEntity> connections = findAllRemoteCacheConnections();

        // Sort connectors by combination name
        Collections.sort((java.util.List<RemoteCacheEntity>) connections, new Comparator<RemoteCacheEntity>() {
            @Override
            public int compare(RemoteCacheEntity o1, RemoteCacheEntity o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        // Add all items into the combo box.
        updateAssertionsRemoteCacheConnectionComboBox.setModel(Utilities.comboBoxModel(connections));

        if (selectedItem != null && updateAssertionsRemoteCacheConnectionComboBox.getModel().getSize() > 0) {
            updateAssertionsRemoteCacheConnectionComboBox.setSelectedItem(selectedItem);
            if (updateAssertionsRemoteCacheConnectionComboBox.getSelectedIndex() == -1) {
                updateAssertionsRemoteCacheConnectionComboBox.setSelectedIndex(0);
            }
        }
    }

    private java.util.List<RemoteCacheEntity> findAllRemoteCacheConnections() {
        try {
            final RemoteCacheEntityAdmin admin = getEntityManager();
            ArrayList<RemoteCacheEntity> entities = new ArrayList<RemoteCacheEntity>();
            for (RemoteCacheEntity entity : admin.findAll()) {
                entities.add(entity);
            }
            return entities;
        } catch (IllegalStateException e) {
            // no admin context available
            logger.info("Unable to access queues from server.");
        } catch (FindException e) {
            ErrorManager.getDefault().notify(Level.WARNING, e, "Error loading queues");
        }
        return emptyList();
    }

    private void enableAndDisableComponents() {
        final boolean enableSelection = updateAssertionsRemoteCacheConnectionComboBox.getModel().getSize() > 0;
        changeRadioButton.setEnabled(enableSelection);

        if (!changeRadioButton.isEnabled() && changeRadioButton.isSelected()) {
            removeRadioButton.setSelected(true);
        }
    }

    private String getConnectionInfo(final RemoteCacheEntity connection) {
        final StringBuilder builder = new StringBuilder();
        builder.append(connection.getName());
        return builder.toString();
    }
}
