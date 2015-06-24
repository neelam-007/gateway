package com.l7tech.console.panels.solutionkit.install;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.gateway.common.api.solutionkit.SkarProcessor;
import com.l7tech.gateway.common.api.solutionkit.SolutionKitsConfig;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.PauseListenerAdapter;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wizard panel which allows the user to select a solution kit file to install.
 */
public class SolutionKitLoadPanel extends WizardStepPanel<SolutionKitsConfig> {
    private static final Logger logger = Logger.getLogger(SolutionKitLoadPanel.class.getName());

    private static final String STEP_LABEL = "Choose Solution Kit File";
    private static final String STEP_DESC = "Specify the location of solution kit file (skar) to install.";

    private static final FileFilter SK_FILE_FILTER = FileChooserUtil.buildFilter(".skar", "Skar (*.skar)");

    private JPanel mainPanel;
    private JTextField fileTextField;
    private JButton fileButton;

    private SolutionKitsConfig solutionKitsConfig;

    public SolutionKitLoadPanel() {
        super(null);
        initialize();
    }

    @Override
    public String getStepLabel() {
        return STEP_LABEL;
    }

    @Override
    public String getDescription() {
        return STEP_DESC;
    }

    @Override
    public boolean canAdvance() {
        return !fileTextField.getText().trim().isEmpty();
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public void readSettings(SolutionKitsConfig settings) throws IllegalArgumentException {
        solutionKitsConfig = settings;
        solutionKitsConfig.clear(false);
    }

    @Override
    public boolean onNextButton() {
        File file = new File(fileTextField.getText().trim());
        if (!file.exists()) {
            DialogDisplayer.showMessageDialog(this.getOwner(), "The file does not exist.", "Error", JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        if (!file.isFile()) {
            DialogDisplayer.showMessageDialog(this.getOwner(), "The file must be a file type.", "Error", JOptionPane.ERROR_MESSAGE, null);
            return false;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            new SkarProcessor(solutionKitsConfig).load(fis);
        } catch (IOException | SolutionKitException e) {
            solutionKitsConfig.clear(false);
            final String msg = "Unable to open solution kit: " + ExceptionUtils.getMessage(e);
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            DialogDisplayer.showMessageDialog(this.getOwner(), msg, "Error", JOptionPane.ERROR_MESSAGE, null);
            return false;
        } finally {
            ResourceUtils.closeQuietly(fis);
        }

        return true;
    }

    private void initialize() {
        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(fileTextField, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                notifyListeners();
            }
        }, 300);

        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onFileButton();
            }
        });

        setLayout(new BorderLayout());
        add(mainPanel);
    }

    private void onFileButton() {
        FileChooserUtil.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                fc.setDialogTitle("Choose Solution Kit");
                fc.setDialogType(JFileChooser.OPEN_DIALOG);
                fc.setMultiSelectionEnabled(false);
                fc.setFileFilter(SK_FILE_FILTER);

                int result = fc.showOpenDialog(SolutionKitLoadPanel.this);
                if (JFileChooser.APPROVE_OPTION != result) {
                    return;
                }

                File file = fc.getSelectedFile();
                if (file == null) {
                    return;
                }

                fileTextField.setText(file.getAbsolutePath());
                notifyListeners();
            }
        });
    }
}