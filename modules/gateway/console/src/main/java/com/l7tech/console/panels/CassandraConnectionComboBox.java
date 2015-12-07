package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ComboBox that can be used to choose an available CassandraConnection.
 */
public class CassandraConnectionComboBox extends JComboBox<CassandraConnection> {
    private static Logger logger = Logger.getLogger(CassandraConnectionComboBox.class.getName());
    private List<CassandraConnection> cassandraConnections;

    public CassandraConnectionComboBox() {
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    setText(((CassandraConnection) value).getName());
                }
                return this;
            }
        });
        reload();
    }

    public void reload() {
        try {
            final CassandraConnection selectedCassandraConnection = getSelectedCassandraConnection();

            cassandraConnections = new ArrayList<>(Registry.getDefault().getCassandraConnectionAdmin().getAllCassandraConnections());
            Collections.sort(cassandraConnections, new ResolvingComparator<>(new Resolver<CassandraConnection, String>() {
                @Override
                public String resolve(final CassandraConnection key) {
                    return key.getName().toLowerCase();
                }
            }, false));
            setModel(new DefaultComboBoxModel<>(cassandraConnections.toArray(new CassandraConnection[cassandraConnections.size()])));

            if (selectedCassandraConnection != null && containsItem(selectedCassandraConnection.getGoid())) {
                // Select currently selected item.
                setSelectedCassandraConnection(selectedCassandraConnection.getGoid());
            }
        } catch (FindException e) {
            final String msg = "Unable to list cassandra connections: " + ExceptionUtils.getMessage(e);
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException(e));
            DialogDisplayer.showMessageDialog(this, "Unable to list cassandra connections", msg, e);
        }
    }

    public CassandraConnection getSelectedCassandraConnection() {
        return (CassandraConnection) getSelectedItem();
    }

    /**
     * @param goid the goid of the CassandraConnection to select in the dropdown or
     *             #CassandraConnection.DEFAULT_GOID if none should be selected.
     *             If the goid does not correspond to an available CassandraConnection, the selected item will
     *             be shown as 'cassandra connections details are unavailable'.
     */
    public void setSelectedCassandraConnection(Goid goid) {
        Integer selectedIndex = null;
        for (int i = 0; i < cassandraConnections.size(); i++) {
            CassandraConnection CassandraConnection = cassandraConnections.get(i);
            if (Goid.equals(goid, CassandraConnection.getGoid())) {
                selectedIndex = i;
                break;
            }
        }
        if (selectedIndex != null) {
            setSelectedIndex(selectedIndex);
        } else if (!Goid.isDefault(goid)) {
            // oid not found in available cassandra connections - could be not readable by current user
            logger.log(Level.WARNING, "Cassandra Connection goid not available to current user");
            final CassandraConnection unavailableCassandraConnection = new CassandraConnection();
            unavailableCassandraConnection.setGoid(goid);
            unavailableCassandraConnection.setName("Current cassandra connections (cassandra connections details are unavailable)");
            cassandraConnections.add(0, unavailableCassandraConnection);
            setModel(new DefaultComboBoxModel<>(cassandraConnections.toArray(new CassandraConnection[cassandraConnections.size()])));
            setSelectedIndex(0);
        } else {
            // encapsulated assertion does not yet exist in the database
            setSelectedItem(null);
        }
    }

    public boolean containsItem(Goid goid) {
        for (CassandraConnection CassandraConnection : cassandraConnections) {
            if (Goid.equals(CassandraConnection.getGoid(), goid)) {
                return true;
            }
        }

        return false;
    }
}
