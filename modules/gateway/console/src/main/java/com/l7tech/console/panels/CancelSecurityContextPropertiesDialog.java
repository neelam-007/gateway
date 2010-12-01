package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.xmlsec.CancelSecurityContext;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

/**
 * @author ghuang
 */
public class CancelSecurityContextPropertiesDialog extends AssertionPropertiesEditorSupport<CancelSecurityContext> {

    private static final ResourceBundle resources = ResourceBundle.getBundle( CancelSecurityContextPropertiesDialog.class.getName() );

    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JCheckBox failIfNotExistCheckBox;
    private JCheckBox failIfExpiredCheckBox;
    private JComboBox permitCancellationComboBox;

    private CancelSecurityContext assertion;
    private boolean confirmed;

    public CancelSecurityContextPropertiesDialog(Window owner, CancelSecurityContext assertion) {
        super(owner, assertion);
        setData(assertion);
        initialize();
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(CancelSecurityContext assertion) {
        this.assertion = assertion;
    }

    @Override
    public CancelSecurityContext getData(CancelSecurityContext assertion) {
        viewToModel(assertion);
        return assertion;
    }

    private void initialize() {
        setContentPane(mainPanel);

        permitCancellationComboBox.setModel( new DefaultComboBoxModel( CancelSecurityContext.AuthorizationType.values() ) );
        permitCancellationComboBox.setRenderer( new TextListCellRenderer<CancelSecurityContext.AuthorizationType>( new Functions.Unary<String,CancelSecurityContext.AuthorizationType>(){
            @Override
            public String call( final CancelSecurityContext.AuthorizationType authorizationType ) {
                return resources.getString( "permit-cancellation." + authorizationType );
            }
        } ) );

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setMinimumSize( getContentPane().getMinimumSize() );
        pack();
        getRootPane().setDefaultButton(okButton);
        Utilities.centerOnParentWindow(this);
        Utilities.setEscKeyStrokeDisposes(this);

        modelToView();
    }

    private void modelToView() {
        if ( assertion.getRequiredAuthorization() != null ) {
            permitCancellationComboBox.setSelectedItem(assertion.getRequiredAuthorization());
        }
        failIfNotExistCheckBox.setSelected(assertion.isFailIfNotExist());
        failIfExpiredCheckBox.setSelected(assertion.isFailIfExpired());
    }

    private void viewToModel(CancelSecurityContext assertion) {
        assertion.setRequiredAuthorization( (CancelSecurityContext.AuthorizationType)permitCancellationComboBox.getSelectedItem() );
        assertion.setFailIfNotExist(failIfNotExistCheckBox.isSelected());
        assertion.setFailIfExpired(failIfExpiredCheckBox.isSelected());
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}