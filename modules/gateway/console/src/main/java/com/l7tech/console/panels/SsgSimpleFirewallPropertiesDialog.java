package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A dialog to create a simple firewall rule.
 * @author K.Diep
 */
public class SsgSimpleFirewallPropertiesDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(SsgSimpleFirewallPropertiesDialog.class.getName());

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField ruleName;
    private JCheckBox enableCheckBox;
    private JComboBox ruleType;
    private JTextField fromPort;
    private JTextField toPort;
    private JLabel toPortLabel;
    private JComboBox protocolType;
    private JComboBox interfaceComboBox;
    private JButton manageInterface;
    private DefaultComboBoxModel actionComboBoxModel;
    private DefaultComboBoxModel protocolTypeComboBoxModel;
    private DefaultComboBoxModel interfaceComboBoxModel;

    private SsgFirewallRule rule;
    private boolean confirmed;

    private InputValidator inputValidator;

    public SsgSimpleFirewallPropertiesDialog(final Window owner, final SsgFirewallRule rule) {
        super(owner, "Simple Firewall Rule Properties");
        this.rule = rule;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        initialize();
    }

    private void initialize() {
        manageInterface.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                InterfaceTagsDialog.show(SsgSimpleFirewallPropertiesDialog.this, new Functions.UnaryVoid<String>() {
                    @Override
                    public void call(String newInterfaceTags) {
                        Object prev = interfaceComboBox.getSelectedItem();
                        initializeInterfaceComboBox(newInterfaceTags);
                        interfaceComboBox.getModel().setSelectedItem(prev);
                    }
                });
            }
        });
        initializeInterfaceComboBox(null);

        actionComboBoxModel = new DefaultComboBoxModel(new Object[]{"Accept", "Redirect"});
        ruleType.setModel(actionComboBoxModel);

        protocolTypeComboBoxModel = new DefaultComboBoxModel(new Object[]{"tcp", "udp"});
        protocolType.setModel(protocolTypeComboBoxModel);

        toPortLabel.setVisible(false);
        toPort.setVisible(false);
        toPort.setEnabled(false);
        ruleType.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                boolean isRedirect = "Redirect".equals(ruleType.getSelectedItem());
                toPortLabel.setVisible(isRedirect);
                toPort.setVisible(isRedirect);
                toPort.setEnabled(isRedirect);
                DialogDisplayer.pack(SsgSimpleFirewallPropertiesDialog.this);
            }
        });

        inputValidator = new InputValidator(this, "Simple Firewall Settings");
        inputValidator.attachToButton(buttonOK, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        inputValidator.constrainTextFieldToNumberRange("From Port", fromPort, 1, 65535);
        inputValidator.constrainTextFieldToNumberRange("To Port", toPort, 1, 65535);
        inputValidator.constrainTextFieldToMaxChars("Rule Name", ruleName, 128, inputValidator.constrainTextFieldToBeNonEmpty("Rule Name", ruleName, null));
        inputValidator.validateWhenDocumentChanges(fromPort);
        inputValidator.validateWhenDocumentChanges(toPort);
        inputValidator.validateWhenDocumentChanges(ruleName);

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        if(rule != null){
            modelToView();
        }
        else {
            rule = new SsgFirewallRule();
        }

    }

    private void modelToView(){
        ruleName.setText(rule.getName());
        enableCheckBox.setSelected(rule.isEnabled());
        fromPort.setText(rule.getProperty("destination-port"));
        protocolTypeComboBoxModel.setSelectedItem(rule.getProtocol());
        String toPortValue = rule.getProperty("to-ports");
        if(toPortValue == null){
            ruleType.setSelectedItem("Accept");
        }
        else {
            ruleType.setSelectedItem("Redirect");
            toPort.setText(rule.getProperty("to-ports"));
        }
        String in = rule.getProperty("in-interface");
        if(in == null) interfaceComboBox.setSelectedIndex(0);
        else interfaceComboBoxModel.setSelectedItem(in);
    }

    private void viewToModel(){
        rule.setName(ruleName.getText().trim());
        rule.setEnabled(enableCheckBox.isSelected());

        for(String p : rule.getPropertyNames()){
            rule.removeProperty(p);
        }

        rule.putProperty("protocol", protocolTypeComboBoxModel.getSelectedItem().toString());
        if("Accept".equals(actionComboBoxModel.getSelectedItem())){
            //allow rule
            rule.putProperty("chain", "INPUT");
            rule.putProperty("table", "filter");
            rule.putProperty("jump", "ACCEPT");
            rule.putProperty("destination-port", fromPort.getText().trim());
        }
        else {
            //redirect rule
            rule.putProperty("chain", "PREROUTING");
            rule.putProperty("table", "NAT");
            rule.putProperty("jump", "REDIRECT");
            rule.putProperty("destination-port", fromPort.getText().trim());
            rule.putProperty("to-ports", toPort.getText().trim());
        }
        Object inter = interfaceComboBoxModel.getSelectedItem();
        if(inter != null && !"(ALL)".equalsIgnoreCase(inter.toString())){
            rule.putProperty("in-interface", inter.toString());
        }
    }

    private void initializeInterfaceComboBox(@Nullable String interfaceTags) {
        java.util.List<String> entries = new ArrayList<String>();

        entries.add("(All)");

        try {
            if (interfaceTags == null) {
                ClusterProperty tagProp = Registry.getDefault().getClusterStatusAdmin().findPropertyByName(InterfaceTag.PROPERTY_NAME);
                if (tagProp != null){
                    interfaceTags = tagProp.getValue();
                }
            }
            if (interfaceTags != null) {
                for (InterfaceTag tag : InterfaceTag.parseMultiple(interfaceTags))
                    entries.add(tag.getName());
            }
        } catch (FindException e) {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "FindException looking up cluster property " + InterfaceTag.PROPERTY_NAME + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (ParseException e) {
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "Bad value for cluster property " + InterfaceTag.PROPERTY_NAME + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }

        InetAddress[] addrs = Registry.getDefault().getTransportAdmin().getAvailableBindAddresses();
        for (InetAddress addr : addrs) {
            entries.add(addr.getHostAddress());
        }

        interfaceComboBoxModel = new DefaultComboBoxModel(entries.toArray());
        interfaceComboBox.setModel(interfaceComboBoxModel);
    }

    private void onOK() {
        viewToModel();
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public SsgFirewallRule getRule() {
        return rule;
    }
}
