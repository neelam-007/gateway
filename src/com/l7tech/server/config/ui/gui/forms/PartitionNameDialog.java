package com.l7tech.server.config.ui.gui.forms;

import com.l7tech.console.text.FilterDocument;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.RunOnChangeListener;

import javax.swing.*;
import java.awt.event.*;

public class PartitionNameDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField partitionName;
    private boolean wasCancelled;

    public PartitionNameDialog(JDialog parent, String oldName) {
        super(parent, "Rename Partition", true);
        initialize(oldName);
    }

    public PartitionNameDialog(JFrame parent, String oldName) {
        super(parent, "Rename Partition", true);
        initialize(oldName);
    }

    private void initialize(String oldName) {
        FilterDocument.Filter partitionNameFilter = new FilterDocument.Filter() {
            public boolean accept(String s) {
                return s.matches(PartitionInformation.DEFAULT_PARTITION_NAME) || s.matches(PartitionInformation.ALLOWED_PARTITION_NAME_PATTERN);
            }
        };
        partitionName.setDocument(new FilterDocument(128, partitionNameFilter));
        partitionName.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            public void run() {
                buttonOK.setEnabled(partitionName.getDocument().getLength() != 0);
            }
        }));

        partitionName.setText((oldName == null?"":oldName));

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        Utilities.equalizeButtonSizes(new JButton[]{
            buttonOK,
            buttonCancel
        });
        
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);
        pack();
    }

    public String getPartitionName() {
        return partitionName.getText();
    }

    private void onOK() {
        wasCancelled = false;
        dispose();
    }

    private void onCancel() {
        wasCancelled = true;
        dispose();
    }

    public boolean wasCancelled() {
        return wasCancelled;
    }
}
