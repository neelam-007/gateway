package com.l7tech.console.panels;

import com.l7tech.gateway.common.audit.AuditAdmin;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.OpaqueId;
import com.l7tech.util.FileUtils;
import com.l7tech.console.MainWindow;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.jcalendar.TimeRangePicker;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.util.SyspropUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A window that let user choose audit events to download.
 *
 * <p>This is a window rather than a dialog because:
 * <ul>
 *  <li>this can be a long running task and therefore should not be modal (which
 *  would block the use of the parent window)
 *  <li>because not modal, it can be hidden behind other windows and therefore
 *  needs its own icon on the desktop task bar
 * </ul>
 *
 * <p>The window is obtained using the {@link #getInstance} method. It enforces
 * at most one audit download at any time; because that can be
 * database/CPU/network instensive.
 *
 * @since SecureSpan 4.2
 * @author rmak
 */
public class DownloadAuditEventsWindow extends JFrame {
    private JPanel mainPanel;
    private JPanel selectionPanel;
    private JRadioButton allTimeRadioButton;
    private JRadioButton selectedTimeRadioButton;
    private TimeRangePicker timeRangePicker;
    private JRadioButton allPublishedServiceRadioButton;
    private JRadioButton selectedPublishedServiceRadioButton;
    private JList publishedServiceList;
    private JTextField filePathTextField;
    private JButton browseButton;
    private JLabel progressLabel;
    private JProgressBar progressBar;
    private JButton downloadButton;
    private JButton cancelOrCloseButton;
    private JScrollPane serviceScrollPane;

    /** Life cycle states of this window. */
    private enum State {
        /** User is configuring parameters. */
        CONFIGURE,
        /** Download is in progress. */
        DOWNLOAD,
        /** Download is being cancelled; performing cleanup. */
        CANCEL,
        /** Download is completed, failed or cancelled; waiting for user to close the window. */
        DONE
    };

    private static final Logger logger = Logger.getLogger(DownloadAuditEventsWindow.class.getName());
    private static final String WINDOW_TITLE_BASE = "Download Audit Events";

    /** The current window. Not exactly a singleton because it can be
        re-instantiated many times. */
    private static DownloadAuditEventsWindow instance = null;
    private static final Object instanceLock = new Object();

    private com.l7tech.gui.util.SwingWorker downloadWorker;

    /** The file where audit events are saved to. */
    private File outputFile;

    /** Current state. */
    private State state = State.CONFIGURE;

    /**
     * Returns the audit download window; which may already be in progress.
     *
     * @return the audit download window
     */
    public static DownloadAuditEventsWindow getInstance() {
        synchronized ( instanceLock ) {
            DownloadAuditEventsWindow currentInstance = instance;

            if ( currentInstance == null ) {
                currentInstance = new DownloadAuditEventsWindow();
                instance = currentInstance;
            }

            return currentInstance;
        }
    }

    /**
     * @throws RuntimeException if failed to get list of published services from
     *         Gateway; with root exception in cause
     */
    private DownloadAuditEventsWindow() {
        super(WINDOW_TITLE_BASE);

        ImageIcon imageIcon = new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/layer7_logo_small_32x32.png"));
        setIconImage(imageIcon.getImage());

        final ActionListener l = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableOrDisableComponents();
            }
        };
        allTimeRadioButton.addActionListener(l);
        selectedTimeRadioButton.addActionListener(l);
        allPublishedServiceRadioButton.addActionListener(l);
        selectedPublishedServiceRadioButton.addActionListener(l);

        initPublishedServiceList();

        filePathTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            public void removeUpdate(DocumentEvent e) { enableOrDisableComponents(); }
            public void changedUpdate(DocumentEvent e) { enableOrDisableComponents(); }
        });

        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onBrowse();
            }
        });

        progressLabel.setText(" ");
        progressBar.setVisible(false);

        downloadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onDownload();
            }
        });

        cancelOrCloseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancelOrClose();
            }
        });

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancelOrClose();
            }
        });

        enableOrDisableComponents();

        getContentPane().add(mainPanel);
        setSize(620, 400);
        Utilities.centerOnScreen(this);
        setVisible(true);
    }

    /**
     * @throws RuntimeException if failed to get list of published services from
     *         Gateway; with root exception in cause
     */
    private void initPublishedServiceList() {
        try {
            final ServiceAdmin serviceAdmin = Registry.getDefault().getServiceManager();
            final EntityHeader[] publishedServices = serviceAdmin.findAllPublishedServices();

            // Sorts alphabetically by name.
            Arrays.sort(publishedServices, new Comparator<EntityHeader>() {
                public int compare(EntityHeader eh1, EntityHeader eh2) {
                    String name1 = eh1.getName();
                    String name2 = eh2.getName();
                    if (name1 == null) name1 = "";
                    if (name2 == null) name2 = "";
                    return name1.toLowerCase().compareTo(name2.toLowerCase());
                }
            });

            final DefaultListModel model = new DefaultListModel();
            for (EntityHeader service : publishedServices) {
                model.addElement(service);
            }
            publishedServiceList.setModel(model);
        } catch (FindException e) {
            throw new RuntimeException("Cannot get list of published services from Gateway.", e);
        }

        publishedServiceList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        publishedServiceList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableComponents();
            }
        });
    }

    private void enableOrDisableComponents() {
        Utilities.setEnabled(selectionPanel, state == State.CONFIGURE);
        timeRangePicker.setEnabled(state == State.CONFIGURE && selectedTimeRadioButton.isSelected());
        publishedServiceList.setEnabled(state == State.CONFIGURE && selectedPublishedServiceRadioButton.isSelected());
        downloadButton.setEnabled(state == State.CONFIGURE
                && (allPublishedServiceRadioButton.isSelected() || publishedServiceList.getSelectedValues().length > 0)
                && filePathTextField.getText().trim().length() != 0);
    }

    /**
     * Handles browse button click.
     */
    private void onBrowse() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            public void useFileChooser(JFileChooser fc) {
                File startingPath = new File(filePathTextField.getText());
                if(!startingPath.exists()) {
                    startingPath = FileChooserUtil.getStartingDirectory();
                }

                final JFileChooser chooser = new JFileChooser(startingPath);
                FileChooserUtil.addListenerToFileChooser(chooser);
                chooser.setDialogTitle("Save Audit Events As");
                chooser.setDialogType(JFileChooser.SAVE_DIALOG);
                chooser.setMultiSelectionEnabled(false);
                final FileFilter fileFilter = new FileFilter() {
                    public boolean accept(File f) {
                        return (f.isDirectory() || f.getName().toLowerCase().endsWith(".zip"));
                    }

                    public String getDescription() {
                        return "ZIP archives (*.zip)";
                    }
                };
                chooser.setFileFilter(fileFilter);
                final int result = chooser.showSaveDialog(DownloadAuditEventsWindow.this);
                if (result != JFileChooser.APPROVE_OPTION)
                    return;

                File filePath = chooser.getSelectedFile();
                if (filePath == null)
                    return;

                // Adds "zip" extension if ZIP filter is selected, the selected file
                // does not exist and the name does not end with ZIP (case insensitive).
                if (chooser.getFileFilter() == fileFilter &&
                    !filePath.exists() &&
                    !filePath.getName().toLowerCase().endsWith(".zip")) {
                    filePath = new File(filePath.getPath() + ".zip");
                }

                filePathTextField.setText(filePath.getAbsolutePath());
            }
        });
    }

    /**
     * Handles download button click.
     */
    private void onDownload() {
        // Adds "zip" extension if not already.
        String filePath = filePathTextField.getText();
        if (!filePath.toLowerCase().endsWith(".zip")) {
            filePath = filePath + ".zip";
            filePathTextField.setText(filePath);
        }
        outputFile = new File(filePath);
        if (! outputFile.isAbsolute()) {
            String defaultDirectory = FileUtils.getDefaultDirectory();
            String separator = SyspropUtil.getProperty( "file.separator" );
            if (! filePath.startsWith(defaultDirectory)) {
                filePath = defaultDirectory + separator + filePath;
                outputFile = new File(filePath);
                filePathTextField.setText(filePath);
            }
        }

        if (outputFile.exists()) {
            String[] options = { "Overwrite", "Cancel" };
            int result = JOptionPane.showOptionDialog(this,
                                                      "The file\n" +
                                                      "    " + outputFile.toString() + "\n" +
                                                      "already exists.  Overwrite?",
                                                      "Overwrite File",
                                                      JOptionPane.YES_NO_OPTION,
                                                      JOptionPane.WARNING_MESSAGE,
                                                      null, options, options[1]);
            if (result == 1)
                return;
        }

        final long fromTime = allTimeRadioButton.isSelected() ? -1 : timeRangePicker.getStartTime().getTime();
        final long toTime = allTimeRadioButton.isSelected() ? -1 : timeRangePicker.getEndTime().getTime();

        final Object[] selected = publishedServiceList.getSelectedValues();
        final int numSelected= selected.length;
        final Goid[] serviceOids = allPublishedServiceRadioButton.isSelected() ? null : new Goid[numSelected];
        if (selectedPublishedServiceRadioButton.isSelected()) {
            for (int i = 0; i < numSelected; ++ i) {
                serviceOids[i] = ((EntityHeader)selected[i]).getGoid();
            }
        }

        try {
            final FileOutputStream outputStream = new FileOutputStream(outputFile);

            state = State.DOWNLOAD;
            enableOrDisableComponents();
            progressBar.setVisible(true);
            progressBar.setIndeterminate(true);
            progressLabel.setText("Preparing data on server ...");

            downloadWorker = new com.l7tech.gui.util.SwingWorker() {
                private long numDownloaded;

                /**
                 * @return Boolean.TRUE if download completed successfully;
                 *         null if interrupted;
                 *         Throwable if error
                 */
                @Override
                public Object construct() {
                    try {
                        final AuditAdmin auditAdmin = Registry.getDefault().getAuditAdmin();
                        OpaqueId downloadId = auditAdmin.downloadAllAudits(fromTime, toTime, serviceOids, 0);
                        AuditAdmin.DownloadChunk chunk;
                        final Thread currentThread = Thread.currentThread();
                        while (isAlive() && !currentThread.isInterrupted() && (chunk = auditAdmin.downloadNextChunk(downloadId)) != null) {
                            if (chunk.chunk != null && chunk.chunk.length > 0) {
                                logger.log(Level.INFO, "Downloaded chunk of audit records: " + chunk.auditsDownloaded + "/" + chunk.approxTotalAudits + "  chunkSize=" + chunk.chunk.length + " bytes");
                                outputStream.write(chunk.chunk);

                                numDownloaded = chunk.auditsDownloaded;
                                final long n = chunk.auditsDownloaded;
                                final long total = chunk.approxTotalAudits;
                                final int percent = (int)Math.round(100. * n / total);
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        int max;
                                        int value;
                                        if (total > Integer.MAX_VALUE) {
                                            // Scales down from long to int proportionally.
                                            final double scale = (double)Integer.MAX_VALUE / total;
                                            max = (int)(scale * total);
                                            value = (int)(scale * n);
                                        } else {
                                            max = (int)total;
                                            value = (int)n;
                                        }
                                        progressBar.setIndeterminate(false);
                                        progressBar.setMinimum(0);
                                        progressBar.setMaximum(max);
                                        progressBar.setValue(value);
                                        progressLabel.setText("Downloaded " + n + " of " + total + " audit records (" + percent + "%)");
                                        setTitle(WINDOW_TITLE_BASE + " (" + percent + "%)");
                                    }
                                });
                            } else if (chunk.chunk != null) {
                                logger.log(Level.INFO, "Audit download still being prepared on server side; will try again.");
                            }
                        }

                        outputStream.close();

                        if (currentThread.isInterrupted() || !isAlive()) {
                            return null;
                        }

                        return Boolean.TRUE;
                    } catch (IOException e) {
                        return e;
                    }
                }

                @Override
                public void finished() {
                    final Object result = get();
                    if (result == Boolean.TRUE) {
                        progressLabel.setText("Download completed. (" + numDownloaded + " audit records)");
                        setTitle(WINDOW_TITLE_BASE + " (Completed)");
                    } else if (result == null) {
                        if (outputFile != null && outputFile.exists()) {
                            try {
                                outputFile.delete();
                            } catch (SecurityException e) {
                                JOptionPane.showMessageDialog(DownloadAuditEventsWindow.this,
                                        "Cannot delete partially downloaded file:\n    " + outputFile.getAbsolutePath() + "\n" + e.getMessage(),
                                        "Cancel Download", JOptionPane.WARNING_MESSAGE);
                            }
                        }
                        progressLabel.setText("Download cancelled.");
                        setTitle(WINDOW_TITLE_BASE + " (Cancelled)");
                    } else if (result instanceof Throwable) {
                        JOptionPane.showMessageDialog(DownloadAuditEventsWindow.this,
                                "Download failed:\n    " + ((Throwable)result).getMessage(),
                                "Download Failure", JOptionPane.WARNING_MESSAGE);
                        progressLabel.setText("Download failed.");
                        setTitle(WINDOW_TITLE_BASE + " (Failed)");
                    }

                    progressBar.setValue(0);
                    progressBar.setIndeterminate(false);
                    progressBar.setVisible(false);
                    state = State.DONE;
                    cancelOrCloseButton.setText("Close");
                    cancelOrCloseButton.setEnabled(true);
                }
            };

            downloadWorker.start();
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this,
                    "Cannot write to file:\n    " + outputFile.getAbsolutePath() + "\n" + e.getMessage(),
                    "Download Failure", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }

    /**
     * Handles cancel/close button click.
     */
    private void onCancelOrClose() {
        if (state == State.DOWNLOAD) {
            String[] options = { "Abort", "Don't Abort" };
            int result = JOptionPane.showOptionDialog(this,
                                                      "Download in progress. Abort?",
                                                      "Abort Confirmation",
                                                      JOptionPane.YES_NO_OPTION,
                                                      JOptionPane.WARNING_MESSAGE,
                                                      null, options, options[1]);
            if (result == 0 && state == State.DOWNLOAD /* make sure still in progress */) {
                cancelOrCloseButton.setEnabled(false);
                progressLabel.setText("Cancelling download...");
                progressBar.setIndeterminate(true);
                downloadWorker.interrupt();
                state = State.CANCEL;
            }
        } else if (state == State.CANCEL) {
            // Already cancelling. Nothing to do but wait for cancel to finish.
        } else {
            // Only in CONFIGURE and DONE states do we close the window.
            dispose();
        }
    }

    @Override
    public void dispose() {
        synchronized ( instanceLock ) {
            instance = null;    // Let the next call to getInstance() instantiate afresh.
        }
        super.dispose();
    }
}
