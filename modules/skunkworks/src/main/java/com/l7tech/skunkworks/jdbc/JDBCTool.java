package com.l7tech.skunkworks.jdbc;

import com.ddtek.jdbc.extensions.ExtEmbeddedConnection;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.ArrayList;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.sql.Clob;
import java.io.Reader;
import java.io.IOException;

/**
 * Tool to run JDBC query
 */
public class JDBCTool {

    private static final Logger logger = Logger.getLogger( JDBCTool.class.getName() );

    private JTable queryTable;
    private JTextField jdbcUrlTextField;
    private JTextField usernameTextField;
    private JPasswordField passwordField;
    private JButton queryButton;
    private JEditorPane queryEditorPane;
    private JPanel mainPanel;
    private JButton updateButton;
    private JLabel statusLabel;
    private static final String[] JDBC_DRIVER_NAMES = { "com.mysql.jdbc.Driver", "org.apache.derby.jdbc.EmbeddedDriver","com.l7tech.jdbc.sqlserver.SQLServerDriver"};
    private static final String deployDirectory;

    static {
        for ( String jdbcDriverClass : JDBC_DRIVER_NAMES ) {
            try {
                Class.forName( jdbcDriverClass );
            } catch( ClassNotFoundException cnfe ) {
                logger.info("JDBC driver not available '"+jdbcDriverClass+"'.");        
            }
        }
        String deployDir = "build/deploy";
        try {
            final File file = new File(new File(SyspropUtil.getString( "user.home", "" )), "build.properties");
            if ( file.canRead() ) {
                final Properties properties = IOUtils.loadProperties( file );
                deployDir = properties.getProperty( "dev.deploy.dir", deployDir );
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        deployDirectory = deployDir;
    }


    /**
     *
     */
    public static void main( final String[] args ) {
        new JDBCTool();
    }

    public JDBCTool() {
        jdbcUrlTextField.setText("jdbc:derby:"+deployDirectory+"/Gateway/node/default/var/db/ssgdb");
        queryButton.addActionListener( new ActionListener() {
            public void actionPerformed( final ActionEvent actionEvent ) {
                runQuery();
            }
        } );
        updateButton.addActionListener( new ActionListener() {
            public void actionPerformed( final ActionEvent actionEvent ) {
                runUpdate();
            }
        });

        JFrame frame = new JFrame("JDBC Tool v0.2");
        frame.setContentPane(mainPanel);
        frame.pack();
        Utilities.centerOnScreen(frame);
        Utilities.setEscKeyStrokeDisposes(frame);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setVisible(true);
    }

    private void runQuery() {
        String url = jdbcUrlTextField.getText();
        String username = usernameTextField.getText();
        String password = new String(passwordField.getPassword());

        Connection conn = null;
        Statement statement = null;
        ResultSet results = null;
        try {
            conn = DriverManager.getConnection(url, username, password);
            if (conn instanceof ExtEmbeddedConnection) {
                ExtEmbeddedConnection embeddedCon = (ExtEmbeddedConnection)conn;
                boolean unlocked = embeddedCon.unlock("Layer7!@Tech#$");
                if(!unlocked)  logger.log( Level.WARNING, "failed unlocking" );
            }

            statement = conn.createStatement();
            results = statement.executeQuery( queryEditorPane.getText() );
            queryTable.setModel( buildResultsModel( results ) );
            queryTable.setRowSorter( new TableRowSorter<DefaultTableModel>((DefaultTableModel)queryTable.getModel()) );            
            statusLabel.setText("Query successful.");
        } catch ( Exception ex ) {
            logger.log( Level.WARNING, "Error processing query", ex );
            statusLabel.setText( ExceptionUtils.getMessage(ex) );
        } finally {
            ResourceUtils.closeQuietly(results);
            ResourceUtils.closeQuietly(statement);
            ResourceUtils.closeQuietly(conn);
        }
    }

    private void runUpdate() {
        String url = jdbcUrlTextField.getText();
        String username = usernameTextField.getText();
        String password = new String(passwordField.getPassword());

        Connection conn = null;
        Statement statement = null;
        ResultSet results = null;
        try {
            conn = DriverManager.getConnection(url, username, password);
            statement = conn.createStatement();
            int count = statement.executeUpdate( queryEditorPane.getText() );
            statusLabel.setText("Updated rows: " + count);
        } catch ( Exception ex ) {
            logger.log( Level.WARNING, "Error processing query", ex );
            statusLabel.setText( ExceptionUtils.getMessage(ex) );
        } finally {
            ResourceUtils.closeQuietly(results);
            ResourceUtils.closeQuietly(statement);
            ResourceUtils.closeQuietly(conn);
        }
    }

    private TableModel buildResultsModel( final ResultSet results ) throws SQLException {
        java.util.List<String> columnNames = new ArrayList<String>();        
        ResultSetMetaData meta = results.getMetaData();
        final int cols = meta.getColumnCount();
        for ( int i=0; i<cols+1; i++ ) {
            if ( i==0 ) {
                columnNames.add( "#");
                continue;
            }
            columnNames.add( meta.getColumnName(i) );
        }

        DefaultTableModel model = new DefaultTableModel( columnNames.toArray(new String[cols]), 0 ){
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if ( columnIndex == 0 ) {
                    return Integer.class;                    
                }
                return super.getColumnClass(columnIndex);
            }
        };
        int rowcount = 0;
        while ( results.next() ) {
            java.util.List<Object> columnValues = new ArrayList<Object>();
            for ( int i=0; i<cols+1; i++ ) {
                if ( i==0 ) {
                    columnValues.add( ++rowcount );
                    continue;
                }
                Object value = results.getObject( i );
                if ( value instanceof Clob) {
                    Clob clobValue = (Clob) value;
                    Reader reader = null;
                    try {
                        reader = clobValue.getCharacterStream();
                        char[] string = new char[8192];
                        int read = reader.read(string);
                        if ( read > -1 ) {
                            value = new String( string, 0, read );
                        }
                    } catch( IOException ioe ) {
                        ioe.printStackTrace();
                    } finally {
                        ResourceUtils.closeQuietly(reader);
                    }
                }
                columnValues.add( value );
            }

            model.addRow( columnValues.toArray(new Object[cols]) );
        }

        return model;
    }
}
