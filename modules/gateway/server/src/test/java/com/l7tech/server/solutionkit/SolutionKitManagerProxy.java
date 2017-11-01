package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.gateway.common.solutionkit.SolutionKitImportInfo;
import com.l7tech.objectmodel.*;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Utility class used for unit testing to mock a {@link SolutionKitManager} and, at the same time, proxy unmocked methods to the real {@link #solutionKitManager}.<br/>
 *
 * IMPORTANT: <br/>
 * When adding new methods into SolutionKitManager make sure the new methods are properly proxied via {@link #solutionKitManager}.
 * In addition if the method is transactional (not read-only) make sure you call {@link #flushSession()}.
 *
 */
public class SolutionKitManagerProxy implements SolutionKitManager {

    /**
     * Proxy {@link SolutionKitManager}.<br/>
     * All methods will be proxied via this instance.<br/>
     * Required and cannot be {@code null}.
     */
    @NotNull
    private final SolutionKitManager solutionKitManager;

    /**
     * Optional hibernate session flusher callback.
     * Convenient if you want to flush hibernate session after each non-read-only action (i.e. create, delete or update).
     */
    @Nullable
    private final Functions.NullaryVoidThrows<RuntimeException> flashSessionCallback;

    public SolutionKitManagerProxy(@NotNull final SolutionKitManager solutionKitManager) {
        this(solutionKitManager, null);
    }

    public SolutionKitManagerProxy(@NotNull final SolutionKitManager solutionKitManager, @Nullable final Functions.NullaryVoidThrows<RuntimeException> flashSessionCallback) {
        this.solutionKitManager = solutionKitManager;
        this.flashSessionCallback = flashSessionCallback;
    }

    @NotNull
    @Override
    public String importBundle(@NotNull String bundle, @NotNull SolutionKit metadata, boolean isTest) throws Exception {
        final String mappings = solutionKitManager.importBundle(bundle, metadata, isTest);
        flushSession();
        return mappings;
    }

    @Override
    @NotNull
    public String importBundles(@NotNull SolutionKitImportInfo solutionKitImportInfo, boolean isTest) throws Exception {
        final String mappings = solutionKitManager.importBundles(solutionKitImportInfo, isTest);
        flushSession();
        return mappings;
    }

    @Override
    public List<SolutionKit> findBySolutionKitGuid(@NotNull String solutionKitGuid) throws FindException {
        return solutionKitManager.findBySolutionKitGuid(solutionKitGuid);
    }

    @Override
    public SolutionKit findBySolutionKitGuidAndIM(@NotNull String solutionKitGuid, @Nullable String instanceModifier) throws FindException {
        return solutionKitManager.findBySolutionKitGuidAndIM(solutionKitGuid, instanceModifier);
    }

    @Override
    public List<SolutionKitHeader> findAllChildrenHeadersByParentGoid(@NotNull Goid parentGoid) throws FindException {
        return solutionKitManager.findAllChildrenHeadersByParentGoid(parentGoid);
    }

    @NotNull
    @Override
    public Collection<SolutionKit> findAllChildrenByParentGoid(@NotNull Goid parentGoid) throws FindException {
        return solutionKitManager.findAllChildrenByParentGoid(parentGoid);
    }

    @Override
    public void decrementEntitiesVersionStamp(@NotNull Collection<String> entityIds, @NotNull Goid solutionKit) throws UpdateException {
        solutionKitManager.decrementEntitiesVersionStamp(entityIds, solutionKit);
        flushSession();
    }

    @Nullable
    @Override
    public SolutionKit findByPrimaryKey(Goid goid) throws FindException {
        return solutionKitManager.findByPrimaryKey(goid);
    }

    @Override
    public Collection<SolutionKitHeader> findAllHeaders() throws FindException {
        return solutionKitManager.findAllHeaders();
    }

    @Override
    public Collection<SolutionKitHeader> findAllHeaders(int offset, int windowSize) throws FindException {
        return solutionKitManager.findAllHeaders(offset, windowSize);
    }

    @Override
    public Collection<SolutionKit> findAll() throws FindException {
        return solutionKitManager.findAll();
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return solutionKitManager.getImpClass();
    }

    @Override
    public Goid save(SolutionKit entity) throws SaveException {
        final Goid goid = solutionKitManager.save(entity);
        flushSession();
        return goid;
    }

    @Override
    public void save(Goid id, SolutionKit entity) throws SaveException {
        solutionKitManager.save(id, entity);
        flushSession();
    }

    @Override
    public Integer getVersion(Goid goid) throws FindException {
        return solutionKitManager.getVersion(goid);
    }

    @Override
    public Map<Goid, Integer> findVersionMap() throws FindException {
        return solutionKitManager.findVersionMap();
    }

    @Override
    public void delete(SolutionKit entity) throws DeleteException {
        solutionKitManager.delete(entity);
        flushSession();
    }

    @Nullable
    @Override
    public SolutionKit getCachedEntity(Goid goid, int maxAge) throws FindException {
        return solutionKitManager.getCachedEntity(goid, maxAge);
    }

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return solutionKitManager.getInterfaceClass();
    }

    @Override
    public EntityType getEntityType() {
        return solutionKitManager.getEntityType();
    }

    @Override
    public String getTableName() {
        return solutionKitManager.getTableName();
    }

    @Nullable
    @Override
    public SolutionKit findByUniqueName(String name) throws FindException {
        return solutionKitManager.findByUniqueName(name);
    }

    @Override
    public void delete(Goid goid) throws DeleteException, FindException {
        solutionKitManager.delete(goid);
        flushSession();
    }

    @Override
    public void update(SolutionKit entity) throws UpdateException {
        solutionKitManager.update(entity);
        flushSession();
    }

    @Nullable
    @Override
    public SolutionKit findByHeader(EntityHeader header) throws FindException {
        return solutionKitManager.findByHeader(header);
    }

    @Override
    public List<SolutionKit> findPagedMatching(int offset, int count, @Nullable String sortProperty, @Nullable Boolean ascending, @Nullable Map<String, List<Object>> matchProperties) throws FindException {
        return solutionKitManager.findPagedMatching(offset, count, sortProperty, ascending, matchProperties);
    }

    private void flushSession() {
        if (flashSessionCallback != null) {
            flashSessionCallback.call();
        }
    }
}
