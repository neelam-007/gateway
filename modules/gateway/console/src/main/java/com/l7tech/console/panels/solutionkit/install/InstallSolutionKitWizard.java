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
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.xml.transform.stream.StreamSource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
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

    private SolutionKitAdmin solutionKitAdmin;

    public static InstallSolutionKitWizard getInstance(@NotNull ManageSolutionKitsDialog parent, @Nullable SolutionKit solutionKitToUpgrade) {
        final SolutionKitResolveMappingErrorsPanel third = new SolutionKitResolveMappingErrorsPanel();

        final SolutionKitSelectionPanel second = new SolutionKitSelectionPanel();
        second.setNextPanel(third);

        final SolutionKitLoadPanel first = new SolutionKitLoadPanel();
        first.setNextPanel(second);

        SolutionKitsConfig solutionKitsConfig = new SolutionKitsConfig();
        solutionKitsConfig.setSolutionKitToUpgrade(solutionKitToUpgrade);
        setInstanceModifiers(solutionKitsConfig.getInstanceModifiers(), parent.getSolutionKitHeaders());

        return new InstallSolutionKitWizard(parent, first, solutionKitsConfig);
    }

    public static InstallSolutionKitWizard getInstance(@NotNull ManageSolutionKitsDialog parent) {
        return getInstance(parent, null);
    }

    private InstallSolutionKitWizard(Window parent, WizardStepPanel<SolutionKitsConfig> panel, @NotNull SolutionKitsConfig solutionKitsConfig) {
        super(parent, panel, solutionKitsConfig);
        initialize();
    }

    private static void setInstanceModifiers(@NotNull final Map<String, List<String>> instanceModifiers, @Nullable final Collection<SolutionKitHeader> solutionKitHeaders) {
        if (solutionKitHeaders != null) {
            for (SolutionKitHeader solutionKitHeader: solutionKitHeaders) {
                String solutionKitGuid = solutionKitHeader.getSolutionKitGuid();
                java.util.List<String> usedInstanceModifiers = instanceModifiers.get(solutionKitGuid);
                if (usedInstanceModifiers == null) {
                    usedInstanceModifiers = new ArrayList<>();
                }
                usedInstanceModifiers.add(solutionKitHeader.getInstanceModifier());
                instanceModifiers.put(solutionKitGuid, usedInstanceModifiers);
            }
        }
    }

    @Override
    protected void finish(ActionEvent evt) {
        if (wizardInput != null) {
            this.getSelectedWizardPanel().storeSettings(getWizardInput());
        }

        try {
            Either<String, Goid> result = AdminGuiUtils.doAsyncAdmin(
                    solutionKitAdmin,
                    this.getOwner(),
                    "Install Solution Kit",
                    "The gateway is installing selected solution kit(s)",
                    new SkarProcessor(getWizardInput()).installOrUpgrade(solutionKitAdmin));

            if (result.isLeft()) {
                // Solution kit failed to install.
                String msg = result.left();
                logger.log(Level.WARNING, msg);
                displayErrorDialog(msg);
            } else if (result.isRight()) {
                // Solution kit installed successfully.
                getWizardInput().clear();
                super.finish(evt);
            }
        } catch (SolutionKitException e) {
            String msg = ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            DialogDisplayer.showMessageDialog(this.getOwner(), msg, "Error", JOptionPane.ERROR_MESSAGE, null);
        } catch (InvocationTargetException e) {
            String msg = ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            displayErrorDialog(msg);
        } catch (InterruptedException e) {
            // user cancelled. do nothing.
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
     *
     * @param msg the message.
     */
    private void displayErrorDialog(String msg) {
        Mappings mappings = null;
        try {
            Item item = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(msg)));
            mappings = (Mappings)item.getContent();
        } catch (IOException e) {
            // msg is not a bundle mapping xml.
        }

        final String dialogTitle = "Install Solution Kit Error";
        if (mappings != null) {
            // Only display mappings with errors.
            //
            java.util.List<Mapping> errorMappings = new ArrayList<>();
            for (Mapping aMapping : mappings.getMappings()) {
                if (aMapping.getErrorType() != null) {
                    errorMappings.add(aMapping);
                }
            }
            mappings.setMappings(errorMappings);

            // need the selected solution kit
            SolutionKit solutionKit = getWizardInput().getSingleSelectedSolutionKit();
            if (solutionKit == null) {
                JOptionPane.showMessageDialog(this.getOwner(), msg, dialogTitle, JOptionPane.ERROR_MESSAGE);
                return;
            }

            // need the solution kit bundle
            Bundle bundle = getWizardInput().getBundle(solutionKit);
            if (bundle == null) {
                JOptionPane.showMessageDialog(this.getOwner(), msg, dialogTitle, JOptionPane.ERROR_MESSAGE);
                return;
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

            DialogDisplayer.showMessageDialog(this.getOwner(), errorPanel, dialogTitle, JOptionPane.ERROR_MESSAGE, null);
        } else {
            JOptionPane.showMessageDialog(this.getOwner(), msg, dialogTitle, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initialize() {
        setTitle(WIZARD_TITLE);
        getButtonHelp().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(InstallSolutionKitWizard.this);
            }
        });

        solutionKitAdmin = Registry.getDefault().getSolutionKitAdmin();

        // upgrade - find previously installed mappings where srcId differs from targetId (e.g. user resolved)
        SolutionKit solutionKitToUpgrade = getWizardInput().getSolutionKitToUpgrade();
        if (solutionKitToUpgrade != null) {
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