package com.l7tech.console.panels;

import com.l7tech.common.gui.widgets.WrappingLabel;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.BuildInfo;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.SyspropUtil;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ResourceBundle;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    private static ResourceBundle resources;
    private static Runnable shutdownHandler;

    static {
        DialogDisplayer.suppressSheetDisplay(ErrorMessageDialog.class);
        resources = ResourceBundle.getBundle("com/l7tech/console/resources/ErrorMessageDialog");
        shutdownHandler = new Runnable() {public void run(){System.exit(-1);}};
    }

    private JPanel mainPanel;
    private JButton okButton;
    private JButton closeManagerButton;
    private JButton reportButton;
    private JLabel iconLabel;
    private WrappingLabel messageLabel;

    private final Throwable throwable;

    /** Create a neverDisplay exception dialog that never displays anything. */
    public ErrorMessageDialog(final Frame parent) {
        super(parent, false);
        this.throwable = null;
    }

    /** Create a neverDisplay exception dialog that never displays anything. */
    public ErrorMessageDialog(final Dialog parent) {
        super(parent, false);
        this.throwable = null;
    }

    /**
     * Constructor ErrorMessageDialog
     */
    public ErrorMessageDialog(final Frame parent, final String errorMessage, final Throwable throwable) {
        super(parent, true);
        this.throwable = throwable;
        initialize(errorMessage);
    }

    /**
     * Constructor ErrorMessageDialog
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
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.setAlwaysOnTop(this, true);
        Utilities.centerOnScreen(this);

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
    }

    public void actionPerformed(final ActionEvent e) {
        final Object source = e.getSource();
        if (source == okButton) {
            setVisible(false);
        } else if (source == closeManagerButton) {
            shutdownHandler.run();
            setVisible(false);
        } else if (source == reportButton) {
            saveReport();
        }
    }

    private void saveReport() {
        // Create a JFileChooser and setup it
        JFileChooser fileChooser = new JFileChooser(); // use the user's default directory
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
        // Pop up the save file dialog
        int ret = fileChooser.showSaveDialog(TopComponents.getInstance().getTopParent());
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
        if (file == null) {
            JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(),
                    "Cannot create the file " + fileName,
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        } else if (file.exists()) {
            int overwritten = JOptionPane.showConfirmDialog(TopComponents.getInstance().getTopParent(),
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
            JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(),
                    fileName + " not found or no pemission to write it.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(),
                    "Cannot read/write the file " + fileName,
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        } finally {
            if (fileWriter != null) try {
                fileWriter.close();
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(),
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

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd 'at' HH:mm:ss z");
        sb.append(resources.getString("date.time")).append(dateFormat.format(calendar.getTime()));
        sb.append(resources.getString("build.info")).append(BuildInfo.getLongBuildString());
        sb.append(resources.getString("system.properties"));
        for (String property : propertyKeys) {
            sb.append("\t").append(property).append(": ").append(System.getProperty(property)).append("\n");
        }
        sb.append(resources.getString("memory.usage"));
        sb.append(Runtime.getRuntime().freeMemory()).append(resources.getString("free.memory"));
        sb.append(Runtime.getRuntime().totalMemory()).append(resources.getString("total.memory"));
        sb.append(resources.getString("stack.trace"));
        sb.append(stackTrace(throwable));
        sb.append(resources.getString("help.centre"));
        return sb.toString();
    }

    private String stackTrace(Throwable throwable) {
        if (throwable == null) {
            return resources.getString("no.stack.trace");
        }

        Throwable cause = ExceptionUtils.unnestToRoot(throwable);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        cause.printStackTrace(printWriter);
        printWriter.flush();
        return stringWriter.toString();
    }
}
