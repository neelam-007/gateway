package com.l7tech.assertion.base.util.classloaders.console;

import com.l7tech.assertion.base.util.classloaders.ClassLoaderEntityAdmin;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.SaveException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 16/09/13
 * Time: 10:08 AM
 * To change this template use File | Settings | File Templates.
 */
public class ManageLibrariesDialog extends JDialog {

    private static final Logger logger = Logger.getLogger(ManageLibrariesDialog.class.getName());

    private JButton closeButton;

    private ClassLoaderEntityAdmin entityAdmin;

    public ManageLibrariesDialog(JDialog parent, String entityAdminClass) {
        super(parent, "Manage Libraries", true);
        try {
            entityAdmin = (ClassLoaderEntityAdmin)Registry.getDefault().getExtensionInterface(Class.forName(entityAdminClass), null);
        } catch (Exception e) {
            logger.log(Level.WARNING, "There was a problem with obtaining the class loader entity admin: " + e);
        }

        initComponents();
    }

    public ManageLibrariesDialog(Frame parent, String entityAdminClass) {
        super(parent, "Manage Libraries", true);
        try {
            entityAdmin = (ClassLoaderEntityAdmin)Registry.getDefault().getExtensionInterface(Class.forName(entityAdminClass), null);
        } catch (Exception e) {
            logger.log(Level.WARNING, "There was a problem with obtaining the class loader entity admin: " + e);
        }

        initComponents();
    }

    private void initComponents() {
        getContentPane().setLayout(new BorderLayout());

        JPanel uploadPanel = new JPanel();
        uploadPanel.setLayout(new GridLayout(0, 3));
        uploadPanel.setBorder(BorderFactory.createTitledBorder("Libraries"));

        closeButton = new JButton("Close");
        getRootPane().setDefaultButton(closeButton);

        java.util.List<String> libraries = entityAdmin.getInstalledLibraries();

        for(Map.Entry<String, String> library : entityAdmin.getDefinedLibrariesToUpload().entrySet()) {
            JLabel libraryLabel = new JLabel(library.getKey());
            JLabel statusLabel = new JLabel();
            updateStatusLabel(library.getKey(), statusLabel, libraries);

            JButton button = new JButton("Upload");
            button.addActionListener(createUploadButtonActionListener(library.getKey(), statusLabel, library.getValue()));

            uploadPanel.add(libraryLabel);
            uploadPanel.add(statusLabel);
            uploadPanel.add(button);
        }

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ManageLibrariesDialog.this.setVisible(false);
            }
        });

        add(uploadPanel);
        pack();
        Utilities.centerOnParent(this);
    }

    private void updateStatusLabel(String name, JLabel statusLabel, java.util.List<String> libraries) {
        boolean found = false;
        for(String library : libraries) {
            if(name.equals(library)) {
                found = true;
                break;
            }
        }

        if(found) {
            statusLabel.setText("Installed");
        } else {
            statusLabel.setText("Not Installed");
        }
    }

    private ActionListener createUploadButtonActionListener(final String name, final JLabel statusLabel, final String md5sum) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = FileChooserUtil.createJFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setMultiSelectionEnabled(false);
                Utilities.centerOnParent(getContentPane());
                if(JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(getContentPane())) {
                    try {

                        if(StringUtils.isNotBlank(md5sum)) {
                            String checksum = DigestUtils.md5Hex(new FileInputStream(fileChooser.getSelectedFile()));
                            if(!checksum.equals(md5sum)) {
                                JOptionPane.showMessageDialog(getContentPane(), "An incorrect Jar file was selected.", "Error", JOptionPane.ERROR_MESSAGE);
                            } else {
                                writeLibrary(fileChooser, name);
                                statusLabel.setText("Installed");
                            }
                        } else {
                            writeLibrary(fileChooser, name);
                            statusLabel.setText("Installed");
                        }

                    } catch(IOException ex) {
                        JOptionPane.showMessageDialog(getContentPane(), "Unable to upload library.", "Error", JOptionPane.ERROR_MESSAGE);
                    } catch(SaveException ex) {
                        JOptionPane.showMessageDialog(getContentPane(), "Unable to upload library.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
    }

    private void writeLibrary(JFileChooser fileChooser, String name) throws IOException, SaveException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileInputStream fis = new FileInputStream(fileChooser.getSelectedFile());
        byte[] buffer = new byte[4096];
        int bytesRead = fis.read(buffer);
        while(bytesRead > -1) {
            baos.write(buffer, 0, bytesRead);
            bytesRead = fis.read(buffer);
        }

        entityAdmin.addLibrary(name, baos.toByteArray());
    }
}
