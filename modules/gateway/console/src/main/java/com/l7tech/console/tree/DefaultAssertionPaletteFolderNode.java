package com.l7tech.console.tree;

import com.l7tech.policy.assertion.ext.Category;

/**
 * PaletteFolderNode for assertions of a given category.
 */
public class DefaultAssertionPaletteFolderNode extends AbstractPaletteFolderNode {

    //- PUBLIC

    /**
     * Create a paletter folder node for the given assertion category.
     *
     * @param name The folder name (must not be null)
     * @param id The folder id, used to load modular assertions (must not be null)
     * @param category The category, used to load custom assertions (must not be null)
     */
    public DefaultAssertionPaletteFolderNode( final String name,
                                              final String id,
                                              final Category category ) {
        super( name, id );
        this.category = category;
    }

    //- PROTECTED

    @Override
    protected void doLoadChildren() {
        // Load modular assertions and insert in sort order
        insertMatchingModularAssertions(-1);
        insertMatchingCustomAssertions(getChildCount(), category);
    }

    //- PRIVATE

    private final Category category;
 
}
