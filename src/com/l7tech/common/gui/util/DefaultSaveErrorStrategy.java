package com.l7tech.common.gui.util;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @auther: ghuang
 */

/**
 * The class specifies a default save-error strategy using Save As dialog to save error into a file.
 * The super class is {@link SaveErrorStrategy}
 */
public class DefaultSaveErrorStrategy extends SaveErrorStrategy {
    /**
     * Helper method to save an error report via a Saving-file dialog
     */
    public void saveErrorReportFile() throws UnableToSaveException {
        // Get a file chooser
        final JFileChooser fileChooser = getFileChooser();
        // If the fileChoose is not available, it means that the applet ssm doesn't have right to save a file.
        if (fileChooser == null) {
            throw new UnableToSaveException("Unable to save a report.");
        }

        // Pop up the save file dialog
        int ret = fileChooser.showSaveDialog(errorMessageDialog);
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
            int overwritten = JOptionPane.showConfirmDialog(errorMessageDialog,
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
            JOptionPane.showMessageDialog(errorMessageDialog,
                    fileName + " not found or no pemission to write it.",
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(errorMessageDialog,
                    "Cannot read/write the file " + fileName,
                    "Warning",
                    JOptionPane.WARNING_MESSAGE);
        } finally {
            if (fileWriter != null) try {
                fileWriter.close();
            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(errorMessageDialog,
                        "Cannot read/write the file " + fileName,
                        "Warning",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * Build a file chooser for saving files with a suggestion name with txt/TXT extension.
     * @return a file chooser
     */
    private JFileChooser getFileChooser() {
        try {
            // Create a JFileChooser and setup it
            final JFileChooser fileChooser = new JFileChooser(FileChooserUtil.getStartingDirectory()); // use the user's default directory
            FileChooserUtil.addListenerToFileChooser(fileChooser);

            fileChooser.setDialogTitle(resources.getString("save.dialog.title"));
            fileChooser.setMultiSelectionEnabled(false);  // Allow single selection only
            fileChooser.setFileFilter(new FileFilter() {
                public boolean accept(File file) {
                    return file.getAbsolutePath().endsWith(".txt") || file.getAbsolutePath().endsWith(".TXT") || file.isDirectory();
                }
                public String getDescription() {
                    return "Text Files (*.txt)";
                }
            });

            // Suggest a name for the being saved file
            final String suggestedName = getSuggestedFileName();
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
        } catch(Throwable ex) {
            // If there is any error/exception, just return a null file chooser.
            // If it is an applet, then applet will download an error report from the browser.
            return null;
        }
    }
}
