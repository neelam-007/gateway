package com.l7tech.console.panels;

import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.exporter.JMSEndpointReference;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

/**
 * This wizard panel is used by the ResolveExternalPolicyReferencesWizard when
 * imported assertions are referring to a JMS queue that does not exist locally
 * and that cannot be automatically resolved.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 27, 2004<br/>
 */
public class ResolveForeignJMSEndpointPanel extends WizardStepPanel {
    public ResolveForeignJMSEndpointPanel(WizardStepPanel next, JMSEndpointReference foreignRef) {
        super(next);
        this.foreignRef = foreignRef;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);
        // Show details of the unresolved reference
        jndiUrlTxtField.setText(foreignRef.getJndiUrl());
        initialContextFactoryTxtField.setText(foreignRef.getInitialContextFactoryClassname());
        queueFactoryUrlTxtField.setText(foreignRef.getQueueFactoryUrl());

        // group the radios
        actionRadios = new ButtonGroup();
        actionRadios.add(changeRadio);
        actionRadios.add(deleteRadio);
        actionRadios.add(ignoreRadio);
        // default is delete
        deleteRadio.setSelected(true);
        queueSelector.setEnabled(false);

        // offer the list of existing jms endpoints
        populateQueueSelector();

        // enable/disable provider selector as per action type selected
        changeRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                queueSelector.setEnabled(true);
            }
        });
        deleteRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                queueSelector.setEnabled(false);
            }
        });
        ignoreRadio.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                queueSelector.setEnabled(false);
            }
        });

        createJMSEndpoint.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createJMSPressed();
            }
        });
    }

    /**
     * called when the user presses the button to manage JMS queues.
     */
    private void createJMSPressed() {
        JmsConnection jmsConnection = new JmsConnection();
        jmsConnection.setInitialContextFactoryClassname(foreignRef.getInitialContextFactoryClassname());
        jmsConnection.setJndiUrl(foreignRef.getJndiUrl());
        jmsConnection.setQueueFactoryUrl(foreignRef.getQueueFactoryUrl());
        jmsConnection.setTopicFactoryUrl(foreignRef.getTopicFactoryUrl());
        jmsConnection.setDestinationFactoryUrl(foreignRef.getDestinationFactoryUrl());
        JmsEndpoint jmsEndpoint = new JmsEndpoint();
        jmsEndpoint.setDestinationName(foreignRef.getDestinationName());

        final JmsQueuePropertiesDialog jqpd = JmsQueuePropertiesDialog.createInstance(this.getOwner(), jmsConnection, jmsEndpoint, false);
        jqpd.pack();
        Utilities.centerOnScreen(jqpd);
        DialogDisplayer.display(jqpd, new Runnable() {
            @Override
            public void run() {
                populateQueueSelector();

                if(!jqpd.isCanceled() && jqpd.getEndpoint().getOid() > 0) {
                    queueSelector.setSelectedItem(jqpd.getEndpoint().getName());
                    changeRadio.setSelected(true);
                    queueSelector.setEnabled(true);
                }
            }
        });
    }

    @Override
    public boolean onNextButton() {
        // collect actions details and store in the reference for resolution
        if (changeRadio.isSelected()) {
            Long newEndpointId = getEndpointIdFromName(queueSelector.getSelectedItem().toString());
            if (newEndpointId == null) {
                // this cannot happen
                logger.severe("Could not get provider from name " + queueSelector.getSelectedItem().toString());
                return false;
            }
            foreignRef.setLocalizeReplace(newEndpointId.longValue());
        } else if (deleteRadio.isSelected()) {
            foreignRef.setLocalizeDelete();
        } else if (ignoreRadio.isSelected()) {
            foreignRef.setLocalizeIgnore();
        }
        return true;
    }

    private Long getEndpointIdFromName(String name) {
        JmsAdmin admin = Registry.getDefault().getJmsManager();
        if (admin == null) {
            logger.severe("Cannot get the JMSAdmin");
            return null;
        }
        try {
            JmsAdmin.JmsTuple[] tuples = admin.findAllTuples();
            for (int i = 0; i < tuples.length; i++) {
                if (tuples[i].getEndpoint().getName().equals(name)) {
                    long newOid = tuples[i].getEndpoint().getOid();
                    logger.info("the oid of the chosen jms endpoint is " + newOid);
                    return newOid;
                }
            }
        } catch (FindException e) {
            logger.severe("Error geting tuples");
        }
        logger.severe("The endpoint name " + name + " did not match any endpoint name.");
        return null;
    }

    private void populateQueueSelector() {
        JmsAdmin admin = Registry.getDefault().getJmsManager();
        if (admin == null) {
            logger.severe("Cannot get the JMSAdmin");
            return;
        }

        DefaultComboBoxModel model = new DefaultComboBoxModel();
        try {
            JmsAdmin.JmsTuple[] tuples = admin.findAllTuples();
            for (int i = 0; i < tuples.length; i++) {
                if (!tuples[i].getEndpoint().isMessageSource()) {
                    model.addElement(tuples[i].getEndpoint().getName());
                }
            }
        } catch (FindException e) {
            logger.severe("Error geting tuples");
            return;
        }
        queueSelector.setModel(model);
        if (model.getSize() < 1) {
            // disable this option since there are no other options
            changeRadio.setEnabled(false);
            // check if we need to deselect this option
            if (changeRadio.isSelected()) {
                changeRadio.setSelected(false);
                deleteRadio.setSelected(true);
            }
        } else {
            changeRadio.setEnabled(true);
        }
    }

    @Override
    public String getDescription() {
        return getStepLabel();
    }

    @Override
    public String getStepLabel() {
        return "Unresolved JMS endpoint " + foreignRef.getDisplayName();
    }

    @Override
    public boolean canFinish() {
        if (hasNextPanel()) return false;
        return true;
    }

    private JMSEndpointReference foreignRef;

    private JPanel mainPanel;
    private JTextField jndiUrlTxtField;
    private JTextField initialContextFactoryTxtField;
    private JTextField queueFactoryUrlTxtField;
    private JRadioButton changeRadio;
    private JRadioButton deleteRadio;
    private JRadioButton ignoreRadio;
    private JComboBox queueSelector;
    private JButton createJMSEndpoint;
    private ButtonGroup actionRadios;

    private final Logger logger = Logger.getLogger(ResolveForeignJMSEndpointPanel.class.getName());

}
