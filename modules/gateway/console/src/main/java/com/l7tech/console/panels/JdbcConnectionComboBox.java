package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link javax.swing.JComboBox} populated with {@link com.l7tech.gateway.common.jdbc.JdbcConnection JdbcConnections}.
 */
public class JdbcConnectionComboBox extends JComboBox<String> {
    private static Logger logger = Logger.getLogger(JdbcConnectionComboBox.class.getName());

    private final JdbcAdmin admin;
    private List<JdbcConnection> loadedJdbcConnections;

    /**
     * Creates a {@link javax.swing.JComboBox} populated with {@link com.l7tech.gateway.common.jdbc.JdbcConnection JdbcConnections}.
     */
    public JdbcConnectionComboBox() {
        super();
        admin = Registry.getDefault().getJdbcConnectionAdmin();
        reload();
    }

    /**
     * Reload the combo box.
     */
    public void reload() {
        if (admin == null) {
            logger.log(Level.WARNING, "Unable to populate JDBC Connection combo box: JdbcAdmin interface is null.");
            return;
        }

        Goid selected = getSelectedJdbcConnection();

        try {
            loadedJdbcConnections = admin.getAllJdbcConnections();
            List<String> connections = admin.getAllJdbcConnectionNames();
            Collections.sort(connections);
            setModel(new DefaultComboBoxModel<>(connections.toArray(new String[connections.size()])));
        } catch (FindException e) {
            loadedJdbcConnections.clear();
            setModel(new DefaultComboBoxModel<>(new String[0]));
            logger.log(Level.WARNING, "Unable to populate JDBC Connection combo box: ", ExceptionUtils.getDebugException(e));
        }

        if (selected != null) {
            setSelectedJdbcConnection(selected);
        }
    }

    /**
     * Select the specified JDBC Connection in the combo box.
     *
     * @param goid the GOID of the JDBC Connection to select
     */
    public void setSelectedJdbcConnection(@NotNull Goid goid) {
        String name = null;
        for (JdbcConnection aJdbcConnection : loadedJdbcConnections) {
            if (Goid.equals(aJdbcConnection.getGoid(), goid)) {
                name = aJdbcConnection.getName();
                break;
            }
        }

        int index = -1;
        if (name != null) {
            for (int ix = 0; ix < getItemCount(); ix++) {
                if (name.equals(getItemAt(ix))) {
                    index = ix;
                    break;
                }
            }
        }
        setSelectedIndex(index);
    }

    /**
     * Gets the selected JDBC Connection in the combo box.
     *
     * @return the GOID of the selected JDBC Connection. Null if a JDBC Connection is not selected.
     */
    public Goid getSelectedJdbcConnection() {
        Goid goid = null;
        String selected = (String) getSelectedItem();
        if (selected != null) {
            for (JdbcConnection aJdbcConnection : loadedJdbcConnections) {
                if (selected.equals(aJdbcConnection.getName())) {
                    goid = aJdbcConnection.getGoid();
                    break;
                }
            }
        }

        return goid;
    }
}