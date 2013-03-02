package com.l7tech.console.panels;

import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.PauseListener;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;

/**
 * User: wlui
 * //todo explain how to use this Panel
 * Usage:
 *     - the following fields must be set to work properly:
 *         - assertion
 *         - suffixes ( if available)
 */

public class TargetVariablePanel  extends JPanel {
    private JTextField prefixOrVariableField;
    private JLabel prefixLabel;
    private JLabel statusLabel;

    private String prefix = "";
    private Set<String> predecessorVariables = new TreeSet<String>();
    private boolean entryValid;

    private final List<String> suffixes = new ArrayList<String>();
    private boolean acceptEmpty;
    private boolean valueWillBeRead;
    private boolean valueWillBeWritten = true;
    private boolean alwaysPermitSyntax = false;

    private static ResourceBundle resources = ResourceBundle.getBundle(TargetVariablePanel.class.getName());
    private final ImageIcon BLANK_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Transparent16.png"));
    private final ImageIcon OK_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Check16.png"));
    private final ImageIcon WARNING_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Warning16.png"));

    private String defaultVariableOrPrefix;

    public TargetVariablePanel() {

        prefixOrVariableField = new JTextField();
        prefixLabel = new JLabel("");
        statusLabel = new JLabel("");
        prefixOrVariableField.setColumns(20);
        prefixOrVariableField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (defaultVariableOrPrefix != null) {
                    if (prefixOrVariableField.getText().trim().isEmpty()) {
                        prefixOrVariableField.setText(TargetVariablePanel.this.defaultVariableOrPrefix);
                    }
                }
            }
        });

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weighty = 0;
        c.weightx = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        add(prefixLabel,c);

        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weighty = 1;
        c.weightx = 1;
        c.anchor = GridBagConstraints.LINE_START ;
        c.fill = GridBagConstraints.HORIZONTAL ;
        add(prefixOrVariableField,c);

        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weighty = 1;
        c.weightx = 0.5;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets.top = 4;
        add(statusLabel,c);

        Utilities.attachDefaultContextMenu(prefixOrVariableField);
        Utilities.enableGrayOnDisabled(prefixOrVariableField);
        clearVariableNameStatus();
        
        TextComponentPauseListenerManager.registerPauseListener(
                prefixOrVariableField,
                new PauseListener() {
                    @Override
                    public void textEntryPaused(JTextComponent component, long msecs) {
                        validateFields();
                        notifyListeners();
                    }

                    @Override
                    public void textEntryResumed(JTextComponent component) {
                        clearVariableNameStatus();
                        notifyListeners();
                    }
                },
                300);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        prefixOrVariableField.setEnabled(enabled);
        if (!enabled){
            // clear status and tooltips
            clearVariableNameStatus();
            //mainPanel.setToolTipText(null);
            prefixOrVariableField.setToolTipText(null);
            entryValid = true;
        }
        else validateFields();
    }

    public void setAssertion(final Assertion assertion, final Assertion previousAssertion) {
        Set<String> vars =
                (assertion.getParent() != null) ? SsmPolicyVariableUtils.getVariablesSetByPredecessors( assertion ).keySet() :
                (previousAssertion != null)? SsmPolicyVariableUtils.getVariablesSetByPredecessorsAndSelf( previousAssertion ).keySet() :
                new TreeSet<String>();

        // convert all vars to lower
         predecessorVariables = new TreeSet<String>();

        for(String var : vars){
            predecessorVariables.add(var.toLowerCase());
        }
    }

    public void setSuffixes(@NotNull String[] suffixes) {
        setSuffixes(Arrays.asList(suffixes));
    }

    public void setSuffixes(@NotNull Collection<String> suffixes) {
        this.suffixes.clear();
        this.suffixes.addAll(suffixes);
    }

    /**
     * Set a default value for the variable prefix or variable name.
     * <p/>
     * If set, then TargetVariablePanel will ensure the default value is always shown when there is no input.
     *
     * @param defaultVariableOrPrefix if set, then the target variable panel will always show this default value when
     *                                nothing is entered.
     */
    public void setDefaultVariableOrPrefix(String defaultVariableOrPrefix) {
        this.defaultVariableOrPrefix = defaultVariableOrPrefix;
    }

    public String getErrorMessage(){
        validateFields();
        return isEntryValid()?null:statusLabel.getText();
    }

    /**
     * Only used in TargetVariablePanelTest
    */
    protected String getMessage(){
        validateFields();
        return statusLabel.getText();
    }

    /**
     * Only used in TargetVariablePanelTest
     */
    protected void setPredecessorVariables(Set<String> predecessorVariables){
        this.predecessorVariables = predecessorVariables;
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    /**
     * Remove a listener to changes of the panel's validity.
     * <p/>
     * The default is a simple implementation that supports a single
     * listener.
     *
     * @param l the listener to remove
     */
    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    /**
     * notify listeners of the state change
     */
    protected void notifyListeners() {
        ChangeEvent event = new ChangeEvent(this);
        EventListener[] listeners = listenerList.getListeners(ChangeListener.class);
        for (EventListener listener : listeners) {
            ((ChangeListener) listener).stateChanged(event);
        }
    }

    public boolean isEntryValid() {
            return entryValid;
    }

    /**
     * Set the variable or the prefix depending on the usage. If suffixes are defined, then a prefix is being set,
     * otherwise a variable is.
     *
     * @param var name of variable or prefix
     */
    public void setVariable(String var){
        prefixOrVariableField.setText(var);
        prefixOrVariableField.setCaretPosition(0);
        validateFields();
    }

    /**
     * @return the entire variable, including the prefix if available
     */
    public String getVariable(){
        String ret = prefix.isEmpty()? prefixOrVariableField.getText(): prefix + '.' + prefixOrVariableField.getText();

        Matcher m = Syntax.oneVarPattern.matcher(ret.trim());
        if (m.matches()) {
            ret = m.group(1);
        }

        return ret;
    }

    /**
     *
     * @return just the user edited part
     */
    public String getSuffix(){
        return prefixOrVariableField.getText();
    }

    public void setPrefix(@NotNull String prefix ) {
        this.prefix = prefix;
        prefixLabel.setText(prefix + '.');
        validateFields();
    }

    public void setAcceptEmpty(boolean acceptEmpty) {
        this.acceptEmpty = acceptEmpty;
    }

    public boolean isValueWillBeRead() {
        return valueWillBeRead;
    }

    public void setValueWillBeRead(boolean valueWillBeRead) {
        this.valueWillBeRead = valueWillBeRead;
    }

    public boolean isValueWillBeWritten() {
        return valueWillBeWritten;
    }

    public void setValueWillBeWritten(boolean valueWillBeWritten) {
        this.valueWillBeWritten = valueWillBeWritten;
    }

    public boolean isAlwaysPermitSyntax() {
        return alwaysPermitSyntax;
    }

    /**
     * Set whether to allow variable syntax such as array dereferencing (eg, "foo[4]").
     *
     * @param alwaysPermitSyntax true if additional syntax should always be allowed.
     */
    public void setAlwaysPermitSyntax(boolean alwaysPermitSyntax) {
        this.alwaysPermitSyntax = alwaysPermitSyntax;
    }

    /**
     * Validates values in various fields and sets the status labels as appropriate.
     */
    private synchronized void validateFields() {
        if(!isEnabled()) {
            entryValid = true ;
            return;
        }

        final String variableName = prefix.isEmpty()? getVariable(): getSuffix();
        String validateNameResult;
        entryValid = true;
        //mainPanel.setToolTipText(null);
        prefixOrVariableField.setToolTipText(null);

        // check empty
        if( acceptEmpty && getSuffix().trim().isEmpty())
        {
            statusLabel.setIcon(OK_ICON);        
            statusLabel.setText(resources.getString("label.ok"));
        }
        else if ((validateNameResult = VariableMetadata.validateName(variableName, alwaysPermitSyntax || (valueWillBeRead&&!valueWillBeWritten))) != null) {
            entryValid = false;
            statusLabel.setIcon(WARNING_ICON);        
            statusLabel.setText("Invalid Syntax");
            statusLabel.setToolTipText(reconstructLongStringByAddingLineBreakTags(validateNameResult, 58));
            prefixOrVariableField.setToolTipText(reconstructLongStringByAddingLineBreakTags(validateNameResult, 58));
        }
        else {
            final boolean exists;
            final boolean builtin;
            final boolean unsettable;
            final VariableMetadata meta = BuiltinVariables.getMetadata(variableName);
            if (meta != null) {
                exists = true;
                builtin = true;
                unsettable = !meta.isSettable();
            } else {
                final String name = Syntax.parse( variableName, Syntax.DEFAULT_MV_DELIMITER ).remainingName;
                exists = Syntax.getMatchingName(name, predecessorVariables) != null;
                builtin = false;
                unsettable = false;
            }

            final String okPrefix = !suffixes.isEmpty() ? "ok.prefix" : "label.ok";
            final String label;
            if (valueWillBeWritten) {
                if (unsettable) {
                    entryValid = false;
                    label = "built.in.not.settable";
                } else {
                    entryValid = true;
                    label = builtin ? "ok.built.in.settable" : (exists || isAtLeastOneSuffixOverwritten()) ? "ok.overwrite" : okPrefix;
                }
            } else if (valueWillBeRead) {
                if (!exists || !isEverySuffixPresentInPredecessors()) {
                    entryValid = false;
                    label = "invalid.notfound";
                } else {
                    entryValid = true;
                    label = okPrefix;
                }
            } else {
                entryValid = true;
                label = "label.ok";
            }
            statusLabel.setIcon(entryValid ? OK_ICON : WARNING_ICON);
            statusLabel.setText(resources.getString(label));

        }
    }

    private boolean isAtLeastOneSuffixOverwritten() {
        boolean ret = false;
        final String variablePrefix = getVariable();
        for (String suffix: suffixes) {
            //need to convert to lowercase because all predecessorVariables are lowercase
            if (predecessorVariables.contains((variablePrefix + "." + suffix).toLowerCase())) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    private boolean isEverySuffixPresentInPredecessors() {
        boolean ret = true;
        final String variablePrefix = getVariable();
        for (String suffix: suffixes) {
            //need to convert to lowercase because all predecessorVariables are lowercase
            if (!predecessorVariables.contains((variablePrefix + "." + suffix).toLowerCase())) {
                ret = false;
                break;
            }
        }
        return ret;
    }

    private void clearVariableNameStatus() {
        statusLabel.setIcon(BLANK_ICON);
        statusLabel.setText(null);
    }
    
    /**
     * Reconstruct a string by adding line break (<br>) tags into it.  The length of each row in the modified string is up to maxLength.
     *
     * @param longString: the string to reconstrctured.
     * @param maxLength: the maximum length for each row of the string.
     * @return a html string composed by the original string with line break tags.
     */
    private String reconstructLongStringByAddingLineBreakTags(String longString, int maxLength) {
        if (longString == null) return null;

        StringBuilder sb = new StringBuilder(longString.length());
        while (longString.length() > maxLength) {
            char lastChar = longString.charAt(maxLength - 1);
            char charInMaxLength = longString.charAt(maxLength);
            if (lastChar == ' ') {
                sb.append(longString.substring(0, maxLength - 1)).append("<br>");
                longString = longString.substring(maxLength);
            } else if (lastChar == ',' || lastChar == '.') {
                sb.append(longString.substring(0, maxLength)).append("<br>");
                if (longString.charAt(maxLength) == ' ')
                    longString = longString.substring(maxLength + 1);
                else
                    longString = longString.substring(maxLength);
            } else if (charInMaxLength == ' ' || charInMaxLength == ',' || charInMaxLength == '.') {
                if (charInMaxLength == ' ') {
                    sb.append(longString.substring(0, maxLength)).append("<br>");
                } else {
                    sb.append(longString.substring(0, maxLength + 1)).append("<br>");
                }
                try {
                    longString = longString.substring(maxLength + 1);
                } catch (IndexOutOfBoundsException e) {
                    longString = "";
                }
            } else {
                String tmp = longString.substring(0, maxLength);
                int lastSpaceIdx = tmp.lastIndexOf(' ');
                int lastCommaIdx = tmp.lastIndexOf(',');
                int lastPeriodIdx = tmp.lastIndexOf('.');
                int maxIdx = Math.max(Math.max(lastSpaceIdx, lastCommaIdx), lastPeriodIdx);
                if (maxIdx < 0)  maxIdx = maxLength - 1;

                char tmpChar = tmp.charAt(maxIdx);
                if (tmpChar == ' ') {
                    sb.append(longString.substring(0, maxIdx)).append("<br>");
                } else {
                    sb.append(longString.substring(0, maxIdx + 1)).append("<br>");
                }
                longString = longString.substring(maxIdx + 1);
            }
        }
        sb.append(longString);
        sb.insert(0, "<html><body>").append("</body></html>");

        return sb.toString();
    }


    public void updateStatus() {
        validateFields();
    }
}

