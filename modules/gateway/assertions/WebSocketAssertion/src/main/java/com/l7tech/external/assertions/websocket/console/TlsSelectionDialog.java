package com.l7tech.external.assertions.websocket.console;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.JCheckBoxListModel;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * This module allows for the selection of the TLS protocol(s).  It provides a JList dialog where each item is a
 * JCheckBox. The default set of enabled protocols is supplied by a call to the Registry.
 * The selected items will be returned as a String array.
 *
 * Created by chaja24 on 10/20/2016.
 */
public class TlsSelectionDialog extends JDialog {
    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private JList tlsSelectionList;
    private boolean confirmed = false;

    private JCheckBoxListModel tlsProtocolListModel;

    private TlsSelectionDialog(Window owner, String title, ModalityType modalityType, String[] selectedTlsProtocols) {
        super(owner, title, modalityType);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel);

        this.getTlsProtocolListModel().attachToJList(tlsSelectionList);
        this.setSelectedTlsProtocols(selectedTlsProtocols);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!tlsProtocolListModel.isAnyEntryChecked()) {
                    DialogDisplayer.showMessageDialog(tlsSelectionList, "At least one TLS protocol must be enabled.", "Error", JOptionPane.ERROR_MESSAGE, null);
                    return;
                }

                confirmed = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmed = false;
                dispose();
            }
        });

        getRootPane().setDefaultButton(okButton);
        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.equalizeButtonSizes(new JButton[] { okButton, cancelButton });
    }


    /**
     * Show a dialog that offers to (re)configure a cipher suite list string.
     *  @param owner the owner dialog.  required.
     * @param title the title, or null to use a default.
     * @param modalityType the modality type, or null to use the system default for modal dialogs.
     * @param selectedTlsProtocols the TLS protocols that are to be enabled.
     * @param confirmCallback a confirmation callback that will be invoked only if the dialog is confirmed.
     */
    public static void show(Window owner, String title, ModalityType modalityType, String[] selectedTlsProtocols, final Functions.UnaryVoid<String[]> confirmCallback) {
        if (title == null)
            title = "TLS Protocol Configuration";
        if (modalityType == null)
            modalityType = JDialog.DEFAULT_MODALITY_TYPE;
        final TlsSelectionDialog dlg = new TlsSelectionDialog(owner, title, modalityType, selectedTlsProtocols);
        dlg.setModal(true);
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed() && confirmCallback != null) {
                    confirmCallback.call(dlg.getSelectedTlsProtocol());
                }
            }
        });
    }

    private boolean isConfirmed() {

        return confirmed;
    }


   private JCheckBoxListModel getTlsProtocolListModel(){

       if (tlsProtocolListModel != null) {
           return tlsProtocolListModel;
       }

       String[] allTlsProtocols = Registry.getDefault().getTransportAdmin().getAllProtocolVersions(true);
       java.util.List<JCheckBox> checkBoxes = new ArrayList<>();
       for (String tlsProtocol: allTlsProtocols){
           checkBoxes.add(new JCheckBox(tlsProtocol));
       }
       tlsProtocolListModel = new JCheckBoxListModel(checkBoxes);
       return tlsProtocolListModel;
   }


    private void setSelectedTlsProtocols(String[] selectedTlsProtocols){
        final Set<String> selected;
        if (selectedTlsProtocols != null && selectedTlsProtocols.length > 0) {
            selected = new HashSet<>(Arrays.asList(selectedTlsProtocols));
        } else {
            selected = new HashSet<>(Arrays.asList((WebSocketConstants.DEFAULT_TLS_PROTOCOL_LIST)));
        }

        this.getTlsProtocolListModel().visitEntries(new Functions.Binary<Boolean, Integer, JCheckBox>() {
            @Override
            public Boolean call(Integer integer, JCheckBox cb){
                return selected.contains(cb.getText());
            }
        });
    }


    private String[] getSelectedTlsProtocol() {
        final Set<String> selectedTlsProtocols = new HashSet<>();
        this.getTlsProtocolListModel().visitEntries(new Functions.Binary<Boolean, Integer, JCheckBox>(){
            @Override
            public Boolean call(Integer integer, JCheckBox cb){
                if (cb.isSelected()){
                    selectedTlsProtocols.add(cb.getText());
                }
                return null;
            }
        });

        return selectedTlsProtocols.toArray(new String[selectedTlsProtocols.size()]);
    }

}