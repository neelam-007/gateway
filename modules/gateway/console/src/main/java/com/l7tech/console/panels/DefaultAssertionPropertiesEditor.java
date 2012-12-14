package com.l7tech.console.panels;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.InvalidContextVariableException;
import com.l7tech.policy.wsp.TypeMappingUtils;
import com.l7tech.util.ExceptionUtils;

import java.awt.*;
import java.beans.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Generic Assertion bean property editor that provides poor-quality dialog for any assertion bean that has
 * at least one WSP-visible property, using introspection to base properties on the bean's WSP-visible bean
 * properties (getter and setter methods).
 */
public class DefaultAssertionPropertiesEditor<AT extends Assertion> extends EditRowBasedAssertionPropertiesEditor<AT> {

    /**
     * Create a DefaultAssertionPropertiesEditor dialog owned by the specified Frame and prepared to edit the
     * specified assertion bean.
     *
     * @param parent owner frame from which the dialog is displayed.
     * @param assertion the assertion bean to edit. Required.
     */
    public DefaultAssertionPropertiesEditor(Window parent, AT assertion) {
        super(parent, assertion);
        initComponents();
        setData(assertion);
    }

    /**
     * Check if the specified assertion class has at least one property that is readable, writable,
     * and not WSP ignorable.
     *
     * @param c the assertion class to examine
     * @return true if the specified class has at least one public property that is readable and writable
     *         and is not ignorable.
     */
    public static boolean hasEditableProperties(Class<? extends Assertion> c) {
        try {
            return !getWspProperties(c).isEmpty();
        } catch (IntrospectionException e) {
            logger.log(Level.WARNING, "Unable to introspect assertion class: " + ExceptionUtils.getMessage(e), e);
            return false;
        }
    }

    /**
     * Find all Wsp-visible properties for the specified assertion class.
     * <p/>
     * This method uses Introspector to examine the bean class, and returns all property descriptors that
     * are readable, writable, and not marked as ignorable by {@link TypeMappingUtils#isIgnorableProperty(String)}.
     *
     * @param assertionClass the assertion bean class. Required.
     * @return a set of property descriptors.  May be empty, but never null.
     * @throws IntrospectionException if the assertion bean class cannot be introspected.
     */
    public static Set<PropertyDescriptor> getWspProperties(Class<? extends Assertion> assertionClass) throws IntrospectionException {
        Set<PropertyDescriptor> ret = new HashSet<PropertyDescriptor>();
        BeanInfo info = Introspector.getBeanInfo(assertionClass);
        PropertyDescriptor[] props = info.getPropertyDescriptors();
        for (PropertyDescriptor prop : props) {
            String name = prop.getName();
            if (TypeMappingUtils.isIgnorableProperty(name))
                continue;
            Method reader = prop.getReadMethod();
            Method writer = prop.getWriteMethod();
            if (reader != null && writer != null && reader.getDeclaringClass() == assertionClass && writer.getDeclaringClass() == assertionClass)
                ret.add(prop);
        }
        return ret;
    }

    @Override
    protected Set<PropertyInfo> getPropertyInfos() {
        try {
            Set<PropertyDescriptor> propertyDescriptors = DefaultAssertionPropertiesEditor.getWspProperties(getBeanClass());
            Set<PropertyInfo> ret = new HashSet<PropertyInfo>();
            for (PropertyDescriptor descriptor : propertyDescriptors) {
                ret.add(new PropertyDescriptorPropertyInfo(descriptor));
            }
            return ret;
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String getDisplayName(PropertyInfo prop) {
        if (prop instanceof PropertyDescriptorPropertyInfo) {
            PropertyDescriptorPropertyInfo info = (PropertyDescriptorPropertyInfo) prop;
            return info.getDisplayName();
        }
        return super.getDisplayName(prop);
    }

    protected static class PropertyDescriptorPropertyInfo implements PropertyInfo {
        final PropertyDescriptor pd;

        public PropertyDescriptorPropertyInfo(PropertyDescriptor descriptor) {
            this.pd = descriptor;
        }

        String getDisplayName() {
            return pd.getDisplayName();
        }

        @Override
        public String getName() {
            return pd.getName();
        }

        @Override
        public PropertyEditor createPropertyEditor() {
            try {
                return getPropertyEditor(pd);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object readValueFromBean(Object assertion) {
            try {
                return pd.getReadMethod().invoke(assertion);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void writeValueToBean(Object assertion, Object value) {
            try {
                pd.getWriteMethod().invoke(assertion, value);
            } catch (IllegalAccessException e) {
                logger.log(Level.WARNING, ExceptionUtils.getMessage(e), e);
            } catch (InvocationTargetException e) {
                if (ExceptionUtils.causedBy(e, InvalidContextVariableException.class)) {
                    logger.log(Level.WARNING, ExceptionUtils.getMessage(e));
                    throw new BadViewValueException(ExceptionUtils.getMessage(e), e, pd.getName());
                } else {
                    logger.log(Level.WARNING, ExceptionUtils.getMessage(e), e);
                }
            }
        }

        /**
         * Find a PropertyEditor instance for the specified property.
         * <p/>
         * This method tries the following, in the following order:
         * <ol>
         * <li>Calls {@link java.beans.PropertyDescriptor#getPropertyEditorClass} on the property.
         * <li>Calls {@link java.beans.PropertyEditorManager#findEditor(Class)} to check for a globally-registered editor.
         * <li>If the field value is an Enum, creates an {@link com.l7tech.console.policy.EnumPropertyEditor}.
         * <li>Otherwise, this method gives up and returns null.
         * </ol>
         * <p/>
         *
         * @param prop property to examine. Require.
         * @return a PropertyEditor that can be used to edit this property value, or null.
         */
        protected PropertyEditor getPropertyEditor(PropertyDescriptor prop) throws IllegalAccessException, InstantiationException {
            PropertyEditor editor = null;

            Class<?> propEditClass = prop.getPropertyEditorClass();
            if (propEditClass != null)
                editor = (PropertyEditor)propEditClass.newInstance();

            if (editor == null)
                editor = findPropertyEditorForType(prop.getPropertyType());

            return editor;
        }
    }
}
