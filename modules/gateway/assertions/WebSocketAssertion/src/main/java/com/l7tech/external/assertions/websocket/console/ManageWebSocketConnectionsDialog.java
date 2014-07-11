package com.l7tech.external.assertions.websocket.console;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.websocket.WebSocketConnectionEntity;
import com.l7tech.external.assertions.websocket.WebSocketConnectionEntityAdmin;
import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.external.assertions.websocket.WebSocketUtils;
import com.l7tech.gui.SimpleTableModel;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;
import static com.l7tech.util.Functions.propertyTransform;

public class ManageWebSocketConnectionsDialog extends JDialog {
    private static final Logger logger = Logger.getLogger(ManageWebSocketConnectionsDialog.class.getName());

    private JPanel contentPane;
    private JButton cloneButton;
    private JButton closeButton;
    private JButton removeButton;
    private JButton editButton;
    private JButton addButton;
    private JTable entityTable;
    private SimpleTableModel<WebSocketConnectionEntity> entityTableModel;

    public ManageWebSocketConnectionsDialog(Window owner) {
        super(owner, WebSocketConstants.MANAGE_CONNECTIONS_TITLE);
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(closeButton);


        //Table Initializer
        entityTableModel = TableUtil.configureTable(entityTable,
                column("Enabled", 25, 50, 200, propertyTransform(WebSocketConnectionEntity.class, "enabled")),
                column("Name", 25, 90, 200, propertyTransform(WebSocketConnectionEntity.class, "name")),
                column("Listen Port", 25, 60, 99999, propertyTransform(WebSocketConnectionEntity.class, "inboundListenPort")));
        Utilities.setRowSorter( entityTable, entityTableModel, new int[]{0,1,2}, new boolean[]{false,true,false}, new Comparator[]{null, String.CASE_INSENSITIVE_ORDER, null}  );
        entityTable.setModel(entityTableModel);
        entityTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                editButton.setEnabled(entityTable.getSelectedRow() > -1);
                cloneButton.setEnabled(entityTable.getSelectedRow() > -1);
                removeButton.setEnabled(entityTable.getSelectedRow() > -1);
                if (e.getClickCount() == 2 && !e.isConsumed()) {
                    e.consume();
                    showSelectedEntity(false, connectionSaver);
                }
            }
        });

        //Button Initializer
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showEntity(new WebSocketConnectionEntity(), false, connectionSaver);
            }
        });

        cloneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cloneSelectedEntity(connectionSaver);
            }
        });

        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSelectedEntity(false, connectionSaver);
            }
        });

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final WebSocketConnectionEntity selectedEntity = getSelectedEntity();
                if (selectedEntity == null) {
                    return;
                }

                DialogDisplayer.showConfirmDialog(removeButton, WebSocketConstants.DELETE_CONN_CHALLENGE + selectedEntity.getName() + "?", WebSocketConstants.DELETE_CONN_CONFIRM,
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, new DialogDisplayer.OptionListener() {
                    @Override
                    public void reportResult(int option) {
                        if (JOptionPane.OK_OPTION == option) {
                            try {
                                getEntityManager().delete(selectedEntity);
                            } catch (DeleteException e1) {
                                error(WebSocketConstants.DELETE_CONN_ERROR + ExceptionUtils.getMessage(e1));
                            } catch (FindException e1) {
                                error(WebSocketConstants.DELETE_CONN_ERROR + ExceptionUtils.getMessage(e1));
                            }
                            reloadTable();
                        }

                    }
                });
            }
        });


        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        reloadTable();


    }

    final Functions.UnaryVoid<WebSocketConnectionEntity> connectionSaver = new Functions.UnaryVoid<WebSocketConnectionEntity>() {
        @Override
        public void call(WebSocketConnectionEntity connection) {
            try {
                if (validateConnection(connection)) {
                    getEntityManager().save(connection);
                } else {
                    //The entity is not valid and can't be saved. Let the user know and go back to edit mode
                    error(WebSocketConstants.CONN_VALIDATION);
                    showEntity(connection, false, connectionSaver);
                }
            } catch (SaveException e1) {
                logger.log(Level.WARNING, WebSocketConstants.CONN_SAVE_ERROR);
                error(WebSocketConstants.CONN_SAVE_ERROR + ExceptionUtils.getMessage(e1));
            } catch (UpdateException e1) {
                logger.log(Level.WARNING, WebSocketConstants.CONN_SAVE_ERROR);
                error(WebSocketConstants.CONN_SAVE_ERROR + ExceptionUtils.getMessage(e1));
            }
            reloadTable();
        }
    };

    private void cloneSelectedEntity(final @Nullable Functions.UnaryVoid<WebSocketConnectionEntity> continuation) {
        WebSocketConnectionEntity entity = getSelectedEntity();
        if (entity == null)
            return;
        WebSocketConnectionEntity clonedEntity = WebSocketUtils.cloneEntity(entity);
        showEntity(clonedEntity, false, continuation);
    }

    private void showSelectedEntity(boolean readOnly, final @Nullable Functions.UnaryVoid<WebSocketConnectionEntity> continuation) {
        WebSocketConnectionEntity entity = getSelectedEntity();
        if (entity == null)
            return;
        showEntity(entity, readOnly, continuation);
    }

    private void showEntity(WebSocketConnectionEntity entity, boolean readOnly, final Functions.UnaryVoid<WebSocketConnectionEntity> continuation) {
        final WebSocketConnectionDialog dlg = new WebSocketConnectionDialog(this, entity);
        dlg.setReadOnly(readOnly);
        dlg.pack();
        Utilities.centerOnParent(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.isConfirmed()) {
                    if (continuation != null)
                        continuation.call(dlg.getConnection());
                }
            }
        });
    }

    private WebSocketConnectionEntity getSelectedEntity() {
        return entityTableModel.getRowObject(entityTable.getRowSorter().convertRowIndexToModel(entityTable.getSelectedRow()));
    }

    /**
     * Validates whether or not the connection  is unique. The Name and Listen Port must be unique.
     *
     * @param connection WebSocketConnectionEntity
     * @return boolean true if connection description is valid, false otherwise.
     */
    private boolean validateConnection(WebSocketConnectionEntity connection) {
        try {
            Collection<WebSocketConnectionEntity> mapCollection = getEntityManager().findAll();
            for (WebSocketConnectionEntity ws : mapCollection) {
                if (!Goid.equals(ws.getGoid(), connection.getGoid())) { //if not an update
                    if (ws.getName().equals(connection.getName())) { //can't have a matching name
                        return false;
                    }
                    if (ws.getInboundListenPort() == connection.getInboundListenPort()) {
                        return false;
                    }
                }
            }
        } catch (FindException e) {
            logger.log(Level.WARNING, WebSocketConstants.CONN_LOAD_ERROR);
            error(WebSocketConstants.CONN_LOAD_ERROR + ExceptionUtils.getMessage(e));
            return false;
        }

        return true;
    }

    private void reloadTable() {
        try {
            Collection<WebSocketConnectionEntity> rowCollection = getEntityManager().findAll();
            java.util.List<WebSocketConnectionEntity> rowList = new ArrayList<WebSocketConnectionEntity>(rowCollection);
            entityTableModel.setRows(rowList);
            if (rowList.size() == 0) {
                editButton.setEnabled(false);
                cloneButton.setEnabled(false);
                removeButton.setEnabled(false);
            } else { //There are still rows
                editButton.setEnabled(entityTable.getSelectedRow() > -1);
                cloneButton.setEnabled(entityTable.getSelectedRow() > -1);
                removeButton.setEnabled(entityTable.getSelectedRow() > -1);
            }

        } catch (FindException e) {
            logger.log(Level.WARNING, WebSocketConstants.CONN_LOAD_ERROR);
            error(WebSocketConstants.CONN_LOAD_ERROR + ExceptionUtils.getMessage(e));
        }
    }

    private void error(String s) {
        DialogDisplayer.showMessageDialog(this, s, null);
    }

    private static WebSocketConnectionEntityAdmin getEntityManager() {
        return Registry.getDefault().getExtensionInterface(WebSocketConnectionEntityAdmin.class, null);
    }

    private void onCancel() {
        dispose();
    }

}
