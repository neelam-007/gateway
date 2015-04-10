package com.l7tech.console.panels.solutionkit;

import com.l7tech.console.panels.solutionkit.install.InstallSolutionKitWizard;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog used to manage solution kits.
 */
public class ManageSolutionKitsDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(ManageSolutionKitsDialog.class.getName());

    private JPanel mainPanel;
    private SolutionKitTablePanel solutionKitTablePanel;
    private JButton installButton;
    private JButton uninstallButton;
    private JButton upgradeButton;
    private JButton propertiesButton;
    private JButton createButton;
    private JButton closeButton;

    private final SolutionKitAdmin solutionKitAdmin;

    /**
     * Create dialog.
     *
     * @param owner the owner of the dialog
     */
    public ManageSolutionKitsDialog(Frame owner) {
        super(owner, "Manage Solution Kits", true);

        solutionKitAdmin = Registry.getDefault().getSolutionKitAdmin();
        initialize();
        refreshSolutionKitsTable();
        refreshSolutionKitsTableButtons();
    }

    private void initialize() {
        solutionKitTablePanel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                refreshSolutionKitsTableButtons();
            }
        });

        installButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onInstall();
            }
        });

        uninstallButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onUninstall();
            }
        });

        upgradeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onUpgrade();
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onProperties();
            }
        });

        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCreate();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onClose();
            }
        });

        solutionKitTablePanel.setDoubleClickAction(propertiesButton);

        Utilities.setEscKeyStrokeDisposes(this);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(mainPanel);
    }

    private void onInstall() {
        final InstallSolutionKitWizard wizard = InstallSolutionKitWizard.getInstance(this);
        wizard.pack();
        Utilities.centerOnParentWindow(wizard);
        DialogDisplayer.display(wizard, new Runnable() {
            @Override
            public void run() {
                refreshSolutionKitsTable();
            }
        });
    }

    private void onUninstall() {
        final SolutionKitHeader header = solutionKitTablePanel.getSelectedSolutionKit();
        if (header == null) {
            return;
        }

        DialogDisplayer.showConfirmDialog(
            this.getOwner(),
            "Are you sure you want to uninstall the selected solution kit?",
            "Uninstall Solution Kit",
            JOptionPane.YES_NO_OPTION,
            new DialogDisplayer.OptionListener() {
                @Override
                public void reportResult(int option) {
                    if (option == JOptionPane.NO_OPTION) {
                        return;
                    }

                    boolean cancelled = false;
                    boolean successful = false;
                    String msg = "";
                    try {
                        Either<String, String> result = AdminGuiUtils.doAsyncAdmin(
                            solutionKitAdmin,
                            ManageSolutionKitsDialog.this,
                            "Uninstall Solution Kit",
                            "The gateway is uninstalling selected solution kit",
                            solutionKitAdmin.uninstall(header.getGoid()));

                        if (result.isLeft()) {
                            msg = result.left();
                            logger.log(Level.WARNING, msg);
                        } else if (result.isRight()) {
                            msg = "Solution kit uninstalled successfully.";
                            successful = true;
                        }
                    } catch (InvocationTargetException e) {
                        msg = ExceptionUtils.getMessage(e);
                        logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
                    } catch (InterruptedException e) {
                        // do nothing.
                        cancelled = true;
                    }

                    if (!cancelled) {
                        if (successful) {
                            DialogDisplayer.showMessageDialog(ManageSolutionKitsDialog.this, msg, "Uninstall Solution Kit", JOptionPane.INFORMATION_MESSAGE, null);
                        } else {
                            DialogDisplayer.showMessageDialog(ManageSolutionKitsDialog.this, msg, "Uninstall Solution Kit", JOptionPane.ERROR_MESSAGE, null);
                        }
                    }

                    refreshSolutionKitsTable();
                }
            }
        );
    }

    private void onUpgrade() {
        SolutionKitHeader header = solutionKitTablePanel.getSelectedSolutionKit();
        if (header != null) {
            try {
                SolutionKit solutionKitToUpgrade = solutionKitAdmin.get(header.getGoid());
                final InstallSolutionKitWizard wizard = InstallSolutionKitWizard.getInstance(this, solutionKitToUpgrade);
                wizard.pack();
                Utilities.centerOnParentWindow(wizard);
                DialogDisplayer.display(wizard, new Runnable() {
                    @Override
                    public void run() {
                        refreshSolutionKitsTable();
                    }
                });
            } catch (FindException e) {
                final String msg = "Unable to upgrade solution kit: " + ExceptionUtils.getMessage(e);
                logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
                DialogDisplayer.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE, null);
            }
        }
    }

    private void onProperties() {
        SolutionKitHeader header = solutionKitTablePanel.getSelectedSolutionKit();
        if (header != null) {
            try {
                SolutionKit solutionKit = solutionKitAdmin.get(header.getGoid());
                final SolutionKitPropertiesDialog dlg = new SolutionKitPropertiesDialog(this, solutionKit);
                dlg.pack();
                Utilities.centerOnParentWindow(dlg);
                DialogDisplayer.display(dlg);
            } catch (FindException e) {
                final String msg = "Unable to view solution kit properties: " + ExceptionUtils.getMessage(e);
                logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
                DialogDisplayer.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE, null);
            }
        }
    }

    private void onCreate() {
        DialogDisplayer.showMessageDialog(this, "Not implemented yet.", "Solution Kit Manager", JOptionPane.ERROR_MESSAGE, null);
    }

    private void onClose() {
        TopComponents.getInstance().refreshPoliciesFolderNode();
        dispose();
    }

    private void refreshSolutionKitsTable() {
        try {
            solutionKitTablePanel.setData(new ArrayList<>(solutionKitAdmin.findSolutionKits()));
        } catch (FindException e) {
            logger.log(Level.WARNING, "Error loading solution kits.", ExceptionUtils.getDebugException(e));
        }
    }

    private void refreshSolutionKitsTableButtons() {
        boolean selected = solutionKitTablePanel.isSolutionKitSelected();
        uninstallButton.setEnabled(selected);
        upgradeButton.setEnabled(selected);
        propertiesButton.setEnabled(selected);
    }
}