package com.l7tech.console.api;

import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ext.commonui.CustomTargetVariablePanel;

import javax.swing.*;
import javax.swing.event.ChangeListener;

/**
 * Implementation of CustomTargetVariablePanel interface.
 */
public class CustomTargetVariablePanelImpl implements CustomTargetVariablePanel {

    private final TargetVariablePanel panel;

    public CustomTargetVariablePanelImpl (final Assertion assertion, final Assertion previousAssertion) {
        this.panel = new TargetVariablePanel();

        // CustomTargetVariablePanelImpl is also used by CustomAssertionHolderAction to display a Task Action.
        // In this case, there isn't associated assertion and previous assertion.
        //
        if (assertion != null && previousAssertion != null) {
            panel.setAssertion(assertion, previousAssertion);
        }
    }

    @Override
    public void setEnabled (boolean enabled){
        panel.setEnabled(enabled);
    }

    @Override
    public void setAcceptEmpty (boolean acceptEmpty){
        panel.setAcceptEmpty(acceptEmpty);
    }

    @Override
    public void setValueWillBeRead(boolean valueWillBeRead) {
        panel.setValueWillBeRead(valueWillBeRead);
    }

    @Override
    public void setValueWillBeWritten(boolean valueWillBeWritten) {
        panel.setValueWillBeWritten(valueWillBeWritten);
    }

    @Override
    public void setAlwaysPermitSyntax(boolean alwaysPermitSyntax) {
        panel.setAlwaysPermitSyntax(alwaysPermitSyntax);
    }

    @Override
    public boolean isEntryValid (){
        return panel.isEntryValid();
    }

    @Override
    public void setSuffixes (String[] suffixes){
        panel.setSuffixes(suffixes);
    }

    @Override
    public void setVariable (String var){
        panel.setVariable(var);
    }

    @Override
    public String getVariable (){
        return panel.getVariable();
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        panel.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        panel.removeChangeListener(listener);
    }

    @Override
    public JPanel getPanel (){
        return panel;
    }
}