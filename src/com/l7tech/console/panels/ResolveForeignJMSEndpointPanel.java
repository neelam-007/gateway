package com.l7tech.console.panels;

import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.exporter.JMSEndpointReference;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.GridConstraints;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.rmi.RemoteException;
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
 * $Id$<br/>
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
            public void actionPerformed(ActionEvent e) {
                queueSelector.setEnabled(true);
            }
        });
        deleteRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                queueSelector.setEnabled(false);
            }
        });
        ignoreRadio.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                queueSelector.setEnabled(false);
            }
        });

        manageJMSEndpoints.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                manageJMSPressed();
            }
        });
    }

    /**
     * called when the user presses the button to manage JMS queues.
     */
    private void manageJMSPressed() {
        JmsQueuesWindow jqw = JmsQueuesWindow.createInstance(this.getOwner());
        Utilities.centerOnScreen(jqw);
        jqw.show();
        jqw.dispose();
        populateQueueSelector();
    }

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
                    return new Long(newOid);
                }
            }
        } catch (RemoteException e) {
            logger.severe("Error geting tuples");
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
        } catch (RemoteException e) {
            logger.severe("Error geting tuples");
            return;
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

    public String getDescription() {
        return getStepLabel();
    }

    public String getStepLabel() {
        return "Unresolved JMS endpoint " + foreignRef.getEndpointName();
    }

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
    private JButton manageJMSEndpoints;
    private ButtonGroup actionRadios;

    private final Logger logger = Logger.getLogger(ResolveForeignJMSEndpointPanel.class.getName());

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// !!! IMPORTANT !!!
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * !!! IMPORTANT !!!
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Policy contains assertion(s) refering to unknown JMS Endpoints");
        mainPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Missing JMS Endpoint Details"));
        final JLabel label2 = new JLabel();
        label2.setText("JNDI URL");
        panel1.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label3 = new JLabel();
        label3.setText("Queue Factory URL");
        panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label4 = new JLabel();
        label4.setText("Initial Context Factory");
        panel1.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        jndiUrlTxtField = new JTextField();
        jndiUrlTxtField.setEditable(false);
        panel1.add(jndiUrlTxtField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        initialContextFactoryTxtField = new JTextField();
        initialContextFactoryTxtField.setEditable(false);
        panel1.add(initialContextFactoryTxtField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        queueFactoryUrlTxtField = new JTextField();
        queueFactoryUrlTxtField.setEditable(false);
        panel1.add(queueFactoryUrlTxtField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Action"));
        manageJMSEndpoints = new JButton();
        manageJMSEndpoints.setText("Manage JMS Queues");
        panel2.add(manageJMSEndpoints, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        changeRadio = new JRadioButton();
        changeRadio.setText("Change assertions to use this endpoint");
        panel2.add(changeRadio, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        deleteRadio = new JRadioButton();
        deleteRadio.setText("Remove assertions that refer to the missing endpoint");
        panel2.add(deleteRadio, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        ignoreRadio = new JRadioButton();
        ignoreRadio.setText("Import erroneous assertions as-is");
        panel2.add(ignoreRadio, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        queueSelector = new JComboBox();
        panel2.add(queueSelector, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
    }
}
