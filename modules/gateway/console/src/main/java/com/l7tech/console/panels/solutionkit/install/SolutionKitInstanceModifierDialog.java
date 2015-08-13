package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.OkCancelPanel;
import com.l7tech.console.util.SquigglyFieldUtils;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.common.api.solutionkit.SolutionKitsConfig;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gui.util.PauseListenerAdapter;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;
import com.l7tech.util.ValidationUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URLDecoder;
import java.util.*;
import java.util.List;

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
                        return validatePrefixedURI(s);
                    }
                });
            }
        }, 500);

        instanceModifierTextField.getDocument().addDocumentListener(new RunOnChangeListener(){
            @Override
            protected void run() {
                final String validationWarning = validatePrefixedURI(instanceModifierTextField.getText());
                // If the validation warning is null, then set the OK button enabled.  Otherwise, set to be disabled.
                okCancelPanel.getOkButton().setEnabled(validationWarning == null);
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
        if (StringUtils.isEmpty(newInstanceModifier)) return;

        for (SolutionKit solutionKit: solutionKits) {
            solutionKit.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, newInstanceModifier);
        }

        dispose();
    }

    /**
     * Calculate what the max length of instance modifier could be for all selected solution kit.
     * The value dynamically depends on given names of folder, service, policy, and encapsulated assertion.
     *
     * @return the minimum allowed length among folder name, service name, policy name, encapsulated assertion name combining with instance modifier.
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
     * @return the minimum allowed length among folder name, service name, policy name, encapsulated assertion name combining with instance modifier.
     */
    private int getMaxLengthForInstanceModifier(@NotNull final SolutionKit solutionKit) {
        Integer maxLength = instanceModifierMaxLengthMap.get(solutionKit);

        if (maxLength == null) {
            Map<SolutionKit, Bundle> kitBundleMap = settings.getLoadedSolutionKits();
            Bundle bundle = kitBundleMap.get(solutionKit);

            // Compute a new max length
            maxLength = getMaxLengthForInstanceModifier(bundle.getReferences());

            // Save the max value for this solution kit in the map
            instanceModifierMaxLengthMap.put(solutionKit, maxLength);
        }

        return maxLength;
    }

    /**
     * Calculate what the max length of instance modifier could be.
     * The value dynamically depends on given names of folder, service, policy, and encapsulated assertion.
     *
     * TODO make this validation available to headless interface (i.e. SolutionKitManagerResource)
     *
     * @return the minimum allowed length among folder name, service name, policy name, encapsulated assertion name combining with instance modifier.
     */
    private int getMaxLengthForInstanceModifier(@NotNull final List<Item> bundleReferenceItems) {
        int maxAllowedLengthAllow = Integer.MAX_VALUE;
        int allowedLength;
        String entityName;
        EntityType entityType;

        for (Item item: bundleReferenceItems) {
            entityName = item.getName();
            entityType = EntityType.valueOf(item.getType());

            if (entityType == EntityType.FOLDER || entityType == EntityType.ENCAPSULATED_ASSERTION) {
                // The format of a folder name is "<folder_name> <instance_modifier>".
                // The format of a encapsulated assertion name is "<instance_modifier> <encapsulated_assertion_name>".
                // The max length of a folder name or an encapsulated assertion name is 128.
                allowedLength = 128 - entityName.length() - 1; // 1 represents one char of white space.
            } else if (entityType == EntityType.POLICY) {
                // The format of a policy name is "<instance_modifier> <policy_name>".
                // The max length of a policy name is 255.
                allowedLength = 255 - entityName.length() - 1; // 1 represents one char of white space.
            } else if (entityType == EntityType.SERVICE) {
                // The max length of a service routing uri is 128
                // The format of a service routing uri is "/<instance_modifier>/<service_name>".
                allowedLength = 128 - entityName.length() - 2; // 2 represents two chars of '/' in the routing uri.
            }  else {
                continue;
            }

            if (maxAllowedLengthAllow > allowedLength) {
                maxAllowedLengthAllow = allowedLength;
            }
        }

        if (maxAllowedLengthAllow < 0) maxAllowedLengthAllow = 0;

        return maxAllowedLengthAllow;
    }

    /**
     * Validate if an instance modifier is a valid part of a URI.
     *
     * TODO make this validation available to headless interface (i.e. SolutionKitManagerResource)
     *
     * @return null if the instance modifier is valid.  Otherwise, a string explaining invalid reason.
     */
    private String validatePrefixedURI(@NotNull String instanceModifier) {
        // Service Routing URI must not start with '/ssg'
        if (instanceModifier.startsWith("ssg")) {
            return "Instance modifier must not start with 'ssg', since Service Routing URI must not start with '/ssg'";
        }

        // validate for XML chars and new line char
        String [] invalidChars = new String[]{"\"", "&", "'", "<", ">", "\n"};
        for (String invalidChar : invalidChars) {
            if (instanceModifier.contains(invalidChar)) {
                if (invalidChar.equals("\n")) invalidChar = "\\n";
                return "Invalid character '" + invalidChar + "' is not allowed in the installation prefix.";
            }
        }

        String testUri = "http://ssg.com:8080/" + instanceModifier + "/query";
        if (!ValidationUtils.isValidUrl(testUri)) {
            return "Invalid prefix '" + instanceModifier + "'. It must be possible to construct a valid routing URI using the prefix.";
        }

        try {
            URLDecoder.decode(instanceModifier, "UTF-8");
        } catch (Exception e) {
            return "Invalid prefix '" + instanceModifier + "'. It must be possible to construct a valid routing URL using the prefix.";
        }

        return null;
    }
}