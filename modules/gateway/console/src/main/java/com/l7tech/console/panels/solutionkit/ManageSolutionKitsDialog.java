package com.l7tech.console.panels.solutionkit;

import com.l7tech.console.action.DeleteEntityNodeAction;
import com.l7tech.console.panels.solutionkit.install.InstallSolutionKitWizard;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.gateway.common.solutionkit.SolutionKitUtils;
import com.l7tech.gui.ErrorMessageDialog;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.apache.commons.lang.WordUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
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
                final SolutionKitHeader solutionKitHT = solutionKitTablePanel.getFirstSelectedSolutionKit();
                try {
                    // The solution kit is a parent solution kit
                    if (solutionKitAdmin.findHeaders(solutionKitHT.getGoid()).size() > 0) {
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

        // disable features causing permission exceptions in the applet security manager
        if (!TopComponents.getInstance().isApplet()) {
            solutionKitTablePanel.setDoubleClickAction(propertiesButton);
        }

        Utilities.setEscKeyStrokeDisposes(this);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(mainPanel);
    }

    private void onInstall() {
        try {
            final InstallSolutionKitWizard wizard = InstallSolutionKitWizard.getInstance(this);
            wizard.pack();
            Utilities.centerOnParentWindow(wizard);
            DialogDisplayer.display(wizard, new Runnable() {
                @Override
                public void run() {
                    refreshSolutionKitsTable();
                }
            });
        } catch (FindException e) {
            final String msg = "Unable to install solution kit: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            DialogDisplayer.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE, null);
        }
    }

    private void onUninstall() {
        final Collection<SolutionKitHeader> headers = solutionKitTablePanel.getSelectedSolutionKits();
        if (headers == null || headers.isEmpty()) {
            return;
        }

        DialogDisplayer.showSafeConfirmDialog(
                ManageSolutionKitsDialog.this,
                WordUtils.wrap("Are you sure you want to uninstall the selected solution kit(s)?", DeleteEntityNodeAction.LINE_CHAR_LIMIT, null, true),
                "Confirm Solution Kit Uninstall",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (option != JOptionPane.YES_OPTION) {
                            return;
                        }

                        List<String> resultMsgs = new ArrayList<>();

                        for (SolutionKitHeader header : headers) {
                            Pair<Boolean, String> result = new Pair<>(true, "");
                            try {
                                Collection<SolutionKitHeader> children = solutionKitAdmin.findHeaders(header.getGoid());
                                if (!children.isEmpty()) {
                                    for (SolutionKitHeader child : children) {
                                        result = uninstallSolutionKit(child.getName(), child.getGoid());
                                        if (!result.left) {
                                            resultMsgs.add("<br/>Solution Kit that failed at uninstall:<br/>- " + child.getName() + " with Instance Modifier: '" +
                                                    child.getInstanceModifier() + "' <br/><br/>Please check audit logs for more details. ");
                                            break;
                                        }
                                        resultMsgs.add("- " + child.getName() + " with Instance Modifier: '" + child.getInstanceModifier() + "' <br/> ");
                                    }
                                }

                                final SolutionKit selectedSK = solutionKitAdmin.get(header.getGoid());
                                if (result.left && selectedSK != null) {
                                    if (SolutionKitUtils.isParentSolutionKit(selectedSK)) {
                                        solutionKitAdmin.delete(header.getGoid());
                                        result = new Pair<>(true, "Solution kit " + "'" + header.getName() + "' uninstalled successfully.");
                                    } else {
                                        result = uninstallSolutionKit(header.getName(), header.getGoid());
                                    }
                                }
                            } catch (Exception e) {
                                String msg = ExceptionUtils.getMessage(e);
                                logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
                                result = new Pair<>(false, msg);
                            }

                            if (!result.left) {
                                Throwable throwable = new Throwable(result.right);

                                String resultsWithErrors = "<html>";
                                if (resultMsgs.size()>1) {
                                    resultsWithErrors += "Number of solution kits that uninstalled before failure: " + (resultMsgs.size()-1) +
                                            "<br/><br/>Solution Kit(s) that uninstalled successfully: <br/>";
                                    for (String resultString: resultMsgs) {
                                        resultsWithErrors += resultString;
                                    }
                                } else
                                if (resultMsgs.size()==1){
                                    resultsWithErrors += resultMsgs.get(0);
                                } else {
                                    resultsWithErrors += "Solution Kit Manager failed to uninstall this solution kit.<br/> Please check audit logs for details. ";
                                }
                                resultsWithErrors += "</html>";

                                ErrorMessageDialog errorMessageDialog = new ErrorMessageDialog(ManageSolutionKitsDialog.this, resultsWithErrors, throwable);
                                Utilities.centerOnParentWindow(errorMessageDialog);
                                DialogDisplayer.pack(errorMessageDialog);
                                DialogDisplayer.display(errorMessageDialog);
                            }
                        }

                        refreshSolutionKitsTable();
                    }
                });
    }

    private Pair<Boolean, String> uninstallSolutionKit(@NotNull final String skName, @NotNull final Goid skGoid) throws InvocationTargetException, InterruptedException {
        boolean successful = false;
        String msg = "";

        Either<String, String> result = AdminGuiUtils.doAsyncAdmin(
            solutionKitAdmin,
            ManageSolutionKitsDialog.this,
            "Uninstall Solution Kit",
            "The gateway is uninstalling the solution kit, " + skName,
            solutionKitAdmin.uninstallAsync(skGoid),
            false);

        if (result.isLeft()) {
            msg = result.left();
            logger.log(Level.WARNING, msg);
        } else if (result.isRight()) {
            if ("".equals(result.right())) {
                msg = "This solution kit requires a manual uninstall. The solution kit record has been deleted, please manually delete entities previously installed by this solution kit.";
            } else {
                msg = "Solution kit " + "'" + skName + "' uninstalled successfully.";
            }
            successful = true;
        }

        return new Pair<>(successful, msg);
    }

    private void onUpgrade() {
        SolutionKitHeader header = solutionKitTablePanel.getFirstSelectedSolutionKit();
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
        SolutionKitHeader header = solutionKitTablePanel.getFirstSelectedSolutionKit();
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
        TopComponents.getInstance().refreshEntitiesProtectionCache();
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
        int selectedRowCount = solutionKitTablePanel.getSelectedRowCount();
        uninstallButton.setEnabled(selectedRowCount > 0);

        // disable features causing permission exceptions in the applet security manager
        if (TopComponents.getInstance().isApplet()) {
            installButton.setEnabled(false);
            upgradeButton.setEnabled(false);
            propertiesButton.setEnabled(false);
        } else {
            upgradeButton.setEnabled(selectedRowCount == 1);
            propertiesButton.setEnabled(selectedRowCount == 1);
        }
    }
}