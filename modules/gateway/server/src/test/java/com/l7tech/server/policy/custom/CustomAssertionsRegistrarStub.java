package com.l7tech.server.policy.custom;

import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.module.ModuleLoadingException;
import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;
import com.l7tech.policy.assertion.ext.CustomCredentialSource;
import com.l7tech.policy.assertion.ext.action.CustomTaskActionUI;
import com.l7tech.policy.assertion.ext.entity.CustomEntitySerializer;
import com.l7tech.policy.assertion.ext.licensing.CustomFeatureSetName;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author emil
 * @version 16-Feb-2004
 */
public class CustomAssertionsRegistrarStub implements CustomAssertionsRegistrar {
    static Logger logger = Logger.getLogger( CustomAssertionsRegistrar.class.getName());

    static {
        loadTestCustomAssertions();
    }

    private static void loadTestCustomAssertions() {
        //noinspection serial
        CustomAssertionDescriptor eh = new CustomAssertionDescriptor("Test.Assertion",
                TestAssertionProperties.class,
                TestServiceInvocation.class,
                new HashSet<Category>() {{
                    add(Category.ACCESS_CONTROL);
                }}
        );
        CustomAssertions.register(eh);
    }

    @Override
    public byte[] getAssertionClass(String className) {
        return null;
    }

    @Override
    public byte[] getAssertionResourceBytes(String path) {
        return null;
    }

    @Override
    public AssertionResourceData getAssertionResourceData( String name ) {
        return null;
    }

    /**
     * @return the list of all assertions known to the runtime
     */
    @Override
    public Collection<CustomAssertionHolder> getAssertions() {
        Set customAssertionDescriptors = CustomAssertions.getAllDescriptors();
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     */
    @Override
    public Collection<CustomAssertionHolder> getAssertions(Category c) {
        final Set customAssertionDescriptors = CustomAssertions.getDescriptors(c);
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    /**
     * Checks if there is a CustomAssertion registered which either, implements the
     * {@link com.l7tech.policy.assertion.ext.CustomCredentialSource CustomCredentialSource} interface and returns <code>true</code> for
     * {@link com.l7tech.policy.assertion.ext.CustomCredentialSource#isCredentialSource() CustomCredentialSource.isCredentialSource()} method,
     * or is placed into {@link Category#ACCESS_CONTROL ACCESS_CONTROL} category.
     *
     * @return true if there is a CustomAssertion registered which is credential source, false otherwise.
     */
    @Override
    public boolean hasCustomCredentialSource() {
        try
        {
            //noinspection unchecked
            Set<CustomAssertionDescriptor> descriptors = CustomAssertions.getDescriptors();
            for (CustomAssertionDescriptor descriptor : descriptors) {
                if (descriptor.hasCategory(Category.ACCESS_CONTROL)) {
                    return true;
                } else if (CustomCredentialSource.class.isAssignableFrom(descriptor.getAssertion())) {
                    final CustomCredentialSource customAssertion = (CustomCredentialSource)descriptor.getAssertion().newInstance();
                    if (customAssertion.isCredentialSource()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while determining if there is a CustomAssertion registered which is credential source.", e);
        }

        return false;
    }

    /**
     * Get the assertion known to the runtime for a given custom assertion class name
     * @param customAssertionClassName the custom assertion class name
     * @return the custom assertion holder
     */
    public CustomAssertionHolder getAssertion(final String customAssertionClassName) {
        return asCustomAssertionHolder(CustomAssertions.getDescriptor(customAssertionClassName));
    }

    /**
     * Return the <code>CustomAssertionUI</code> class for a given assertion or
     * <b>null<b>
     *
     * @param a the assertion class
     * @return the custom assertion UI class or <b>null</b>
     */
    @Override
    public CustomAssertionUI getUI(String a) {
        return CustomAssertions.getUI(a);
    }

    @Override
    public CustomTaskActionUI getTaskActionUI(String a) {
        return CustomAssertions.getTaskActionUI(a);
    }

    @Override
    public CustomEntitySerializer getExternalEntitySerializer(String extEntitySerializerClassName) {
        return null;
    }

    /**
     * Return the <code>CustomAssertionDescriptor</code> for a given assertion or
     * <b>null<b>
     *
     * @param a the assertion class
     * @return the custom assertion descriptor class or <b>null</b>
     */
    @Override
    public CustomAssertionDescriptor getDescriptor(Class a) {
        return CustomAssertions.getDescriptor(a);
    }

    private Collection<CustomAssertionHolder> asCustomAssertionHolders(final Set customAssertionDescriptors) {
        Collection<CustomAssertionHolder> result = new ArrayList<>();
        for (Object customAssertionDescriptor : customAssertionDescriptors) {
            CustomAssertionHolder customAssertionHolder = asCustomAssertionHolder((CustomAssertionDescriptor) customAssertionDescriptor);
            if (customAssertionHolder != null) {
                result.add(customAssertionHolder);
            }
        }
        return result;
    }

    private CustomAssertionHolder asCustomAssertionHolder(final CustomAssertionDescriptor customAssertionDescriptor) {
        CustomAssertionHolder customAssertionHolder = null;
        try {
            Class ca = customAssertionDescriptor.getAssertion();
            customAssertionHolder = new CustomAssertionHolder();
            final CustomAssertion cas = (CustomAssertion) ca.newInstance();
            customAssertionHolder.setCustomAssertion(cas);
            customAssertionHolder.setCategories(customAssertionDescriptor.getCategories());
            customAssertionHolder.setDescriptionText(customAssertionDescriptor.getDescription());
            customAssertionHolder.setPaletteNodeName(customAssertionDescriptor.getPaletteNodeName());
            customAssertionHolder.setPolicyNodeName(customAssertionDescriptor.getPolicyNodeName());
            customAssertionHolder.setIsUiAutoOpen(customAssertionDescriptor.getIsUiAutoOpen());
            customAssertionHolder.setModuleFileName(customAssertionDescriptor.getModuleFileName());
            if (cas instanceof CustomFeatureSetName) {
                CustomFeatureSetName customFeatureSetName = (CustomFeatureSetName) cas;
                customAssertionHolder.setRegisteredCustomFeatureSetName(customFeatureSetName.getFeatureSetName());
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to instantiate custom assertion", e);
        }
        return customAssertionHolder;
    }

    @Override
    public void loadModule(@NotNull final File stagedFile, @NotNull final ServerModuleFile moduleEntity) throws ModuleLoadingException {
        // nothing to do
    }

    @Override
    public void unloadModule(@NotNull final File stagedFile, @NotNull final ServerModuleFile moduleEntity) throws ModuleLoadingException {
        // nothing to do
    }
}
