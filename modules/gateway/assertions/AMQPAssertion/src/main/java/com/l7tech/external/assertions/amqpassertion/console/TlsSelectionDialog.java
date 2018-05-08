package com.l7tech.external.assertions.amqpassertion.console;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.amqpassertion.AMQPDestination;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.JCheckBoxListModel;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.stream.Stream;

public class TlsSelectionDialog extends JDialog {

    private static final String PROTOCOL_SSLV2HELLO = "SSLv2Hello";
    private static final String TITLE_ERROR = "Error";
    private static final String MESSAGE_ENABLE_ONE_PROTOCOL = "At least one TLS protocol must be enabled.";
    private static final String MESSAGE_ENABLE_ANOTHER_PROTOCOL = PROTOCOL_SSLV2HELLO + " requires at least one other protocol to be enabled";

    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
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
                    DialogDisplayer.showMessageDialog(tlsSelectionList, MESSAGE_ENABLE_ONE_PROTOCOL, TITLE_ERROR,
                            JOptionPane.ERROR_MESSAGE, null);
                    return;
                }

                final java.util.List<String> selectedProtocols = Arrays.asList(getSelectedTlsProtocol());
                final int numberOfCheckedEntries = tlsProtocolListModel.getAllCheckedEntries().size();
                if (selectedProtocols.contains(PROTOCOL_SSLV2HELLO) && numberOfCheckedEntries < 2) {
                    DialogDisplayer.showMessageDialog(tlsSelectionList, MESSAGE_ENABLE_ANOTHER_PROTOCOL, TITLE_ERROR,
                            JOptionPane.ERROR_MESSAGE, null);
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
            selected = new HashSet<>(Arrays.asList((AMQPDestination.DEFAULT_TLS_PROTOCOL_LIST.toArray(new String[]{}))));
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
