package com.l7tech.console.panels;

import com.l7tech.gui.util.ImageCache;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import java.util.Set;

/**
 *
 */
public class RoutingDialogUtils {

    public static final String CLIENT_PROP_SEC_HEADER_TAG = "com.l7tech.console.panels.RoutingDialogUtils.secHeaderTag";

    private static final int[] TAGS = {
            RoutingAssertion.IGNORE_SECURITY_HEADER,
            RoutingAssertion.CLEANUP_CURRENT_SECURITY_HEADER,
            RoutingAssertion.REMOVE_CURRENT_SECURITY_HEADER,
            RoutingAssertion.PROMOTE_OTHER_SECURITY_HEADER
    };

    /**
     * Tag buttons.  Must be called to configure radio buttons that are to be passed to {@link #configSecurityHeaderRadioButtons}
     * or {@link #configSecurityHeaderHandling}.
     *
     * @param buttons a four element array of buttons for ignore, cleanup, remove, and promote, in that order;
     *                individual elements may be null if that option isn't available.
     */
    public static void tagSecurityHeaderHandlingButtons(AbstractButton[] buttons) {
        for (int i = 0; i < buttons.length; i++) {
            AbstractButton button = buttons[i];
            if (button != null)
                button.putClientProperty(CLIENT_PROP_SEC_HEADER_TAG, TAGS[i]);
        }
    }

    /**
     * Configure the specified radio buttons using the sec header processing setting from the specified
     * routing assertion.
     *
     * @param ass  a RoutingAssertion from which to configure the radio buttons.  Required.
     * @param defaultAction  action to default to if the assertion's action is unrecognized or unavailable.
     * @param promoteComponent component to enable iff. the selected action is PROMOTE_OTHER, or null.
     * @param buttons array of buttons that must have already been registered with {@link #tagSecurityHeaderHandlingButtons(javax.swing.AbstractButton[])}.  May contain nulls.
     */
    public static void configSecurityHeaderRadioButtons(RoutingAssertion ass,
                                                        int defaultAction,
                                                        JComponent promoteComponent,
                                                        AbstractButton[] buttons)
    {
        if (!configSecurityHeaderRadioButtons(ass.getCurrentSecurityHeaderHandling(), promoteComponent, buttons))
            configSecurityHeaderRadioButtons(defaultAction, promoteComponent, buttons);
    }

    private static boolean configSecurityHeaderRadioButtons(int action, JComponent promoteComponent, AbstractButton[] buttons) {
        boolean selectedOne = false;
        for (AbstractButton button : buttons) {
            if (button != null) {
                boolean b = Integer.valueOf(action).equals(button.getClientProperty(CLIENT_PROP_SEC_HEADER_TAG));
                button.setSelected(b);
                if (b) selectedOne = true;
            }
        }
        if (promoteComponent != null)
            promoteComponent.setEnabled(action == RoutingAssertion.PROMOTE_OTHER_SECURITY_HEADER);
        return selectedOne;
    }

    /**
     * Configure the specified RoutingAssertion based on the tag on the selected button.
     *
     * @param ass             the assertion to configure. Required.
     * @param defaultAction   the action to use if no action is selected, or -1 to avoid changing the assertion in this case.
     * @param buttons         array of buttons that must have already been registered with {@link #tagSecurityHeaderHandlingButtons(javax.swing.AbstractButton[])}.  May contain nulls.
     */
    public static void configSecurityHeaderHandling(RoutingAssertion ass, int defaultAction, AbstractButton[] buttons) {
        for (AbstractButton button : buttons) {
            if (button != null && button.isSelected()) {
                Object valObj = button.getClientProperty(CLIENT_PROP_SEC_HEADER_TAG);
                if (valObj instanceof Integer) {
                    Integer val = (Integer) valObj;
                    ass.setCurrentSecurityHeaderHandling(val);
                    return;
                }
            }
        }
        if (defaultAction != -1)
            ass.setCurrentSecurityHeaderHandling(defaultAction);
    }

    /**
     * Validates a text field that should contain a context variable reference, without the enclosing ${} syntax.
     *
     * @param textField the text field to be validated
     * @param enabled true if the configuration represented by the text field is active and validation should be performed;
     *                false if inactive and validation should not be performed; only the status label is cleared in this case
     * @param statusLabel the status label to be set with an appropriate message and icon
     * @return true if the validation is successful or skipped (if the validation flag is false); false otherwise
     */
    public static boolean validateMessageDestinationTextField(
        JTextField textField, boolean enabled, JLabel statusLabel, Set<String> predecessorVariables) {

        final ImageIcon blankIcon = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Transparent16.png"));
        final ImageIcon infoIcon = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Info16.png"));
        final ImageIcon warningIcon = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Warning16.png"));

        statusLabel.setIcon(blankIcon);
        statusLabel.setText(null);
        textField.setEnabled(enabled);
        boolean valid = true;

        if (enabled) {
            String validateNameResult;
            String variableName = textField.getText();
            if (variableName.length() == 0) {
                valid = false;
            } else if ((validateNameResult = VariableMetadata.validateName(variableName)) != null) {
                valid = false;
                statusLabel.setIcon(warningIcon);
                statusLabel.setText(validateNameResult);
            } else {
                final VariableMetadata meta = BuiltinVariables.getMetadata(variableName);
                if (meta == null) {
                    statusLabel.setIcon(infoIcon);
                    statusLabel.setText("New variable will be created");
                } else {
                    if (meta.isSettable()) {
                        if (meta.getType() == DataType.MESSAGE) {
                            statusLabel.setIcon(infoIcon);
                            statusLabel.setText("Built-in, settable");
                        } else {
                            valid = false;
                            statusLabel.setIcon(warningIcon);
                            statusLabel.setText("Built-in, settable but not message type");
                        }
                    } else {
                        valid = false;
                        statusLabel.setIcon(warningIcon);
                        statusLabel.setText("Built-in, not settable");
                    }
                }

                if (predecessorVariables.contains(variableName)) {
                    statusLabel.setIcon(infoIcon);
                    statusLabel.setText("User defined, will overwrite");
                }
            }
        }
        return valid;
    }
}
