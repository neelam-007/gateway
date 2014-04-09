package com.l7tech.policy.assertion.ext.entity;

/**
 * Implement this interface to provide optional Entity details.
 */
public interface CustomEntityDescriptor {
    /**
     * Property name: <i>{@code NAME}</i>, return type: <i>{@code String}</i><p/>
     *
     * A unique name for the entity.<br/>
     * For example the Salesforce connection name "My Development Server Connection".
     */
    static public final String NAME = "NameOrIdentifier";

    /**
     * Property name: <i>{@code TYPE}</i>, return type: <i>{@code String}</i><p/>
     *
     * A user friendly display type that describes the entity.<br/>
     * For example if the external entity holds a connection to Salesforce, then the type should be
     * "Salesforce.com Connection".
     */
    static public final String TYPE = "Type";

    /**
     * Property name: <i>{@code DESCRIPTION}</i>, return type: <i>{@code String}</i><p/>
     *
     * A more detail description of the entity to show the user in the Import Wizard.
     */
    static public final String DESCRIPTION = "Description";

    /**
     * Property name: <i>{@code SUMMARY}</i>, return type: <i>{@code String}</i><p/>
     *
     * A user friendly summary description for this entity. Typically an entity summary can be described as
     * {@link #NAME} [{@link #DESCRIPTION}].<br/>
     * This property is used for "Change assertions to use this TYPE:" drop-down field.
     */
    static public final String SUMMARY = "Summary";

    /**
     * Provide code for extracting each of the standard properties:
     * <ul>
     *     <li>{@link #NAME}</li>
     *     <li>{@link #TYPE}</li>
     *     <li>{@link #DESCRIPTION}</li>
     *     <li>{@link #SUMMARY}</li>
     * </ul>
     * In addition this method can provide extraction for future property-names and their proprietary return types.<p/>
     *
     * <p>For example, the following code fragment extracts the external entity name and type:
     * <blockquote><pre>
     *  {@code
     *  String entityName = entityDescriptor.getProperty(CustomEntityDescriptor.NAME, String.class);
     *  String entityType = entityDescriptor.getProperty(CustomEntityDescriptor.TYPE, String.class);
     *  }
     * </pre></blockquote>
     * </p>
     *
     * @param name      the property name to be extracted.  Could be one of the predefined properties: {@link #NAME},
     *                  {@link #TYPE}, {@link #DESCRIPTION}, {@link #SUMMARY}, or a property name defined in the future.
     * @param rClass    the {@code Class} object corresponding to the property return type.
     * @param <R>       the return type of the class modeled by {@code rClass}.
     *
     * @return the property value, specified with the return type, or {@code null} if the requested property and type is not supported.
     */
    <R> R getProperty(String name, Class<R> rClass);

    /**
     * Property name: <i>{@code MISSING_DETAIL_UI_OBJECT}</i>, return type: requested UI class object,
     * typically <i>{@code JPanel}</i>, but may change in the future.
     * <p/>
     * Represents the user interface object to display missing entity details. Currently we only support {@code JPanel}.<br/>
     * If the UI object is not provided (i.e. {@code null} is returned), then the missing entity will be displayed with
     * the default details (i.e. Key Id and Key Prefix fields).
     */
    static public final String MISSING_DETAIL_UI_OBJECT = "MissingDetailUiObject";

    /**
     * Property name: <i>{@code CREATE_UI_OBJECT}</i>, return type: requested UI class object also implementing
     * typically <i>{@code JPanel}</i>, but may change in the future.
     * <p/>
     * Represents the user interface object responsible to create the missing entity. Currently we only support {@code JPanel}.<br/>
     * The User interface object must also implement {@link CustomEntityCreateUiObject} interface, providing code
     * for modifying the missing entity.<br/>
     * If the UI object is not provided (i.e. {@code null} is returned), then the default behavior is to create the
     * entity without user modification.<br/>
     * If the UI object returned is set to an unsupported interface object (e.g. not a {@link CustomEntityCreateUiObject}),
     * then {@code RuntimeException} will be thrown.
     */
    static public final String CREATE_UI_OBJECT = "CreateUiObject";

    /**
     * Provide the user interfaces to display in the Import Wizard by providing code for each properties:
     * <ul>
     *     <li>{@link #MISSING_DETAIL_UI_OBJECT}</li>
     *     <li>{@link #CREATE_UI_OBJECT}</li>
     * </ul>
     * In addition this method can provide extraction for future ui-object-names and their proprietary return types.<p/>
     *
     * <p>For example, the following code fragment extracts the external entity details UI object:
     * <blockquote><pre>
     *  {@code 
     *  JPanel detailsObject = entityDescriptor.getProperty(CustomEntityDescriptor.MISSING_DETAIL_UI_OBJECT, JPanel.class);
     *  }
     * </pre></blockquote>
     * Another example, the following code fragment gathers external entity create object:
     * <blockquote><pre>
     *  {@code 
     *  JPanel createPanel = entityDescriptor.getProperty(CustomEntityDescriptor.CREATE_UI_OBJECT, JPanel.class);
     *  if (!(createPanel instanceof CustomEntityCreateUiObject)) {
     *      // throw exception here
     *  }
     *  }
     * </pre></blockquote>
     * </p>
     *
     * @param uiName     the UI property name to be extracted. Could be one of the predefined properties:
     *                   {@link #MISSING_DETAIL_UI_OBJECT}, {@link #CREATE_UI_OBJECT}, or a property name defined in the future.
     * @param uiClass    the {@code Class} object corresponding to the UI object return type.
     * @param <R>        the return type of the class modeled by {@code rClass}.
     *
     * @return the UI object, specified with the return type, or {@code null} if the requested property and type is not supported.
     */
    <R> R getUiObject(String uiName, Class<R> uiClass);
}
