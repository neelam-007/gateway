package com.l7tech.console.panels.solutionkit;

import com.l7tech.console.panels.solutionkit.install.InstallSolutionKitWizard;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 *
 */
public class ManageSolutionKitsDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(ManageSolutionKitsDialog.class.getName());

    private JPanel mainPanel;
    private JTable solutionKitsTable;
    private JButton installButton;
    private JButton uninstallButton;
    private JButton upgradeButton;
    private JButton propertiesButton;
    private JButton createButton;
    private JButton closeButton;

    private SimpleTableModel<SolutionKitHeader> solutionKitsModel;
    private final SolutionKitAdmin solutionKitAdmin;

    public ManageSolutionKitsDialog(Frame owner) {
        super(owner, "Manage Solution Kits", true);

        solutionKitAdmin = Registry.getDefault().getSolutionKitAdmin();
        initialize();
        refreshSolutionKitsTable();
    }

    private void initialize() {
        solutionKitsModel = TableUtil.configureTable(solutionKitsTable,
            column("Name", 30, 100, 99999, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    return solutionKitHeader.getName();
                }
            }),
            column("Version", 30, 100, 99999, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    return solutionKitHeader.getSolutionKitVersion();
                }
            }),
            column("Description", 30, 100, 99999, new Functions.Unary<String, SolutionKitHeader>() {
                @Override
                public String call(SolutionKitHeader solutionKitHeader) {
                    return solutionKitHeader.getDescription();
                }
            })
        );

        solutionKitsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        solutionKitsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                refreshSolutionKitTableButtons();
            }
        });

        Utilities.setRowSorter(solutionKitsTable, solutionKitsModel, new int[]{0, 1, 2}, new boolean[]{true, true, true}, new Comparator[]{null, null, null});
        Utilities.setDoubleClickAction(solutionKitsTable, propertiesButton);

        installButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onInstall();
            }
        });

        uninstallButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onUnintall();
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

        Utilities.setEscKeyStrokeDisposes(this);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(mainPanel);

        refreshSolutionKitTableButtons();
    }

    private void onInstall() {
        final InstallSolutionKitWizard wizard = InstallSolutionKitWizard.getInstance(this);
        wizard.pack();
        DialogDisplayer.display(wizard, new Runnable() {
            @Override
            public void run() {
                refreshSolutionKitsTable();
            }
        });
    }

    private void onUnintall() {
        int rowIndex = solutionKitsTable.getSelectedRow();
        if (rowIndex != -1) {
            int modelIndex = solutionKitsTable.getRowSorter().convertRowIndexToModel(rowIndex);
            SolutionKitHeader header = solutionKitsModel.getRowObject(modelIndex);

            boolean cancelled = false;
            boolean successful = false;
            String msg = "";
            try {
                Either<String, String> result = AdminGuiUtils.doAsyncAdmin(
                    solutionKitAdmin,
                    this.getOwner(),
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
                    DialogDisplayer.showMessageDialog(this.getOwner(), msg, "Uninstall Solution Kit", JOptionPane.INFORMATION_MESSAGE, null);
                } else {
                    DialogDisplayer.showMessageDialog(this.getOwner(), msg, "Uninstall Solution Kit", JOptionPane.ERROR_MESSAGE, null);
                }
            }

            refreshSolutionKitsTable();
        }
    }

    private void onUpgrade() {
        DialogDisplayer.showMessageDialog(this, "Not implemented yet.", "Solution Kit Manager", JOptionPane.ERROR_MESSAGE, null);
    }

    private void onProperties() {
        DialogDisplayer.showMessageDialog(this, "Not implemented yet.", "Solution Kit Manager", JOptionPane.ERROR_MESSAGE, null);
    }

    private void onCreate() {
        DialogDisplayer.showMessageDialog(this, "Not implemented yet.", "Solution Kit Manager", JOptionPane.ERROR_MESSAGE, null);
    }

    private void onClose() {
        dispose();
    }

    private void refreshSolutionKitsTable() {
        try {
            solutionKitsModel.setRows(new ArrayList<>(solutionKitAdmin.findSolutionKits()));
        } catch (FindException e) {
            logger.log(Level.WARNING, "Error loading solution kits.", ExceptionUtils.getDebugException(e));
        }
    }

    private void refreshSolutionKitTableButtons() {
        boolean selected = solutionKitsTable.getSelectedRowCount() > 0;
        uninstallButton.setEnabled(selected);
        upgradeButton.setEnabled(selected);
        propertiesButton.setEnabled(selected);
    }
}