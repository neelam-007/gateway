package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.console.table.WsdlPortTable;
import com.l7tech.console.util.Registry;
import com.l7tech.uddi.WsdlPortInfo;
import com.l7tech.objectmodel.FindException;

import javax.swing.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Dialog to select a wsdl:port from the list of valid bindingTemplates for a BusinessService in UDDI
 *
 * @author darmstrong
 */
public class SelectWsdlPortDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JScrollPane viewPort;
    private WsdlPortTable wsdlPortTable;
    private final long uddiRegistryOid;
    private final String serviceKey;

    public SelectWsdlPortDialog(final JDialog parent,
                                final long uddiRegistryOid,
                                final String serviceKey) throws FindException {
        super(parent, "Select wsdl:port");
        this.uddiRegistryOid = uddiRegistryOid;
        this.serviceKey = serviceKey;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        initialize();
    }

    private void initialize() throws FindException {
        if (getOwner() == null)
            Utilities.setAlwaysOnTop(this, true);

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);

        wsdlPortTable = new WsdlPortTable();
        viewPort.setViewportView(wsdlPortTable);

        //get all application wsdl infos
        final WsdlPortInfo[] allApplicableWsdlInfos =
                Registry.getDefault().getServiceManager().findWsdlInfosForSingleBusinessService(uddiRegistryOid, serviceKey, false);
        final Vector data = new Vector();
        for(WsdlPortInfo wsdlPortInfo: allApplicableWsdlInfos){
            data.add(wsdlPortInfo);
        }
        wsdlPortTable.getTableSorter().setData(data);

        pack();
        Utilities.centerOnScreen(this);
    }

    private final List<ItemSelectedListener> listeners = new ArrayList<ItemSelectedListener>();

    public void addSelectionListener(ItemSelectedListener selectedListener){
        listeners.add(selectedListener);
    }

    public static interface ItemSelectedListener{
        void itemSelected(Object item);
    }
    
    private void onOK() {

        int row = wsdlPortTable.getSelectedRow();
        if(row == -1){
            dispose();
            return;
        }

        final WsdlPortInfo wsdlPortInfo = (WsdlPortInfo) wsdlPortTable.getTableSorter().getData(row);
        //this will show any required warnings to the user
        final boolean canDispose = SearchUddiDialog.validateWsdlSelection(SelectWsdlPortDialog.this, wsdlPortInfo);
        if(canDispose) fireItemSelectedEvent(wsdlPortInfo);

        dispose();
    }

    /**
     * notify the listeners
     *
     * @param  item Object to send to listeners
     */
    private void fireItemSelectedEvent(final Object item) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for(ItemSelectedListener itemSelectedListener: listeners) {
                    itemSelectedListener.itemSelected(item);
                }
            }
        });
    }
    
    private void onCancel() {
        dispose();
    }

    public static void main(String[] args) throws FindException {
        SelectWsdlPortDialog dialog = new SelectWsdlPortDialog(null, -1, "");
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
