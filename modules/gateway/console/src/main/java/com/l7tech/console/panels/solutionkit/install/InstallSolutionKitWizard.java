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
import com.l7tech.gateway.common.api.solutionkit.SolutionKitsConfig;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.xml.transform.stream.StreamSource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard for installing solution kits.
 */
public class InstallSolutionKitWizard extends Wizard<SolutionKitsConfig> {
    private static final Logger logger = Logger.getLogger(InstallSolutionKitWizard.class.getName());
    private static final String WIZARD_TITLE = "Solution Kit Installation Wizard";

    public static InstallSolutionKitWizard getInstance(@NotNull ManageSolutionKitsDialog parent, @Nullable SolutionKit solutionKitToUpgrade) {
        final SolutionKitResolveMappingErrorsPanel third = new SolutionKitResolveMappingErrorsPanel();

        final SolutionKitSelectionPanel second = new SolutionKitSelectionPanel();
        second.setNextPanel(third);

        final SolutionKitLoadPanel first = new SolutionKitLoadPanel();
        first.setNextPanel(second);

        SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();
        solutionKitsConfig.setSolutionKitsToUpgrade(getListOfSolutionKitsToUpgrade(solutionKitToUpgrade));
        solutionKitsConfig.setInstanceModifiers(getInstanceModifiers());

        return new InstallSolutionKitWizard(parent, first, solutionKitsConfig);
    }

    public static InstallSolutionKitWizard getInstance(@NotNull ManageSolutionKitsDialog parent) {
        return getInstance(parent, null);
    }

    private InstallSolutionKitWizard(Window parent, WizardStepPanel<SolutionKitsConfig> panel, @NotNull SolutionKitsConfig solutionKitsConfig) {
        super(parent, panel, solutionKitsConfig);
        initialize();
    }

    private static Map<String, List<String>> getInstanceModifiers() {
        final Map<String, List<String>> instanceModifiers = new HashMap<>();

        try {
            final Collection<SolutionKitHeader> solutionKitHeaders = Registry.getDefault().getSolutionKitAdmin().findSolutionKits();

            for (SolutionKitHeader solutionKitHeader: solutionKitHeaders) {
                String solutionKitGuid = solutionKitHeader.getSolutionKitGuid();
                java.util.List<String> usedInstanceModifiers = instanceModifiers.get(solutionKitGuid);
                if (usedInstanceModifiers == null) {
                    usedInstanceModifiers = new ArrayList<>();
                }
                usedInstanceModifiers.add(solutionKitHeader.getInstanceModifier());
                instanceModifiers.put(solutionKitGuid, usedInstanceModifiers);
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }

        return instanceModifiers;
    }

    @Override
    protected void finish(ActionEvent evt) {
        if (wizardInput != null) { // wizardInput is a SolutionKitConfig object.
            getSelectedWizardPanel().storeSettings(wizardInput);
        }

        final  SolutionKitAdmin solutionKitAdmin = Registry.getDefault().getSolutionKitAdmin();
        final List<Pair<String, SolutionKit>> errorKitList = new ArrayList<>();
        final SolutionKit parentSK = wizardInput.getParentSolutionKit(); // Note: The parent solution kit has a dummy default GOID.
        Goid parentGoid = null;

        if (parentSK != null) {
            final List<SolutionKit> solutionKitsToUpgrade = wizardInput.getSolutionKitsToUpgrade();
            final boolean isParentSKToUpgrade = (! solutionKitsToUpgrade.isEmpty()) && solutionKitsToUpgrade.get(0).getSolutionKitGuid().equals(parentSK.getSolutionKitGuid());

            try {
                // Case 1: Parent for upgrade
                if (isParentSKToUpgrade) {
                    final SolutionKit firstElement = solutionKitsToUpgrade.get(0); // The first element is a real parent solution kit.
                    parentGoid = firstElement.getGoid();
                    // Update the parent solution kit attributes
                    firstElement.setName(parentSK.getName());
                    firstElement.setSolutionKitVersion(parentSK.getSolutionKitVersion());
                    firstElement.setXmlProperties(parentSK.getXmlProperties());

                    solutionKitAdmin.updateSolutionKit(firstElement);
                }
                // Case 2: Parent for install
                else {
                    final List<SolutionKit> solutionKitsExistingOnGateway = (List<SolutionKit>) solutionKitAdmin.findBySolutionKitGuid(parentSK.getSolutionKitGuid());
                    // Case 2.1: Find the parent already installed on gateway
                    if (solutionKitsExistingOnGateway.size() > 0) {
                        final SolutionKit parentExistingOnGateway = solutionKitsExistingOnGateway.get(0);
                        parentGoid = parentExistingOnGateway.getGoid();
                        solutionKitAdmin.updateSolutionKit(parentExistingOnGateway);
                    }
                    // Case 2.2: No such parent installed on gateway
                    else {
                        parentGoid = solutionKitAdmin.saveSolutionKit(parentSK);
                    }
                }
            } catch (Exception e) {
                String msg = ExceptionUtils.getMessage(e);
                logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
                errorKitList.add(new Pair<>(msg, parentSK));
            }
        }

        for (SolutionKit solutionKit: wizardInput.getSelectedSolutionKits()) {
            if (parentSK != null) {
                solutionKit.setParentGoid(parentGoid);
            }
            try {
                Either<String, Goid> result = AdminGuiUtils.doAsyncAdmin(
                    solutionKitAdmin,
                    this.getOwner(),
                    "Install Solution Kit",
                    "The gateway is installing the selected solution kit, \"" + solutionKit.getName() + "\".",
                    new SkarProcessor(wizardInput).installOrUpgrade(solutionKitAdmin, solutionKit),
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
        // Finish installation successfully if no errors exist.  Otherwise, display errors if applicable
        if (errorKitList.isEmpty()) {
            wizardInput.clear();
            super.finish(evt);
        } else {
            displayErrorDialog(errorKitList);
        }
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

                Map<String, String> resolvedEntityId = getWizardInput().getResolvedEntityIds(solutionKit);

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
        final List<SolutionKit> solutionKitsToUpgrade = getWizardInput().getSolutionKitsToUpgrade();

        // Note that if it is a collection of solution kits for upgrade, then the first element in solutionKitsToUpgrade is a parent solution kit, which should not be upgraded.
        final int startIdx = solutionKitsToUpgrade.size() > 1? 1 : 0;

        for (int i = startIdx; i < solutionKitsToUpgrade.size(); i++) {
            SolutionKit solutionKitToUpgrade = solutionKitsToUpgrade.get(i);

            if (solutionKitsToUpgrade != null && !SolutionKit.PARENT_SOLUTION_KIT_DUMMY_MAPPINGS.equals(solutionKitToUpgrade.getMappings())) {
                try {
                    Item item = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(solutionKitToUpgrade.getMappings())));
                    Mappings mappings = (Mappings) item.getContent();
                    Map<String, String> previouslyResolvedIds = new HashMap<>();
                    for (Mapping mapping : mappings.getMappings()) {
                        if (!mapping.getSrcId().equals(mapping.getTargetId()) ) {
                            previouslyResolvedIds.put(mapping.getSrcId(), mapping.getTargetId());
                        }
                    }
                    if (!previouslyResolvedIds.isEmpty()) {
                        getWizardInput().getResolvedEntityIds().put(solutionKitToUpgrade, previouslyResolvedIds);
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException(ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        }
    }

    /**
     * Get a list of solution kits for upgrade, depending on the following three cases:
     * Case 1: if the selected solution kit is a child, then add the parent and the selected child into the return list.
     * Case 2: if the selected solution kit is a neither parent nor child, then add only the selected solution kit into the return list.
     * Case 3: if the selected solution kit is a parent, then add the parent and all children into the return list.
     *
     * @param solutionKit: the selected solution kit, which user selects to upgrade.
     * @return a list of solution kits for upgrade
     */
    private static List<SolutionKit> getListOfSolutionKitsToUpgrade(SolutionKit solutionKit) {
        final List<SolutionKit> skList = new ArrayList<>();
        if (solutionKit == null) return skList;

        final SolutionKitAdmin solutionKitAdmin = Registry.getDefault().getSolutionKitAdmin();

        // Case 1:
        final Goid parentGoid = solutionKit.getParentGoid();
        if (parentGoid != null) {
            try {
                final SolutionKit parent = solutionKitAdmin.get(parentGoid);
                skList.add(parent);
            } catch (FindException e) {
                String errMsg = "Cannot retrieve the solution kit (GOID = '" + parentGoid + "')";
                logger.warning(errMsg);
                throw new RuntimeException(errMsg);
            }
        }

        // Case 1 + Case 2 + Case 3:
        skList.add(solutionKit);

        // Case 3:
        final Collection<SolutionKitHeader> children;
        try {
            children = solutionKitAdmin.findAllChildrenByParentGoid(solutionKit.getGoid());
        } catch (FindException e) {
            String errMsg = "Cannot find child solution kits for '" + solutionKit.getName() + "'";
            logger.warning(errMsg);
            throw new RuntimeException(errMsg);
        }
        for (SolutionKitHeader child: children) {
            try {
                skList.add(solutionKitAdmin.get(child.getGoid()));
            } catch (FindException e) {
                String errMsg = "Cannot find the solution kit, '" + child.getName() + "'";
                logger.warning(errMsg);
                throw new RuntimeException(errMsg);
            }
        }

        return skList;
    }
}