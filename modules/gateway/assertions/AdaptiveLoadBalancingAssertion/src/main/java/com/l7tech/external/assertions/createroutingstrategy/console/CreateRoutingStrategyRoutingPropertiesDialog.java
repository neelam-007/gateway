package com.l7tech.external.assertions.createroutingstrategy.console;

import com.l7tech.common.io.failover.Service;
import com.l7tech.console.panels.SimplePropertyDialog;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.external.assertions.createroutingstrategy.CreateRoutingStrategyAssertion;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.*;
import com.l7tech.util.Functions;
import com.l7tech.util.NameValuePair;
import com.l7tech.util.Pair;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class CreateRoutingStrategyRoutingPropertiesDialog extends JDialog {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.createroutingstrategy.console.resources.CreateRoutingStrategyResources");
    private static final String NAME = "strategy.configure.name";
    private static final String VALUE = "strategy.configure.value";
    private final ImageIcon BLANK_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Transparent16.png"));
    private final ImageIcon OK_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Check16.png"));
    private final ImageIcon WARNING_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Warning16.png"));
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField serviceTextField;
    private JButton addPropertyButton;
    private JButton deletePropertyButton;
    private JButton editPropertyButton;
    private JTable propertyTable;
    private boolean confirmed;
    private final Set<String> predecessorVariables;
    private SimpleTableModel<NameValuePair> propertiesTableModel;

    public CreateRoutingStrategyRoutingPropertiesDialog(final Window parent, String title, CreateRoutingStrategyAssertion assertion) {
        this(parent, title, assertion, null);
    }

    public CreateRoutingStrategyRoutingPropertiesDialog(final Window parent, String title, CreateRoutingStrategyAssertion assertion, Service service) {
        super(parent, title);
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        buttonOK.setEnabled(false);

        TextComponentPauseListenerManager.registerPauseListener(
                serviceTextField,
                new PauseListener() {
                    @Override
                    public void textEntryPaused(JTextComponent component, long msecs) {
                        validateService();
                    }

                    @Override
                    public void textEntryResumed(JTextComponent component) {
                    }
                },
                300);

        buttonOK.addActionListener(new

                ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        onOK();
                    }
                }

        );

        buttonCancel.addActionListener(new

                ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        onCancel();
                    }
                }

        );

        Set<String> vars = SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion).keySet();
        // convert all vars to lower
        predecessorVariables = new TreeSet<String>();
        for (String var : vars) {
            predecessorVariables.add(var.toLowerCase());
        }

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        }
        );

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new

                ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        onCancel();
                    }
                }

                , KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        initComponents();
        setService(service);
    }

    private void initComponents() {
        propertiesTableModel = TableUtil.configureTable(
                propertyTable,
                TableUtil.column(resources.getString(NAME), 50, 100, 100000, property("key"), String.class),
                TableUtil.column(resources.getString(VALUE), 50, 100, 100000, property("value"), String.class)
        );
        propertyTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        propertyTable.getTableHeader().setReorderingAllowed(false);

        addPropertyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editProperty(null);
            }
        });

        editPropertyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int viewRow = propertyTable.getSelectedRow();
                if (viewRow > -1) {
                    editProperty(propertiesTableModel.getRowObject(propertyTable.convertRowIndexToModel(viewRow)));
                }
            }
        });

        deletePropertyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int viewRow = propertyTable.getSelectedRow();
                if (viewRow > -1) {
                    propertiesTableModel.removeRowAt(propertyTable.convertRowIndexToModel(viewRow));
                }
            }
        });
    }

    private void editProperty(final NameValuePair nameValuePair) {
        final SimplePropertyDialog dlg = nameValuePair == null ?
                new SimplePropertyDialog((Frame) getParent()) :
                new SimplePropertyDialog((Frame) getParent(), new Pair<String, String>(nameValuePair.getKey(), nameValuePair.getValue()));
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    final Pair<String, String> property = dlg.getData();
                    for (final NameValuePair pair : new ArrayList<NameValuePair>(propertiesTableModel.getRows())) {
                        if (pair.getKey().equals(property.left)) {
                            propertiesTableModel.removeRow(pair);
                        }
                    }
                    if (nameValuePair != null) propertiesTableModel.removeRow(nameValuePair);

                    propertiesTableModel.addRow(new NameValuePair(property.left, property.right));
                }
            }
        });
    }

    private static Functions.Unary<String, NameValuePair> property(final String propName) {
        return Functions.propertyTransform(NameValuePair.class, propName);
    }

    private void validateService() {
        if (serviceTextField.getText().trim().isEmpty()) {
            buttonOK.setEnabled(false);
        } else {
            buttonOK.setEnabled(true);
        }
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        confirmed = false;
        dispose();
    }

    public Service getService() {
        List<NameValuePair> props = propertiesTableModel.getRows();
        Map<String, String> map = new HashMap<String, String>();
        for (Iterator<NameValuePair> iterator = props.iterator(); iterator.hasNext(); ) {
            NameValuePair next = iterator.next();
            map.put(next.getKey().trim(), next.getValue().trim());
        }
        return new Service(serviceTextField.getText().trim(), map);

    }

    public void setService(Service service) {
        if (service != null) {
            serviceTextField.setText(service.getName());
            Map<String, String> props = service.getProperties();
            if (props != null) {
                List<NameValuePair> propList = new ArrayList<NameValuePair>(props.size());
                for (Map.Entry<String, String> entry : props.entrySet()) {
                    NameValuePair pair = new NameValuePair(entry.getKey(), entry.getValue());
                    propList.add(pair);
                }
                propertiesTableModel.setRows(propList);
            }
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }


}
