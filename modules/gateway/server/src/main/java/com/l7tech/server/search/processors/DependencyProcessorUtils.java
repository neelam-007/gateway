package com.l7tech.server.search.processors;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.GuidEntityHeader;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class DependencyProcessorUtils {

    /**
     * This will find a mapped header in the given headers map it will first check by id, then by guid, then by name
     *
     * @param replacementMap  The map to search for a mapped header
     * @param dependentHeader The depended entity header to find a mapping for
     * @return The mapped entity header, or null if it cant find one.
     */
    @Nullable
    public static EntityHeader findMappedHeader(@NotNull final Map<EntityHeader, EntityHeader> replacementMap, @NotNull final EntityHeader dependentHeader) {
        //try to find normally
        final EntityHeader header = replacementMap.get(dependentHeader);
        if (header != null) {
            return header;
        }
        // check by ID
        final EntityHeader idHeaderKey = Functions.grepFirst(replacementMap.keySet(), new Functions.Unary<Boolean, EntityHeader>() {
            @Override
            public Boolean call(@NotNull final EntityHeader entityHeader) {
                return entityHeader.getType().equals(dependentHeader.getType())
                        && entityHeader.getGoid().equals(dependentHeader.getGoid());
            }
        });
        if (idHeaderKey != null) {
            return replacementMap.get(idHeaderKey);
        }

        // check by GUID
        if (dependentHeader instanceof GuidEntityHeader) {
            final EntityHeader guidHeaderKey = Functions.grepFirst(replacementMap.keySet(), new Functions.Unary<Boolean, EntityHeader>() {
                @Override
                public Boolean call(@NotNull final EntityHeader entityHeader) {
                    return entityHeader instanceof GuidEntityHeader && entityHeader.getType().equals(dependentHeader.getType())
                            && StringUtils.equals(((GuidEntityHeader) entityHeader).getGuid(), ((GuidEntityHeader) dependentHeader).getGuid());
                }
            });
            if (guidHeaderKey != null) {
                return replacementMap.get(guidHeaderKey);
            }
        }

        //check by name
        if (dependentHeader.getName() != null) {
            final EntityHeader nameHeaderKey = Functions.grepFirst(replacementMap.keySet(), new Functions.Unary<Boolean, EntityHeader>() {
                @Override
                public Boolean call(@NotNull final EntityHeader entityHeader) {
                    return entityHeader.getType().equals(dependentHeader.getType())
                            && StringUtils.equals(entityHeader.getName(), dependentHeader.getName());
                }
            });
            if (nameHeaderKey != null) {
                return replacementMap.get(nameHeaderKey);
            }
        }
        return null;
    }
}
