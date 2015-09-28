package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.OkCancelPanel;
import com.l7tech.console.util.SquigglyFieldUtils;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.common.solutionkit.InstanceModifier;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitsConfig;
import com.l7tech.gui.util.PauseListenerAdapter;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A dialog gets an input as an instance modifier.
 */
public class SolutionKitInstanceModifierDialog extends JDialog {
    private SquigglyTextField instanceModifierTextField;
    private JPanel mainPanel;
    private OkCancelPanel okCancelPanel;

    private Map<SolutionKit, Integer> instanceModifierMaxLengthMap = new HashMap<>();
    private List<SolutionKit> solutionKits;
    private SolutionKitsConfig settings;

    public SolutionKitInstanceModifierDialog(final Frame owner, final List<SolutionKit> solutionKits, final SolutionKitsConfig settings) {
        super(owner, "Add an Instance Modifier", true);
        this.solutionKits = solutionKits;
        this.settings = settings;

        initialize();
    }

    private void initialize() {
        instanceModifierTextField.setText(
            solutionKits.size() == 1? solutionKits.get(0).getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY) : null
        );

        setContentPane(mainPanel);
        Utilities.setEscKeyStrokeDisposes(this);
        getRootPane().setDefaultButton(okCancelPanel.getOkButton());

        Utilities.setMaxLength(instanceModifierTextField.getDocument(), getMaxLengthForInstanceModifier());

        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(instanceModifierTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                SquigglyFieldUtils.validateSquigglyTextFieldState(instanceModifierTextField, new Functions.Unary<String, String>() {
                    @Override
                    public String call(String s) {
                        // Return a validation warning by calling a validation method, validatePrefixedURI.
                        return InstanceModifier.validatePrefixedURI(s);
                    }
                });
            }
        }, 500);

        instanceModifierTextField.getDocument().addDocumentListener(new RunOnChangeListener(){
            @Override
            protected void run() {
                final String instanceModifier = instanceModifierTextField.getText();
                boolean enabled;

                if (StringUtils.isBlank(instanceModifier)) {
                    enabled = true;
                } else {
                    final String validationWarning = InstanceModifier.validatePrefixedURI(instanceModifierTextField.getText());
                    enabled = validationWarning == null;
                }

                okCancelPanel.getOkButton().setEnabled(enabled);
            }
        });

        okCancelPanel.getOkButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        okCancelPanel.getCancelButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        pack();
    }

    private void onOK() {
        final String newInstanceModifier = instanceModifierTextField.getText();

        for (SolutionKit solutionKit: solutionKits) {
            solutionKit.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, StringUtils.isBlank(newInstanceModifier)? null : newInstanceModifier);
            InstanceModifier.setCustomContext(settings, solutionKit);
        }

        dispose();
    }

    /**
     * Calculate what the max length of instance modifier could be for all selected solution kit.
     * The value dynamically depends on given names of folder, service, policy, and encapsulated assertion.
     *
     * @return the max allowed length among folder name, service name, policy name, encapsulated assertion name combining with instance modifier.
     */
    private int getMaxLengthForInstanceModifier() {
        int maxAllowedLengthAllow = Integer.MAX_VALUE;

        int allowedLength;
        for (SolutionKit solutionKit: solutionKits) {
            allowedLength = getMaxLengthForInstanceModifier(solutionKit);
            if (allowedLength < maxAllowedLengthAllow) {
                maxAllowedLengthAllow = allowedLength;
            }
        }

        return maxAllowedLengthAllow;
    }

    /**
     * Calculate what the max length of instance modifier could be for a particular solution kit.
     * The value dynamically depends on given names of folder, service, policy, and encapsulated assertion.
     *
     * @return the max allowed length among folder name, service name, policy name, encapsulated assertion name combining with instance modifier.
     */
    private int getMaxLengthForInstanceModifier(@NotNull final SolutionKit solutionKit) {
        Integer maxLength = instanceModifierMaxLengthMap.get(solutionKit);

        if (maxLength == null) {
            Map<SolutionKit, Bundle> kitBundleMap = settings.getLoadedSolutionKits();
            Bundle bundle = kitBundleMap.get(solutionKit);

            // Compute a new max length
            maxLength = InstanceModifier.getMaxAllowedLength(bundle.getReferences());

            // Save the max value for this solution kit in the map
            instanceModifierMaxLengthMap.put(solutionKit, maxLength);
        }

        return maxLength;
    }
}