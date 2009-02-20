package com.l7tech.console.panels;

import com.l7tech.policy.assertion.RoutingAssertion;

import javax.swing.*;

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
}
