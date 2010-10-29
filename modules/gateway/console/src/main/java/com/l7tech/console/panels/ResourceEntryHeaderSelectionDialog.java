package com.l7tech.console.panels;

import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.ResourceBundle;

/**
 * Dialog for selection of one of a collection of resource entry headers. 
 */
public class ResourceEntryHeaderSelectionDialog extends JDialog {

    private static final ResourceBundle resources = ResourceBundle.getBundle( ResourceEntryHeaderSelectionDialog.class.getName() );

    private JPanel mainPanel;
    private JTextArea descriptionTextArea;
    private JComboBox resourceEntryComboBox;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField resourceKeyTextField;
    private JLabel resourceKeyLabel;
    private JLabel promptLabel;

    private boolean wasOk = false;

    public ResourceEntryHeaderSelectionDialog( final Window parent,
                                               final ResourceType resourceType,
                                               final String resourceKey,
                                               final Collection<ResourceEntryHeader> resourceEntryHeaders ) {
        super( parent, JDialog.DEFAULT_MODALITY_TYPE );
        init( resourceType, resourceKey, resourceEntryHeaders );
    }

    public boolean wasOk() {
        return wasOk;
    }

    public ResourceEntryHeader getSelection() {
        return (ResourceEntryHeader) resourceEntryComboBox.getSelectedItem();
    }

    private void init( final ResourceType resourceType,
                       final String resourceKey,
                       final Collection<ResourceEntryHeader> resourceEntryHeaders ) {
        setTitle( resources.getString( "dialog.title" ));
        setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
        add( mainPanel );

        promptLabel.setText( resources.getString( "prompt." +resourceType.name()+ ".label" ) );
        resourceKeyLabel.setText( resources.getString( "resource-key." +resourceType.name()+ ".label" ) );
        resourceKeyTextField.setText( resourceKey );
        resourceKeyTextField.setCaretPosition( 0 );

        resourceEntryComboBox.setModel( new DefaultComboBoxModel(resourceEntryHeaders.toArray()) );
        resourceEntryComboBox.setRenderer( new TextListCellRenderer<ResourceEntryHeader>( Functions.<String,ResourceEntryHeader>propertyTransform( ResourceEntryHeader.class, "uri" )));
        resourceEntryComboBox.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                updateDescription();
            }
        });

        okButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doOk();
            }
        } );
        cancelButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doCancel();
            }
        }  );

        pack();
        getRootPane().setDefaultButton( cancelButton );
        setMinimumSize( getContentPane().getMinimumSize() );
        Utilities.setEscKeyStrokeDisposes( this );
        Utilities.centerOnParentWindow( this );
        updateDescription();
    }

    private void doOk() {
        wasOk = true;
        dispose();
    }

    private void doCancel() {
        dispose();
    }
    
    private void updateDescription() {
        final ResourceEntryHeader header = (ResourceEntryHeader)resourceEntryComboBox.getSelectedItem();
        if ( header == null || header.getDescription() == null ) {
            descriptionTextArea.setText( "" );
        } else {
            descriptionTextArea.setText( header.getDescription() );
            descriptionTextArea.setCaretPosition( 0 );
        }
    }
}
