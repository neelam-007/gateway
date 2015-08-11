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
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
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
        solutionKitTablePanel.addEnterKeyBinding(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SolutionKitHeader solutionKitHT = solutionKitTablePanel.getSelectedSolutionKit();
                try {
                    // The solution kit is a parent solution kit
                    if (solutionKitAdmin.findAllChildrenByParentGoid(solutionKitHT.getGoid()).size() > 0) {
                        if (solutionKitTablePanel.findDisplayingType(solutionKitHT) == SolutionKitTablePanel.SolutionKitDisplayingType.PARENT_EXPENDED) {
                            onProperties();
                        } else {
                            solutionKitTablePanel.insertChildren(solutionKitHT);
                        }
                    }
                    // The solution kit is a child solution kit
                    else {
                        onProperties();
                    }
                } catch (FindException e1) {
                    logger.warning(ExceptionUtils.getMessage(e1));
                    DialogDisplayer.showMessageDialog(ManageSolutionKitsDialog.this, "Error on finding child solution kits of '" + solutionKitHT.getName() + "'", "Manage Solution Kits", JOptionPane.ERROR_MESSAGE, null);
                }
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
                    Pair<Boolean, String> result = new Pair<>(false, "");
                    try {
                        Collection<SolutionKitHeader> children = solutionKitAdmin.findAllChildrenByParentGoid(header.getGoid());
                        if (! children.isEmpty()) {
                            for (SolutionKitHeader child: children) {
                                result = uninstallSolutionKit(child.getName(), child.getGoid());
                                if (! result.left) break;
                            }
                        }

                        final SolutionKit selectedSK = solutionKitAdmin.get(header.getGoid());
                        if (SolutionKit.PARENT_SOLUTION_KIT_DUMMY_MAPPINGS.equals(selectedSK.getMappings())) {
                            solutionKitAdmin.deleteSolutionKit(header.getGoid());
                        } else {
                            result = uninstallSolutionKit(header.getName(), header.getGoid());
                        }
                    } catch (InterruptedException e) {
                        // do nothing.
                        cancelled = true;
                    } catch (Exception e) {
                        String msg = ExceptionUtils.getMessage(e);
                        logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
                        result = new Pair<>(false, msg);
                    }

                    if (!cancelled) {
                        if (result.left) {
                            DialogDisplayer.showMessageDialog(ManageSolutionKitsDialog.this, result.right, "Uninstall Solution Kit", JOptionPane.INFORMATION_MESSAGE, null);
                        } else {
                            DialogDisplayer.showMessageDialog(ManageSolutionKitsDialog.this, result.right, "Uninstall Solution Kit", JOptionPane.ERROR_MESSAGE, null);
                        }
                    }

                    refreshSolutionKitsTable();
                }
            }
        );
    }

    private Pair<Boolean, String> uninstallSolutionKit(@NotNull final String skName, @NotNull final Goid skGoid) throws InvocationTargetException, InterruptedException {
        boolean successful = false;
        String msg = "";

        Either<String, String> result = AdminGuiUtils.doAsyncAdmin(
            solutionKitAdmin,
            ManageSolutionKitsDialog.this,
            "Uninstall Solution Kit",
            "The gateway is uninstalling the solution kit, " + skName,
            solutionKitAdmin.uninstall(skGoid),
            false);

        if (result.isLeft()) {
            msg = result.left();
            logger.log(Level.WARNING, msg);
        } else if (result.isRight()) {
            if ("".equals(result.right())) {
                msg = "This solution kit requires a manual uninstall. The solution kit record has been deleted, please manually delete entities previously installed by this solution kit.";
            } else {
                msg = "Solution kit uninstalled successfully.";
            }
            successful = true;
        }

        return new Pair<>(successful, msg);
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
            solutionKitTablePanel.refreshSolutionKitTable();
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