package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.solutionkit.ManageSolutionKitsDialog;
import com.l7tech.console.panels.solutionkit.SolutionKitMappingsPanel;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.api.solutionkit.SkarProcessor;
import com.l7tech.gateway.common.api.solutionkit.SolutionKitUtils;
import com.l7tech.gateway.common.api.solutionkit.SolutionKitsConfig;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.xml.transform.stream.StreamSource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard for installing solution kits.
 */
public class InstallSolutionKitWizard extends Wizard<SolutionKitsConfig> {
    private static final Logger logger = Logger.getLogger(InstallSolutionKitWizard.class.getName());
    private static final String WIZARD_TITLE = "Solution Kit Installation Wizard";

    public static InstallSolutionKitWizard getInstance(@NotNull ManageSolutionKitsDialog parent, @Nullable SolutionKit solutionKitToUpgrade) throws FindException {
        final SolutionKitResolveMappingErrorsPanel third = new SolutionKitResolveMappingErrorsPanel();

        final SolutionKitSelectionPanel second = new SolutionKitSelectionPanel();
        second.setNextPanel(third);

        final SolutionKitLoadPanel first = new SolutionKitLoadPanel();
        first.setNextPanel(second);

        final SolutionKitAdmin solutionKitAdmin = Registry.getDefault().getSolutionKitAdmin();
        SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();
        solutionKitsConfig.setSolutionKitsToUpgrade(solutionKitAdmin.getSolutionKitsToUpgrade(solutionKitToUpgrade));
        try {
            solutionKitsConfig.setInstanceModifiers(SolutionKitUtils.getInstanceModifiers(solutionKitAdmin.findSolutionKits()));
        } catch (FindException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }

        return new InstallSolutionKitWizard(parent, first, solutionKitsConfig);
    }

    public static InstallSolutionKitWizard getInstance(@NotNull ManageSolutionKitsDialog parent) throws FindException {
        return getInstance(parent, null);
    }

    private InstallSolutionKitWizard(Window parent, WizardStepPanel<SolutionKitsConfig> panel, @NotNull SolutionKitsConfig solutionKitsConfig) {
        super(parent, panel, solutionKitsConfig);
        initialize();
    }

    @Override
    protected void finish(ActionEvent evt) {
        if (wizardInput != null) { // wizardInput is a SolutionKitConfig object.
            try {
                getSelectedWizardPanel().storeSettings(wizardInput);
            } catch (Exception e) {
                // do nothing, just exit "Finish" and the wizard still starts opened.
                return;
            }
        }

        final  SolutionKitAdmin solutionKitAdmin = Registry.getDefault().getSolutionKitAdmin();
        final List<Pair<String, SolutionKit>> errorKitList = new ArrayList<>();
        final SolutionKit parentSKFromLoad = wizardInput.getParentSolutionKit(); // Note: The parent solution kit has a dummy default GOID.
        Goid parentGoid = null;

        if (parentSKFromLoad != null) {
            final List<SolutionKit> solutionKitsToUpgrade = wizardInput.getSolutionKitsToUpgrade();
            final boolean isParentSKToUpgrade = (! solutionKitsToUpgrade.isEmpty()) && solutionKitsToUpgrade.get(0).getSolutionKitGuid().equals(parentSKFromLoad.getSolutionKitGuid());

            try {
                // Case 1: Parent for upgrade
                if (isParentSKToUpgrade) {
                    final SolutionKit parentSKFromDB = solutionKitsToUpgrade.get(0); // The first element is a real parent solution kit.
                    parentGoid = parentSKFromDB.getGoid();
                    // Update the parent solution kit attributes
                    parentSKFromDB.setName(parentSKFromLoad.getName());
                    parentSKFromDB.setSolutionKitVersion(parentSKFromLoad.getSolutionKitVersion());
                    parentSKFromDB.setXmlProperties(parentSKFromLoad.getXmlProperties());

                    solutionKitAdmin.updateSolutionKit(parentSKFromDB);
                }
                // Case 2: Parent for install
                else {
                    final List<SolutionKit> solutionKitsExistingOnGateway = (List<SolutionKit>) solutionKitAdmin.findBySolutionKitGuid(parentSKFromLoad.getSolutionKitGuid());
                    // Case 2.1: Find the parent already installed on gateway
                    if (solutionKitsExistingOnGateway.size() > 0) {
                        final SolutionKit parentExistingOnGateway = solutionKitsExistingOnGateway.get(0);
                        parentGoid = parentExistingOnGateway.getGoid();
                        solutionKitAdmin.updateSolutionKit(parentExistingOnGateway);
                    }
                    // Case 2.2: No such parent installed on gateway
                    else {
                        parentGoid = solutionKitAdmin.saveSolutionKit(parentSKFromLoad);
                    }
                }
            } catch (Exception e) {
                String msg = ExceptionUtils.getMessage(e);
                logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
                errorKitList.add(new Pair<>(msg, parentSKFromLoad));
            }
        }

        for (SolutionKit solutionKit: wizardInput.getSelectedSolutionKits()) {
            if (parentSKFromLoad != null) {
                solutionKit.setParentGoid(parentGoid);
            }
            try {
                Triple<SolutionKit, String, Boolean> loaded = new SkarProcessor(wizardInput).installOrUpgrade(solutionKit);
                Either<String, Goid> result = AdminGuiUtils.doAsyncAdmin(
                    solutionKitAdmin,
                    this.getOwner(),
                    "Install Solution Kit",
                    "The gateway is installing the selected solution kit, \"" + solutionKit.getName() + "\".",
                    solutionKitAdmin.install(loaded.left, loaded.middle, loaded.right),
                    false);

                if (result.isLeft()) {
                    // Solution kit failed to install.
                    String msg = result.left();
                    logger.log(Level.WARNING, msg);
                    errorKitList.add(new Pair<>(msg, solutionKit));
                }
            } catch (InterruptedException e) {
                // user cancelled. do nothing.
            } catch (Exception e) {
                String msg = ExceptionUtils.getMessage(e);
                logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
                errorKitList.add(new Pair<>(msg, solutionKit));
            }
        }

        // Display errors if applicable
        if (! errorKitList.isEmpty()) {
            displayErrorDialog(errorKitList);
        }

        // Finish installation no matter if successful or not
        super.finish(evt);
        wizardInput.clear();
    }

    @SuppressWarnings("unused")
    protected void clickButtonNext() {
        super.getButtonNext().doClick();
    }

    /**
     * Display a error dialog.
     * If the specified msg is a bundle mapping xml (ie. response message from RESTMAN import bundle API), then
     * display the mapping errors in a table format. Otherwise, display the msg string as-is.
     */
    private void displayErrorDialog(@NotNull final List<Pair<String, SolutionKit>> errorKitMatchList) {
        final JTabbedPane errorTabbedPane = new JTabbedPane();

        String msg;
        SolutionKit solutionKit;
        for (Pair<String, SolutionKit> errorKit: errorKitMatchList) {
            msg = errorKit.left;
            solutionKit = errorKit.right;

            Mappings mappings = null;
            try {
                Item item = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(msg)));
                mappings = (Mappings)item.getContent();
            } catch (IOException e) {
                // msg is not a bundle mapping xml.
            }

            if (mappings != null) {
                java.util.List<Mapping> errorMappings = new ArrayList<>();
                for (Mapping aMapping : mappings.getMappings()) {
                    if (aMapping.getErrorType() != null) {
                        errorMappings.add(aMapping);
                    }
                }
                mappings.setMappings(errorMappings);

                // need the solution kit bundle
                Bundle bundle = getWizardInput().getBundle(solutionKit);
                if (bundle == null) {
                    errorTabbedPane.add(solutionKit.getName(), new JLabel(msg));
                    continue;
                }

                Map<String, String> resolvedEntityId = getWizardInput().getResolvedEntityIds(solutionKit.getSolutionKitGuid()).right;

                JPanel errorPanel = new JPanel();
                JLabel label = new JLabel("Failed to install solution kit(s) due to following entity conflicts:");
                SolutionKitMappingsPanel solutionKitMappingsPanel = new SolutionKitMappingsPanel();
                solutionKitMappingsPanel.setPreferredSize(new Dimension(1000, 400));
                solutionKitMappingsPanel.hideTargetIdColumn();
                solutionKitMappingsPanel.setData(mappings, bundle, resolvedEntityId);

                errorPanel.setLayout(new BorderLayout());
                errorPanel.add(label, BorderLayout.NORTH);
                errorPanel.add(solutionKitMappingsPanel, BorderLayout.CENTER);

                errorTabbedPane.add(solutionKit.getName(), errorPanel);
            } else {
                errorTabbedPane.add(solutionKit.getName(), new JLabel(msg));
            }
        }

        DialogDisplayer.showMessageDialog(this.getOwner(), errorTabbedPane, "Install Solution Kit Error", JOptionPane.ERROR_MESSAGE, null);
    }

    private void initialize() {
        setTitle(WIZARD_TITLE);
        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(InstallSolutionKitWizard.this);
            }
        });

        // upgrade - find previously installed mappings where srcId differs from targetId (e.g. user resolved)
        getWizardInput().onUpgradeResetPreviouslyInstalledMappings();
    }
}