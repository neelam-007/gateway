package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.licensing.ManageLicensesDialog;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.signer.SignerUtils;
import com.l7tech.gateway.common.security.signer.TrustedSignerCertsAdmin;
import com.l7tech.gateway.common.security.signer.TrustedSignerCertsHelper;
import com.l7tech.gateway.common.solutionkit.*;
import com.l7tech.gui.util.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Option;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.security.SignatureException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard panel which allows the user to select a solution kit file to install.
 */
public class SolutionKitLoadPanel extends WizardStepPanel<SolutionKitsConfig> {
    private static final Logger logger = Logger.getLogger(SolutionKitLoadPanel.class.getName());

    private static final String STEP_LABEL = "Choose Solution Kit File";
    private static final String STEP_DESC = "Specify the location of the solution kit file.";

    private static final FileFilter SK_FILE_FILTER = FileChooserUtil.buildFilter(".skar", "Skar (*.skar)");
    private static final FileFilter SIGNED_SK_FILE_FILTER = FileChooserUtil.buildFilter(".sskar", "Signed Skar (*.sskar)");

    private JPanel mainPanel;
    private JTextField fileTextField;
    private JButton fileButton;

    private SolutionKitsConfig solutionKitsConfig;
    @NotNull
    private final TrustedSignerCertsAdmin trustedSignerCertsAdmin;

    public SolutionKitLoadPanel() {
        super(null);
        final Option<TrustedSignerCertsAdmin> option = Registry.getDefault().getAdminInterface(TrustedSignerCertsAdmin.class);
        if (option.isSome()) {
            this.trustedSignerCertsAdmin = option.some();
        } else {
            throw new RuntimeException("TrustedSignerCertsAdmin interface not found.");
        }
        initialize();
    }

    @Override
    public String getStepLabel() {
        return STEP_LABEL;
    }

    @Override
    public String getDescription() {
        return STEP_DESC;
    }

    @Override
    public boolean canAdvance() {
        return !fileTextField.getText().trim().isEmpty();
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public void readSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        solutionKitsConfig = settings;
        solutionKitsConfig.clear(false);
    }

    @Override
    public boolean onNextButton() {
        File file = new File(fileTextField.getText().trim());
        if (!file.exists()) {
            DialogDisplayer.showMessageDialog(this.getOwner(), "The file does not exist.", "Error", JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        if (!file.isFile()) {
            DialogDisplayer.showMessageDialog(this.getOwner(), "The file must be a file type.", "Error", JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        final SignerUtils.SignedZip signedZip = new SignerUtils.SignedZip(TrustedSignerCertsHelper.getTrustedCertificatesForSolutionKits(trustedSignerCertsAdmin));
        try (final SkarPayload payload = signedZip.load(file, new SkarPayloadFactory(solutionKitsConfig))) {
            payload.process();
        } catch (IOException | SignatureException | SolutionKitException e) {
            solutionKitsConfig.clear(false);
            final String msg = "Unable to open solution kit: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            DialogDisplayer.showMessageDialog(this.getOwner(), msg, "Error", JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        // Check the license of the parent SKAR, before the next wizard step SolutionKitSelectionPanel proceeds.
        final SolutionKit parentSK = solutionKitsConfig.getParentSolutionKitLoaded();
        if (parentSK != null) {
            final String featureSet = parentSK.getProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY);
            if (! (StringUtils.isEmpty(featureSet) || ConsoleLicenseManager.getInstance().isFeatureEnabled(featureSet))) {
                // If the parent SK is not licensed, then provide user an error dialog to resolve the missed license.
                ResolveSolutionKitLicenseDialog dialog = new ResolveSolutionKitLicenseDialog(
                    TopComponents.getInstance().getTopParent(),
                    parentSK.getName(),
                    parentSK.getProperty(SolutionKit.SK_PROP_FEATURE_SET_KEY)
                );
                Utilities.centerOnParentWindow(dialog);
                DialogDisplayer.display(dialog);

                return false;
            }
        }

        //Check that at least one SK from upgrade and loaded share the same guid
        if (solutionKitsConfig.isUpgrade()) {
            if (!SolutionKitUtils.doesShareGuid(solutionKitsConfig.getLoadedSolutionKits().keySet(),
                    solutionKitsConfig.getSolutionKitsToUpgrade())) {
                solutionKitsConfig.clear(false);
                DialogDisplayer.showMessageDialog(this,
                        "Unable to upgrade. No Solution Kits on database match that of the Solution Kit(s) on the .sskar file. Consider installing instead.",
                        "Solution Kit Upgrade Error",
                        JOptionPane.ERROR_MESSAGE,
                        null);
                return false;
            }
        }

        return true;
    }

    private class ResolveSolutionKitLicenseDialog extends JDialog {
        private JButton manageLicensesButton;
        private JButton cancelButton;
        private JLabel unlicensedMessageLabel;
        private JPanel mainPanel;

        private ResolveSolutionKitLicenseDialog(@NotNull Frame owner, @NotNull final String solutionKitName, @NotNull final String featureSetName) {
            super(owner, "Unlicensed Solution Kit Error", true);

            setContentPane(mainPanel);

            Utilities.setEscKeyStrokeDisposes(this);

            unlicensedMessageLabel.setText(
                MessageFormat.format("The parent solution kit, \"{0}\" is unlicensed.  Required feature set is \"{1}\".", solutionKitName, featureSetName)
            );

            manageLicensesButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onManageLicenses();
                    dispose();
                }
            });

            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });

            pack();
        }
    }

    private void onManageLicenses() {
        final Frame mainWindow = TopComponents.getInstance().getTopParent();
        ManageLicensesDialog dialog = new ManageLicensesDialog(mainWindow);
        dialog.pack();
        Utilities.centerOnParentWindow(dialog);
        dialog.setModal(true);
        DialogDisplayer.display(dialog);
    }

    private void initialize() {
        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(fileTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                notifyListeners();
            }
        }, 300);

        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onFileButton();
            }
        });

        setLayout(new BorderLayout());
        add(mainPanel);
    }

    private void onFileButton() {
        FileChooserUtil.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                fc.setDialogTitle("Choose Solution Kit");
                fc.setDialogType(JFileChooser.OPEN_DIALOG);
                fc.setMultiSelectionEnabled(false);
                fc.addChoosableFileFilter(SK_FILE_FILTER);
                fc.addChoosableFileFilter(SIGNED_SK_FILE_FILTER);
                fc.setFileFilter(SIGNED_SK_FILE_FILTER);

                int result = fc.showOpenDialog(SolutionKitLoadPanel.this);
                if (JFileChooser.APPROVE_OPTION != result) {
                    return;
                }

                File file = fc.getSelectedFile();
                if (file == null) {
                    return;
                }

                fileTextField.setText(file.getAbsolutePath());
                notifyListeners();
            }
        });
    }
}