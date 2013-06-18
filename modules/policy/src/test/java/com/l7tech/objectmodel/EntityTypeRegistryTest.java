package com.l7tech.objectmodel;

import com.l7tech.objectmodel.folder.Folder;
import org.junit.Test;

import static org.junit.Assert.*;

public class EntityTypeRegistryTest {

    @Test
    public void getEntityTypeForEntityClass() {
        assertEquals(EntityType.ANY, EntityTypeRegistry.getEntityType(Entity.class));
    }

    @Test
    public void getEntityTypeForSubclass() {
        assertEquals(EntityType.FOLDER, EntityTypeRegistry.getEntityType(FolderSubclass.class));
    }

    @Test
    public void getUnknownEntitySubclass() {
        assertEquals(EntityType.ANY, EntityTypeRegistry.getEntityType(EntityStub.class));
    }

    /**
     * A stub entity subclass that is not specifically mapped in the EntityTypeRegistry but its parent class is.
     */
    private class FolderSubclass extends Folder {

    }

    /**
     * A stub entity subclass that is not mapped in the EntityTypeRegistry.
     */
    private class EntityStub implements Entity {

        @Override
        public String getId() {
            return null;
        }
    }
}
