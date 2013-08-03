package com.l7tech.console.security.rbac;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.SecurityZoneUtil;
import com.l7tech.gui.CheckBoxSelectableTableModel;
import com.l7tech.gui.util.TableUtil;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.util.ArrayList;
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
    private JPanel contentPanel;
    private JTabbedPane tabPanel;
    private JPanel zonesPanel;
    private SelectableFilterableTablePanel tablePanel;
    private CheckBoxSelectableTableModel<SecurityZone> zonesModel;

    public PermissionScopeSelectionPanel() {
        super(null);
        setLayout(new BorderLayout());
        setShowDescriptionPanel(false);
        add(contentPanel);
        initTable();
    }

    @Override
    public String getStepLabel() {
        return OBJECT_SELECTION;
    }

    @Override
    public boolean canAdvance() {
        return !zonesModel.getSelected().isEmpty();
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
        } else {
            logger.log(Level.WARNING, "Cannot store settings because received invalid settings object: " + settings);
        }
    }


    private void initTable() {
        zonesModel = TableUtil.configureSelectableTable(tablePanel.getSelectableTable(), CHECK_COL_INDEX,
                column(StringUtils.EMPTY, 30, 30, 99999, new Functions.Unary<Boolean, SecurityZone>() {
                    @Override
                    public Boolean call(final SecurityZone zone) {
                        return zonesModel.isSelected(zone);
                    }
                }),
                column("Name", 30, 200, 99999, new Functions.Unary<String, SecurityZone>() {
                    @Override
                    public String call(final SecurityZone zone) {
                        return zone.getName();
                    }
                }),
                column("Description", 30, 400, 99999, new Functions.Unary<String, SecurityZone>() {
                    @Override
                    public String call(final SecurityZone zone) {
                        return zone.getDescription();
                    }
                }));

        zonesModel.setSelectableObjects(new ArrayList<>(SecurityZoneUtil.getSortedReadableSecurityZones()));
        zonesModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(final TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    notifyListeners();
                }
            }
        });
        tablePanel.configure(zonesModel, new int[]{NAME_COL_INDEX}, "zones");
    }
}
