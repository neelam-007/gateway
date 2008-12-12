package com.l7tech.server.management.api.node;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.Entity;
import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.MigratedItem;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;
import java.util.*;

/**
 * API for Policy Migration.
 *
 * @author jbufu
 */
@WebService(name="Migration", targetNamespace="http://www.layer7tech.com/management/migration")
public interface MigrationApi {

    @WebMethod(operationName="ListEntities")
    Collection<EntityHeader> listEntities(  @WebParam(name="EntityClass") Class<? extends Entity> clazz ) throws MigrationException;

    @WebMethod(operationName="FindDependencies")
    MigrationMetadata findDependencies( @WebParam(name="EntityHeaders") Collection<EntityHeader> headers ) throws MigrationException;

    @WebMethod(operationName="ExportBundle")
    MigrationBundle exportBundle( @WebParam(name="EntityHeaders") Collection<EntityHeader> headers ) throws MigrationException;

    @WebMethod(operationName="RetrieveMappingCandidates")
    Collection<MappingCandidate> retrieveMappingCandidates( @WebParam(name="EntityHeaders") Collection<EntityHeader> mappables,
                                                            @WebParam(name="Filter") String filter ) throws MigrationException;

    @WebMethod(operationName="ImportBundle")
    Collection<MigratedItem> importBundle( @WebParam(name="Bundle") MigrationBundle bundle,
                       @WebParam(name="TargetFolder") EntityHeader targetFolder,
                       @WebParam(name="FlattenFolders") boolean flattenFolders,
                       @WebParam(name="OverwriteExisting") boolean overwriteExisting,
                       @WebParam(name="EnableServices") boolean enableServices,
                       @WebParam(name="DryRun") boolean dryRun
    ) throws MigrationException;

    @XmlRootElement(name="MappingCandidate", namespace="http://www.layer7tech.com/management/migration")
    final class MappingCandidate {
        private EntityHeader header;
        private EntityHeaderSet candidates;

        public MappingCandidate() {
        }

        public MappingCandidate( final EntityHeader header, final EntityHeaderSet candidates ) {
            this.header = header;
            this.candidates = candidates;
        }

        public EntityHeader getHeader() {
            return header;
        }

        public void setHeader(EntityHeader header) {
            this.header = header;
        }

        public EntityHeaderSet getCandidates() {
            return candidates;
        }

        public void setCandidates(EntityHeaderSet candidates) {
            this.candidates = candidates;
        }

        public static Collection<MappingCandidate> asCandidates( final Map<EntityHeader,EntityHeaderSet> map ) {
            Collection<MappingCandidate> candidates = new ArrayList<MappingCandidate>();

            if ( map != null ) {
                for ( Map.Entry<EntityHeader,EntityHeaderSet> entry : map.entrySet() ) {
                    candidates.add( new MappingCandidate( entry.getKey(), entry.getValue() ) );
                }
            }

            return candidates;
        }

        public static Map<EntityHeader,EntityHeaderSet> fromCandidates( final Collection<MappingCandidate> candidates ) {
            final Map<EntityHeader,EntityHeaderSet> map = new LinkedHashMap<EntityHeader,EntityHeaderSet>();

            if ( candidates != null ) {
                for ( MappingCandidate candidate : candidates ) {
                    map.put( candidate.getHeader(), candidate.getCandidates() );
                }
            }

            return map;
        }
    }

    @XmlRootElement
    public static class MigrationException extends Exception {

        private String errors;

        public MigrationException() {}

        public MigrationException(String message) {
            super(message);
        }

        public MigrationException(Collection<String> errors) {
            this(null, errors);
        }

        public MigrationException(String message, Collection<String> errors) {
            super(message);
            StringBuffer sb = new StringBuffer();
            for (String error : errors) {
                sb.append(error).append("\n");
            }
            this.errors = sb.toString();
        }

        public MigrationException(Throwable cause) {
            super(cause);
        }

        public MigrationException(String message, Throwable cause) {
            super(message, cause);
        }

        public String getErrors() {
            return errors;
        }

        public void setErrors(String errors) {
            this.errors = errors;
        }

        public boolean hasErrors() {
            return errors != null && errors.length() > 0;
        }
    }
}
