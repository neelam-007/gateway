package com.l7tech.gateway.common.custom;

import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;
import com.l7tech.policy.assertion.ext.ServiceInvocation;
import com.l7tech.policy.assertion.ext.action.CustomTaskActionUI;
import com.l7tech.policy.assertion.ext.cei.CustomExtensionInterfaceBinding;
import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;

import java.util.*;

/**
 * The class <code>CustomAssertionDescriptor</code> contains the runtime information that represent a custom assertion.
 * Required:
 * <ul>
 * <li> assertion name
 * <li> <code>CustomAssertion</code> class
 * <li> corresponding server side <code>ServiceInvocation</code> class
 * <li> assertion <code>Category</code>
 * </ul>
 * Optional:
 * <ul>
 * <li> UI class, must be implementation of <code>CustomAssertionUI</code>
 * <li> task action UI class, must be implementation of <code>CustomTaskActionUI</code>
 * <li> optional description
 * <li> flag indicating whether or not the UI should be opened automatically when the assertion is added to a policy
 * <li> white list allowed packages for use by the Policy Manager
 * <li> white list allowed resources for use by the Policy Manager
 * <li> display name for the node in the left palette
 * <li> display name for the node in the right policy editor
 * <li> display module file name in the Assertion View Info dialog
 * <li> {@code Class} object containing the extension Interface, must be implementation of {@link CustomExtensionInterfaceBinding}</li>
 * <li> {@code Collection} of {@code Class} objects of registered external entity serializers, must be implementation of {@link CustomEntitySerializer}</li>
 * </ul>
 */
public class CustomAssertionDescriptor {
    private final Class assertion;
    private final Class serverAssertion;
    private final String name;

    private final Set<Category> categories;
    private final String categoriesFriendlyPrintString;

    private Class uiClass;
    private Class taskActionUiClass;
    private String moduleFileName;
    private String description;
    private String paletteNodeName;
    private String policyNodeName;
    private boolean isUiAutoOpen;
    private String[] uiAllowedPackages = new String[0];
    private Set<String> uiAllowedResources = new HashSet<>();
    private Class<? extends CustomExtensionInterfaceBinding> extensionInterfaceClass;
    private Collection<Class<? extends CustomEntitySerializer>> extEntitySerializers = new ArrayList<>();

    /**
     * Create the new extensibility holder instance with the assertion and server assertion class.
     *
     * @param name                 the assertion name
     * @param assertionClass       the assertion class
     * @param serverAssertionClass the server side assertion class
     * @param categories           the category list in which the assertion is placed
     */
    public CustomAssertionDescriptor(final String name,
                                     final Class assertionClass,
                                     final Class serverAssertionClass,
                                     final Set<Category> categories) {
        this.name = name;
        this.assertion = assertionClass;

        this.categories = categories;
        this.categoriesFriendlyPrintString = CustomAssertionHolder.friendlyPrintCategories(this.categories);

        if (!CustomAssertion.class.isAssignableFrom(assertionClass)) {
            throw new IllegalArgumentException("Assertion " + assertionClass);
        }

        this.serverAssertion = serverAssertionClass;

        if (!ServiceInvocation.class.isAssignableFrom(serverAssertionClass)) {
            throw new IllegalArgumentException("Server assertion " + serverAssertionClass);
        }
    }

    /**
     * @return the custom assertion name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the display module file name in the Assertion View Info dialog or <b>null</b> if it has not been set
     */
    public String getModuleFileName() {
        return moduleFileName;
    }

    /**
     * @return the custom assertion description or <b>null</b> if it has not been set
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the display name for the node in the left palette or <b>null</b> if it has not been set
     */
    public String getPaletteNodeName() {
        return paletteNodeName;
    }

    /**
     * @return the display name for the node in the right policy editor or <b>null</b> if it has not been set
     */
    public String getPolicyNodeName() {
        return policyNodeName;
    }

    /**
     * @return the flag indicating whether or not the UI should be opened automatically when the assertion is added to a policy
     */
    public boolean getIsUiAutoOpen() {
        return isUiAutoOpen;
    }

    /**
     * @return the white list allowed packages for use by the SSM or or <b>empty array</b> if has not been set
     */
    public String[] getUiAllowedPackages() {
        return uiAllowedPackages;
    }

    /**
     * @return the white list allowed resources for use by the SSM or <b>empty set</b> if has not been set
     */
    public Set<String> getUiAllowedResources() {
        return uiAllowedResources;
    }

    /**
     * @return the assertion class
     */
    public Class getAssertion() {
        return assertion;
    }

    /**
     * @return the server assertion class
     */
    public Class getServerAssertion() {
        return serverAssertion;
    }

    /**
     * @return the categories set in which the assertion is placed.
     */
    public Set<Category> getCategories() {
        return categories;
    }

    /**
     * @return true if the assertion is placed into the specified <code>category</code>.
     */
    public boolean hasCategory(Category category) {
        return categories.contains(category);
    }

    /**
     * @return the UI class or <b>null</b> if it has not been set
     */
    public Class getUiClass() {
        return uiClass;
    }

    /**
     * @return the task action UI class or <b>null</b> if it has not been set
     */
    public Class getTakActionUiClass() {
        return taskActionUiClass;
    }

    /**
     * @return the extension interface {@code Class} object or <b>null</b> if it has not been set
     */
    public Class<? extends CustomExtensionInterfaceBinding> getExtensionInterfaceClass() {
        return extensionInterfaceClass;
    }

    /**
     * @return a read-only {@code Collection} of registered custom external entity serializers.
     */
    public Collection<Class<? extends CustomEntitySerializer>> getExternalEntitySerializers() {
        return Collections.unmodifiableCollection(extEntitySerializers);
    }

    public String toString() {
        return "[" + "; name='" + name + "'" + "; categories=" + categoriesFriendlyPrintString + "; assertion=" +
                safeName(assertion) + "; serverAssertion=" + safeName(serverAssertion) + "; editorClass=" +
                safeName(uiClass) + "]" + super.toString();
    }

    private String safeName(Class cl) {
        return cl != null ? cl.getName() : "null";
    }

    /**
     * @param uiClass UI class, must be implementation of <code>CustomAssertionUI</code>
     */
    public void setUiClass(Class uiClass) {
        this.uiClass = uiClass;
        if (uiClass != null && !CustomAssertionUI.class.isAssignableFrom(uiClass)) {
            throw new IllegalArgumentException("Editor assertion " + uiClass);
        }
    }

    /**
     * @param taskActionUiClass task action UI class, must be implementation of <code>CustomTaskActionUI</code>
     */
    public void setTaskActionUiClass(Class taskActionUiClass) {
        this.taskActionUiClass = taskActionUiClass;
        if (taskActionUiClass != null && !CustomTaskActionUI.class.isAssignableFrom(taskActionUiClass)) {
            throw new IllegalArgumentException("Task Action UI " + taskActionUiClass);
        }
    }

    /**
     * @param moduleFileName module file name to display
     */
    public void setModuleFileName(String moduleFileName) {
        this.moduleFileName = moduleFileName;
    }

    /**
     * @param description custom assertion description to display
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param paletteNodeName name for the node in the palette to display
     */
    public void setPaletteNodeName(String paletteNodeName) {
        this.paletteNodeName = paletteNodeName;
    }

    /**
     * @param policyNodeName name for the node in the policy editor to display
     */
    public void setPolicyNodeName(String policyNodeName) {
        this.policyNodeName = policyNodeName;
    }

    /**
     * @param uiAutoOpen flag indicating whether or not the UI should be opened automatically when the assertion is added to a policy
     */
    public void setUiAutoOpen(boolean uiAutoOpen) {
        isUiAutoOpen = uiAutoOpen;
    }

    /**
     * @param uiAllowedPackages white list allowed packages for use in the UI
     */
    public void setUiAllowedPackages(String uiAllowedPackages) {
        if (uiAllowedPackages != null) {
            this.uiAllowedPackages = uiAllowedPackages.split(",");
            for (int ix = 0; ix < this.uiAllowedPackages.length; ix++) {
                this.uiAllowedPackages[ix] = this.uiAllowedPackages[ix].replace('.', '/').trim();
            }
        }
    }

    /**
     * @param uiAllowedResources white list allowed resources for use in the UI
     */
    public void setUiAllowedResources(String uiAllowedResources) {
        if (uiAllowedResources != null) {
            String[] split = uiAllowedResources.split(",");
            for (String aSplit : split) {
                this.uiAllowedResources.add(aSplit.trim());
            }
        }
    }

    /**
     * @param extensionInterfaceClass extension interface {@code Class} object, must be implementation of <code>CustomExtensionInterfaceBinding</code>
     */
    public void setExtensionInterfaceClass(final Class<? extends CustomExtensionInterfaceBinding> extensionInterfaceClass) {
        this.extensionInterfaceClass = extensionInterfaceClass;
    }

    /**
     * @param extEntitySerializers    {@code Collection} holding external entity serializer {@code Class} objects,
     *                                which must be implementation of {@link CustomEntitySerializer}
     */
    public void setExternalEntitySerializers(final Collection<Class<? extends CustomEntitySerializer>> extEntitySerializers) {
        if (extEntitySerializers != null) {
            this.extEntitySerializers = extEntitySerializers;
        }
    }
}