package com.l7tech.external.assertions.kerberosmapping.console;

import com.l7tech.console.panels.AssertionPropertiesEditor;
import com.l7tech.external.assertions.kerberosmapping.KerberosMappingAssertion;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;

import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JDialog;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.Frame;
import java.awt.Dialog;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ArrayList;

/**
 * Assertion properties dialog for KerberosMappingAssertion.
 *
 * @author steve
 */
public class KerberosMappingPropertiesDialog extends JDialog implements AssertionPropertiesEditor<KerberosMappingAssertion> {

    //- PUBLIC

    public KerberosMappingPropertiesDialog( final Frame owner, final KerberosMappingAssertion assertion ) {
        super( owner, TITLE, true );
        init();
        setData( assertion );
    }

    public KerberosMappingPropertiesDialog( final Dialog owner, final KerberosMappingAssertion assertion ) {
        super( owner, TITLE, true );
        init();
        setData( assertion );
    }

    public JDialog getDialog() {
        return this;
    }

    public boolean isConfirmed() {
        return wasOk;
    }

    public KerberosMappingAssertion getData( final KerberosMappingAssertion assertion ) {
        final DefaultTableModel model = (DefaultTableModel) mappingTable.getModel();
        List<String> mappings = new ArrayList<String>();
        for ( int i=0; i<model.getRowCount(); i++ ) {
            mappings.add( model.getValueAt( i, 0 ) + "!!" +
                          model.getValueAt( i, 1 ) );
        }

        assertion.setMappings( mappings.toArray( new String[mappings.size()] ));

        return assertion;
    }

    public void setData( final KerberosMappingAssertion assertion ) {
        final DefaultTableModel model = (DefaultTableModel) mappingTable.getModel();
        model.setRowCount( 0 );
        if ( assertion.getMappings() != null  ) {
            for ( String mappingStr : assertion.getMappings() ) {
                model.addRow( mappingStr.split( "!!", 2 ));
            }
        }
    }

    public Object getParameter( String name ) {
        return null;
    }

    public void setParameter( String name, Object value ) {
    }

    //- PRIVATE

    private static final String TITLE = "Kerberos Mapping Assertion Properties";

    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private JTable mappingTable;
    private JButton addButton;
    private JButton removeButton;
    private JButton propertiesButton;

    private boolean wasOk = false;

    private void init() {
        Utilities.setEscKeyStrokeDisposes(this);

        okButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                wasOk = true;
                dispose();
            }
        });

        cancelButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                dispose();
            }
        });

        addButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                addMapping();
            }
        } );

        removeButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                removeMapping();
            }
        } );

        propertiesButton.addActionListener(  new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                editMapping();
            }
        } );

        mappingTable.setModel( new DefaultTableModel(new Object[]{ "Realm", "UPN Suffix" }, 0) {
            @Override
            public boolean isCellEditable( int row, int column ) {
                return false;
            }
        } );
        mappingTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

        Utilities.setDoubleClickAction(mappingTable, propertiesButton);
        
        add(mainPanel);
        pack();
        Utilities.centerOnParentWindow( this );
    }

    private void addMapping() {
        final MappingItem mappingItem = new MappingItem("", "");
        final KerberosMappingItemPropertiesDialog itemDialog = new KerberosMappingItemPropertiesDialog(this, mappingItem);
        DialogDisplayer.display( itemDialog, new Runnable() {
            public void run() {
                if ( itemDialog.wasConfirmed() ) {
                    ((DefaultTableModel)mappingTable.getModel()).addRow(
                            new Object[]{ mappingItem.getRealm(),  mappingItem.getUpnSuffix() }
                    );
                }
            }
        });
    }

    private void removeMapping() {
        int selected = mappingTable.getSelectedRow();
        if ( selected > -1 ) {
            ((DefaultTableModel)mappingTable.getModel()).removeRow( selected );
        }
    }

    private void editMapping() {
        final int selected = mappingTable.getSelectedRow();
        if ( selected > -1 ) {
            final DefaultTableModel model = (DefaultTableModel) mappingTable.getModel();
            final MappingItem mappingItem = new MappingItem( (String) model.getValueAt( selected, 0 ),
                                                             (String) model.getValueAt( selected, 1 ));
            final KerberosMappingItemPropertiesDialog itemDialog = new KerberosMappingItemPropertiesDialog(this, mappingItem);
            DialogDisplayer.display( itemDialog, new Runnable() {
                public void run() {
                    if ( itemDialog.wasConfirmed() ) {
                        model.setValueAt( mappingItem.getRealm(), selected, 0 );
                        model.setValueAt( mappingItem.getUpnSuffix(), selected, 1 );
                    }
                }
            });
        }
    }

}
