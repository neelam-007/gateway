package com.l7tech.external.assertions.sophos.console;

import com.l7tech.common.io.failover.FailoverStrategy;
import com.l7tech.common.io.failover.FailoverStrategyFactory;
import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.sophos.SophosAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 5-Jan-2011
 * Time: 2:08:24 PM
 * To change this template use File | Settings | File Templates.
 */
public class SophosAssertionPropertiesPanel extends AssertionPropertiesOkCancelSupport<SophosAssertion> {
    private final SophosAssertion assertion;
    private JPanel mainPanel;
    private JButton addButton;
    private JButton editButton;
    private JButton removeButton;
    private JList ipPortList;
    private DefaultListModel ipPortListModel;
    private JComboBox cbStrategy;
    private JCheckBox failAssertionOnErrorCheckBox;
    private JPanel listPanel;

    public SophosAssertionPropertiesPanel(Frame parent, SophosAssertion assertion) {
        super(SophosAssertion.class, parent, assertion.getPropertiesDialogTitle(), true);

        initComponents();
        setData(assertion);

        this.assertion = assertion;

    }

    @Override
    protected void updateOkButtonEnableState() {
        boolean enableAny = !isReadOnly() && ipPortList.getModel().getSize() > 0;
        getOkButton().setEnabled( enableAny );
    }

    @Override
    protected void initComponents() {

        super.initComponents();

        ipPortList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ipPortList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                removeButton.setEnabled(ipPortList.getSelectedIndex() > -1);
                editButton.setEnabled(ipPortList.getSelectedIndex() > -1);
            }
        });


        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ipPortListModel.remove(ipPortList.getSelectedIndex());
            }
        });
        removeButton.setEnabled(false);

        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Container rootPane = SwingUtilities.getWindowAncestor(SophosAssertionPropertiesPanel.this);

                if(ipPortList.getSelectedIndex() == -1) {
                    return;
                }

                String hostPort = (String)ipPortList.getSelectedValue();
                String host = hostPort.substring(0, hostPort.lastIndexOf(':'));
                int port = Integer.parseInt(hostPort.substring(hostPort.lastIndexOf(':') + 1));

                final GetIpPortDialog dlg;
                final String title = "Edit Host/Port";
                if (rootPane instanceof Frame)
                    dlg = new GetIpPortDialog((Frame)rootPane, title, host, port);
                else if (rootPane instanceof Dialog)
                    dlg = new GetIpPortDialog((Dialog)rootPane, title, host, port);
                else
                    dlg = new GetIpPortDialog((Frame)null, title, host, port);

                dlg.pack();
                Utilities.centerOnScreen(dlg);

                dlg.setVisible(true);
                if(dlg.isConfirmed()) {
                    ipPortListModel.setElementAt(dlg.getHost() + ":" + dlg.getPort(), ipPortList.getSelectedIndex());
                }
            }
        });
        editButton.setEnabled(false);

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Container rootPane = SwingUtilities.getWindowAncestor(SophosAssertionPropertiesPanel.this);
                final GetIpPortDialog dlg;
                final String title = "Add Host/Port";
                if (rootPane instanceof Frame)
                    dlg = new GetIpPortDialog((Frame)rootPane, title, null, 4000);
                else if (rootPane instanceof Dialog)
                    dlg = new GetIpPortDialog((Dialog)rootPane, title, null, 4000);
                else
                    dlg = new GetIpPortDialog((Frame)null, title, null, 4000);

                dlg.pack();
                Utilities.centerOnScreen(dlg);

                dlg.setVisible(true);
                if(dlg.isConfirmed()) {
                    ipPortListModel.addElement(dlg.getHost() + ":" + dlg.getPort());
                }
            }
        });
        
        cbStrategy.setModel(new DefaultComboBoxModel(FailoverStrategyFactory.getFailoverStrategyNames()));
        cbStrategy.setSelectedIndex(0);

    }

    @Override
    protected JPanel createPropertyPanel() {
        getOkButton().setEnabled(false);
        return mainPanel;
    }

    @Override
    public void setData(SophosAssertion assertion) {
        ipPortListModel = new DefaultListModel();
        for(String hostPort : assertion.getAddresses()) {
            ipPortListModel.addElement(hostPort);
        }
        ipPortListModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                enableDisableComponents();
            }
            @Override
            public void intervalRemoved(ListDataEvent e) {
                enableDisableComponents();
            }
            @Override
            public void contentsChanged(ListDataEvent e) {
                enableDisableComponents();
            }
        });
        ipPortList.setModel(ipPortListModel);
        failAssertionOnErrorCheckBox.setSelected(assertion.isFailOnError());
        if(assertion.getFailoverStrategyName() != null) {
            for(int i = 0;i < cbStrategy.getModel().getSize();i++) {
                FailoverStrategy fs = (FailoverStrategy)cbStrategy.getModel().getElementAt(i);
                if(assertion.getFailoverStrategyName().equals(fs.getName())) {
                    cbStrategy.setSelectedItem(fs);
                }
            }
        } else {
            cbStrategy.setSelectedIndex(0);
        }
        enableDisableComponents();
    }

    @Override
    public SophosAssertion getData(SophosAssertion assertion) throws ValidationException {

        String[] hostPorts = new String[ipPortListModel.size()];
        ipPortListModel.copyInto(hostPorts);

        assertion.setAddresses(hostPorts);

        assertion.setFailoverStrategyName(((FailoverStrategy)cbStrategy.getSelectedItem()).getName());
        assertion.setFailOnError(failAssertionOnErrorCheckBox.isSelected());

        return assertion;
    }

    private void enableDisableComponents() {
        boolean enableAny = !isReadOnly() && ipPortList.getModel().getSize() > 0;
        getOkButton().setEnabled( enableAny );

    }
}
