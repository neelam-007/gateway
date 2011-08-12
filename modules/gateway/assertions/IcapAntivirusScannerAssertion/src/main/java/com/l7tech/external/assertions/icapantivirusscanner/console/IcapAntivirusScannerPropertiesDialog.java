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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class IcapAntivirusScannerPropertiesDialog extends AssertionPropertiesOkCancelSupport<IcapAntivirusScannerAssertion> {
    private static final String DIALOG_TITLE = "ICAP Anti-Virus Servers";
    private static final String DIALOG_TITLE_NEW_SERVER = "New Server";
    private static final String DIALOG_TITLE_EDIT_SERVER_PREFIX = "Properties for ";
    private static final int DOUBLE_CLICK = 2;

    private JPanel contentPane;
    private JButton add;
    private JButton remove;
    private JButton edit;
    private JList serverList;
    private JComboBox cbStrategy;
    private JCheckBox failAssertionIfVirusCheckBox;

    private DefaultListModel serverListModel;

    public IcapAntivirusScannerPropertiesDialog(final Window owner, final IcapAntivirusScannerAssertion assertion) {
        super(IcapAntivirusScannerAssertion.class, owner, DIALOG_TITLE, true);
        initComponents(owner);
        setData(assertion);
    }

    private void initComponents(final Window owner) {
        super.initComponents();
        edit.setEnabled(false);
        remove.setEnabled(false);
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
                remove.setEnabled(enabled);
                edit.setEnabled(enabled);
            }
        });
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                IcapConnectionDetail conn = new IcapConnectionDetail();
                showPropertiesDialog(owner, DIALOG_TITLE_NEW_SERVER, conn);
                serverListModel.addElement(conn);
            }
        });
        edit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                editSelectedServer(owner);
            }
        });
        remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                if (!serverList.isSelectionEmpty()) {
                    serverListModel.remove(serverList.getSelectedIndex());
                }
            }
        });
        cbStrategy.setModel(new DefaultComboBoxModel(FailoverStrategyFactory.getFailoverStrategyNames()));
    }

    private void showPropertiesDialog(final Window owner, final String title, final IcapConnectionDetail connectionDetail) {
        IcapServerPropertiesDialog ispd = new IcapServerPropertiesDialog((Frame) owner, title, connectionDetail);
        ispd.pack();
        Utilities.centerOnScreen(ispd);
        ispd.setVisible(true);
    }

    private void editSelectedServer(final Window owner) {
        int selectedIndex = serverList.getSelectedIndex();
        IcapConnectionDetail conn = (IcapConnectionDetail) serverListModel.get(selectedIndex);
        showPropertiesDialog(owner, DIALOG_TITLE_EDIT_SERVER_PREFIX + conn.getConnectionName(), conn);
        serverListModel.set(selectedIndex, conn);
    }

    @Override
    public void setData(IcapAntivirusScannerAssertion assertion) {
        failAssertionIfVirusCheckBox.setSelected(assertion.isFailOnVirusFound());
        for (int i = 0; i < cbStrategy.getModel().getSize(); i++) {
            FailoverStrategy fs = (FailoverStrategy) cbStrategy.getModel().getElementAt(i);
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
    }

    @Override
    public IcapAntivirusScannerAssertion getData(IcapAntivirusScannerAssertion assertion) throws ValidationException {
        assertion.setFailOnVirusFound(failAssertionIfVirusCheckBox.isSelected());
        assertion.setFailoverStrategy(((FailoverStrategy) cbStrategy.getSelectedItem()).getName());
        assertion.getConnectionDetails().clear();
        for (int i = 0; i < serverListModel.size(); ++i) {
            assertion.getConnectionDetails().add((IcapConnectionDetail) serverListModel.get(i));
        }
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }
}
