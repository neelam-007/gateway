package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.solutionkit.SolutionKitMappingsPanel;
import com.l7tech.console.panels.solutionkit.SolutionKitsConfig;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.xml.transform.stream.StreamSource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard for installing solution kits.
 */
public class InstallSolutionKitWizard extends Wizard<SolutionKitsConfig> {
    private static final Logger logger = Logger.getLogger(InstallSolutionKitWizard.class.getName());
    private static final String WIZARD_TITLE = "Solution Kit Installation Wizard";

    private SolutionKitAdmin solutionKitAdmin;

    public static InstallSolutionKitWizard getInstance(Window parent) {
        final SolutionKitResolveMappingErrorsPanel third = new SolutionKitResolveMappingErrorsPanel();

        final SolutionKitSelectionPanel second = new SolutionKitSelectionPanel();
        second.setNextPanel(third);

        final SolutionKitLoadPanel first = new SolutionKitLoadPanel();
        first.setNextPanel(second);

        return new InstallSolutionKitWizard(parent, first);
    }

    private InstallSolutionKitWizard(Window parent, WizardStepPanel<SolutionKitsConfig> panel) {
        super(parent, panel, new SolutionKitsConfig());

        solutionKitAdmin = Registry.getDefault().getSolutionKitAdmin();
        initialize();
    }

    @Override
    protected void finish(ActionEvent evt) {
        if (wizardInput != null) {
            this.getSelectedWizardPanel().storeSettings(wizardInput);
        }

        try {
            SolutionKit solutionKit = wizardInput.getSingleSelectedSolutionKit();

            // Update resolved mapping target IDs.
            Map<String, String> resolvedEntityIds = wizardInput.getResolvedEntityIds(solutionKit);
            Bundle bundle = wizardInput.getBundle(solutionKit);
            for (Mapping mapping : bundle.getMappings()) {
                String resolvedId = resolvedEntityIds.get(mapping.getSrcId());
                if (resolvedId != null) {
                    mapping.setTargetId(resolvedId);
                }
            }

            String bundleXml = wizardInput.getBundleAsString(solutionKit);
            Either<String, Goid> result = AdminGuiUtils.doAsyncAdmin(
                solutionKitAdmin,
                this.getOwner(),
                "Install Solution Kit",
                "The gateway is installing selected solution kit(s)",
                solutionKitAdmin.install(solutionKit, bundleXml));

            if (result.isLeft()) {
                // Solution kit failed to install.
                String msg = result.left();
                logger.log(Level.WARNING, msg);
                displayErrorDialog(msg);
            } else if (result.isRight()) {
                // Solution kit installed successfully.
                super.finish(evt);
            }
        } catch (InvocationTargetException e) {
            String msg = ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            displayErrorDialog(msg);
        } catch (InterruptedException e) {
            // user cancelled. do nothing.
        }
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

            SolutionKit solutionKit = wizardInput.getSingleSelectedSolutionKit();
            Bundle bundle = wizardInput.getBundle(solutionKit);
            Map<String, String> resolvedEntityId = wizardInput.getResolvedEntityIds(solutionKit);

            JPanel errorPanel = new JPanel();
            JLabel label = new JLabel("Failed to install solution kit(s) due to following entity conflicts:");
            SolutionKitMappingsPanel solutionKitMappingsPanel = new SolutionKitMappingsPanel();
            solutionKitMappingsPanel.setPreferredSize(new Dimension(1000, 400));
            solutionKitMappingsPanel.hideTargetIdColumn();
            solutionKitMappingsPanel.setData(mappings, bundle, resolvedEntityId);

            errorPanel.setLayout(new BorderLayout());
            errorPanel.add(label, BorderLayout.NORTH);
            errorPanel.add(solutionKitMappingsPanel, BorderLayout.CENTER);

            DialogDisplayer.showMessageDialog(this.getOwner(), errorPanel, "Install Solution Kit Error", JOptionPane.ERROR_MESSAGE, null);
        } else {
            JOptionPane.showMessageDialog(this.getOwner(), msg, "Install Solution Kit Error", JOptionPane.ERROR_MESSAGE);
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
    }
}