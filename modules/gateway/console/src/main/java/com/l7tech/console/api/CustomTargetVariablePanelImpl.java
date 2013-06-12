package com.l7tech.console.api;

import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ext.commonui.CustomTargetVariablePanel;

import javax.swing.*;

/**
 * Implementation of CustomTargetVariablePanel interface.
 */
public class CustomTargetVariablePanelImpl implements CustomTargetVariablePanel {

    private final TargetVariablePanel panel;

    public CustomTargetVariablePanelImpl (final Assertion assertion, final Assertion previousAssertion) {
        this.panel = new TargetVariablePanel();
        panel.setAssertion(assertion, previousAssertion);
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
    public JPanel getPanel (){
        return panel;
    }
}