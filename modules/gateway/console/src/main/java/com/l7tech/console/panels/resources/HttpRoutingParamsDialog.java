package com.l7tech.console.panels.resources;

import com.l7tech.console.table.HttpParamRuleTableHandler;
import com.l7tech.console.table.HttpRuleTableHandler;
import com.l7tech.policy.assertion.HttpRoutingAssertion;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog for configuring HTTP request HTML form parameter mapping rules.
 */
public class HttpRoutingParamsDialog extends JDialog {
    private JPanel mainPanel;

    private JButton reqParamsAdd;
    private JButton reqParamsRemove;
    private JButton editReqPmButton;
    private JTable reqParamsTable;
    private JCheckBox requestParamsCustomCheckBox;
    private JButton closeButton;

    private final HttpRoutingAssertion assertion;
    private final HttpRuleTableHandler requestParamsRulesTableHandler;

    public HttpRoutingParamsDialog( JDialog owner, HttpRoutingAssertion assertion ) {
        super( owner, "Parameters", true);
        setContentPane( mainPanel );
        this.assertion = assertion;

        requestParamsRulesTableHandler = new HttpParamRuleTableHandler(reqParamsTable, reqParamsAdd,
                reqParamsRemove, editReqPmButton,
                assertion.getRequestParamRules());

        ActionListener updateTable = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ( requestParamsCustomCheckBox.isSelected() ) {
                    reqParamsTable.setEnabled(true);
                    requestParamsRulesTableHandler.setEditable(true);
                    reqParamsAdd.setEnabled(true);
                    reqParamsRemove.setEnabled(true);
                    requestParamsRulesTableHandler.updateeditState();
                } else {
                    reqParamsTable.setEnabled(false);
                    requestParamsRulesTableHandler.setEditable(false);
                    reqParamsAdd.setEnabled(false);
                    reqParamsRemove.setEnabled(false);
                    editReqPmButton.setEnabled(false);
                }
            }
        };
        requestParamsCustomCheckBox.addActionListener(updateTable);

        if (assertion.getRequestParamRules().isForwardAll()) {
            requestParamsCustomCheckBox.setSelected( false );
            reqParamsAdd.setEnabled(false);
            reqParamsRemove.setEnabled(false);
            editReqPmButton.setEnabled(false);
            reqParamsTable.setEnabled(false);
            requestParamsRulesTableHandler.setEditable(false);
        } else {
            requestParamsCustomCheckBox.setSelected( true );
            requestParamsRulesTableHandler.updateeditState();
        }

        closeButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                setVisible( false );
            }
        } );
    }

    public void updateAssertion() {
        assertion.getRequestParamRules().setRules( requestParamsRulesTableHandler.getData() );
        assertion.getRequestParamRules().setForwardAll( !requestParamsCustomCheckBox.isSelected() );
    }
}
