package com.l7tech.console.panels;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

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

    private SimpleTableModel<SsgFirewallRule> firewallTableModel;

    private PermissionFlags permissionFlags;

    private boolean isDirty;

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

        buttonClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(isDirty){
                    final TransportAdmin ta = Registry.getDefault().getTransportAdmin();
                    try {
                        for(SsgFirewallRule r : firewallTableModel.getRows()){
                            ta.saveFirewallRule(r);
                        }
                    } catch (Exception e1) {
                        logger.warning("Error updating firewall rule ordinal.");
                    }
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
                column("Ordinal", 30, 50, 50, ordinalPropertyExtractor, Integer.class),
                column("Name", 30, 100, 100, namePropertyExtractor, String.class),
                column("Protocol", 30, 100, 100, protocollPropertyExtractor, String.class),
                column("Source", 30, 100, 200, sourcePropertyExtractor, String.class),
                column("Destination", 30, 100, 200, destinationPropertyExtractor, String.class),
                column("Port", 30, 100, 100, portPropertyExtractor, String.class),
                column("Target", 30, 100, 100, targetPropertyExtractor, String.class)
        );

        loadFirewallRules();
        moveUpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                reorderRule(-1);
                isDirty = true;
            }
        });
        moveDownButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                reorderRule(1);
                isDirty = true;
            }
        });

        addButton.setEnabled(permissionFlags.canCreateSome() || permissionFlags.canCreateAll());
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                displayPropertiesDialog(null);
            }
        });
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                displayPropertiesDialog(getSelectedInput());
            }
        });
        Utilities.setDoubleClickAction(firewallRulesTable, editButton);
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

    private void loadFirewallRules(){
        firewallTableModel.setRows(Functions.sort(findAllFirewallRules(), new Comparator<SsgFirewallRule>() {
            @Override
            public int compare(SsgFirewallRule a, SsgFirewallRule b) {
                return Integer.compare(a.getOrdinal(), b.getOrdinal());
            }
        }));
    }

    private void reorderRule(int offset){
        int selected = firewallRulesTable.getSelectedRow();
        int destination = selected + offset;

        SsgFirewallRule r1 = firewallTableModel.getRowObject(selected);
        SsgFirewallRule r2 = firewallTableModel.getRowObject(destination);

        int ordinal = r1.getOrdinal();
        r1.setOrdinal(r2.getOrdinal());
        r2.setOrdinal(ordinal);
        try{
            firewallTableModel.setRowObject(selected, r2);
            firewallTableModel.setRowObject(destination, r1);
            firewallTableModel.fireTableRowsUpdated(Math.min(selected, destination), Math.max(selected, destination));
            firewallRulesTable.getSelectionModel().addSelectionInterval(destination, destination);
        }
        catch(Exception e){
            logger.warning("Error occurred while re-ordering rule ordinal: " + ExceptionUtils.getDebugException(e));
        }
    }

    private void toggleButtonState(){
        editButton.setEnabled((permissionFlags.canUpdateSome() || permissionFlags.canUpdateAll()) && firewallRulesTable.getSelectedRow() > -1);
        deleteButton.setEnabled((permissionFlags.canDeleteSome() || permissionFlags.canDeleteAll()) && firewallRulesTable.getSelectedRow() > -1);

        //can't move up if we are at the top
        moveUpButton.setEnabled((permissionFlags.canUpdateSome() || permissionFlags.canUpdateAll()) && firewallRulesTable.getSelectedRow() > 0);
        //can't move down if we are at the bottom and we have something selected
        moveDownButton.setEnabled((permissionFlags.canUpdateSome() || permissionFlags.canUpdateAll()) && (firewallRulesTable.getSelectedRow() > -1 && !firewallRulesTable.getSelectionModel().isSelectedIndex(firewallRulesTable.getRowCount() - 1)));
    }

    private SsgFirewallRule getSelectedInput(){
        return firewallTableModel.getRowObject(firewallRulesTable.getSelectedRow());
    }

    private void displayPropertiesDialog(final SsgFirewallRule rule){
        final SsgFirewallPropertiesDialog dlg = new SsgFirewallPropertiesDialog(this, rule);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable(){
            @Override
            public void run() {
                if(dlg.isConfirmed()){
                    try {
                        final SsgFirewallRule r = dlg.getRule();
                        if(rule == null){
                            r.setOrdinal(firewallRulesTable.getRowCount() + 1);
                        }
                        Registry.getDefault().getTransportAdmin().saveFirewallRule(r);
                    } catch (Exception e) {
                        logger.warning("Unable to save firewall rule: " + ExceptionUtils.getDebugException(e));
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
    private static final Functions.Unary<Integer, SsgFirewallRule> ordinalPropertyExtractor = Functions.<Integer, SsgFirewallRule>propertyTransform(SsgFirewallRule.class, "ordinal");
    private static final Functions.Unary<String, SsgFirewallRule> namePropertyExtractor = Functions.<String, SsgFirewallRule>propertyTransform(SsgFirewallRule.class, "name");
    private static final Functions.Unary<String, SsgFirewallRule> protocollPropertyExtractor = Functions.<String, SsgFirewallRule>propertyTransform(SsgFirewallRule.class, "protocol");
    private static final Functions.Unary<String, SsgFirewallRule> sourcePropertyExtractor = Functions.<String, SsgFirewallRule>propertyTransform(SsgFirewallRule.class, "source");
    private static final Functions.Unary<String, SsgFirewallRule> destinationPropertyExtractor = Functions.<String, SsgFirewallRule>propertyTransform(SsgFirewallRule.class, "destination");
    private static final Functions.Unary<String, SsgFirewallRule> portPropertyExtractor = Functions.<String, SsgFirewallRule>propertyTransform(SsgFirewallRule.class, "port");
    private static final Functions.Unary<String, SsgFirewallRule> targetPropertyExtractor = Functions.<String, SsgFirewallRule>propertyTransform(SsgFirewallRule.class, "jump");
}

