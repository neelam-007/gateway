package com.l7tech.console.panels;

import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.exporter.JMSEndpointReference;

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
                model.addElement(tuples[i].getEndpoint().getName());
            }
        } catch (RemoteException e) {
            logger.severe("Error geting tuples");
            return;
        } catch (FindException e) {
            logger.severe("Error geting tuples");
            return;
        }
        queueSelector.setModel(model);
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
    private ButtonGroup actionRadios;

    private final Logger logger = Logger.getLogger(ResolveForeignJMSEndpointPanel.class.getName());

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        final JPanel _1;
        _1 = new JPanel();
        mainPanel = _1;
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel _2;
        _2 = new JLabel();
        _2.setText("Policy contains assertion(s) refering to unknown JMS Endpoints");
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JPanel _3;
        _3 = new JPanel();
        _3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        _3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Missing JMS Endpoint Details"));
        final JLabel _4;
        _4 = new JLabel();
        _4.setText("JNDI URL");
        _3.add(_4, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _5;
        _5 = new JLabel();
        _5.setText("Queue Factory URL");
        _3.add(_5, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JLabel _6;
        _6 = new JLabel();
        _6.setText("Initial Context Factory");
        _3.add(_6, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTextField _7;
        _7 = new JTextField();
        jndiUrlTxtField = _7;
        _7.setEditable(false);
        _3.add(_7, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JTextField _8;
        _8 = new JTextField();
        initialContextFactoryTxtField = _8;
        _8.setEditable(false);
        _3.add(_8, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JTextField _9;
        _9 = new JTextField();
        queueFactoryUrlTxtField = _9;
        _9.setEditable(false);
        _3.add(_9, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JPanel _10;
        _10 = new JPanel();
        _10.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_10, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        _10.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Action"));
        final JButton _11;
        _11 = new JButton();
        _11.setText("Create New JMS Endpoint");
        _10.add(_11, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JRadioButton _12;
        _12 = new JRadioButton();
        changeRadio = _12;
        _12.setText("Change assertions to use this endpoint");
        _10.add(_12, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JRadioButton _13;
        _13 = new JRadioButton();
        deleteRadio = _13;
        _13.setText("Remove assertions that refer to the missing endpoint");
        _10.add(_13, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JRadioButton _14;
        _14 = new JRadioButton();
        ignoreRadio = _14;
        _14.setText("Import erroneous assertions as-is");
        _10.add(_14, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JComboBox _15;
        _15 = new JComboBox();
        queueSelector = _15;
        _10.add(_15, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 1, 2, 0, null, null, null));
    }

}
