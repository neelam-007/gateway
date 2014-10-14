package com.l7tech.console.panels.bundles;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.policy.bundle.PolicyBundleDryRunResult;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class ConflictDisplayerDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTabbedPane tabbedPane;
    private boolean wasoked;
    private boolean hasInfoOnlyConflict;

    // holds the aggregate of selected resolutions across all bundles
    private Map<String, String> selectedMigrationResolutions = new Hashtable<>();

    public ConflictDisplayerDialog(final Window owner,
                                   final List<BundleInfo> bundleInfos,
                                   final PolicyBundleDryRunResult dryRunResult) throws PolicyBundleDryRunResult.UnknownBundleIdException {
        super(owner, "Conflicts detected");
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

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
        getRootPane().setDefaultButton(buttonCancel);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        tabbedPane.removeAll();
        for (BundleInfo bundleInfo : bundleInfos) {
            if (dryRunResult.anyConflictsForBundle(bundleInfo.getId())) {
                BundleConflictComponent bundleConflictComponent = new BundleConflictComponent(this, bundleInfo.getId(), dryRunResult, selectedMigrationResolutions);
                JPanel mainPanel = bundleConflictComponent.getMainPanel();

                if (bundleConflictComponent.hasInfoOnlyConflict()) {
                    hasInfoOnlyConflict = true;
                }

                tabbedPane.add(bundleInfo.getName(), mainPanel);
            }
        }
    }

    public boolean wasOKed() {
        return wasoked;
    }

    public Map<String, String> getSelectedMigrationResolutions() {
        return selectedMigrationResolutions;
    }

    private void onOK() {
        if (hasInfoOnlyConflict) {
            DialogDisplayer.showSafeConfirmDialog(
                    this,
                    "<html><left><p>Be aware that continuing installation with detected conflicts may result in partial installation of components: (1) Components with</p>" +
                            "<p>detected conflicts will NOT be installed. (2) References to a conflicted component will be replaced with references to a version of</p>" +
                            "<p>the same component on the target Gateway, if one exists. These references may occur in service policies or policy fragments.</p></left></html>",
                    "Confirm Installation with Conflicts",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    new DialogDisplayer.OptionListener() {
                        @Override
                        public void reportResult(int option) {
                            if (option == JOptionPane.CANCEL_OPTION) {
                                return;
                            }
                            wasoked = true;
                            dispose();
                        }
                    }
            );
        } else {
            wasoked = true;
            dispose();
        }
    }

    private void onCancel() {
        dispose();
    }
}
