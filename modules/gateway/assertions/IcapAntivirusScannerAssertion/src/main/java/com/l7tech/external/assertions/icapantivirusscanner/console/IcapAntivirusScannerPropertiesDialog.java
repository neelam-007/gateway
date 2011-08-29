package com.l7tech.external.assertions.icapantivirusscanner.console;

import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.icapantivirusscanner.IcapAntivirusScannerAssertion;
import com.l7tech.external.assertions.icapantivirusscanner.IcapConnectionDetail;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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

    private static final String DIALOG_TITLE = "ICAP Anti-Virus Servers";
    private static final String DIALOG_TITLE_NEW_SERVER = "Add New Server";
    private static final String DIALOG_TITLE_EDIT_SERVER_PROPERTIES = "Edit Server Properties";

    private static final String DIALOG_TITLE_NEW_PARAMETER = "Add New Service Parameter";
    private static final String DIALOG_TITLE_EDIT_PARAMETER = "Edit Service Parameter";

    private static final int DOUBLE_CLICK = 2;

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

    private DefaultListModel serverListModel;
    private DefaultTableModel serviceParamTableModel;

    public IcapAntivirusScannerPropertiesDialog(final Window owner, final IcapAntivirusScannerAssertion assertion) {
        super(IcapAntivirusScannerAssertion.class, owner, DIALOG_TITLE, true);
        initComponents();
        intializeServerListSection(owner);
        intializeServiceParametersSection(owner);
        cbStrategy.setModel(new DefaultComboBoxModel(FailoverStrategyFactory.getFailoverStrategyNames()));
        setData(assertion);
    }

    private void intializeServerListSection(final Window owner) {
        editServer.setEnabled(false);
        removeServer.setEnabled(false);

        serverList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        serverList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(final MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == DOUBLE_CLICK) {
                    editSelectedServer(owner);
                }
            }
        });
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
                IcapServerPropertiesDialog ispd = new IcapServerPropertiesDialog((Frame) owner, DIALOG_TITLE_NEW_SERVER);
                ispd.pack();
                Utilities.centerOnScreen(ispd);
                IcapConnectionDetail conn = new IcapConnectionDetail();
                ispd.setViewData(conn);
                ispd.setVisible(true);
                if (ispd.isConfirmed()) {
                    conn = ispd.getConnectionDetail();
                    serverListModel.addElement(conn);
                }
            }
        });
        editServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                editSelectedServer(owner);
            }
        });
        removeServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (!serverList.isSelectionEmpty()) {
                    serverListModel.remove(serverList.getSelectedIndex());
                }
            }
        });
    }

    private void intializeServiceParametersSection(final Window owner) {
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
        serviceParams.addMouseListener(new MouseAdapter() {
            public void mouseClicked(final MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == DOUBLE_CLICK) {
                    editServerParameter(owner);
                }
            }
        });

        addParam.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                IcapServerParametersDialog dspd = new IcapServerParametersDialog((Frame) owner, DIALOG_TITLE_NEW_PARAMETER);
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

        removeParam.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                int selectedIndex = serviceParams.getSelectedRow();
                if (selectedIndex >= 0) {
                    serviceParamTableModel.removeRow(selectedIndex);
                }
            }
        });
    }

    private void editSelectedServer(Window owner) {
        int selectedIndex = serverList.getSelectedIndex();
        if (selectedIndex >= 0) {
            IcapConnectionDetail connectionDetail = (IcapConnectionDetail) serverListModel.get(selectedIndex);
            IcapServerPropertiesDialog ispd = new IcapServerPropertiesDialog((Frame) owner,
                    DIALOG_TITLE_EDIT_SERVER_PROPERTIES);
            ispd.setViewData(connectionDetail);
            ispd.pack();
            Utilities.centerOnScreen(ispd);
            ispd.setVisible(true);
            if (ispd.isConfirmed()) {
                connectionDetail = ispd.getConnectionDetail();
                serverListModel.set(selectedIndex, connectionDetail);
            }
        }
    }

    private void editServerParameter(Window owner) {
        int selectedIndex = serviceParams.getSelectedRow();
        if (selectedIndex >= 0) {
            String name = serviceParamTableModel.getValueAt(selectedIndex, 0).toString();
            String value = serviceParamTableModel.getValueAt(selectedIndex, 1).toString();
            IcapServerParametersDialog dspd = new IcapServerParametersDialog((Frame) owner, DIALOG_TITLE_EDIT_PARAMETER);
            dspd.pack();
            Utilities.centerOnScreen(dspd);
            dspd.setParameterName(name);
            dspd.setParameterValue(value);
            dspd.setVisible(true);
            if (dspd.isConfirmed()) {
                serviceParamTableModel.setValueAt(dspd.getParameterName(), selectedIndex, 0);
                serviceParamTableModel.setValueAt(dspd.getParameterValue(), selectedIndex, 1);
            }
        }
    }

    private static final String PARAMETER_NAME = "Parameter Name";
    private static final String PARAMETER_VALUE = "Parameter Value";

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
        for (IcapConnectionDetail icd : assertion.getConnectionDetails()) {
            serverListModel.addElement(icd);
        }
        serviceParamTableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(final int row, final int column) {
                return false;
            }
        };
        serviceParamTableModel.setColumnIdentifiers(new String[]{PARAMETER_NAME, PARAMETER_VALUE});
        for (Map.Entry<String, String> ent : assertion.getServiceParameters().entrySet()) {
            serviceParamTableModel.addRow(new String[]{ent.getKey(), ent.getValue()});
        }
        serviceParams.setModel(serviceParamTableModel);
    }

    @Override
    public IcapAntivirusScannerAssertion getData(IcapAntivirusScannerAssertion assertion) throws ValidationException {
        assertion.setContinueOnVirusFound(continueIfVirusFound.isSelected());
        assertion.setFailoverStrategy(((FailoverStrategy) cbStrategy.getSelectedItem()).getName());
        assertion.getConnectionDetails().clear();
        for (int i = 0; i < serverListModel.size(); ++i) {
            assertion.getConnectionDetails().add((IcapConnectionDetail) serverListModel.get(i));
        }

        Map<String, String> serviceParams = new HashMap<String, String>();
        for (int i = 0; i < serviceParamTableModel.getRowCount(); ++i) {
            String key = (String) serviceParamTableModel.getValueAt(i, 0);
            String value = (String) serviceParamTableModel.getValueAt(i, 1);
            serviceParams.put(key, value);
        }
        assertion.setServiceParameters(serviceParams);
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }
}
