package com.l7tech.console.panels;

import com.l7tech.console.util.JmsUtilities;
import com.l7tech.gateway.common.transport.jms.JmsAdmin;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.util.Registry;
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
        jmsConnection.setTemplate(foreignRef.isConnectionTemplate());
        jmsConnection.setInitialContextFactoryClassname(foreignRef.getInitialContextFactoryClassname());
        jmsConnection.setJndiUrl(foreignRef.getJndiUrl());
        jmsConnection.setQueueFactoryUrl(foreignRef.getQueueFactoryUrl());
        jmsConnection.setTopicFactoryUrl(foreignRef.getTopicFactoryUrl());
        jmsConnection.setDestinationFactoryUrl(foreignRef.getDestinationFactoryUrl());
        JmsEndpoint jmsEndpoint = new JmsEndpoint();
        jmsEndpoint.setName(foreignRef.getName());
        jmsEndpoint.setQueue(foreignRef.isQueue());
        jmsEndpoint.setTemplate(foreignRef.isEndpointTemplate());
        jmsEndpoint.setDestinationName(foreignRef.getDestinationName());
        jmsEndpoint.setOldOid(foreignRef.getOldOid());

        final JmsQueuePropertiesDialog jqpd = JmsQueuePropertiesDialog.createInstance(this.getOwner(), jmsConnection, jmsEndpoint, false);
        jqpd.pack();
        Utilities.centerOnScreen(jqpd);
        DialogDisplayer.display(jqpd, new Runnable() {
            @Override
            public void run() {
                populateQueueSelector();

                if(!jqpd.isCanceled() && !jqpd.getEndpoint().getGoid().equals(JmsEndpoint.DEFAULT_GOID)) {
                    JmsUtilities.selectEndpoint(queueSelector, jqpd.getEndpoint().getGoid());
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
            JmsUtilities.QueueItem queueItem = (JmsUtilities.QueueItem) queueSelector.getSelectedItem();
            if (queueItem == null || queueItem.getQueue() == null || queueItem.getQueue().getEndpoint() == null) {
                // this cannot happen
                logger.severe("No provider selected");
                return false;
            }
            foreignRef.setLocalizeReplace(queueItem.getQueue().getEndpoint().getGoid());
        } else if (deleteRadio.isSelected()) {
            foreignRef.setLocalizeDelete();
        } else if (ignoreRadio.isSelected()) {
            foreignRef.setLocalizeIgnore();
        }
        return true;
    }

    @Override
    public void notifyActive() {
        populateQueueSelector();
    }

    private void populateQueueSelector() {
        JmsAdmin admin = Registry.getDefault().getJmsManager();
        if (admin == null) {
            logger.severe("Cannot get the JMSAdmin");
            return;
        }

        final Object selectedItem = queueSelector.getSelectedItem();
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (JmsUtilities.QueueItem queueItem : JmsUtilities.loadQueueItems()) {
            if (!queueItem.getQueue().getEndpoint().isMessageSource()) {
                model.addElement(queueItem);
            }
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

            if ( selectedItem != null ) {
                queueSelector.setSelectedItem( selectedItem );
                if ( queueSelector.getSelectedIndex() == -1 ) {
                    queueSelector.setSelectedIndex( 0 );
                }
            }
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
        return ! hasNextPanel();
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
