package com.l7tech.console.panels;

import com.l7tech.util.InetAddressUtil;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.HttpProxyConfiguration;
import com.l7tech.gateway.common.resources.ResourceAdmin;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ResourceBundle;

/**
 * HTTP configuration manager dialog.
 */
public class HttpConfigurationManagerDialog extends JDialog {

    private static final ResourceBundle resources = ResourceBundle.getBundle( HttpConfigurationManagerDialog.class.getName() );

    private JPanel mainPanel;
    private JButton closeButton;
    private JButton helpButton;
    private JButton addButton;
    private JButton editButton;
    private JButton copyButton;
    private JButton removeButton;
    private JTable httpConfigurationTable;
    private JButton editDefaultProxyButton;
    private JTextField defaultHttpProxyTextField;
    private JLabel defaultProxyLabel;

    private final PermissionFlags cpFlags = PermissionFlags.get( EntityType.CLUSTER_PROPERTY );
    private final PermissionFlags flags = PermissionFlags.get( EntityType.HTTP_CONFIGURATION );
    private SimpleTableModel<HttpConfiguration> httpConfigurationTableModel;

    public HttpConfigurationManagerDialog( final Window parent ) {
        super( parent, JDialog.DEFAULT_MODALITY_TYPE );
        init();
        loadHttpConfigurations();
        loadDefaultHttpProxy();
    }

    private void init() {
        setTitle( getResourceString( "dialog.title" ) );
        add( mainPanel );

        httpConfigurationTableModel = TableUtil.configureTable(
                httpConfigurationTable,
                TableUtil.column(getResourceString("table.column.host"),     40, 150,   400, property("host"), String.class),
                TableUtil.column(getResourceString("table.column.port"),     40,  60,   100, intProperty("port"), Integer.class),
                TableUtil.column(getResourceString("table.column.protocol"), 40,  70,   100, protocolProperty("protocol"), HttpConfiguration.Protocol.class),
                TableUtil.column(getResourceString("table.column.path"),     40, 240, 10000, property("path"), String.class),
                TableUtil.column(getResourceString("table.column.proxy"),    40, 150,   400, new Functions.Unary<String,HttpConfiguration>(){
                    @Override
                    public String call( final HttpConfiguration httpConfiguration ) {
                        String proxy = null;
                        final HttpProxyConfiguration proxyConfiguration = httpConfiguration.getProxyConfiguration();
                        if ( proxyConfiguration != null && proxyConfiguration.getHost() != null ) {
                            proxy = InetAddressUtil.getHostForUrl(proxyConfiguration.getHost()) + ":" + proxyConfiguration.getPort();
                        }
                        return proxy;
                    }
                }, String.class)
        );

        httpConfigurationTable.setDefaultRenderer( Integer.class, new DefaultTableCellRenderer(){ // don't render "0"
            { this.setHorizontalAlignment( SwingConstants.RIGHT ); }
            @Override
            public Component getTableCellRendererComponent( final JTable table, final Object value, final boolean isSelected,
                                                            final boolean hasFocus, final int row, final int column ) {
                if ( value instanceof Integer && (Integer)value == 0 ) {
                    return super.getTableCellRendererComponent( table, "", isSelected, hasFocus, row, column );
                } else {
                    return super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, column );
                }
            }
        } );
        httpConfigurationTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        httpConfigurationTable.getTableHeader().setReorderingAllowed( false );
        httpConfigurationTable.setModel( httpConfigurationTableModel );
        Utilities.setRowSorter( httpConfigurationTable, httpConfigurationTableModel,
                new int[]{0,1,2,3},
                new boolean[]{true, true, true, true},
                new Comparator[]{String.CASE_INSENSITIVE_ORDER, null, null, null} );

        RunOnChangeListener enableDisableListener = new RunOnChangeListener(){
            @Override
            public void run() {
                enableAndDisableComponents();
            }
        };

        httpConfigurationTable.getSelectionModel().addListSelectionListener( enableDisableListener );

        addButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doAdd();
            }
        } );
        editButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doEdit();
            }
        } );
        copyButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doCopy();
            }
        } );
        removeButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doRemove();
            }
        } );
        editDefaultProxyButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                editDefaultHttpProxy();
            }
        } );
        helpButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                Actions.invokeHelp(HttpConfigurationManagerDialog.this);
            }
        } );
        closeButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doClose();
            }
        } );

        pack();
        getRootPane().setDefaultButton( closeButton );
        Utilities.setButtonAccelerator( this, helpButton, KeyEvent.VK_F1 );
        Utilities.setDoubleClickAction( httpConfigurationTable, editButton );
        Utilities.centerOnParentWindow( this );
        Utilities.setEscKeyStrokeDisposes( this );
        enableAndDisableComponents();
    }

    private void loadHttpConfigurations() {
        final ResourceAdmin admin = Registry.getDefault().getResourceAdmin();
        try {
            httpConfigurationTableModel.setRows( new ArrayList<HttpConfiguration>(admin.findAllHttpConfigurations()) );
        } catch ( ObjectModelException e ) {
            throw ExceptionUtils.wrap( e );
        }
    }

    private void editDefaultHttpProxy() {
        final ResourceAdmin admin = Registry.getDefault().getResourceAdmin();
        try {
            final HttpProxyConfiguration proxyConfiguration = admin.getDefaultHttpProxyConfiguration();
            final HttpProxyPropertiesDialog dialog = new HttpProxyPropertiesDialog( this, proxyConfiguration );
            DialogDisplayer.display( dialog, new Runnable(){
                @Override
                public void run() {
                    if ( dialog.wasOKed() ) {
                        try {
                            final HttpProxyConfiguration proxyConfiguration = dialog.getValue();
                            admin.setDefaultHttpProxyConfiguration( proxyConfiguration );
                        } catch ( ObjectModelException e ) {
                            throw ExceptionUtils.wrap( e );
                        }
                        
                        loadDefaultHttpProxy();
                    }
                }
            } );
        } catch ( ObjectModelException e ) {
            throw ExceptionUtils.wrap( e );
        }
    }

    private void loadDefaultHttpProxy() {
        if ( cpFlags.canReadAll() ) {
            final ResourceAdmin admin = Registry.getDefault().getResourceAdmin();
            try {
                final HttpProxyConfiguration proxyConfiguration = admin.getDefaultHttpProxyConfiguration();
                if ( proxyConfiguration!=null && proxyConfiguration.getHost() != null ) {
                    defaultHttpProxyTextField.setText( InetAddressUtil.getHostForUrl(proxyConfiguration.getHost()) + ":" + proxyConfiguration.getPort() );
                    defaultHttpProxyTextField.setCaretPosition( 0 );
                } else {
                    defaultHttpProxyTextField.setText( getResourceString("no-default-proxy") );
                }
            } catch ( ObjectModelException e ) {
                throw ExceptionUtils.wrap( e );
            }
        }
    }

    private void doAdd(){
        doEdit( new HttpConfiguration(), false );
    }

    private void doEdit(){
        final int selectedRow = httpConfigurationTable.getSelectedRow();
        if ( selectedRow >= 0 ) {
            final int modelRow = httpConfigurationTable.convertRowIndexToModel( selectedRow );
            doEdit( httpConfigurationTableModel.getRowObject( modelRow ), !flags.canUpdateSome() );
        }
    }

    private void doCopy(){
        final int selectedRow = httpConfigurationTable.getSelectedRow();
        if ( selectedRow >= 0 ) {
            final int modelRow = httpConfigurationTable.convertRowIndexToModel( selectedRow );
            final HttpConfiguration httpConfiguration = new HttpConfiguration( httpConfigurationTableModel.getRowObject( modelRow ), false );
            httpConfiguration.setGoid( HttpConfiguration.DEFAULT_GOID );
            httpConfiguration.setVersion( 0 );
            doEdit( httpConfiguration, false );
        }
    }

    private void doEdit( final HttpConfiguration httpConfiguration,
                         final boolean readOnly ){
        final HttpConfigurationPropertiesDialog dialog = new HttpConfigurationPropertiesDialog( this, httpConfiguration, readOnly );
        DialogDisplayer.display( dialog, new Runnable(){
            @Override
            public void run() {
                if ( dialog.wasOk() ) {
                    final ResourceAdmin admin = Registry.getDefault().getResourceAdmin();
                    try {
                        admin.saveHttpConfiguration( httpConfiguration );
                    } catch ( final DuplicateObjectException e ) {
                        handleDuplicateError(httpConfiguration);
                    } catch ( final ObjectModelException e ) {
                        if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                            handleDuplicateError(httpConfiguration);
                        } else {
                            throw ExceptionUtils.wrap( e );
                        }
                    }

                    loadHttpConfigurations();
                }
            }
        } );
    }

    private void handleDuplicateError(final HttpConfiguration httpConfiguration) {
        DialogDisplayer.showMessageDialog(
                this,
                "The HTTP Options conflict with existing options.\nPlease update the 'General' settings and try again.",
                "Duplicate HTTP Options",
                JOptionPane.INFORMATION_MESSAGE,
                new Runnable() {
                    @Override
                    public void run() {
                        doEdit(httpConfiguration, false);
                    }
                });
    }

    private void doRemove(){
        final int selectedRow = httpConfigurationTable.getSelectedRow();
        if ( selectedRow >= 0 ) {
            final int modelRow = httpConfigurationTable.convertRowIndexToModel( selectedRow );
            final HttpConfiguration httpConfiguration = httpConfigurationTableModel.getRowObject( modelRow );

            final String message = "Are you sure you want to remove the HTTP options matching \""+describe(httpConfiguration)+"\" ?";

            final int width = Utilities.computeStringWidth(this.getFontMetrics(this.getFont()), message);
            final Object messageObject;
            if(width > 600){
                messageObject = Utilities.getTextDisplayComponent(message, 600, 100, -1, -1);
            }else{
                messageObject = message;
            }

            DialogDisplayer.showConfirmDialog( this, messageObject, "Confirm Deletion", JOptionPane.OK_CANCEL_OPTION, new DialogDisplayer.OptionListener(){
                @Override
                public void reportResult( final int option ) {
                    if ( option == JOptionPane.OK_OPTION ) {
                        final ResourceAdmin admin = Registry.getDefault().getResourceAdmin();
                        try {
                            admin.deleteHttpConfiguration( httpConfiguration );
                            httpConfigurationTableModel.removeRow( httpConfiguration );
                        } catch ( ObjectModelException e ) {
                            throw ExceptionUtils.wrap( e );
                        }                        
                    }
                }
            } );
        }
    }

    /**
     * Create a description of the host configuration general (match) settings.
     *
     * <p>If only the host is set then just show the name, else show
     * a pseudo URL for the options.</p>
     *
     * @param httpConfiguration the configuration to describe.
     * @return The description.
     */
    private String describe( final HttpConfiguration httpConfiguration ) {
        final StringBuilder description = new StringBuilder();

        if ( httpConfiguration.getPort()<=0 && httpConfiguration.getProtocol()==null && httpConfiguration.getPath()==null ) {
            description.append( httpConfiguration.getHost() );
        } else {
            if ( httpConfiguration.getProtocol() == null ) {
                description.append( "*" );
            } else {
                description.append( httpConfiguration.getProtocol().name().toLowerCase() );
            }
            description.append("://");
            description.append( InetAddressUtil.getHostForUrl(httpConfiguration.getHost()) );
            description.append(':');
            if ( httpConfiguration.getPort()<=0 ) {
                description.append( "*" );
            } else {
                description.append( httpConfiguration.getPort() );
            }
            if ( httpConfiguration.getPath() == null ) {
                description.append( "/*" );
            } else {
                if ( !httpConfiguration.getPath().startsWith( "/" )) {
                    description.append( '/' );                    
                }
                description.append( httpConfiguration.getPath() );
            }
        }

        return description.toString();
    }

    private void doClose() {
        dispose();
    }

    private void enableAndDisableComponents() {
        final boolean selectedRow = httpConfigurationTable.getSelectedRow() > -1;
        addButton.setEnabled( flags.canCreateSome() );
        editButton.setEnabled( selectedRow );
        copyButton.setEnabled( selectedRow && flags.canCreateSome() );
        removeButton.setEnabled( selectedRow && flags.canDeleteSome() );
        editDefaultProxyButton.setEnabled( cpFlags.canCreateSome() );
        defaultProxyLabel.setEnabled( cpFlags.canReadAll() );
        defaultHttpProxyTextField.setEnabled( cpFlags.canReadAll() );
    }

    private String getResourceString( final String key ) {
        return resources.getString( key );
    }

    private static Functions.Unary<String,HttpConfiguration> property( final String propName ) {
        return typedProperty( propName );
    }

    private static Functions.Unary<Integer,HttpConfiguration> intProperty( final String propName ) {
        return typedProperty( propName );
    }

    private static Functions.Unary<HttpConfiguration.Protocol,HttpConfiguration> protocolProperty( final String propName ) {
        return typedProperty( propName );
    }

    private static <RT> Functions.Unary<RT,HttpConfiguration> typedProperty( final String propName ) {
        return Functions.propertyTransform(HttpConfiguration.class, propName);
    }

}
