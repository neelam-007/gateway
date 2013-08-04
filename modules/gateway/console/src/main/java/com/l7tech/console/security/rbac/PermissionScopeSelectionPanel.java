package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gui.CheckBoxSelectableTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gui.util.TableUtil.column;

/**
 * Step panel which allows the user to configure the scope of role permissions.
 */
public class PermissionScopeSelectionPanel extends WizardStepPanel {
    private static final Logger logger = Logger.getLogger(PermissionScopeSelectionPanel.class.getName());
    private static final int NAME_COL_INDEX = 1;
    private static final String OBJECT_SELECTION = "Object selection";
    private static final int CHECK_COL_INDEX = 0;
    private static final String NO_SECURITY_ZONE = "(no security zone)";
    private static final String UNAVAILABLE = "unavailable";
    private static final String NAME = "Name";
    private static final String PATH = "Path";
    private static final String ZONE = "Zone";
    private static final String DESCRIPTION = "Description";
    private static final int MAX_WIDTH = 99999;
    private static final int CHECK_BOX_WIDTH = 30;
    private JPanel contentPanel;
    private JTabbedPane tabPanel;
    private JPanel zonesPanel;
    private SelectableFilterableTablePanel zonesTablePanel;
    private JPanel folderPanel;
    private JCheckBox transitiveCheckBox;
    private JCheckBox ancestryCheckBox;
    private SelectableFilterableTablePanel foldersTablePanel;
    private CheckBoxSelectableTableModel<SecurityZone> zonesModel;
    private CheckBoxSelectableTableModel<FolderHeader> foldersModel;

    public PermissionScopeSelectionPanel() {
        super(null);
        setLayout(new BorderLayout());
        setShowDescriptionPanel(false);
        add(contentPanel);
        initTables();
    }

    @Override
    public String getStepLabel() {
        return OBJECT_SELECTION;
    }

    @Override
    public boolean canAdvance() {
        return !zonesModel.getSelected().isEmpty() || !foldersModel.getSelected().isEmpty();
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public boolean canSkip(final Object settings) {
        boolean canSkip = false;
        if (settings instanceof AddPermissionsWizard.PermissionConfig) {
            final AddPermissionsWizard.PermissionConfig config = (AddPermissionsWizard.PermissionConfig) settings;
            canSkip = !config.isHasScope();
        } else {
            logger.log(Level.WARNING, "Cannot handle settings because received invalid settings object: " + settings);
        }
        return canSkip;
    }

    @Override
    public void readSettings(final Object settings) throws IllegalArgumentException {
        setSkipped(canSkip(settings));
    }

    @Override
    public void storeSettings(final Object settings) throws IllegalArgumentException {
        if (settings instanceof AddPermissionsWizard.PermissionConfig) {
            final AddPermissionsWizard.PermissionConfig config = (AddPermissionsWizard.PermissionConfig) settings;
            config.setSelectedZones(new HashSet<>(zonesModel.getSelected()));
            config.setSelectedFolders(new HashSet<>(foldersModel.getSelected()));
            config.setFolderTransitive(transitiveCheckBox.isSelected());
            config.setFolderAncestry(ancestryCheckBox.isSelected());
        } else {
            logger.log(Level.WARNING, "Cannot store settings because received invalid settings object: " + settings);
        }
    }

    private void initTables() {
        final TableListener tableListener = new TableListener();
        zonesModel = TableUtil.configureSelectableTable(zonesTablePanel.getSelectableTable(), CHECK_COL_INDEX,
                column(StringUtils.EMPTY, CHECK_BOX_WIDTH, CHECK_BOX_WIDTH, MAX_WIDTH, new Functions.Unary<Boolean, SecurityZone>() {
                    @Override
                    public Boolean call(final SecurityZone zone) {
                        return zonesModel.isSelected(zone);
                    }
                }),
                column(NAME, 30, 200, MAX_WIDTH, new Functions.Unary<String, SecurityZone>() {
                    @Override
                    public String call(final SecurityZone zone) {
                        return zone.getName();
                    }
                }),
                column(DESCRIPTION, 30, 400, MAX_WIDTH, new Functions.Unary<String, SecurityZone>() {
                    @Override
                    public String call(final SecurityZone zone) {
                        return zone.getDescription();
                    }
                }));
        zonesModel.setSelectableObjects(new ArrayList<>(SecurityZoneUtil.getSortedReadableSecurityZones()));
        zonesModel.addTableModelListener(tableListener);
        zonesTablePanel.configure(zonesModel, new int[]{NAME_COL_INDEX}, "zones");

        foldersModel = TableUtil.configureSelectableTable(foldersTablePanel.getSelectableTable(), CHECK_COL_INDEX,
                column(StringUtils.EMPTY, CHECK_BOX_WIDTH, CHECK_BOX_WIDTH, MAX_WIDTH, new Functions.Unary<Boolean, FolderHeader>() {
                    @Override
                    public Boolean call(final FolderHeader folder) {
                        return foldersModel.isSelected(folder);
                    }
                }),
                column(NAME, 30, 200, MAX_WIDTH, new Functions.Unary<String, FolderHeader>() {
                    @Override
                    public String call(final FolderHeader folder) {
                        String name = UNAVAILABLE;
                        try {
                            name = Registry.getDefault().getEntityNameResolver().getNameForHeader(folder, false);
                        } catch (final FindException e) {
                            logger.log(Level.WARNING, "Unable to resolve name for folder: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        }
                        return name;
                    }
                }),
                column(PATH, 30, 400, MAX_WIDTH, new Functions.Unary<String, FolderHeader>() {
                    @Override
                    public String call(final FolderHeader folder) {
                        String path = UNAVAILABLE;
                        try {
                            path = Registry.getDefault().getEntityNameResolver().getPath(folder);
                        } catch (final FindException e) {
                            logger.log(Level.WARNING, "Unable to resolve path for folder header: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                        }
                        return path;
                    }
                }),
                column(ZONE, 30, 200, MAX_WIDTH, new Functions.Unary<String, FolderHeader>() {
                    @Override
                    public String call(final FolderHeader folder) {
                        SecurityZone zone = null;
                        if (folder.getSecurityZoneGoid() != null) {
                            zone = SecurityZoneUtil.getSecurityZoneByGoid(folder.getSecurityZoneGoid());
                        }
                        return zone == null ? NO_SECURITY_ZONE : zone.getName();
                    }
                }));
        foldersModel.addTableModelListener(tableListener);
        try {
            final Collection<FolderHeader> folders = Registry.getDefault().getFolderAdmin().findAllFolders();
            foldersModel.setSelectableObjects(new ArrayList<>(folders));
        } catch (final FindException e) {
            foldersModel.setSelectableObjects(Collections.<FolderHeader>emptyList());
        }
        foldersTablePanel.configure(foldersModel, new int[]{NAME_COL_INDEX}, "folders");
    }

    private class TableListener implements TableModelListener {
        @Override
        public void tableChanged(final TableModelEvent e) {
            if (e.getType() == TableModelEvent.UPDATE) {
                notifyListeners();
            }
        }
    }
}
