package com.l7tech.console.panels;

import com.l7tech.console.util.EntityUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.transport.TransportAdmin;
import com.l7tech.gateway.common.transport.firewall.SsgFirewallRule;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * <p>A dialog to display all the firewall rules in the system</p>
 * @author K.Diep
 */
public class SsgFirewallManagerDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(SsgFirewallManagerDialog.class.getName());

    private JPanel contentPane;

    private JButton buttonClose;
    private JButton addButton;
    private JButton editButton;
    private JButton deleteButton;
    private JTable firewallRulesTable;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private JButton restoreDefaultsButton;
    private JButton cloneButton;
    private JButton advancedButton;
    private JButton advancePropertiesButton;

    private SimpleTableModel<SsgFirewallRule> firewallTableModel;

    private static final java.util.List<String> ADVANCE_PROPERTIES = new ArrayList<String>(Arrays.asList(new String[]{
            "source", "destination", "tcp-flags", "tcp-option", "icmp-type", "source-port", "to-destination"}));

    private PermissionFlags permissionFlags;

    public SsgFirewallManagerDialog(final Window owner) {
        super(owner, "Manage Firewall Rules", ModalityType.DOCUMENT_MODAL);
        setContentPane(contentPane);
        setModal(true);
        permissionFlags = PermissionFlags.get(EntityType.FIREWALL_RULE);
        initialize();
    }

    private void initialize(){
        getRootPane().setDefaultButton(buttonClose);
        setupRestoreDefaultButton();
        toggleButtonState();

        cloneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final SsgFirewallRule rule = getSelectedInput();
                if (rule == null)
                    return;

                final SsgFirewallRule newRule = rule.getCopy();
                newRule.setOrdinal(rule.getOrdinal() + 1);
                EntityUtils.updateCopy(newRule);
                if(canDisplaySimpleDialog(newRule)){
                    displaySimplePropertiesDialog(newRule);
                }
                else {
                    DialogDisplayer.showSafeConfirmDialog(
                            contentPane,
                            "<html><center><p>Warning: You are about to clone a rule with advanced configurations.  Mis-configurations may prevent access to the Gateway.</p>" +
                                    "<p>Do you wish to continue?</p></center></html>",
                            "Confirm Action",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            new DialogDisplayer.OptionListener() {
                                @Override
                                public void reportResult(int option) {
                                    if (option == JOptionPane.CANCEL_OPTION) {
                                        return;
                                    }
                                    displayAdvancedPropertiesDialog(newRule);
                                }
                            }
                    );
                }
            }
        });
        buttonClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final TransportAdmin ta = Registry.getDefault().getTransportAdmin();
                try {
                    for (int i = 0; i < firewallTableModel.getRowCount(); i++) {
                        int ordinal = i + 1;
                        SsgFirewallRule rule = firewallTableModel.getRowObject(i);
                        if (rule.getOrdinal() != ordinal) {
                            rule.setOrdinal(ordinal);
                            ta.saveFirewallRule(rule);
                        }
                    }
                } catch (Exception e1) {
                    logger.warning("Error updating firewall rule ordinal.");
                }
                onClose();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        firewallTableModel = TableUtil.configureTable(firewallRulesTable,
                column("Enabled", 30, 50, 50, enabledPropertyExtractor, Boolean.class),
                column("Name", 30, 100, 200, namePropertyExtractor, String.class),
                column("Protocol", 30, 100, 100, protocollPropertyExtractor, String.class),
                column("Interface", 30, 100, 200, interfacePropertyExtractor, String.class),
                column("Port", 30, 100, 100, portPropertyExtractor, String.class),
                column("Action", 30, 100, 100, targetPropertyExtractor, String.class)
        );

        loadFirewallRules();

        moveUpButton.addActionListener(TableUtil.createMoveUpAction(firewallRulesTable, firewallTableModel));
        moveDownButton.addActionListener(TableUtil.createMoveDownAction(firewallRulesTable, firewallTableModel));
        advancedButton.setEnabled(permissionFlags.canCreateSome() || permissionFlags.canCreateAll());
        advancedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                confirmAdvanceConfiguration(null);
            }
        });
        advancePropertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                confirmAdvanceConfiguration(getSelectedInput());
            }
        });
        addButton.setEnabled(permissionFlags.canCreateSome() || permissionFlags.canCreateAll());
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                displaySimplePropertiesDialog(null);
            }
        });
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                displaySimplePropertiesDialog(getSelectedInput());
            }
        });

        firewallRulesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if(e.getClickCount() == 2){
                    SsgFirewallRule input = getSelectedInput();
                    if(canDisplaySimpleDialog(input)){
                        displaySimplePropertiesDialog(input);
                    }
                    else {
                        confirmAdvanceConfiguration(input);
                    }
                }
            }
        });

        firewallRulesTable.getSelectionModel().addListSelectionListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                toggleButtonState();
            }
        }));
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                final SsgFirewallRule rule = getSelectedInput();
                int result = JOptionPane.showConfirmDialog(SsgFirewallManagerDialog.this,
                        "Are you sure you want to remove the firewall rule \"" + rule.getName() + "\"?",
                        "Confirm Removal",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE);
                if (result != JOptionPane.YES_OPTION)
                    return;


                int rowIndex = firewallRulesTable.getSelectedRow();
                try {
                    Registry.getDefault().getTransportAdmin().deleteFirewallRule(rule.getOid());
                    firewallTableModel.removeRowAt(rowIndex);
                    for(int i = 0; i < firewallTableModel.getRowCount(); i++){
                        SsgFirewallRule r = firewallTableModel.getRowObject(i);
                        r.setOrdinal(i + 1);
                        Registry.getDefault().getTransportAdmin().saveFirewallRule(r);
                    }
                    loadFirewallRules();
                } catch (Exception e1) {
                    logger.warning("Unable to remove the firewall rule with oid of " + rule.getOid());
                }
            }
        });
    }

    private boolean canDisplaySimpleDialog(final SsgFirewallRule rule) {
        boolean isSimpleAccept = "filter".equals(rule.getProperty("table")) && "INPUT".equals(rule.getProperty("chain")) && "ACCEPT".equals(rule.getProperty("jump")) && !"icmp".equals(rule.getProtocol());
        boolean isSimpleRedirect = "NAT".equals(rule.getProperty("table")) && "PREROUTING".equals(rule.getProperty("chain")) && "REDIRECT".equals(rule.getProperty("jump"));

        boolean containOtherSettings = false;
        java.util.List<String> myProps = rule.getPropertyNames();
        for (final String ap : ADVANCE_PROPERTIES) {
            if (myProps.contains(ap)){
                containOtherSettings = true;
                break;
            }
        }
        return ((isSimpleAccept || isSimpleRedirect) && !containOtherSettings);
    }

    private void confirmAdvanceConfiguration(final SsgFirewallRule rule){
        DialogDisplayer.showSafeConfirmDialog(
                contentPane,
                "<html><center><p>Warning: You are about to access the advance settings.  Mis-configurations may prevent access to the Gateway</p>" +
                        "<p>Do you wish to continue?</p></center></html>",
                "Confirm Action",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option == JOptionPane.CANCEL_OPTION) {
                            return;
                        }
                        displayAdvancedPropertiesDialog(rule);
                    }
                }
        );
    }

    private void loadFirewallRules(){
        firewallTableModel.setRows(Functions.sort(findAllFirewallRules(), new Comparator<SsgFirewallRule>() {
            @Override
            public int compare(SsgFirewallRule a, SsgFirewallRule b) {
                return Integer.compare(a.getOrdinal(), b.getOrdinal());
            }
        }));
    }

    private void toggleButtonState(){
        editButton.setEnabled((permissionFlags.canUpdateSome() || permissionFlags.canUpdateAll()) && firewallRulesTable.getSelectedRow() > -1 && canDisplaySimpleDialog(getSelectedInput()));

        deleteButton.setEnabled((permissionFlags.canDeleteSome() || permissionFlags.canDeleteAll()) && firewallRulesTable.getSelectedRow() > -1);
        cloneButton.setEnabled((permissionFlags.canCreateAll() || permissionFlags.canCreateSome()) && firewallRulesTable.getSelectedRow() > -1);
        advancePropertiesButton.setEnabled((permissionFlags.canCreateAll() || permissionFlags.canCreateSome()) && firewallRulesTable.getSelectedRow() > -1);

        //can't move up if we are at the top
        moveUpButton.setEnabled((permissionFlags.canUpdateSome() || permissionFlags.canUpdateAll()) && firewallRulesTable.getSelectedRow() > 0);
        //can't move down if we are at the bottom and we have something selected
        moveDownButton.setEnabled((permissionFlags.canUpdateSome() || permissionFlags.canUpdateAll()) && (firewallRulesTable.getSelectedRow() > -1 && !firewallRulesTable.getSelectionModel().isSelectedIndex(firewallRulesTable.getRowCount() - 1)));
    }

    private SsgFirewallRule getSelectedInput(){
        return firewallTableModel.getRowObject(firewallRulesTable.getSelectedRow());
    }

    private void displaySimplePropertiesDialog(final SsgFirewallRule rule){
        final SsgSimpleFirewallPropertiesDialog dlg = new SsgSimpleFirewallPropertiesDialog(this, rule);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable(){
            @Override
            public void run() {
                if(dlg.isConfirmed()){
                    final SsgFirewallRule r = dlg.getRule();
                    Runnable reedit = new Runnable() {
                        @Override
                        public void run() {
                            loadFirewallRules();
                            displaySimplePropertiesDialog(r);
                        }
                    };
                    try {
                        if(rule == null){
                            r.setOrdinal(firewallRulesTable.getRowCount() + 1);
                        }
                        Registry.getDefault().getTransportAdmin().saveFirewallRule(r);
                    } catch (Exception e) {
                        logger.warning("Unable to save firewall rule: " +  ExceptionUtils.getMessage(e));
                        DialogDisplayer.showMessageDialog(SsgFirewallManagerDialog.this, "Error saving rule: " + ExceptionUtils.getMessage(e)
                                , "Save Failed", JOptionPane.ERROR_MESSAGE, reedit);
                    }
                }
                loadFirewallRules();
            }
        });
    }

    private void displayAdvancedPropertiesDialog(final SsgFirewallRule rule){
        final SsgFirewallPropertiesDialog dlg = new SsgFirewallPropertiesDialog(this, rule);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable(){
            @Override
            public void run() {
                final SsgFirewallRule r = dlg.getRule();
                Runnable reedit = new Runnable() {
                    @Override
                    public void run() {
                        loadFirewallRules();
                        displayAdvancedPropertiesDialog(r);
                    }
                };
                if(dlg.isConfirmed()){
                    try {
                        if(rule == null){
                            r.setOrdinal(firewallRulesTable.getRowCount() + 1);
                        }
                        Registry.getDefault().getTransportAdmin().saveFirewallRule(r);
                    } catch (Exception e) {
                        logger.warning("Unable to save firewall rule: " +  ExceptionUtils.getMessage(e));
                        DialogDisplayer.showMessageDialog(SsgFirewallManagerDialog.this, "Error saving rule: " + ExceptionUtils.getMessage(e)
                                , "Save Failed", JOptionPane.ERROR_MESSAGE, reedit);
                    }
                }
                loadFirewallRules();
            }
        });
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, null);
    }

    private void setupRestoreDefaultButton(){
        restoreDefaultsButton.setEnabled(permissionFlags.canUpdateAll() || permissionFlags.canUpdateSome());
        restoreDefaultsButton.setEnabled(permissionFlags.canDeleteSome() || permissionFlags.canDeleteAll());
        restoreDefaultsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DialogDisplayer.showSafeConfirmDialog(
                        contentPane,
                        "<html><center><p>Warning: You are about to remove all existing firewall rules.</p>" +
                                "<p>Do you wish to continue?</p></center></html>",
                        "Confirm Firewall Deletion",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        new DialogDisplayer.OptionListener() {
                            @Override
                            public void reportResult(int option) {
                                if (option == JOptionPane.CANCEL_OPTION) {
                                    return;
                                }
                                try {
                                    final Collection<SsgFirewallRule> rules = findAllFirewallRules();
                                    for(final SsgFirewallRule c : rules){
                                        Registry.getDefault().getTransportAdmin().deleteFirewallRule(c.getOid());
                                    }
                                    loadFirewallRules();
                                } catch (FindException e1) {
                                    showErrorMessage("Deletion Failed", "Unable to find firewall port: " + ExceptionUtils.getMessage(e1), e1);
                                } catch (Exception e1) {
                                    showErrorMessage("Deletion Failed", "Unable to delete firewall port: " + ExceptionUtils.getMessage(e1), e1);
                                }
                            }
                        }
                );
            }
        });
    }

    private void onClose() {
        dispose();
    }

    private Collection<SsgFirewallRule> findAllFirewallRules(){
        try{
            return new ArrayList<SsgFirewallRule>(Registry.getDefault().getTransportAdmin().findAllFirewallRules());
        } catch (FindException e) {
            logger.warning("Error retrieving firewall rules: " + ExceptionUtils.getDebugException(e));
        }
        return Collections.EMPTY_LIST;
    }

    private static final Functions.Unary<Boolean, SsgFirewallRule> enabledPropertyExtractor = Functions.<Boolean, SsgFirewallRule>propertyTransform(SsgFirewallRule.class, "enabled");
    private static final Functions.Unary<String, SsgFirewallRule> namePropertyExtractor = Functions.<String, SsgFirewallRule>propertyTransform(SsgFirewallRule.class, "name");
    private static final Functions.Unary<String, SsgFirewallRule> protocollPropertyExtractor = Functions.<String, SsgFirewallRule>propertyTransform(SsgFirewallRule.class, "protocol");
    private static final Functions.Unary<String, SsgFirewallRule> portPropertyExtractor = Functions.<String, SsgFirewallRule>propertyTransform(SsgFirewallRule.class, "port");
    private static final Functions.Unary<String, SsgFirewallRule> targetPropertyExtractor = Functions.<String, SsgFirewallRule>propertyTransform(SsgFirewallRule.class, "jump");
    private static final Functions.Unary<String, SsgFirewallRule> interfacePropertyExtractor = Functions.<String, SsgFirewallRule>propertyTransform(SsgFirewallRule.class, "inInterface");

}

