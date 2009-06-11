/**
 * The class is a utility class to validate variable prefix.
 */
package com.l7tech.console.util;

import org.apache.commons.lang.StringUtils;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.DataType;
import com.l7tech.gui.util.ImageCache;

import javax.swing.*;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @auther: ghuang
 */
public final class VariablePrefixUtil {

    //- PUBLIC
    
    /**
     * Validate the variable prefix against the name convention of context variables.
     * Note: the method also checks if the name is overlapped with other user defined context variables.
     * @param variablePrefix  the new variable prefix to be validated.
     * @param predecessorVariables the predecessor variables defined in a policy.
     * @param suffixes the variable suffixes defined in an assertion.
     * @param variablePrefixStatusLabel the label showing the validation status.
     * @return ture if the name is valid.
     */
    public static boolean validateVariablePrefix(final String variablePrefix,
                                                 final Set<String> predecessorVariables,
                                                 final String[] suffixes,
                                                 JLabel variablePrefixStatusLabel) {
        boolean isValid = true;
        String validateNameResult = null;
        if (StringUtils.isBlank(variablePrefix)) {
            isValid = true;
        } else if ((validateNameResult = VariableMetadata.validateName(variablePrefix)) != null) {
            isValid = false;
        } else {
            final VariableMetadata meta = BuiltinVariables.getMetadata(variablePrefix);
            if (meta == null) {
                validateNameResult = "New variable prefix will be created";
            } else {
                if (meta.isSettable()) {
                    if (meta.getType() == DataType.MESSAGE) {
                        validateNameResult = "Built-in, settable";
                    } else {
                        isValid = false;
                        validateNameResult = "Built-in, settable but not message type";
                    }
                } else {
                    isValid = false;
                    validateNameResult = "Built-in, not settable";
                }
            }
            for (String suffix: suffixes) {
                if (predecessorVariables.contains(variablePrefix + "." + suffix)) {
                    validateNameResult = "User defined, will overwrite";
                    break;
                }
            }
        }

        if (StringUtils.isBlank(variablePrefix)) {
            variablePrefixStatusLabel.setIcon(new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Transparent16.png")));
        } else if (isValid) {
            variablePrefixStatusLabel.setIcon(new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Info16.png")));
        } else {
            variablePrefixStatusLabel.setIcon(new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Warning16.png")));
        }
        variablePrefixStatusLabel.setText(validateNameResult);

        return isValid;
    }

    /**
     * Clean the icon and text in the status label.
     * @param variablePrefixStatusLabel the label showing the validation status.
     */
    public static void clearVariablePrefixStatus(JLabel variablePrefixStatusLabel) {
        variablePrefixStatusLabel.setIcon(new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Transparent16.png")));
        variablePrefixStatusLabel.setText(null);
    }

    /**
     * Fix a variable name by trimming space and removing any surrounding ${...}
     *
     * @param variableName The name to fix.
     * @return The fixed name.
     */
    public static String fixVariableName( final String variableName ) {
        String varname = variableName;
        if ( varname != null ) {
            varname = varname.trim();
            varname = FIXSTART.matcher(varname).replaceAll("");
            varname = FIXEND.matcher(varname).replaceAll("");
        }
        return varname;
        
    }

    //- PRIVATE

    private static final Pattern FIXSTART = Pattern.compile("\\s*(?:\\$\\{)?\\s*");
    private static final Pattern FIXEND = Pattern.compile("\\s*(?:\\})?\\s*");

}
