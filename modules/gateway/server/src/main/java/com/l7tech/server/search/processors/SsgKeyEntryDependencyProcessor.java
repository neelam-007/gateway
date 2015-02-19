package com.l7tech.server.search.processors;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SsgKeyHeader;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.search.objects.DependentEntity;
import com.l7tech.server.search.objects.DependentObject;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.security.KeyStoreException;

/**
 * This is used to create SsgKeyEntry dependent objects. It is needed in the software keystore to enforce lower case
 * aliases.
 *
 * @author Victor Kazakov
 */
public class SsgKeyEntryDependencyProcessor extends DefaultDependencyProcessor<SsgKeyEntry> {

    @Inject
    SsgKeyStoreManager ssgKeyStoreManager;

    @NotNull
    @Override
    public DependentObject createDependentObject(@NotNull final SsgKeyEntry ssgKeyEntry) {
        //If the keystore is a software keystore force the alias to be lowercase (SSL alias can be returned as uppercase in some situations.)
        final boolean caseInsensitiveAlias;
        boolean isPKCS12_SOFTWARE = false;
        try {
            isPKCS12_SOFTWARE = ssgKeyEntry.getKeystoreId() != null && SsgKeyFinder.SsgKeyStoreType.PKCS12_SOFTWARE.equals(ssgKeyStoreManager.findByPrimaryKey(ssgKeyEntry.getKeystoreId()).getType());
        } catch (KeyStoreException | FindException e) {
            //This shouldn't really happen, if it does swallow the exception and assume case insensitive
        }
        caseInsensitiveAlias = isPKCS12_SOFTWARE;

        return new DependentEntity(ssgKeyEntry.getAlias(), EntityHeaderUtils.fromEntity(ssgKeyEntry)) {
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                DependentEntity that = (DependentEntity) o;

                if(!(that.getEntityHeader() instanceof SsgKeyHeader)) return false;
                SsgKeyHeader thatHeader = (SsgKeyHeader)that.getEntityHeader();

                boolean aliasEqual = caseInsensitiveAlias ? ((SsgKeyHeader)getEntityHeader()).getAlias().equalsIgnoreCase(thatHeader.getAlias()) :
                        ((SsgKeyHeader)getEntityHeader()).getAlias().equals(thatHeader.getAlias());

                return aliasEqual && Goid.equals(((SsgKeyHeader)getEntityHeader()).getKeystoreId(), thatHeader.getKeystoreId());
            }
        };
    }
}
