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

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.EventListener;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

/**
 * User: wlui
 *
 * Usage:
 *     - the following fields must be set to work properly:
 *         - assertion
 *         - suffixes ( if avaliable)
 */

public class TargetVariablePanel  extends JPanel {
    private JTextField suffixField;
    private JLabel prefixLabel;
    private JLabel statusLabel;

    private String prefix = "";
    private Set<String> predecessorVariables = new TreeSet<String>();
    private boolean entryValid = false;

    private String[] suffixes = null;
    private boolean acceptEmpty = false;
    private boolean valueWillBeRead = false;
    private boolean valueWillBeWritten = true;

    private static ResourceBundle resources = ResourceBundle.getBundle(TargetVariablePanel.class.getName());
    private final ImageIcon BLANK_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Transparent16.png"));
    private final ImageIcon OK_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Check16.png"));
    private final ImageIcon WARNING_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Warning16.png"));


    public TargetVariablePanel() {

        suffixField = new JTextField();
        prefixLabel = new JLabel("");
        statusLabel = new JLabel("");

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
        add(suffixField,c);

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

        Utilities.attachDefaultContextMenu(suffixField);
        Utilities.enableGrayOnDisabled(suffixField);
        clearVariableNameStatus();
        
        TextComponentPauseListenerManager.registerPauseListener(
                suffixField,
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
        suffixField.setEnabled(enabled);
        if (!enabled){
            // clear status and tooltips
            clearVariableNameStatus();
            //mainPanel.setToolTipText(null);
            suffixField.setToolTipText(null);
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

    public void setSuffixes(String[] suffixes) {
        this.suffixes = suffixes;
    }

    public String getErrorMessage(){
        validateFields();
        return isEntryValid()?null:statusLabel.getText();
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

    public void setVariable(String var){
        suffixField.setText(var);
        validateFields();
    }

    /**
     * @return the entire variable, including the prefix if available
     */
    public String getVariable(){
        String ret = prefix.isEmpty()? suffixField.getText(): prefix + '.' + suffixField.getText();

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
        return suffixField.getText();
    }

    public void setPrefix(String prefix ) {
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
        suffixField.setToolTipText(null);

        // check empty
        if( acceptEmpty && getSuffix().trim().isEmpty())
        {
            statusLabel.setIcon(OK_ICON);        
            statusLabel.setText(resources.getString("label.ok"));
        }
        else if ((validateNameResult = VariableMetadata.validateName(variableName, valueWillBeRead&&!valueWillBeWritten)) != null) {
            entryValid = false;
            statusLabel.setIcon(WARNING_ICON);        
            statusLabel.setText("Invalid Syntax");
            statusLabel.setToolTipText(reconstructLongStringByAddingLineBreakTags(validateNameResult, 58));
            suffixField.setToolTipText(reconstructLongStringByAddingLineBreakTags(validateNameResult, 58));
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

            final String okPrefix = suffixes != null ? "ok.prefix" : "label.ok";
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
        if (suffixes != null) {
            final String variablePrefix = getVariable();
            for (String suffix: suffixes) {
                if (predecessorVariables.contains(variablePrefix + "." + suffix)) {
                    ret = true;
                    break;
                }
            }
        }
        return ret;
    }

    private boolean isEverySuffixPresentInPredecessors() {
        boolean ret = true;
        if (suffixes != null) {
            final String variablePrefix = getVariable();
            for (String suffix: suffixes) {
                if (!predecessorVariables.contains(variablePrefix + "." + suffix)) {
                    ret = false;
                    break;
                }
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

