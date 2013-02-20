package com.l7tech.console.security.rbac;

import com.l7tech.gateway.common.security.rbac.AttributePredicate;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panel for an attribute predicate.
 */
public class ScopeAttributePanel extends ValidatedPanel<AttributePredicate> {
    private static final Logger logger = Logger.getLogger(ScopeAttributePanel.class.getName());

    private JPanel contentPane;
    private JTextField attrValue;
    private JComboBox attrNamesList;
    private JComboBox attrComparisonType;
    private JLabel label;

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RbacGui");

    private final EntityType entityType;
    private final Permission permission;
    private AttributePredicate model;

    public ScopeAttributePanel(@NotNull AttributePredicate model, @NotNull EntityType entityType) {
        super("attributePredicate");
        this.model = model;
        this.permission = model.getPermission();
        this.permission.setEntityType(entityType);
        this.entityType = entityType;
        attrComparisonType.setModel(new DefaultComboBoxModel(CompType.values()));
        init();
    }

    @Override
    protected AttributePredicate getModel() {
        doUpdateModel();
        return model;
    }

    @Override
    protected void initComponents() {
        label.setText(MessageFormat.format(label.getText(), entityType.getPluralName()));

        setupAttributeNames();

        attrNamesList.setSelectedItem(model.getAttribute());
        attrNamesList.addActionListener(syntaxListener());
        attrValue.setText(model.getValue());
        attrValue.getDocument().addDocumentListener(syntaxListener());
        attrComparisonType.setSelectedItem(CompType.findByValue(model.getMode()));
        attrComparisonType.addActionListener(syntaxListener());

        setLayout(new BorderLayout());
        add(contentPane, BorderLayout.CENTER);
    }

    @Override
    public void focusFirstComponent() {
        attrNamesList.requestFocusInWindow();
    }

    @Override
    protected void doUpdateModel() {
        model = new AttributePredicate(permission, (String) attrNamesList.getSelectedItem(), attrValue.getText());
        model.setMode(((CompType)attrComparisonType.getSelectedItem()).value);
    }


    private void setupAttributeNames() {
        attrNamesList.setModel(new DefaultComboBoxModel(findAttributeNames(entityType).toArray(new String[0])));
    }

    private Collection<String> findAttributeNames(final EntityType entityType) {
        Collection<String> names = new ArrayList<String>();
        Class eClazz = entityType.getEntityClass();
        if (eClazz == null)
            return names;
        try {
            BeanInfo info = Introspector.getBeanInfo(eClazz);
            PropertyDescriptor[] props = info.getPropertyDescriptors();
            for (PropertyDescriptor propertyDescriptor : props) {
                Method getter = propertyDescriptor.getReadMethod();
                if (getter != null) {
                    Class rtype = getter.getReturnType();
                    if (Number.class.isAssignableFrom(rtype) || rtype == Long.TYPE || rtype == Integer.TYPE || rtype == Byte.TYPE || rtype == Short.TYPE ||
                            CharSequence.class.isAssignableFrom(rtype) ||
                            rtype == Boolean.TYPE || Boolean.class.isAssignableFrom(rtype) ||
                            Enum.class.isAssignableFrom(rtype))
                    {
                        //there is a getter for this property, so use it in the list
                        names.add(propertyDescriptor.getName());
                    }
                }
            }

            // Allow attempts to use Name for ANY entity, since in practice most will be NamedEntity subclasses
            if (EntityType.ANY.equals(entityType)) {
                names.add("name");
            }

        } catch (IntrospectionException e) {
            String msg = "Unable to introspect " + entityType;
            logger.log(Level.WARNING, msg, e);
            JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE, null);
        }
        return names;
    }

    private static enum CompType {
        EQ(resources.getString("scopeDialog.comparisonMode.eq.text"), "eq"),
        SW(resources.getString("scopeDialog.comparisonMode.sw.text"), "sw"),
        ;

        final String label;
        final String value;

        private CompType(String label, String value) {
            this.label = label;
            this.value = value;
        }

        @Override
        public String toString() {
            return label;
        }

        public static CompType findByValue(String value) {
            if (value == null || value.equals("eq")) {
                return EQ;
            } else if (value.equals("sw")) {
                return SW;
            } else {
                throw new IllegalArgumentException("Unrecognized comparison mode value: " + value);
            }
        }
    }
}
