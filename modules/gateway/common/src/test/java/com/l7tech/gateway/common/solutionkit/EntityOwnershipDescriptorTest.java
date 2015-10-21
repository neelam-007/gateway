package com.l7tech.gateway.common.solutionkit;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EntityOwnershipDescriptorTest {

    @Before
    public void setUp() throws Exception {
    }

    private static SolutionKit createSampleSolutionKit(
            final Goid goid,
            final String name
    ) {
        final SolutionKit solutionKit = new SolutionKit();
        solutionKit.setGoid(goid);
        solutionKit.setName(name);
        return solutionKit;
    }

    @Test
    public void testCopyFrom() throws Exception {
        final SolutionKit sk1 = createSampleSolutionKit(new Goid(0, 1), "test1");
        final SolutionKit sk2 = createSampleSolutionKit(new Goid(0, 2), "test2");

        EntityOwnershipDescriptor descriptor1 = new EntityOwnershipDescriptor(sk1, "id1", EntityType.FOLDER, false);
        descriptor1.setGoid(new Goid(1, 1));
        EntityOwnershipDescriptor descriptor2 = new EntityOwnershipDescriptor(sk2, "id2", EntityType.POLICY, true);
        descriptor2.setGoid(new Goid(1, 2));

        // descriptor1 not equal to descriptor2
        Assert.assertThat(descriptor1, Matchers.not(Matchers.equalTo(descriptor2)));

        descriptor2.copyFrom(descriptor1, sk2);
        // make sure the owners are different
        Assert.assertThat(
                descriptor2.getSolutionKit(),
                Matchers.allOf(
                        Matchers.not(Matchers.sameInstance(descriptor1.getSolutionKit())),
                        Matchers.not(Matchers.equalTo(descriptor1.getSolutionKit()))
                )
        );
        Assert.assertThat(descriptor2, Matchers.equalTo(descriptor1));
        Assert.assertThat(descriptor2.getGoid(), Matchers.equalTo(descriptor1.getGoid()));
        Assert.assertThat(descriptor2.getId(), Matchers.equalTo(descriptor1.getId()));
        Assert.assertThat(descriptor2.getEntityId(), Matchers.equalTo(descriptor1.getEntityId()));
        Assert.assertThat(descriptor2.getEntityType(), Matchers.equalTo(descriptor1.getEntityType()));
        Assert.assertThat(descriptor2.isReadOnly(), Matchers.equalTo(descriptor1.isReadOnly()));

        EntityOwnershipDescriptor newDescriptor = EntityOwnershipDescriptor.createFrom(descriptor1, sk2);
        // make sure the owners are different
        Assert.assertThat(
                newDescriptor.getSolutionKit(),
                Matchers.allOf(
                        Matchers.not(Matchers.sameInstance(descriptor1.getSolutionKit())),
                        Matchers.not(Matchers.equalTo(descriptor1.getSolutionKit()))
                )
        );
        Assert.assertThat(newDescriptor, Matchers.equalTo(descriptor1));
        Assert.assertThat(newDescriptor.getGoid(), Matchers.equalTo(descriptor1.getGoid()));
        Assert.assertThat(newDescriptor.getId(), Matchers.equalTo(descriptor1.getId()));
        Assert.assertThat(newDescriptor.getEntityId(), Matchers.equalTo(descriptor1.getEntityId()));
        Assert.assertThat(newDescriptor.getEntityType(), Matchers.equalTo(descriptor1.getEntityType()));
        Assert.assertThat(newDescriptor.isReadOnly(), Matchers.equalTo(descriptor1.isReadOnly()));
        // make sure the owners are same
        Assert.assertThat(
                newDescriptor.getSolutionKit(),
                Matchers.allOf(
                        Matchers.sameInstance(descriptor2.getSolutionKit()),
                        Matchers.equalTo(descriptor2.getSolutionKit())
                )
        );
        Assert.assertThat(newDescriptor, Matchers.equalTo(descriptor2));
        Assert.assertThat(newDescriptor.getGoid(), Matchers.equalTo(descriptor2.getGoid()));
        Assert.assertThat(newDescriptor.getId(), Matchers.equalTo(descriptor2.getId()));
        Assert.assertThat(newDescriptor.getEntityId(), Matchers.equalTo(descriptor2.getEntityId()));
        Assert.assertThat(newDescriptor.getEntityType(), Matchers.equalTo(descriptor2.getEntityType()));
        Assert.assertThat(newDescriptor.isReadOnly(), Matchers.equalTo(descriptor2.isReadOnly()));
    }

    /**
     * Convenient method for getting the next {@code EntityType} in the specified enum.
     *
     * @param entityType    starting {@code EntityType}.  Required and cannot be {@code null}.
     */
    private static EntityType next(final EntityType entityType) {
        Assert.assertNotNull(entityType);
        return EntityType.values()[(entityType.ordinal() + 1) % EntityType.values().length];
    }

    @Test
    public void testEqualsAndHashCode() throws Exception {
        final SolutionKit sk1 = createSampleSolutionKit(new Goid(0, 1), "test1");
        final SolutionKit sk2 = createSampleSolutionKit(new Goid(0, 2), "test2");

        EntityOwnershipDescriptor descriptor1 = new EntityOwnershipDescriptor(sk1, "id1", EntityType.FOLDER, false);
        descriptor1.setGoid(new Goid(1, 1));
        EntityOwnershipDescriptor descriptor2 = new EntityOwnershipDescriptor(sk2, "id2", EntityType.POLICY, true);
        descriptor2.setGoid(new Goid(1, 2));

        // some sanity check
        Assert.assertThat(descriptor1, Matchers.equalTo(descriptor1));
        Assert.assertThat(descriptor1.hashCode(), Matchers.equalTo(descriptor1.hashCode()));
        Assert.assertThat(descriptor2, Matchers.equalTo(descriptor2));
        Assert.assertThat(descriptor2.hashCode(), Matchers.equalTo(descriptor2.hashCode()));

        // descriptor1 not equal to descriptor2
        Assert.assertThat(descriptor1, Matchers.not(Matchers.equalTo(descriptor2)));
        Assert.assertThat(descriptor1.hashCode(), Matchers.not(Matchers.equalTo(descriptor2.hashCode())));

        // make a copy of descriptor1
        EntityOwnershipDescriptor descriptor1Copy = EntityOwnershipDescriptor.createFrom(descriptor1, sk2);
        // make sure the owners are different
        Assert.assertThat(
                descriptor1Copy.getSolutionKit(),
                Matchers.allOf(
                        Matchers.not(Matchers.sameInstance(descriptor1.getSolutionKit())),
                        Matchers.not(Matchers.equalTo(descriptor1.getSolutionKit()))
                )
        );
        // even though they have different owners they are still equal
        Assert.assertThat(descriptor1Copy, Matchers.equalTo(descriptor1));
        Assert.assertThat(descriptor1Copy.hashCode(), Matchers.equalTo(descriptor1.hashCode()));

        // test diff id's
        descriptor1Copy = EntityOwnershipDescriptor.createFrom(descriptor1, sk1);
        // make sure the owners are same
        Assert.assertThat(
                descriptor1Copy.getSolutionKit(),
                Matchers.allOf(
                        Matchers.sameInstance(descriptor1.getSolutionKit()),
                        Matchers.equalTo(descriptor1.getSolutionKit())
                )
        );
        Assert.assertThat(descriptor1Copy, Matchers.equalTo(descriptor1));
        Assert.assertThat(descriptor1Copy.hashCode(), Matchers.equalTo(descriptor1.hashCode()));
        // now change the id
        descriptor1Copy.setEntityId(descriptor1.getEntityId() + "_some_");
        // make sure they are different
        Assert.assertThat(descriptor1Copy, Matchers.not(Matchers.equalTo(descriptor1)));
        Assert.assertThat(descriptor1Copy.hashCode(), Matchers.not(Matchers.equalTo(descriptor1.hashCode())));

        // test diff types
        descriptor1Copy = EntityOwnershipDescriptor.createFrom(descriptor1, sk1);
        // make sure the owners are same
        Assert.assertThat(
                descriptor1Copy.getSolutionKit(),
                Matchers.allOf(
                        Matchers.sameInstance(descriptor1.getSolutionKit()),
                        Matchers.equalTo(descriptor1.getSolutionKit())
                )
        );
        Assert.assertThat(descriptor1Copy, Matchers.equalTo(descriptor1));
        Assert.assertThat(descriptor1Copy.hashCode(), Matchers.equalTo(descriptor1.hashCode()));
        // now change the type
        descriptor1Copy.setEntityType(next(descriptor1.getEntityType()));
        // make sure they are different
        Assert.assertThat(descriptor1Copy, Matchers.not(Matchers.equalTo(descriptor1)));
        Assert.assertThat(descriptor1Copy.hashCode(), Matchers.not(Matchers.equalTo(descriptor1.hashCode())));

        // test diff read-only flag
        descriptor1Copy = EntityOwnershipDescriptor.createFrom(descriptor1, sk1);
        // make sure the owners are same
        Assert.assertThat(
                descriptor1Copy.getSolutionKit(),
                Matchers.allOf(
                        Matchers.sameInstance(descriptor1.getSolutionKit()),
                        Matchers.equalTo(descriptor1.getSolutionKit())
                )
        );
        Assert.assertThat(descriptor1Copy, Matchers.equalTo(descriptor1));
        Assert.assertThat(descriptor1Copy.hashCode(), Matchers.equalTo(descriptor1.hashCode()));
        // now flip the read-only flag
        descriptor1Copy.setReadOnly(!descriptor1.isReadOnly());
        // make sure they are different
        Assert.assertThat(descriptor1Copy, Matchers.not(Matchers.equalTo(descriptor1)));
        Assert.assertThat(descriptor1Copy.hashCode(), Matchers.not(Matchers.equalTo(descriptor1.hashCode())));

        // test diff goids
        descriptor1Copy = EntityOwnershipDescriptor.createFrom(descriptor1, sk1);
        // make sure the owners are same
        Assert.assertThat(
                descriptor1Copy.getSolutionKit(),
                Matchers.allOf(
                        Matchers.sameInstance(descriptor1.getSolutionKit()),
                        Matchers.equalTo(descriptor1.getSolutionKit())
                )
        );
        Assert.assertThat(descriptor1Copy, Matchers.equalTo(descriptor1));
        Assert.assertThat(descriptor1Copy.hashCode(), Matchers.equalTo(descriptor1.hashCode()));
        // now change the type
        descriptor1Copy.setGoid(new Goid(1, 3));
        Assert.assertThat(descriptor1Copy.getGoid(), Matchers.not(Matchers.equalTo(descriptor1.getGoid())));
        // make sure they are different
        Assert.assertThat(descriptor1Copy, Matchers.not(Matchers.equalTo(descriptor1)));
        Assert.assertThat(descriptor1Copy.hashCode(), Matchers.not(Matchers.equalTo(descriptor1.hashCode())));
    }


}