package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.solutionkit.ManageSolutionKitsDialog;
import com.l7tech.console.panels.solutionkit.SolutionKitMappingsPanel;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.solutionkit.*;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.xml.transform.dom.DOMResult;
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
        // install or upgrade
        final SolutionKitProcessor solutionKitProcessor = new SolutionKitProcessor(wizardInput, solutionKitAdmin);
        if (wizardInput.isUpgrade()) {
            try {
                upgrade(solutionKitAdmin, solutionKitProcessor);
            } catch (Exception e) {
                final String msg = "Unable to upgrade solution kit: " + ExceptionUtils.getMessage(e);
                logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
                DialogDisplayer.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE, null);
            }
        } else {
            try {
                install(solutionKitAdmin, solutionKitProcessor);
            } catch (Exception e) {
                final String msg = "Unable to install solution kit: " + ExceptionUtils.getMessage(e);
                logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
                DialogDisplayer.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE, null);
            }
        }
        // Reload encapsulated assertions via Encapsulated Assertion Registry
        try {
            TopComponents.getInstance().getEncapsulatedAssertionRegistry().updateEncapsulatedAssertions();
        } catch (FindException e) {
            logger.log(Level.WARNING, "Unable to update encapsulated assertions: " + ExceptionUtils.getMessage(e) + ".", ExceptionUtils.getDebugException(e));
        }

        // Finish installation no matter if successful or not
        super.finish(evt);
        wizardInput.clear();
    }

    private void install(SolutionKitAdmin solutionKitAdmin, SolutionKitProcessor solutionKitProcessor) throws Exception {
        final List<Pair<String, SolutionKit>> errorKitList = new ArrayList<>();
        solutionKitProcessor.install(errorKitList, new Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Exception>() {
            @Override
            public void call(Triple<SolutionKit, String, Boolean> loaded) throws Exception {
                Either<String, Goid> result = AdminGuiUtils.doAsyncAdmin(
                        solutionKitAdmin,
                        InstallSolutionKitWizard.this.getOwner(),
                        "Install Solution Kit",
                        "The Gateway is installing solution kit: " + loaded.left.getName() + ".",
                        solutionKitAdmin.installAsync(loaded.left, loaded.middle, loaded.right),
                        false);

                if (result.isLeft()) {
                    // Solution kit failed to install.
                    String msg = result.left();
                    logger.log(Level.WARNING, msg);
                    errorKitList.add(new Pair<>(msg, loaded.left));
                }
            }
        });

        // Display errors if applicable
        if (!errorKitList.isEmpty()) {
            displayInstallErrorDialog(errorKitList);
        }
    }

    private void upgrade(SolutionKitAdmin solutionKitAdmin, SolutionKitProcessor solutionKitProcessor) throws Exception {
        final List<Pair<Mappings, SolutionKit>> errorKitList = new ArrayList<>();
        solutionKitProcessor.upgrade(new Functions.UnaryVoidThrows<SolutionKitInfo, Exception>() {
            @Override
            public void call(SolutionKitInfo loaded) throws Exception {
                Either<String, ArrayList> result = AdminGuiUtils.doAsyncAdmin(
                        solutionKitAdmin,
                        InstallSolutionKitWizard.this.getOwner(),
                        "Upgrade Solution Kit",
                        "Performing Upgrade. Please wait a moment.",
                        solutionKitAdmin.upgradeAsync(loaded),
                        false);

                if (result.isLeft()) {
                    // Error was detected
                    List<SolutionKit> solutionKits = new ArrayList<>();
                    solutionKits.addAll(loaded.getSolutionKitDelete().keySet());
                    solutionKits.addAll(loaded.getSolutionKitInstall().keySet());
                    // Solution kit failed to upgrade.
                    final String msg = result.left();
                    logger.log(Level.WARNING, msg);
                    //Convert string ItemsList
                    final ItemsList<Mappings> itemList = MarshallingUtils.unmarshal(ItemsList.class, new StreamSource(new StringReader(msg)));
                    final List<Item<Mappings>> items = itemList.getContent();

                    if (items.size() != solutionKits.size()) {
                        throw new SolutionKitConflictException("Result mappings size was not the same as Solution Kits size.");
                    }

                    for (int i = 0; i<items.size(); i++) {
                        Mappings mappings = items.get(i).getContent();
                        //Check if this set of solution kit mappings has mapping errors
                        for (Mapping mapping : mappings.getMappings()) {
                            if (mapping.getErrorType() != null) {
                                errorKitList.add(new Pair<>(mappings, solutionKits.get(i)));
                                break;
                            }
                        }
                    }
                }
            }
        });

        // Display errors if applicable
        if (!errorKitList.isEmpty()) {
            displayUpgradeErrorDialog(errorKitList);
        }

    }

    @SuppressWarnings("unused")
    protected void clickButtonNext() {
        super.getButtonNext().doClick();
    }

    /**
     * Display an upgrade error dialog.
     * If the specified msg is a bundle mapping xml (ie. response message from RESTMAN import bundle API), then
     * display the mapping errors in a table format. Otherwise, display the msg string as-is.
     */
    private void displayUpgradeErrorDialog(@NotNull final List<Pair<Mappings, SolutionKit>> errorKitMatchList) {
        final JTabbedPane errorTabbedPane = new JTabbedPane();
        for (Pair<Mappings, SolutionKit> errorKit: errorKitMatchList) {
            final Mappings mappings = errorKit.left;
            final SolutionKit solutionKit = errorKit.right;
            String msg = "";

            try {
                final DOMResult result = new DOMResult();
                MarshallingUtils.marshal(mappings, result, true);
                msg = XmlUtil.nodeToString(result.getNode());
            } catch (IOException e) {
                logger.fine("Problem marshalling the mappings for " + solutionKit.getName());
            }

            if (mappings != null) {
                final List<Mapping> errorMappings = new ArrayList<>();
                for (Mapping aMapping : mappings.getMappings()) {
                    if (aMapping.getErrorType() != null) {
                        errorMappings.add(aMapping);
                    }
                }
                mappings.setMappings(errorMappings);

                // need the solution kit bundle
                final Bundle bundle = getWizardInput().getBundle(solutionKit);
                // bundle is null for delete mapping errors
                if (bundle == null) {
                    errorTabbedPane.add(solutionKit.getName(), new JLabel(msg));
                    continue;
                }

                final Map<String, String> resolvedEntityId = getWizardInput().getResolvedEntityIds(solutionKit.getSolutionKitGuid()).right;

                final JPanel errorPanel = new JPanel();
                final JLabel label = new JLabel("Failed to upgrade Solution Kit(s) due to following entity conflicts:");
                final SolutionKitMappingsPanel solutionKitMappingsPanel = new SolutionKitMappingsPanel("Name");
                solutionKitMappingsPanel.setPreferredSize(new Dimension(1000, 400));
                solutionKitMappingsPanel.hideTargetIdColumn();
                solutionKitMappingsPanel.setData(solutionKit, mappings, bundle, resolvedEntityId);

                errorPanel.setLayout(new BorderLayout());
                errorPanel.add(label, BorderLayout.NORTH);
                errorPanel.add(solutionKitMappingsPanel, BorderLayout.CENTER);

                errorTabbedPane.add(solutionKit.getName(), errorPanel);
            } else {
                errorTabbedPane.add(solutionKit.getName(), new JLabel(msg));
            }
        }

        DialogDisplayer.showMessageDialog(this.getOwner(), errorTabbedPane, "Upgrade Solution Kit Error", JOptionPane.ERROR_MESSAGE, null);
    }

    /**
     * Display a error dialog.
     * If the specified msg is a bundle mapping xml (ie. response message from RESTMAN import bundle API), then
     * display the mapping errors in a table format. Otherwise, display the msg string as-is.
     */
    //TODO: when install uses multi-bundle upgrade, probably use displayUpgradeErrorDialog
    private void displayInstallErrorDialog(@NotNull final List<Pair<String, SolutionKit>> errorKitMatchList) {
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
                SolutionKitMappingsPanel solutionKitMappingsPanel = new SolutionKitMappingsPanel("Name");
                solutionKitMappingsPanel.setPreferredSize(new Dimension(1000, 400));
                solutionKitMappingsPanel.hideTargetIdColumn();
                solutionKitMappingsPanel.setData(solutionKit, mappings, bundle, resolvedEntityId);

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
        this.setPreferredSize(new Dimension(1157, 538));

        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(InstallSolutionKitWizard.this);
            }
        });

        // upgrade - find previously installed mappings where srcId differs from targetId (e.g. user resolved)
        getWizardInput().setPreviouslyResolvedIds();
    }
}