package com.l7tech.common.gui;

import com.l7tech.common.gui.widgets.WrappingLabel;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.BuildInfo;
import com.l7tech.common.util.SyspropUtil;
import com.l7tech.console.util.TopComponents;

import javax.swing.filechooser.FileFilter;
import javax.swing.*;
import java.text.SimpleDateFormat;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.Date;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.*;
import java.io.*;

/**
 * @author: ghuang
 */
public class ErrorMessageDialog extends JDialog implements ActionListener {

    private final static int DIALOG_LENGTH = 500;
    private final static int DIALOG_WIDTH = 320;
    private final static String propertyKeys[] = {
            "java.version",
            "java.specification.version",
            "os.name",
            "os.arch",
    };
    private final Throwable throwable;

    private JPanel mainPanel;
    private JButton okButton;
    private JButton closeManagerButton;
    private JButton reportButton;
    private JLabel iconLabel;
    private WrappingLabel messageLabel;

    private static Runnable shutdownHandler;
    public static ResourceBundle resources;
    
    static {
        DialogDisplayer.suppressSheetDisplay(ErrorMessageDialog.class);
        resources = ResourceBundle.getBundle("com/l7tech/common/resources/ErrorMessageDialog");
        shutdownHandler = new Runnable() {public void run(){System.exit(-1);}};
    }

    /**
     * Constructor ErrorMessageDialog for error messages
     */
    public ErrorMessageDialog(final Frame parent, final String errorMessage, final Throwable throwable) {
        super(parent, true);
        this.throwable = throwable;
        initialize(errorMessage);
    }

    /**
     * Constructor ErrorMessageDialog for error messages
     */
    public ErrorMessageDialog(final Dialog parent, final String errorMessage, final Throwable throwable) {
        super(parent, true);
        this.throwable = throwable;
        initialize(errorMessage);
    }

    public synchronized static void setShutdownHandler(Runnable handler) {
        if (handler == null)
            throw new IllegalArgumentException("handler must not be null");

        shutdownHandler = handler;
    }

    /**
     * Initialize the dialog
     *
     * @param errorMessage .
     */
    private void initialize(String errorMessage) {
        setContentPane(mainPanel);
        setTitle(resources.getString("error.dialog.title"));
        iconLabel.setIcon(UIManager.getLookAndFeelDefaults().getIcon("OptionPane.warningIcon"));
        messageLabel.setText(errorMessage);

        okButton.addActionListener(this);
        reportButton.addActionListener(this);
        closeManagerButton.addActionListener(this);

        // Set okButton as a default button
        getRootPane().setDefaultButton(okButton);

        if (JDialog.isDefaultLookAndFeelDecorated()) {
            boolean supportsWindowDecorations =
                    UIManager.getLookAndFeel().getSupportsWindowDecorations();
            if (supportsWindowDecorations) {
                setUndecorated(true);
                getRootPane().setWindowDecorationStyle(JRootPane.WARNING_DIALOG);
            }
        }

        setResizable(true);
        setSize(DIALOG_LENGTH, DIALOG_WIDTH);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.setAlwaysOnTop(this, true);
        Utilities.centerOnScreen(this);
    }

    public void actionPerformed(final ActionEvent e) {
        final Object source = e.getSource();
        if (source == okButton) {
            //setVisible(false);
            dispose();
        } else if (source == closeManagerButton) {
            shutdownHandler.run();
            //setVisible(false);
            dispose();
        } else if (source == reportButton) {
            saveReport();
        }
    }

    /**
     * Build a file chooser for saving files with a suggestion name with txt/TXT extension.
     * @return a file chooser
     */
    private JFileChooser getFileChooser() {
        // Create a JFileChooser and setup it
        final JFileChooser fileChooser = new JFileChooser(); // use the user's default directory
        fileChooser.setDialogTitle(resources.getString("save.dialog.title"));
        fileChooser.setMultiSelectionEnabled(false);  // Allow single selection only
        fileChooser.setFileFilter(new FileFilter() {
            public boolean accept(File file) {
                if (file.getAbsolutePath().endsWith(".txt") || file.getAbsolutePath().endsWith(".TXT")) {
                    return true;
                }
                if (file.isDirectory()) return true;
                return false;
            }
            public String getDescription() {
                return "Text Files (*.txt)";
            }
        });

        // Suggest a name for the being saved file
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        final String suggestedName = "SecureSpanManager_Error_Report_" + sdf.format(new Date()) + ".txt";
        final File sugFile = new File(suggestedName);
        fileChooser.setSelectedFile(sugFile);
        fileChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(JFileChooser.FILE_FILTER_CHANGED_PROPERTY.equals(e.getActionCommand())) {
                    fileChooser.setSelectedFile(sugFile);
                }
            }
        });

        return fileChooser;
    }

    /**
     * Helper method to save an error report via a Saving-file dialog
     */
    private void saveReport() {
        // Get a file chooser
        final JFileChooser fileChooser = getFileChooser();

        // Pop up the save file dialog
        int ret = fileChooser.showSaveDialog(this);
        if (ret != JFileChooser.APPROVE_OPTION) {
            return;
        }

        // Check if the file extension is txt.
        String fileName = fileChooser.getSelectedFile().getPath();
        if (!fileName.endsWith(".txt") && !fileName.endsWith(".TXT")) { // add extension if not present
            fileName = fileName + ".txt";
        }
        // Check if the file does exist.
        File file = new File(fileName);
        if (file.exists()) {
            int overwritten = JOptionPane.showConfirmDialog(this,
                                                      "Overwrite " + fileName + "?",
                                                      "Warning",
                                                      JOptionPane.YES_NO_OPTION);
            if (overwritten != JOptionPane.YES_OPTION) {
                return;
            }
        }
        // Write the error report content into the file
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file);
            fileWriter.write(getReportContent());
            fileWriter.flush();
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this,
                    fileName + " not found or no pemission to write it.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Cannot read/write the file " + fileName,
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
        } finally {
            if (fileWriter != null) try {
                fileWriter.close();
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(this,
                    "Cannot read/write the file " + fileName,
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * Make the content of an error report
     * @return  a string of the content
     */
    private String getReportContent() {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd 'at' HH:mm:ss z");

        sb.append(MessageFormat.format(resources.getString("date.time"), dateFormat.format(new Date())));
        sb.append(MessageFormat.format(resources.getString("build.info"), BuildInfo.getLongBuildString()));
        sb.append(MessageFormat.format(resources.getString("system.properties"),
                System.getProperty(propertyKeys[0]),
                System.getProperty(propertyKeys[1]),
                System.getProperty(propertyKeys[2]),
                System.getProperty(propertyKeys[3])));
        sb.append(MessageFormat.format(resources.getString("memory.usage"),
                Runtime.getRuntime().freeMemory(),
                Runtime.getRuntime().totalMemory()));
        sb.append(MessageFormat.format(resources.getString("stack.trace"), stackTrace(throwable)));
        sb.append(resources.getString("help.centre"));
        return sb.toString().replaceAll("\n", SyspropUtil.getString("line.separator", "\n"));
    }

    /**
     * Helper method to get the full content of the stack trace. 
     * @return
     */
    private String stackTrace(Throwable throwable) {
        if (throwable == null) {
            return resources.getString("no.stack.trace");
        }

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        throwable.printStackTrace(printWriter);
        printWriter.flush();
        return stringWriter.toString();
    }
}
