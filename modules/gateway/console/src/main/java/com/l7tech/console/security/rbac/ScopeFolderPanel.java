package com.l7tech.console.security.rbac;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.rbac.FolderPredicate;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.folder.Folder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;

/**
 * Dialog for creating or editing a FolderPredicate.
 */
public class ScopeFolderPanel extends ValidatedPanel<FolderPredicate> {
    private JPanel contentPane;
    private JTextField folderText;
    private JButton folderFindButton;
    private JCheckBox transitiveCheckBox;
    private JLabel label;

    private final EntityType entityType;
    private final Permission permission;
    private FolderPredicate model;
    private Folder folder;

    public ScopeFolderPanel(@NotNull FolderPredicate model, @NotNull EntityType entityType) {
        super("folderPredicate");
        this.model = model;
        this.permission = model.getPermission();
        this.folder = model.getFolder();
        this.entityType = entityType;
        init();
    }

    @Override
    protected FolderPredicate getModel() {
        doUpdateModel();
        return model;
    }

    @Override
    protected void initComponents() {
        label.setText(MessageFormat.format(label.getText(), entityType.getPluralName()));

        folderText.setText(model.getFolder() == null ? "" : model.getFolder().getName());
        transitiveCheckBox.setSelected(model.isTransitive());

        transitiveCheckBox.addActionListener(syntaxListener());
        folderFindButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final FindEntityDialog fed = new FindEntityDialog((JDialog)Utilities.getRootPaneContainerAncestor(ScopeFolderPanel.this), EntityType.FOLDER, null);
                fed.pack();
                Utilities.centerOnScreen(fed);
                DialogDisplayer.display(fed, new Runnable() {
                    public void run() {
                        EntityHeader header = fed.getSelectedEntityHeader();
                        if (header != null) {
                            try {
                                folder = Registry.getDefault().getFolderAdmin().findByPrimaryKey(header.getGoid());
                            } catch (FindException e1) {
                                throw new RuntimeException("Couldn't lookup Folder", e1);
                            }
                            folderText.setText(header.getName());
                            checkSyntax();
                        }
                    }
                });
            }
        });

        setLayout(new BorderLayout());
        add(contentPane, BorderLayout.CENTER);
    }

    @Override
    public void focusFirstComponent() {
        folderFindButton.requestFocusInWindow();
    }

    @Override
    protected void doUpdateModel() {
        model = new FolderPredicate(permission, folder, transitiveCheckBox.isSelected());
    }

    @Override
    protected String getSyntaxError(final FolderPredicate model) {
        String error = null;

        if (folder == null) {
            error = "A folder must be selected.";
        }

        return error;
    }
}
