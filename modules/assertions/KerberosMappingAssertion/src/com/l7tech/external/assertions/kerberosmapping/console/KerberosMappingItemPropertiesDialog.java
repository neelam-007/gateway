package com.l7tech.external.assertions.kerberosmapping.console;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.InputValidator;

import javax.swing.JDialog;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPanel;
import java.awt.Frame;
import java.awt.Dialog;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Dialog for edit of kerberos mapping items.
 *
 * @author steve
 */
public class KerberosMappingItemPropertiesDialog extends JDialog {

    //- PUBLIC

    public KerberosMappingItemPropertiesDialog( final Dialog parent, final MappingItem item ) {
        super(parent, "Edit Mapping", true);
        this.item = item;
        init();
    }

    public KerberosMappingItemPropertiesDialog( final Frame parent, final MappingItem item ) {
        super(parent, "Edit Mapping", true);
        this.item = item;
        init();
    }

    public boolean wasConfirmed() {
        return wasOk;
    }

    //- PRIVATE

    private JButton okButton;
    private JButton cancelButton;
    private JTextField realmTextField;
    private JTextField upnSuffixTextField;
    private JPanel mainPanel;

    private final InputValidator validator = new InputValidator(this, "Edit Mapping");
    private final MappingItem item;
    private boolean wasOk = false;

    private void init() {
        Utilities.setEscKeyStrokeDisposes(this);

        validator.attachToButton(okButton, new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                onOk();
            }
        } );

        cancelButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                dispose();
            }
        } );

        validator.constrainTextFieldToBeNonEmpty("Realm", realmTextField, null);

        modelToView();

        add(mainPanel);
        pack();
        Utilities.centerOnParentWindow( this );
    }

    private void onOk() {
        viewToModel();
        wasOk = true;
        dispose();
    }

    private void modelToView() {
        realmTextField.setText( item.getRealm() );
        upnSuffixTextField.setText( item.getUpnSuffix() );
    }

    private void viewToModel() {
        item.setRealm( realmTextField.getText() );
        item.setUpnSuffix( upnSuffixTextField.getText() );
    }    
}
