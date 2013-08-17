package com.l7tech.console.panels;

import com.l7tech.console.SsmApplication;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.log.LogSinkData;
import com.l7tech.gateway.common.log.LogSinkQuery;
import com.l7tech.gui.util.*;
import com.l7tech.gui.util.SwingWorker;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.GZIPInputStream;

/**
 * To display log records.
 */
public class LogViewer extends JFrame {

    private static final Logger logger = Logger.getLogger(LogViewer.class.getName());
    private static final String RESOURCE_PATH = "com/l7tech/console/resources";
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.panels.LogViewer");
    private static final ResourceBundle applicationResources = ResourceBundle.getBundle("com.l7tech.console.resources.console");

    private JPanel contentPane;
    private SquigglyTextField matchesTextField;
    private JButton refreshButton;
    private JCheckBox tailCheckBox;
    private JTextField tailTextField;
    private JButton closeButton;
    private JList logList;
    private JLabel shownLabel;
    private JButton cancelButton;
    private JTextArea logMessageTextArea;
    private JSplitPane splitPane;
    private JCheckBox autoRefreshCheckBox;
    private JLabel lastUpdatedLabel;
    private JLabel cautionLabel;
    private JPanel cautionPanel;


    private JMenuBar windowMenuBar = null;
    private JMenu fileMenu = null;
    private JMenu helpMenu = null;
    private JMenuItem exitMenuItem = null;
    private JMenuItem saveMenuItem = null;
    private JMenuItem helpTopicsMenuItem = null;

    private FilterListModel<String> filteredListModel;
    private Timer logsRefreshTimer;
    private final int logsRefreshInterval = TopComponents.getInstance().getPreferences().getIntProperty( "logViewer.refreshInterval", 3000); // 3secs
    private final ClusterNodeInfo clusterNodeInfo;
    private final Goid sinkId;
    private final String file;
    private SsmPreferences preferences = TopComponents.getInstance().getPreferences();
    private final AtomicReference<LogWorker> workerReference = new AtomicReference<LogWorker>();
    private java.util.List<String> cachedData = new java.util.ArrayList<String>();
    private InputValidator tailTextValidator;

    private long lastReadByte = 0L;
    private long lastReadTime = 0L;

    /**
     * Create a log window for the given node.
     *
     */
    public LogViewer(@NotNull ClusterNodeInfo clusterNodeInfo,
                     Goid sinkId,
                     @NotNull String file) {
        super(MessageFormat.format(resources.getString("title"), file, clusterNodeInfo.getName()));

        this.clusterNodeInfo = clusterNodeInfo;
        this.sinkId  = sinkId;
        this.file = file;

        initialize();
        loadLogs(false);
    }



    private void initialize() {

        ImageIcon imageIcon =
                new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/layer7_logo_small_32x32.png"));

        setIconImage(imageIcon.getImage());
        setBounds(0, 0, 850, 600);
        setJMenuBar(getWindowMenuBar());
        setContentPane(contentPane);

        splitPane.setDividerLocation(350);
        logMessageTextArea.setWrapStyleWord(true);
        logList.setModel(getFilteredListModel());
        logList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateLogMessageText();
            }
        });
        logList.setCellRenderer( new TextListCellRenderer<String>( TextListCellRenderer.<String>toStringAccessor() ) );

        Utilities.setEscKeyStrokeDisposes(this);

        tailTextValidator = new InputValidator(this,getTitle());
        tailTextValidator.disableButtonWhenInvalid(refreshButton);
        tailTextValidator.constrainTextFieldToNumberRange(resources.getString("tail.checkbox.text"),tailTextField,1,100);

        TextComponentPauseListenerManager.registerPauseListener(
            tailTextField,
            new PauseListenerAdapter() {
                @Override
                public void textEntryPaused(JTextComponent component, long msecs) {
                    String error = tailTextValidator.validate();
                    tailTextField.setToolTipText(error);
                    autoRefreshCheckBox.setEnabled(error==null);
                }
            },
            400);

        tailCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshTailButtons();
            }
        });
        autoRefreshCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshTailButtons();
                if(!autoRefreshCheckBox.isSelected())
                {
                    Date date = new Date(System.currentTimeMillis());
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM d yyyy hh:mm:ss aaa");
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    lastUpdatedLabel.setText(MessageFormat.format(resources.getString("last.update.text"),sdf.format(cal.getTime()),""));
                }
            }
        });

        // saveMenuItem listener
        getSaveMenuItem().
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    saveAsEventHandler();
                }
            });

        // exitMenuItem listener
        getExitMenuItem().
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    exitMenuEventHandler();
                }
            });

        // HelpTopics listener
        getHelpTopicsMenuItem().
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    TopComponents.getInstance().showHelpTopics();
                }
            });

        TextComponentPauseListenerManager.registerPauseListener(
            matchesTextField,
            new PauseListenerAdapter() {
                @Override
                public void textEntryPaused(JTextComponent component, long msecs) {
                    Object[]selectedContent = logList.getSelectedValues();

                    filterLogs();

                    int[] indices = new int[selectedContent.length];
                    int i = 0;
                    for(Object content : selectedContent){
                        indices[i++]=filteredListModel.getFilteredIndex(cachedData.indexOf(content));
                    }
                    logList.setSelectedIndices(indices);
                    logList.ensureIndexIsVisible(logList.getSelectedIndex());

                }
            },
            700);

        cancelButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(workerReference.get()!=null)
                    workerReference.get().cancel();
            }
        });
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadLogs(false);
            }
        });

        // set init values
        tailCheckBox.setSelected(true);
        tailTextField.setText("100");
        refreshTailButtons();

        pack();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitMenuEventHandler();
            }
        });

        Utilities.setMinimumSize( this );

        // Load the last window status (size and location).
        Utilities.restoreWindowStatus(this, preferences.asProperties(), 800, 600);
    }

    private void refreshTailButtons() {
        tailTextField.setEnabled(tailCheckBox.isSelected());
        autoRefreshCheckBox.setEnabled(tailCheckBox.isSelected());
        if(tailCheckBox.isSelected() && autoRefreshCheckBox.isSelected()){
            getLogsRefreshTimer().restart();
        }else if (getLogsRefreshTimer().isRunning()){
            getLogsRefreshTimer().stop();
            // stop loading prev
            if(workerReference.get()!=null){
                workerReference.get().interrupt();
                workerReference.get().cancel();
                workerReference.set(null);
            }
        }
        lastReadByte = 0L;
    }

    private void updateLogMessageText() {
        String val = logList.getSelectedIndex()>=filteredListModel.getSize()? null : (String)logList.getSelectedValue();
        logMessageTextArea.setText(val);
    }

    private Timer getLogsRefreshTimer() {
        if (logsRefreshTimer == null) {
            //Create a refresh logs timer.
            logsRefreshTimer = new Timer(logsRefreshInterval, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    loadLogs(true);
                }
            });
            logsRefreshTimer.setInitialDelay(0);
        }

        return logsRefreshTimer;
    }

    private void filterLogs() {
        // update filter
        final String filterString = matchesTextField.getText();
        Pattern filterPattern = null;
        matchesTextField.setNone();
        matchesTextField.setToolTipText(null);
        if ( filterString != null ) {
            try {
                filterPattern = Pattern.compile(filterString, Pattern.CASE_INSENSITIVE);
            } catch ( PatternSyntaxException e ) {
                // ignore pattern show error
                matchesTextField.setSquiggly();
                matchesTextField.setToolTipText(e.getMessage());
                filterPattern = Pattern.compile("");
            }
        }
        final Pattern pattern = filterPattern;

        filteredListModel.setFilter(filterString == null ? null : new Filter<String>() {
            @Override
            public boolean accept(String o) {
                boolean canBeShown = true;

                if (canBeShown && filterString != null && o != null && !filterString.trim().isEmpty() && pattern != null) {
                    final Matcher matcher = pattern.matcher(o);
                    canBeShown = matcher.find();
                }

                return canBeShown;
            }
        });

        // show selected item
        logList.ensureIndexIsVisible(logList.getSelectedIndex());
        logList.repaint();
        updateLogMessageText();

        shownLabel.setText(MessageFormat.format(resources.getString("shownLabel.text"), filteredListModel.getSize(), cachedData.size()));

        // update caution

        final Color bgColor;
        final boolean showWarning = filterString != null && !filterString.isEmpty();
        if (showWarning) {
            bgColor = new Color(255, 255, 225);
            cautionLabel.setText(resources.getString("caution.label.text"));
        } else {
            bgColor = contentPane.getBackground();
            cautionLabel.setText("");
        }
        cautionPanel.setBackground(bgColor);
    }

    @Override
    public void dispose() {
        if(workerReference.get()!=null)
            workerReference.get().cancel();
        super.dispose();
    }

    /**
     * Save currently displayed logs records to file
     */
    private void saveAsEventHandler() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                doSave(fc);
            }
        });
    }

    private void doSave(final JFileChooser fc) {
        fc.setDialogTitle(resources.getString("save.dialog.title"));
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        FileFilter fileFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt");
            }

            @Override
            public String getDescription() {
                return resources.getString("save.filefilter.description");
            }
        };
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        final String suggestedName = "Gateway_Log_" + sdf.format(new Date()) + ".txt";
        fc.setSelectedFile(new File(suggestedName));
        fc.addChoosableFileFilter(fileFilter);
        fc.setMultiSelectionEnabled(false);
        fc.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(e.getActionCommand())) {
                    fc.setSelectedFile(new File(suggestedName));
                }
            }
        });
        int r = fc.showDialog(LogViewer.this, resources.getString("save"));
        if (r == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (file != null) {
                //
                // Can't check parent is writable due to JDK bug (see bug 2349 for info)
                //
                if ((!file.exists() && file.getParentFile() != null /*&& file.getParentFile().canWrite()*/) ||
                        (file.isFile() && file.canWrite())) {
                    OutputStream out = null;
                    byte[] newline = SyspropUtil.getString( "line.separator", "\n" ).getBytes( Charsets.UTF8 );
                    try {
                        out = new BufferedOutputStream( new FileOutputStream( file) );
                        for(int i = 0; i <filteredListModel.getSize();++i){
                            String data = filteredListModel.getElementAt(i);
                            out.write(data.getBytes(Charsets.UTF8));
                            out.write(newline);
                        }
                    } catch (IOException ioe) {
                        file.delete(); // attempt to clean up
                        DialogDisplayer.showMessageDialog(LogViewer.this, null,
                                resources.getString("save.write.error")+"\n'" + file.getAbsolutePath() + "'.", null);
                    } finally {
                        ResourceUtils.flushAndCloseQuietly( out );
                    }
                } else {
                    DialogDisplayer.showMessageDialog(LogViewer.this, null,
                            resources.getString("save.write.cannot")+"\n'" + file.getAbsolutePath() + "'.", null);
                }
            }
        }
    }

    /**
     * Clean up the resources of the window when the user exits the window.
     */
    private void exitMenuEventHandler() {
        try {
            Properties prop = Utilities.getWindowStatus(this);
            preferences.updateFromProperties(prop, true);
            preferences.store();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to save divider location.", e);
        }

        dispose();
    }

    /**
     * Return clusterWindowMenuBar property value
     *
     * @return JMenuBar
     */
    private JMenuBar getWindowMenuBar() {
        if (windowMenuBar == null) {
            windowMenuBar = new JMenuBar();
            windowMenuBar.add(getFileMenu());
            windowMenuBar.add(getHelpMenu());
        }
        return windowMenuBar;
    }

    /**
     * Return fileMenu property value
     *
     * @return JMenu
     */
    private JMenu getFileMenu() {
        if (fileMenu == null) {
            fileMenu = new JMenu();
            fileMenu.setText( applicationResources.getString( "File" ) );
            fileMenu.add(getSaveMenuItem());
            fileMenu.addSeparator();
            fileMenu.add(getExitMenuItem());
            int mnemonic = fileMenu.getText().toCharArray()[0];
            fileMenu.setMnemonic( mnemonic );
        }
        return fileMenu;
    }

    /**
     * Return helpMenu property value
     *
     * @return JMenu
     */
    private JMenu getHelpMenu() {
        if (helpMenu != null) return helpMenu;

        helpMenu = new JMenu();
        helpMenu.setText(applicationResources.getString("Help"));
        helpMenu.add(getHelpTopicsMenuItem());
        int mnemonic = helpMenu.getText().toCharArray()[0];
        helpMenu.setMnemonic(mnemonic);

        return helpMenu;
    }

    /**
     * Return helpTopicsMenuItem property value
     *
     * @return JMenuItem
     */
    private JMenuItem getHelpTopicsMenuItem() {
        if (helpTopicsMenuItem == null) {
            helpTopicsMenuItem = new JMenuItem();
            helpTopicsMenuItem.setText(applicationResources.getString("Help_TopicsMenuItem_text_name"));
            int mnemonic = helpTopicsMenuItem.getText().toCharArray()[0];
            helpTopicsMenuItem.setMnemonic(mnemonic);
            helpTopicsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        }
        return helpTopicsMenuItem;
    }

    /**
     * Return exitMenuItem property value
     *
     * @return JMenuItem
     */
    private JMenuItem getExitMenuItem() {
        if (exitMenuItem == null) {
            exitMenuItem = new JMenuItem();
            exitMenuItem.setText(applicationResources.getString("ExitMenuItem.name"));
            int mnemonic = 'X';
            exitMenuItem.setMnemonic(mnemonic);
            exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return exitMenuItem;
    }

    /**
     * Return saveMenuItem property value
     *
     * @return JMenuItem
     */
    private JMenuItem getSaveMenuItem() {
        if (saveMenuItem == null) {
            saveMenuItem = new JMenuItem();
            saveMenuItem.setText(applicationResources.getString("SaveAsMenuItem.name"));
            int mnemonic = 'S';
            saveMenuItem.setMnemonic(mnemonic);
            saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return saveMenuItem;
    }

    private ListModel getFilteredListModel() {
        if (filteredListModel != null) return filteredListModel;
        filteredListModel = new FilterListModel<String>(new LogModel());
        return filteredListModel;
    }

    public void loadLogs(boolean isAutoRefresh)  {
        if(tailTextValidator.validate()!=null)
            return;

        int tail  = -1 ;
        if(tailCheckBox.isSelected()){
            try{
                tail = Integer.parseInt(tailTextField.getText());
            }catch( Exception e)
            {
                return;
                // abort loading
            }
            lastReadByte = 0;
        }

        // stop loading prev
        if(workerReference.get()!=null){
            workerReference.get().interrupt();
            workerReference.get().cancel();
        }

        cancelButton.setEnabled(tail<0); // not enable cancel button when in 'tail' mode
        final LogWorker infoWorker = new LogWorker(
                Registry.getDefault().getLogSinkAdmin(),
                tail, isAutoRefresh,logList.getSelectedValues());

        workerReference.set(infoWorker);
        infoWorker.start();
    }

    public String getDisplayedLogKey() {
        return clusterNodeInfo.getId() +  sinkId + file;
    }

    private class LogModel implements ListModel {
        @Override
        public int getSize() {
            return cachedData.size();
        }

        @Override
        public Object getElementAt(int index) {
            if(index < cachedData.size())
                return cachedData.get(index);
            return null;
        }

        @Override
        public void addListDataListener(ListDataListener l) {

        }

        @Override
        public void removeListDataListener(ListDataListener l) {

        }
    }

    class LogWorker extends SwingWorker {


        private final LogSinkAdmin logSinkAdmin;
        private final AtomicBoolean cancelled;
        private final int tail;
        private List<String> list;
        private final boolean isAutoRefresh;
        private Object[] selectedContent;
        private boolean hasReload = false;

        /**
         * Create a new log worker.
         * <p/>
         * @param logSinkAdmin  An object reference to the <CODE>LogSinkAdmin</CODE>service
         *
         */
        public LogWorker(final LogSinkAdmin logSinkAdmin, int tail, boolean isAutoRefresh, Object[] selectedContent) {
            this.logSinkAdmin = logSinkAdmin;
            this.cancelled = new AtomicBoolean(false);
            this.tail = tail;
            this.isAutoRefresh = isAutoRefresh;
            list = new ArrayList<String>(cachedData);
            this.selectedContent   = selectedContent;
        }

        /**
         * Construct the value. This function performs the actual work of retrieving logs.
         *
         * @return Object  An object with the value constructed by this function.
         */
        @Override
        public Object construct() {
            try {
                GZIPInputStream inStream = null;
                Reader dis = null;
                try {
                    boolean done = false;
                    boolean reloadFile = false;
                    long startByte = lastReadByte;
                    long fileSize = 0;
                    final StringBuilder sb = new StringBuilder(1024);
                    while(!done && !cancelled.get()){
                        LogSinkData logData;
                        try {
                            LogSinkQuery query = new LogSinkQuery(tail> 0,lastReadTime,startByte);
                            logData = logSinkAdmin.getSinkLogs(clusterNodeInfo.getNodeIdentifier(),sinkId,file, query );
                            if(logData == null){
                                DialogDisplayer.showMessageDialog(LogViewer.this, null,
                                        resources.getString("load.error"), null);
                                break;
                            }
                            fileSize = logData.getFileSize();
                            lastReadTime = logData.getTimeRead();
                        } catch ( FindException e ) {
                            ErrorManager.getDefault().notify( Level.WARNING, e, "Error loading log data" );
                            break;
                        }
                        inStream =  new GZIPInputStream(new ByteArrayInputStream(logData.getData()),8192);
                        dis = new InputStreamReader(inStream);
                        String data;
                        boolean eol ;
                        boolean eof = false;
                        if(logData.isRotated() || startByte == 0){
                            list.clear();
                            startByte = 0;
                            reloadFile = logData.getNextReadPosition() == -1L && logData.getData().length==0;
                            hasReload = hasReload || reloadFile;
                        }
                        int size = 0;
                        while (!eof && !cancelled.get()) {

                            eol = false;
                            while(!eol && !eof){
                                final int read = dis.read();
                                ++size;
                                if(read == -1)
                                    eof = true;
                                else if (read== (int) '\n' )
                                     eol = true;
                                else
                                    sb.append((char)read);
                            }
                            if(!eof){
                                if(sb.length()>1 && (int) sb.charAt( sb.length() - 1 ) == (int) '\r' )
                                   sb.deleteCharAt(sb.length()-1);
                                data = sb.toString();
                                list.add(data);
                                sb.setLength( 0 );
                            }
                        }
                        done = logData.getNextReadPosition()< 0L || ( tail> 0) || reloadFile;
                        reloadFile = false;
                        lastReadByte = size + startByte -1;
                        startByte = logData.getNextReadPosition();
                    }
                    if(tail> 0 )
                    {
                        boolean firstLineComplete = lastReadByte == fileSize ; //  started reading at middle of file?
                        if(!firstLineComplete) list.remove(0); // first line might not be complete
                        if(tail< (long) list.size() ) list = list.subList(list.size() - tail ,list.size());
                    }
                }catch (IOException e) {
                    logger.warning("Error loading logs");
                    DialogDisplayer.showMessageDialog(LogViewer.this, null,
                        resources.getString("load.error"), null);
                } finally {
                    ResourceUtils.closeQuietly(dis);
                    ResourceUtils.closeQuietly(inStream);
                }
            } catch ( RuntimeException re ) {
                if ( isCancelled() ) { // eat any exceptions if we've been cancelled
                    logger.log(Level.INFO, "Ignoring error for cancelled operation ''{0}''.", ExceptionUtils.getMessage(re));
                    return null;
                } else {
                    throw re;
                }
            }
            return null;
        }

        public boolean isCancelled() {
            return cancelled.get();
        }

        public void cancel() {
            cancelled.set(true);
            cancelButton.setEnabled(false);

            updateLastUpdatedText();
            if(workerReference.get(). isAlive())
                lastReadByte = 0L; // reload next time
        }

        @Override
        public void finished() {
            cachedData = Collections.synchronizedList(list);
            filterLogs();
            if(getLogsRefreshTimer().isRunning()){
                logList.ensureIndexIsVisible(filteredListModel.getSize()-1);
            }
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    filteredListModel.filterUpdated();
                }
            });
            if(!workerReference.get().isAlive()){
                cancelButton.setEnabled(false);
            }
            updateLastUpdatedText();

            if(!hasReload && selectedContent.length>0){
                updateSelectedText();
            }else{
                logList.clearSelection();
            }
        }

        private void updateSelectedText() {
            int[] indices = new int[selectedContent.length];
            int i = 0;
            for(Object content : selectedContent){
                indices[i++]=filteredListModel.getFilteredIndex(cachedData.indexOf(content));
            }

            logList.setSelectedIndices(indices);
            logList.ensureIndexIsVisible(logList.getSelectedIndex());

        }

        private void updateLastUpdatedText() {
            Date date = new Date(System.currentTimeMillis());
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d yyyy hh:mm:ss aaa");
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            String method = isAutoRefresh ? resources.getString("auto.refresh.last.update.text") : "";
            lastUpdatedLabel.setText(MessageFormat.format(resources.getString("last.update.text"),sdf.format(cal.getTime()),method));
        }

    }

}
