/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.common.io.ClassLoaderObjectInputStream;
import com.l7tech.util.ResourceUtils;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The custom assertion holder is a placeholder <code>Assertion</code>
 * that wraps a bean instance that implements the <code>CustomAssertion</code>
 * The <code>customAssertionBean</code> property is required to
 * be serializable and must offer JavaBean style get/set operations.
 *
 * @author <a href="mailto:emarceta@layer7tech.com">Emil Marceta</a>
 * @version 1.0
 * @see CustomAssertion
 */
public class CustomAssertionHolder extends Assertion implements UsesVariables, SetsVariables {
    /**
     * Serialization id, maintain to indicate serialization compatibility
     * with a previous versions of the  class.
     */
    private static final long serialVersionUID = 7410439507802944818L;

    private static final Logger logger = Logger.getLogger(CustomAssertionHolder.class.getName());

    public CustomAssertionHolder() {
        this.parent = null;
    }

    /**
     * @return the custom assertion bean
     */
    public CustomAssertion getCustomAssertion() {
        return customAssertion;
    }

    /**
     * @return the custom assertion category
     */
    public Category getCategory() {
        return category;
    }

    /**
     * Set the custom assertion category
     *
     * @param category the new category
     */
    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * Set the custome assertion bean
     *
     * @param ca the new custome assertino bean
     */
    public void setCustomAssertion(CustomAssertion ca) {
        this.customAssertion = ca;
    }

    @Override
    public String toString() {
        if (customAssertion == null) {
            return "[ CustomAssertion = null ]";
        }
        return "[ CustomAssertion = " + customAssertion.toString() + ", Category = " + category + " ]";
    }

    public String getDescriptionText() {
        return descriptionText;
    }

    public void setDescriptionText(String descriptionText) {
        this.descriptionText = descriptionText;
    }

    private CustomAssertion customAssertion;
    private Category category;
    private String descriptionText;

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        if (customAssertion == null || !(customAssertion instanceof UsesVariables) ) {
            return new String[0];
        }
        UsesVariables usesVariables = (UsesVariables) customAssertion;
        return usesVariables.getVariablesUsed();
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if (customAssertion == null || !(customAssertion instanceof SetsVariables) ) {
            return new VariableMetadata[0];
        }
        SetsVariables setsVariables = (SetsVariables) customAssertion;
        return setsVariables.getVariablesSet();
    }

    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException"})
    @Override
    public Object clone() {
        CustomAssertionHolder clone = (CustomAssertionHolder) super.clone();
        clone.category = this.category;
        clone.descriptionText = this.descriptionText;
        clone.customAssertion = this.customAssertion; // in case deep copy fails

        if ( this.customAssertion != null ) {
            // Attempt serialization round trip to avoid returning the same instance
            // every time ... note that custom assertion data objects are NOT cloneable
            ObjectInputStream in = null;
            ObjectOutputStream out = null;
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                out = new ObjectOutputStream(baos);
                out.writeObject(this.customAssertion);
                out.flush();
                in = new ClassLoaderObjectInputStream(
                        new ByteArrayInputStream(baos.toByteArray()),
                        this.customAssertion.getClass().getClassLoader());
                clone.customAssertion  = (CustomAssertion) in.readObject();
            } catch (IOException e) {
                logger.log( Level.FINE, "Error serializing assertion.", e);
            } catch (ClassNotFoundException e) {
                logger.log( Level.FINE, "Error serializing assertion.", e);
            } catch (SecurityException se) {
                logger.log( Level.FINE, "Permission denied when serializing assertion.");
            } catch (RuntimeException re) {
                logger.log( Level.FINE, "Unexpected error when serializing assertion.", re);
            } finally {
                ResourceUtils.closeQuietly(in);
                ResourceUtils.closeQuietly(out);
            }
        }

        return clone;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(SHORT_NAME, "Custom Assertion");
        meta.put(DESCRIPTION, "Custom Assertion");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/custom.gif");
        return meta;
    }

}

