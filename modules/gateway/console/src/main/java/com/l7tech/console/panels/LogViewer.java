package com.l7tech.console.panels;

import com.l7tech.console.SsmApplication;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.log.LogSinkAdmin;
import com.l7tech.gateway.common.log.LogSinkData;
import com.l7tech.gui.util.*;
import com.l7tech.gui.util.SwingWorker;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;
import java.awt.event.*;
import java.io.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
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


    private JMenuBar windowMenuBar = null;
    private JMenu fileMenu = null;
    private JMenu helpMenu = null;
    private JMenuItem exitMenuItem = null;
    private JMenuItem saveMenuItem = null;
    private JMenuItem helpTopicsMenuItem = null;

    private FilterListModel<String> filteredListModel;
    private InputValidator validator;

    private final ClusterNodeInfo clusterNodeInfo;
    private final long sinkId;
    private final String file;
    private SsmPreferences preferences = TopComponents.getInstance().getPreferences();
    private final AtomicReference<LogWorker> workerReference = new AtomicReference<LogWorker>();

    //- PUBLIC


    /**
     * Create a log window for the given node.
     *
     */
    public LogViewer(@NotNull ClusterNodeInfo clusterNodeInfo,
                     long sinkId,
                     @NotNull String file) {
        super(resources.getString("title")+ " "+ file);

        this.clusterNodeInfo = clusterNodeInfo;
        this.sinkId  = sinkId;
        this.file = file;

        initialize();
        loadLogs();
    }



    private void initialize() {

        ImageIcon imageIcon =
                new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/layer7_logo_small_32x32.png"));

        setIconImage(imageIcon.getImage());
        setBounds(0, 0, 850, 600);
        setJMenuBar(getWindowMenuBar());
        setContentPane(contentPane);

        splitPane.setDividerLocation(450);
        logMessageTextArea.setWrapStyleWord(true);
        logList.setModel(getFilteredListModel());
        logList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateLogMessageText();
            }
        });

        Utilities.setEscKeyStrokeDisposes(this);

        validator = new InputValidator(this,getTitle());
        validator.disableButtonWhenInvalid(refreshButton);
        validator.constrainTextFieldToNumberRange(resources.getString("tail.checkbox.text"),tailTextField,0L,Long.MAX_VALUE);

        tailCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tailTextField.setEnabled(tailCheckBox.isSelected());
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
                    filterLogs();
                }
            },
            700);

        cancelButton.setBorder(new EmptyBorder(0,0,0,0));
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
                loadLogs();
            }
        });

        // set init values
        tailCheckBox.setSelected(true);
        tailTextField.setText("100");

        pack();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitMenuEventHandler();
            }
        });

        // Load the last window status (size and location).
        Utilities.restoreWindowStatus(this, preferences.asProperties(), 800, 600);
    }

    private void updateLogMessageText() {
        String val = logList.getSelectedIndex()>filteredListModel.getSize()? null : (String)logList.getSelectedValue();
        logMessageTextArea.setText(val);
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
                    try {
                        ObjectOutputStream oos = null;
                        OutputStream out = null;
                        try {
                            out = new FileOutputStream(file);
                            for(String data : cachedData){
                                out.write(data.getBytes());
                                out.write("\r".getBytes());
                            }
                        }
                        finally {
                            // necessary to get the closing object tag
                            ResourceUtils.closeQuietly(oos);
                            ResourceUtils.closeQuietly(out);
                        }
                    } catch (IOException ioe) {
                        file.delete(); // attempt to clean up
                        DialogDisplayer.showMessageDialog(LogViewer.this, null,
                                resources.getString("save.write.error")+"\n'" + file.getAbsolutePath() + "'.", null);
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
            fileMenu.setText(applicationResources.getString("File"));
            fileMenu.add(getSaveMenuItem());
            fileMenu.addSeparator();
            fileMenu.add(getExitMenuItem());
            int mnemonic = fileMenu.getText().toCharArray()[0];
            fileMenu.setMnemonic(mnemonic);
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



    private java.util.List<String> cachedData = new java.util.ArrayList<String>();

    public boolean loadLogs()  {
        long tail  = -1 ;
        if(tailCheckBox.isSelected()){
            try{
                tail = Long.parseLong(tailTextField.getText());
            }catch( Exception e)
            {
                // do nothing ignore value, should not happen
            }
        }

        // stop loading prev
        if(workerReference.get()!=null){
            workerReference.get().interrupt();
            workerReference.get().cancel();
        }

        cancelButton.setEnabled(true);
        final LogWorker infoWorker = new LogWorker(
                Registry.getDefault().getLogSinkAdmin(),
                tail);

        workerReference.set(infoWorker);
        infoWorker.start();



        return true;
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
        private final long tail;
        private List<String> list;

        /**
         * Create a new log worker.
         * <p/>
         * @param logSinkAdmin  An object reference to the <CODE>LogSinkAdmin</CODE>service
         *
         */
        public LogWorker(final LogSinkAdmin logSinkAdmin, long tail) {
            this.logSinkAdmin = logSinkAdmin;
            this.cancelled = new AtomicBoolean(false);
            this.tail = tail;
            list = new ArrayList<String>();
        }

        /**
         * Construct the value. This function performs the actual work of retrieving logs.
         *
         * @return Object  An object with the value constructed by this function.
         */
        @Override
        public Object construct() {
            list.clear();
            try {
                GZIPInputStream inStream = null;
                BufferedReader dis = null;
                try {
                    boolean done = false;
                    long startByte = 0;
                    StringBuilder sb = new StringBuilder();
                    while(!done && !cancelled.get()){
                        LogSinkData logData = null;
                        try {
                            logData = logSinkAdmin.getSinkLogs(clusterNodeInfo.getNodeIdentifier(),sinkId,file,startByte);
                        } catch ( FindException e ) {
                            ErrorManager.getDefault().notify( Level.WARNING, e, "Error loading log data" );
                            break;
                        }
                        if(logData == null){
                            DialogDisplayer.showMessageDialog(LogViewer.this, null,
                                resources.getString("load.error"), null);
                            break;
                        }
                        inStream =  new GZIPInputStream(new ByteArrayInputStream(logData.getData()));
                        dis = new BufferedReader(new InputStreamReader(inStream));
                        String data = "";
                        boolean eol ;
                        boolean eof = false;

                        while (!eof && !cancelled.get()) {

                            eol = false;
                            while(!eol && !eof){
                                final int read = dis.read();
                                if(read == -1)
                                    eof = true;
                                else if (read=='\n' )
                                     eol = true;

                                else
                                    sb.append((char)read);
                            }
                            if(!eof){
                                if(sb.length()>1 && sb.charAt(sb.length()-1)=='\r')
                                   sb.deleteCharAt(sb.length()-1);
                                data = sb.toString();
                                list.add(data);
                                sb = new StringBuilder();
                            }
                            if(tail>0 && tail<list.size()){
                                break;
                            }
                        }
                        done = logData.getLastReadByteLocation()<0 || ( tail>0 && tail<list.size());
                        startByte = logData.getLastReadByteLocation();
                    }
                    if(tail>0 && tail<list.size()) list = list.subList(0, (int) tail);
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
        }

        @Override
        public void finished() {
            cachedData = Collections.synchronizedList(list);
            filterLogs();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    filteredListModel.filterUpdated();
                }
            });
            if(!workerReference.get().isAlive()){
                cancelButton.setEnabled(false);
            }
        }
    }

}
