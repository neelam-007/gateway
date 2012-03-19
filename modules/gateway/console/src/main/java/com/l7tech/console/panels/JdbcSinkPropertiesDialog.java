package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * TODO Implement stubbed JdbcSinkPropertiesDialog
 */
public class JdbcSinkPropertiesDialog extends JDialog {
    private JButton OKButton;
    private JButton cancelButton;
    private JComboBox comboBox1;
    private JButton manageJDBCConnectionsButton;
    private JButton testButton;
    private JButton createSchemaButton;
    private JButton exportSchemaButton;
    private JCheckBox fallbackToInternalConnectionCheckBox;
    private JCheckBox allAuditsCheckBox;
    private JCheckBox metricsCheckBox;
    private JCheckBox messageProcessingAuditsOnlyCheckBox;
    private JPanel mainPanel;

    public JdbcSinkPropertiesDialog( final Window parent ) {
        super( parent, "JDBC Sink Properties", JDialog.DEFAULT_MODALITY_TYPE );
        setContentPane( mainPanel );
        setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );
        cancelButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                dispose();
            }
        } );
        pack();
        Utilities.setEscKeyStrokeDisposes( this );
        Utilities.setMinimumSize( this );
        Utilities.centerOnParentWindow( this );
    }
}
