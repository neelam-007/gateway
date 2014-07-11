package com.l7tech.external.assertions.websocket.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetMessagePanel;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.websocket.WebSocketValidationAssertion;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.TargetMessageType;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * Created with IntelliJ IDEA.
 * User: nilic
 * Date: 6/29/12
 * Time: 12:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketValidationDialog extends AssertionPropertiesOkCancelSupport<WebSocketValidationAssertion> {

    private JPanel contentPane;
    private JPanel sourcePanel;
    private JPanel messageVariablePrefixTextFieldPanel;
    private WebSocketValidationAssertion webSocketValidationAssertion;
    private TargetMessagePanel targetMessagePanel = new TargetMessagePanel();
    private TargetVariablePanel targetVariablePanel;

    public WebSocketValidationDialog(Window owner, WebSocketValidationAssertion webSocketValidationAssertion) {
        super(WebSocketValidationAssertion.class, owner, "Validate WebSocket message", true);
        if (webSocketValidationAssertion == null)
            throw new IllegalArgumentException();
        this.webSocketValidationAssertion = webSocketValidationAssertion;
        initComponents();
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        sourcePanel.add(targetMessagePanel);
        updateTargetModel(webSocketValidationAssertion);
        targetVariablePanel = new TargetVariablePanel();
        targetVariablePanel.setAcceptEmpty(true);
        messageVariablePrefixTextFieldPanel.setLayout(new BorderLayout());
        messageVariablePrefixTextFieldPanel.add(targetVariablePanel, BorderLayout.CENTER);
        targetVariablePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                enableDisableOkButton();
            }
        });
    }

    @Override
    public JPanel createPropertyPanel() {
         return contentPane;
    }

    //Want to set up the Assertion
    public WebSocketValidationAssertion getData(WebSocketValidationAssertion webSocketValidationAssertion) {
        webSocketValidationAssertion.setUserVariablePrefix(targetVariablePanel.getVariable());
        targetMessagePanel.updateModel(webSocketValidationAssertion);
        return webSocketValidationAssertion;
    }

    //Want to set the dialog
    public void setData(WebSocketValidationAssertion webSocketValidationAssertion) {
        targetVariablePanel.setVariable(webSocketValidationAssertion.getUserVariablePrefix());
        targetVariablePanel.setSuffixes(webSocketValidationAssertion.getVariableSuffix());
        targetVariablePanel.setAssertion(webSocketValidationAssertion, getPreviousAssertion());
    }

    // Set up a fake model for the TMP to avoid mutating the assertion prior to {@link #ok}
    private void updateTargetModel(final WebSocketValidationAssertion assertion) {
        targetMessagePanel.setModel(new MessageTargetableAssertion() {{
            TargetMessageType targetMessageType = assertion.getTarget();
            if ( targetMessageType != null ) {
                setTarget(targetMessageType);
            } else {
                clearTarget();
            }
            setOtherTargetMessageVariable(assertion.getOtherTargetMessageVariable());
        }},getPreviousAssertion());
    }

    private void enableDisableOkButton(){
        boolean enabled = targetVariablePanel.isEntryValid() || targetVariablePanel.getVariable().trim().isEmpty();
        this.getOkButton().setEnabled(enabled);
    }

}
