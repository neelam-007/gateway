package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.panels.solutionkit.SolutionKitsConfig;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class InstallSolutionKitWizard extends Wizard<SolutionKitsConfig> {
    private static final Logger logger = Logger.getLogger(InstallSolutionKitWizard.class.getName());
    private static final String WIZARD_TITLE = "Solution Kit Installation Wizard";

    private SolutionKitAdmin solutionKitAdmin;

    public static InstallSolutionKitWizard getInstance(Window parent) {
        final SolutionKitSelectionPanel second = new SolutionKitSelectionPanel();

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

        Map<SolutionKit, String> solutionsKits = new HashMap<>();
        for (SolutionKit aSolutionKit : wizardInput.getSelectedSolutionKits()) {
            solutionsKits.put(aSolutionKit, wizardInput.getMigrationBundle(aSolutionKit));
        }

        boolean cancelled = false;
        boolean successful = false;
        String msg = "";

        try {
            // todo (kpak) - handle multiple kits. for now, install first one.
            //
            Map.Entry<SolutionKit, String> entry = solutionsKits.entrySet().iterator().next();

            Either<String, String> result = AdminGuiUtils.doAsyncAdmin(
                solutionKitAdmin,
                this.getOwner(),
                "Install Solution Kit",
                "The gateway is installing selected solution kit(s)",
                solutionKitAdmin.install(entry.getKey(), entry.getValue()));

            if (result.isLeft()) {
                msg = result.left();
                logger.log(Level.WARNING, msg);
            } else if (result.isRight()) {
                msg = result.right();
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
                DialogDisplayer.showMessageDialog(this.getOwner(), msg, "Install Solution Kit", JOptionPane.INFORMATION_MESSAGE, null);
            } else {
                DialogDisplayer.showMessageDialog(this.getOwner(), msg, "Install Solution Kit", JOptionPane.ERROR_MESSAGE, null);
            }
            super.finish(evt);
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