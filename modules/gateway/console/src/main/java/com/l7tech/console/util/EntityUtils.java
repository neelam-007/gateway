package com.l7tech.console.util;

import com.l7tech.console.policy.ConsoleAssertionRegistry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.SsgKeyMetadata;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.imp.NamedGoidEntityImp;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.List;

/**
 * Utility methods for entities
 */
public class EntityUtils {

    public static final String COPY_OF_PREFIX = "Copy of ";

    /**
     * Configure an entity as a copy.
     *
     * @param entity The entity to update
     */
    public static void updateCopy(final NamedEntityImp entity) {
        resetIdentity(entity);
        entity.setName(getNameForCopy(entity.getName()));
    }

    /**
     * Configure an entity as a copy.
     *
     * @param entity The entity to update
     */
    public static void updateCopy(final NamedGoidEntityImp entity) {
        resetIdentity(entity);
        entity.setName(getNameForCopy(entity.getName()));
    }

    /**
     * Remove identity information from an entity.
     *
     * @param entity The entity to update.
     */
    public static void resetIdentity(final PersistentEntity entity) {
        entity.setOid(PersistentEntity.DEFAULT_OID);
        entity.setVersion(0);
    }

    /**
     * Remove identity information from an entity.
     *
     * @param entity The entity to update.
     */
    public static void resetIdentity(final GoidEntity entity) {
        entity.setGoid(GoidEntity.DEFAULT_GOID);
        entity.setVersion(0);
    }

    /**
     * Get a possibly updated name to use for a copied item.
     *
     * @param name The original name
     * @return The name to use for the copy
     */
    public static String getNameForCopy(final String name) {
        String updatedName = name;
        if (name != null && !name.startsWith(COPY_OF_PREFIX)) {
            updatedName = COPY_OF_PREFIX + name;
        }
        return updatedName;
    }

    /**
     * Retrieve all entities of the given EntityType.
     *
     * @param type the EntityType of the entities to retrieve.
     * @return an EntityHeaderSet of all entities of the given type.
     * @throws FindException if there was an error retrieving the entities.
     */
    public static EntityHeaderSet<EntityHeader> getEntities(@NotNull final EntityType type) throws FindException {
        final EntityHeaderSet<EntityHeader> entities;
        if (type == EntityType.ASSERTION_ACCESS) {
            // get assertion access from registry as they may not all be persisted in the database
            entities = new EntityHeaderSet<>();
            final ConsoleAssertionRegistry assertionRegistry = TopComponents.getInstance().getAssertionRegistry();
            final Collection<AssertionAccess> assertions = assertionRegistry.getPermittedAssertions();
            long nonPersistedAssertions = 0L;
            boolean customAssertionProcessed = false;
            for (final AssertionAccess assertionAccess : assertions) {
                if (CustomAssertionHolder.class.getName().equals(assertionAccess.getName()) && customAssertionProcessed) {
                    // bundle all CustomAssertions as one
                    continue;
                }
                Goid goid = assertionAccess.getGoid();
                if (assertionAccess.isUnsaved()) {
                    // this assertion access is not yet persisted
                    // give it some wrapped dummy goid so that it won't be considered 'equal' to the other headers
                    goid = new Goid(GoidRange.WRAPPED_OID.getFirstHi(), --nonPersistedAssertions);
                }
                final ZoneableEntityHeader assertionHeader = new ZoneableEntityHeader(goid, EntityType.ASSERTION_ACCESS, assertionAccess.getName(), null, assertionAccess.getVersion());
                assertionHeader.setSecurityZoneGoid(assertionAccess.getSecurityZone() == null ? null : assertionAccess.getSecurityZone().getGoid());
                entities.add(assertionHeader);
            }
        } else if (type == EntityType.SSG_KEY_METADATA) {
            entities = new EntityHeaderSet<>();
            long nonPersistedMetadatas = 0L;
            try {
                final TrustedCertAdmin trustedCertManager = Registry.getDefault().getTrustedCertManager();
                final List<KeystoreFileEntityHeader> keystores = trustedCertManager.findAllKeystores(true);
                for (final KeystoreFileEntityHeader keystore : keystores) {
                    final List<SsgKeyEntry> keys = trustedCertManager.findAllKeys(keystore.getOid(), true);
                    for (final SsgKeyEntry key : keys) {
                        SsgKeyMetadata keyMetadata = key.getKeyMetadata();
                        if (keyMetadata == null) {
                            // this key metadata is not yet persisted
                            keyMetadata = new SsgKeyMetadata(key.getKeystoreId(), key.getAlias(), null);
                            keyMetadata.setOid(--nonPersistedMetadatas);
                        }
                        entities.add(new KeyMetadataHeaderWrapper(keyMetadata));
                    }
                }
            } catch (final IOException | KeyStoreException | CertificateException e) {
                throw new FindException("Error retrieving private key metadata.", e);
            }
        } else {
            entities = Registry.getDefault().getRbacAdmin().findEntities(type);
        }
        return entities;
    }
}
