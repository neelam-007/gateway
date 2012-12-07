package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.Category;

import java.util.Arrays;
import java.util.List;

/**
 * PaletteFolderNode for assertions of a given category.
 */
@SuppressWarnings({ "serial" })
public class DefaultAssertionPaletteFolderNode extends AbstractPaletteFolderNode {

    //- PUBLIC

    /**
     * Create a palette folder node for the given assertion category.
     *
     * @param name The folder name (must not be null)
     * @param id The folder id, used to load modular assertions (must not be null)
     * @param categories The categories, used to load custom assertions (optional)
     */
    public DefaultAssertionPaletteFolderNode( final String name,
                                              final String id,
                                              final Category... categories ) {
        super( name, id );
        this.categories = categories == null || categories.length < 1 ? null : Arrays.asList(categories);
    }

    //- PROTECTED

    @Override
    protected void doLoadChildren() {
        // Load modular assertions and insert in sort order
        insertMatchingModularAssertions();
        if ( categories != null ) {
            for (Category category : categories) {
                insertMatchingCustomAssertions(category);
            }
        }
        insertMatchingEncapsulatedAssertions();
    }

    //- PRIVATE

    private final List<Category> categories;
 
}
