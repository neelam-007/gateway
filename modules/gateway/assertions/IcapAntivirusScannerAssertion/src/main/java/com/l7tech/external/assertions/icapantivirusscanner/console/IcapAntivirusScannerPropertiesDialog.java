package com.l7tech.external.assertions.icapantivirusscanner.console;

import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion;
import com.l7tech.external.assertions.icapantivirusscanner.server.ServerIcapAntivirusScannerAssertion;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ValidationUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * The main GUI for the ICAP Antivirus assertion.
 * </p>
 *
 * @author Ken Diep
 */
public final class IcapAntivirusScannerPropertiesDialog extends AssertionPropertiesOkCancelSupport<IcapAntivirusScannerAssertion> {

    private static final String DIALOG_TITLE = "ICAP Antivirus Scanner Properties";
    private static final String DIALOG_TITLE_NEW_SERVER = "Add New Server";
    private static final String DIALOG_TITLE_EDIT_SERVER_PROPERTIES = "Edit Server Properties";

    private static final String DIALOG_TITLE_NEW_PARAMETER = "Add New Service Parameter";
    private static final String DIALOG_TITLE_EDIT_PARAMETER = "Edit Service Parameter";

    private static final String PARAMETER_NAME = "Parameter Name";
    private static final String PARAMETER_VALUE = "Parameter Value";

    private JPanel contentPane;
    private JButton addServer;
    private JButton removeServer;
    private JButton editServer;
    private JList serverList;
    private JComboBox cbStrategy;
    private JCheckBox continueIfVirusFound;
    private JButton addParam;
    private JButton removeParam;
    private JButton editParam;
    private JTable serviceParams;
    private JTextField connectionTimeoutField;
    private JTextField readTimeoutField;
    private JSpinner maxMimeDepth;
    private TargetVariablePanel varPrefixPanel;

    private DefaultListModel serverListModel;
    private DefaultTableModel serviceParamTableModel;

    public IcapAntivirusScannerPropertiesDialog(final Window owner, final IcapAntivirusScannerAssertion assertion) {
        super(IcapAntivirusScannerAssertion.class, owner, DIALOG_TITLE, true);
        initComponents();
        intializeServerListSection(owner);
        intializeServiceParametersSection(owner);
        cbStrategy.setModel(new DefaultComboBoxModel(FailoverStrategyFactory.getFailoverStrategyNames()));
        maxMimeDepth.setModel(new SpinnerNumberModel(1, 1, 100, 1));    // max of a 100 is a bit excessive

        setData(assertion);
    }

    private void intializeServerListSection(final Window owner) {
        editServer.setEnabled(false);
        removeServer.setEnabled(false);

        serverList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serverList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                boolean enabled = !serverList.isSelectionEmpty();
                removeServer.setEnabled(enabled);
                editServer.setEnabled(enabled);
            }
        });
        addServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                IcapServerPropertiesDialog ispd = new IcapServerPropertiesDialog(owner, DIALOG_TITLE_NEW_SERVER);
                ispd.pack();
                Utilities.centerOnScreen(ispd);
                ispd.setVisible(true);
                if (ispd.isConfirmed()) {
                    addToServerList(ispd.getIcapUri());
                }
            }
        });
        editServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                editSelectedServer(owner);
            }
        });
        Utilities.setDoubleClickAction(serverList, editServer);
        removeServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (!serverList.isSelectionEmpty()) {
                    serverListModel.remove(serverList.getSelectedIndex());
                }
            }
        });
        connectionTimeoutField.setText("30");
        readTimeoutField.setText("30");
    }


    private void intializeServiceParametersSection(final Window owner) {
        serviceParams.getTableHeader().setReorderingAllowed(false);
        removeParam.setEnabled(false);
        editParam.setEnabled(false);

        serviceParams.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        serviceParams.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        serviceParams.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(final ListSelectionEvent e) {
                boolean enabled = serviceParams.getSelectedRow() >= 0;
                removeParam.setEnabled(enabled);
                editParam.setEnabled(enabled);
            }
        });

        addParam.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                IcapServerParametersDialog dspd = new IcapServerParametersDialog(owner, DIALOG_TITLE_NEW_PARAMETER, serviceParamTableModel);
                dspd.pack();
                Utilities.centerOnScreen(dspd);
                dspd.setVisible(true);
                if (dspd.isConfirmed()) {
                    serviceParamTableModel.addRow(new String[]{dspd.getParameterName(), dspd.getParameterValue()});
                }
            }
        });

        editParam.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                editServerParameter(owner);
            }
        });

        Utilities.setDoubleClickAction(serviceParams, editParam);
        removeParam.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                int selectedIndex = serviceParams.getSelectedRow();
                if (selectedIndex >= 0) {
                    int index = serviceParams.getRowSorter().convertRowIndexToModel(selectedIndex);
                    serviceParamTableModel.removeRow(index);
                }
            }
        });
    }

    private void editSelectedServer(final Window owner) {
        final int selectedIndex = serverList.getSelectedIndex();
        if (selectedIndex >= 0) {
            String icapUri = (String) serverListModel.get(selectedIndex);
            IcapServerPropertiesDialog ispd = new IcapServerPropertiesDialog(owner,
                    DIALOG_TITLE_EDIT_SERVER_PROPERTIES);
            ispd.setIcapServerURL(icapUri);

            ispd.pack();
            Utilities.centerOnScreen(ispd);
            ispd.setVisible(true);
            if (ispd.isConfirmed()) {
                serverListModel.removeElement(icapUri);
                serverListModel.addElement(ispd.getIcapUri());
            }
        }
    }

    private void addToServerList(@NotNull final String uri) {
        boolean found = false;
        for (int i = 0; i < serverListModel.getSize(); ++i) {
            String conn = (String) serverListModel.get(i);
            if (conn.equalsIgnoreCase(uri)) {
                serverListModel.set(i, uri);
                found = true;
                break;
            }
        }
        if (!found) {
            serverListModel.addElement(uri);
        }
    }

    private void editServerParameter(final Window owner) {
        final int selectedIndex = serviceParams.getSelectedRow();
        if (selectedIndex >= 0) {
            int index = serviceParams.getRowSorter().convertRowIndexToModel(selectedIndex);
            String name = serviceParamTableModel.getValueAt(index, 0).toString();
            String value = serviceParamTableModel.getValueAt(index, 1).toString();
            IcapServerParametersDialog dspd = new IcapServerParametersDialog(owner, DIALOG_TITLE_EDIT_PARAMETER, serviceParamTableModel);
            dspd.pack();
            Utilities.centerOnScreen(dspd);
            dspd.setParameterName(name);
            dspd.setParameterValue(value);
            dspd.setVisible(true);
            if (dspd.isConfirmed()) {
                serviceParamTableModel.setValueAt(dspd.getParameterName(), index, 0);
                serviceParamTableModel.setValueAt(dspd.getParameterValue(), index, 1);
            }
        }
    }

    @Override
    public void setData(IcapAntivirusScannerAssertion assertion) {
        continueIfVirusFound.setSelected(assertion.isContinueOnVirusFound());
        for (int i = 0; i < cbStrategy.getModel().getSize(); i++) {
            final FailoverStrategy fs = (FailoverStrategy) cbStrategy.getModel().getElementAt(i);
            if (fs.getName().equals(assertion.getFailoverStrategy())) {
                cbStrategy.setSelectedItem(fs);
                break;
            }
        }
        serverListModel = new DefaultListModel();
        serverList.setModel(serverListModel);
        for (String icd : assertion.getIcapServers()) {
            serverListModel.addElement(icd);
        }
        serviceParamTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return false;
            }
        };
        serviceParamTableModel.setColumnIdentifiers(new String[]{PARAMETER_NAME, PARAMETER_VALUE});
        TableRowSorter sorter = new TableRowSorter<TableModel>(serviceParamTableModel);
        sorter.setSortsOnUpdates(true);
        sorter.setComparator(0, new Comparator<String>() {
            @Override
            public int compare(final String o1, final String o2) {
                //should we consider the Locale?
                return o1.toLowerCase().compareTo(o2.toLowerCase());
            }
        });


        serviceParams.setModel(serviceParamTableModel);
        sorter.setSortable(0, true);
        sorter.toggleSortOrder(0);
        serviceParams.setRowSorter(sorter);
        connectionTimeoutField.setText(assertion.getConnectionTimeout());
        readTimeoutField.setText(assertion.getReadTimeout());
        maxMimeDepth.setValue(assertion.getMaxMimeDepth());

        for (Map.Entry<String, String> ent : assertion.getServiceParameters().entrySet()) {
            serviceParamTableModel.addRow(new String[]{ent.getKey(), ent.getValue()});
        }

        varPrefixPanel.setAssertion( assertion, getPreviousAssertion() );
        varPrefixPanel.setDefaultVariableOrPrefix(IcapAntivirusScannerAssertion.VARIABLE_PREFIX);
        varPrefixPanel.setSuffixes( IcapAntivirusScannerAssertion.getVariableSuffixes() );
        varPrefixPanel.setVariable(assertion.getVariablePrefix());
    }

    @Override
    public IcapAntivirusScannerAssertion getData(IcapAntivirusScannerAssertion assertion) throws ValidationException {
        String timeoutText = connectionTimeoutField.getText().trim();
        if (Syntax.getReferencedNames(timeoutText).length == 0 && !ValidationUtils.isValidInteger(timeoutText, false, 1, ServerIcapAntivirusScannerAssertion.MAX_TIMEOUT)) {
            throw new ValidationException("Connection timeout value must be a valid integer with range 1 to 3600 inclusive.");
        }
        timeoutText = readTimeoutField.getText().trim();
        if (Syntax.getReferencedNames(timeoutText).length == 0 && !ValidationUtils.isValidInteger(timeoutText, false, 1, ServerIcapAntivirusScannerAssertion.MAX_TIMEOUT)) {
            throw new ValidationException("Read timeout value must be a valid integer with range 1 to 3600 inclusive.");
        }
        if(serverListModel.size() < 1){
            throw new ValidationException("One or more ICAP server must be added.");
        }
        assertion.setContinueOnVirusFound(continueIfVirusFound.isSelected());
        assertion.setFailoverStrategy(((FailoverStrategy) cbStrategy.getSelectedItem()).getName());
        assertion.getIcapServers().clear();
        for (int i = 0; i < serverListModel.size(); ++i) {
            assertion.getIcapServers().add((String) serverListModel.get(i));
        }
        Map<String, String> serviceParams = new HashMap<String, String>();
        for (int i = 0; i < serviceParamTableModel.getRowCount(); ++i) {
            String key = (String) serviceParamTableModel.getValueAt(i, 0);
            String value = (String) serviceParamTableModel.getValueAt(i, 1);
            serviceParams.put(key, value);
        }
        assertion.setServiceParameters(serviceParams);
        assertion.setConnectionTimeout(connectionTimeoutField.getText());
        assertion.setReadTimeout(readTimeoutField.getText());
        assertion.setMaxMimeDepth(Integer.valueOf(maxMimeDepth.getModel().getValue().toString()));

        if(varPrefixPanel.getErrorMessage()!=null){
            throw new ValidationException(varPrefixPanel.getErrorMessage());
        }
        assertion.setVariablePrefix(varPrefixPanel.getVariable());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }
}
