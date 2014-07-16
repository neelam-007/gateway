package com.l7tech.console.panels;

import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.log.LogFileInfo;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Functions.Unary;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.l7tech.util.Functions.map;
import static com.l7tech.util.Functions.propertyTransform;

public class LogChooserWindow extends JFrame implements LogonListener {
    protected static final Logger logger = Logger.getLogger(LogChooserWindow.class.getName());
    private static final String RESOURCE_PATH = "com/l7tech/console/resources";

    private JPanel contentPane;
    private JButton closeButton;
    private JButton viewButton;
    private JTable logTable;
    private JTextField filterTextField;
    private JLabel filterWarningLabel;
    private JButton refreshButton;
    private HashMap<String,Frame> openedLogViewers;

    private PermissionFlags flags;
    private SimpleTableModel<LogTableRow> logTableModel;

    private static final int COLUMN_FILE_NAME = 0;
    private static final int COLUMN_NODE_NAME = 2;
    private static final int COLUMN_SINK_NAME = 3;
    private static final int COLUMN_SINK_DESC = 4;

    public LogChooserWindow() {
        super("Select Log");
        initialize();
    }

    private void initialize() {
        flags = PermissionFlags.get(EntityType.LOG_SINK);

        setContentPane(contentPane);

        ImageIcon imageIcon =
          new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/CA_Logo_Black_16x16.png"));
        setIconImage(imageIcon.getImage());

        getRootPane().setDefaultButton(closeButton);

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);

        filterWarningLabel.setVisible(false);

        //noinspection unchecked
        logTableModel = TableUtil.configureTable(
                logTable,
                // Update column indexes above if changing or reordering (COLUMN_FILE_NAME, etc)
                TableUtil.column( "File", 40, 80, 999999, stringProperty("file") ),
                TableUtil.column( "Last Modified", 80, 120, 999999, dateProperty( "lastModified" ), Date.class ),
                TableUtil.column( "Node", 40, 80, 999999, stringProperty( "nodeName" ) ),
                TableUtil.column( "Name", 40, 80, 999999, stringProperty( "name" ) ),
                TableUtil.column( "Description", 80, 120, 999999, stringProperty( "description" ) )
        );

        logTable.setModel( logTableModel );
        logTable.getTableHeader().setReorderingAllowed( false );
        logTable.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
        logTable.setAutoCreateRowSorter( true );
        logTable.getRowSorter().toggleSortOrder( 0 );
        logTable.getSelectionModel().addListSelectionListener( new ListSelectionListener() {
            @Override
            public void valueChanged( ListSelectionEvent e ) {
                enableOrDisableButtons();
            }
        } );
        logTable.setDefaultRenderer( Date.class, new DefaultTableCellRenderer() {
            final SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            @Override
            protected void setValue( Object value ) {
                super.setValue( format.format( (Date) value ) );
            }
        } );

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        viewButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doView();
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadLogs();
            }
        });

       TextComponentPauseListenerManager.registerPauseListener( filterTextField, new PauseListenerAdapter() {
           @Override
           public void textEntryPaused( final JTextComponent component, final long msecs ) {
                resetFilter();
           }
       }, 700 );

        filterTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // consume the enter key event, do nothing
            }
        });

        Utilities.setDoubleClickAction(logTable, viewButton);

        loadLogs();
        pack();
        enableOrDisableButtons();
        Utilities.setMinimumSize(this);

        openedLogViewers = new  HashMap<String,Frame> ();
        Frame[] frames = Frame.getFrames();
        for(Frame frame: frames){
            if (frame instanceof LogViewer && frame.isDisplayable()){
                openedLogViewers.put(((LogViewer) frame).getDisplayedLogKey(), frame);
            }
        }
    }

    private static Unary<String,LogTableRow> stringProperty(final String propName) {
        return propertyTransform( LogTableRow.class, propName );
    }

    private static Unary<Date,LogTableRow> dateProperty(final String propName) {
        return propertyTransform( LogTableRow.class, propName );
    }

    private void doView() {
        for( LogTableRow row : getSelectedLogTableRows()){
            showLog(row);
        }
    }

    private void showLog(final LogTableRow row){
        final ClusterNodeInfo nodeInfo = row.getNodeInfo();
        final SinkConfiguration sinkConfig = row.getSinkConfiguration();
        final String file = row.getFile();

        final String key = nodeInfo.getId() +  sinkConfig.getGoid() + file;
        final Frame window;
        if(openedLogViewers.containsKey(key)){
            window = openedLogViewers.get(key);
            int state = window.getExtendedState();
            state &= ~Frame.ICONIFIED;
            window.setExtendedState(state);
            window.setVisible(true);



        }
        else{
            window = new LogViewer(nodeInfo,sinkConfig.getGoid(), file);
            window.pack();
            Utilities.centerOnScreen(window);
            window.setVisible(true);
            openedLogViewers.put(key,window);

            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    openedLogViewers.remove(key);
                }
            });
        }

    }

    private LogTableRow[] getSelectedLogTableRows() {
        ArrayList<LogTableRow> logTableRow = new ArrayList<LogTableRow>();
        for(int viewRow:  logTable.getSelectedRows()){
            final int modelRow = viewRow == -1 ? -1 : logTable.convertRowIndexToModel( viewRow );
            if ( modelRow > -1 && modelRow < logTableModel.getRowCount() ) {
                logTableRow.add(logTableModel.getRowObject( modelRow ));
            }
        }
        return logTableRow.toArray(new LogTableRow[logTableRow.size()]);
    }

    private void enableOrDisableButtons() {
        boolean haveSel = getSelectedLogTableRows().length > 0;
        viewButton.setEnabled(haveSel);
    }

    private void resetFilter() {
        final String filterString = filterTextField.getText();
        try {
            ((DefaultRowSorter<SimpleTableModel<LogTableRow>,Integer>)logTable.getRowSorter()).setRowFilter( getFilter( filterString ) );
            filterWarningLabel.setVisible( !filterString.isEmpty() );
        } catch (PatternSyntaxException e) {
            logger.info("Invalid Regular Expression, \"" + filterString + "\" :" + e.getMessage());
            DialogDisplayer.showMessageDialog(
                    this,
                    "Invalid syntax for the regular expression, \"" + filterString + "\"",
                    "Log View Filter",
                    JOptionPane.WARNING_MESSAGE,
                    null );
        }
    }

    private RowFilter<SimpleTableModel<LogTableRow>,Integer> getFilter( final String filter ) {
        final Pattern pattern = Pattern.compile(filter, Pattern.CASE_INSENSITIVE);
        return new RowFilter<SimpleTableModel<LogTableRow>,Integer>(){
            @Override
            public boolean include( final Entry<? extends SimpleTableModel<LogTableRow>, ? extends Integer> entry ) {
                return pattern.matcher(entry.getStringValue( COLUMN_FILE_NAME )).find() ||
                       pattern.matcher(entry.getStringValue( COLUMN_NODE_NAME )).find() ||
                       pattern.matcher(entry.getStringValue( COLUMN_SINK_NAME )).find() ||
                       pattern.matcher(entry.getStringValue( COLUMN_SINK_DESC )).find();
            }
        };
    }

    /** @return the TransportAdmin interface, or null if not connected or it's unavailable for some other reason */
    private ClusterStatusAdmin getClusterAdmin() {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent())
            return null;
        return reg.getClusterStatusAdmin();
    }

    private void loadLogs() {
        try {
            ClusterStatusAdmin clusterAdmin = getClusterAdmin();
            if (!flags.canReadSome() || clusterAdmin == null) {
                // Not connected to Gateway, or no permission to read cluster status
                logTableModel.setRows( Collections.<LogTableRow>emptyList() );
                return;
            }
            final ClusterNodeInfo[] nodeInfos = clusterAdmin.getClusterStatus();
            final Collection<SinkConfiguration> sinkConfigs = Registry.getDefault().getLogSinkAdmin().findAllSinkConfigurations();
            final List<LogTableRow> rows = new ArrayList<LogTableRow>();
            for ( final ClusterNodeInfo nodeInfo : nodeInfos ){
                for( final SinkConfiguration sinkConfig: sinkConfigs ){
                    try{
                        // try to get log files
                        final Collection<LogFileInfo> files = Registry.getDefault().getLogSinkAdmin().findAllFilesForSinkByNode(nodeInfo.getNodeIdentifier(),sinkConfig.getGoid());
                        rows.addAll( map( files, new Unary<LogTableRow, LogFileInfo>() {
                            @Override
                            public LogTableRow call( final LogFileInfo logFileInfo ) {
                                return new LogTableRow(nodeInfo, sinkConfig, logFileInfo);
                            }
                        } ) );
                    }catch(PermissionDeniedException e){
                        // ignore
                    }
                }
            }
            logTableModel.setRows( rows );
        } catch (FindException e) {
            ErrorManager.getDefault().notify( Level.WARNING, e, "Error loading log listing" );
        }
    }

    /**
     * Intialization when the connection to the server is established.
     */
    @Override
    public void onLogon(LogonEvent e) {
        // do nothing
    }

    /**
     * Clean up the resources when the connection to the server went down.
     */
    @Override
    public void onLogoff(LogonEvent e) {
        dispose();
    }

    public static class LogTableRow {
        private final ClusterNodeInfo nodeInfo;
        private final SinkConfiguration sinkConfiguration;
        private final LogFileInfo logFileInfo;

        private LogTableRow( final ClusterNodeInfo nodeInfo,
                             final SinkConfiguration sinkConfiguration,
                             final LogFileInfo logFileInfo ) {
            this.nodeInfo = nodeInfo;
            this.sinkConfiguration = sinkConfiguration;
            this.logFileInfo = logFileInfo;
        }

        public SinkConfiguration getSinkConfiguration() {
            return sinkConfiguration;
        }

        public ClusterNodeInfo getNodeInfo() {
            return nodeInfo;
        }

        public String getFile() {
            return logFileInfo.getName();
        }

        public Date getLastModified() {
            return new Date(logFileInfo.getLastModified());
        }

        public String getName() {
            return sinkConfiguration.getName();
        }

        public String getNodeName() {
            return nodeInfo.getName();
        }

        public String getDescription(){
            return sinkConfiguration.getDescription();
        }

        public LogFileInfo getLogFileInfo() {
            return logFileInfo;
        }
    }
}
