package com.l7tech.gateway.common.solutionkit;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Triple;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.util.*;

/**
 * Helper {@link SolutionKit} builder class.
*/
@SuppressWarnings("UnusedDeclaration")
public class SolutionKitBuilder {
    private Goid goid;
    private String name;
    private Integer version;
    private String skGuid;
    private String skVersion;
    private String mappings;
    private String uninstallBundle;
    private Long lastUpdateTime;
    private Goid parentGoid;

    private final Map<String, String> properties = new HashMap<>();
    private final Map<String, String> installProperties = new HashMap<>();
    private final Collection<Triple<String, EntityType, Boolean>> ownershipDescriptors = new ArrayList<>();

    /**
     * Pre-attached {@link com.l7tech.gateway.common.solutionkit.SolutionKit}.
     * The builder will append new properties or override existing ones.
     */
    private SolutionKit solutionKit;


    /**
     * Default constructor
     */
    public SolutionKitBuilder() {
        this(new SolutionKit());
    }

    /**
     * Initialize the builder with preexisting solution kit.
     * This way the builder will append new properties or override existing ones.
     *
     * @param solutionKit    the {@code SolutionKit} to attach to.
     */
    public SolutionKitBuilder(final SolutionKit solutionKit) {
        this.solutionKit = solutionKit;
    }

    public SolutionKitBuilder goid(final Goid goid) {
        this.goid = goid;
        return this;
    }

    public SolutionKitBuilder name(final String name) {
        this.name = name;
        return this;
    }

    public SolutionKitBuilder version(final Integer version) {
        this.version = version;
        return this;
    }

    public SolutionKitBuilder skGuid(final String skGuid) {
        this.skGuid = skGuid;
        return this;
    }

    public SolutionKitBuilder skVersion(final String skVersion) {
        this.skVersion = skVersion;
        return this;
    }

    public SolutionKitBuilder mappings(final String mappings) {
        this.mappings = mappings;
        return this;
    }

    public SolutionKitBuilder uninstallBundle(final String uninstallBundle) {
        this.uninstallBundle = uninstallBundle;
        return this;
    }

    public SolutionKitBuilder lastUpdateTime(final Long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
        return this;
    }

    public SolutionKitBuilder addProperty(final String name, final String value) {
        this.properties.put(name, value);
        return this;
    }

    public SolutionKitBuilder clearProperties() {
        this.properties.clear();
        return this;
    }

    public SolutionKitBuilder addInstallProperty(final String name, final String value) {
        this.installProperties.put(name, value);
        return this;
    }

    public SolutionKitBuilder clearInstallProperties() {
        this.installProperties.clear();
        return this;
    }

    public SolutionKitBuilder addOwnershipDescriptor(final String id, final EntityType type, final boolean readOnly) {
        this.ownershipDescriptors.add(Triple.triple(id, type, readOnly));
        return this;
    }

    public SolutionKitBuilder parent(final Goid parentGoid) {
        this.parentGoid = parentGoid;
        return this;
    }

    public SolutionKitBuilder parent(final SolutionKit parentKit) {
        this.parentGoid = parentKit != null ? parentKit.getGoid() : null;
        return this;
    }

    public SolutionKit build() {
        Assert.assertNotNull(solutionKit);

        if (goid != null) {
            solutionKit.setGoid(goid);
        }
        if (name != null) {
            solutionKit.setName(name);
        }
        if (version != null) {
            solutionKit.setVersion(version);
        }
        if (skGuid != null) {
            solutionKit.setSolutionKitGuid(skGuid);
        }
        if (skVersion != null) {
            solutionKit.setSolutionKitVersion(skVersion);
        }
        if (mappings != null) {
            solutionKit.setMappings(mappings);
        }
        if (uninstallBundle != null) {
            solutionKit.setUninstallBundle(uninstallBundle);
        }
        if (lastUpdateTime != null) {
            solutionKit.setLastUpdateTime(lastUpdateTime);
        }
        if (parentGoid != null) {
            solutionKit.setParentGoid(parentGoid);
        }

        final Set<String> propertyKeys = CollectionUtils.set(SolutionKit.getPropertyKeys());
        for (final Map.Entry<String, String> property : properties.entrySet()) {
            if (propertyKeys.contains(property.getKey())) {
                solutionKit.setProperty(property.getKey(), property.getValue());
            } else {
                Assert.fail("Unsupported property: " + property.getKey());
            }
        }

        for (final Map.Entry<String, String> property : installProperties.entrySet()) {
            solutionKit.setInstallationProperty(property.getKey(), property.getValue());
        }

        final Collection<EntityOwnershipDescriptor> ownershipDescriptorSet = new ArrayList<>(ownershipDescriptors.size());
        for (final Triple<String, EntityType, Boolean> ownershipDescriptor : ownershipDescriptors) {
            Assert.assertTrue(StringUtils.isNotEmpty(ownershipDescriptor.left) && ownershipDescriptor.middle != null && ownershipDescriptor.right != null);
            ownershipDescriptorSet.add(
                    new EntityOwnershipDescriptor(
                            solutionKit,
                            ownershipDescriptor.left,
                            ownershipDescriptor.middle,
                            ownershipDescriptor.right
                    )
            );
        }
        if (!ownershipDescriptorSet.isEmpty()) {
            solutionKit.addEntityOwnershipDescriptors(ownershipDescriptorSet);
        }

        return solutionKit;
    }
}
