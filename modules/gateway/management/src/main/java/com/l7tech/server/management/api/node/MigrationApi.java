package com.l7tech.server.management.api.node;

import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.MigratedItem;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

/**
 * API for Policy Migration.
 *
 * @author jbufu
 */
@WebService(name="Migration", targetNamespace="http://www.layer7tech.com/management/migration")
public interface MigrationApi {

    @WebMethod(operationName="ListEntities")
    Collection<ExternalEntityHeader> listEntities(  @WebParam(name="EntityClass") Class<? extends Entity> clazz ) throws MigrationException;

    @WebMethod(operationName="CheckHeaders")
    Collection<ExternalEntityHeader> checkHeaders( @WebParam(name="ExternalEntityHeaders") Collection<ExternalEntityHeader> headers);

    @WebMethod(operationName="FindDependencies")
    MigrationMetadata findDependencies( @WebParam(name="ExternalEntityHeaders") Collection<ExternalEntityHeader> headers ) throws MigrationException;

    @WebMethod(operationName="ExportBundle")
    MigrationBundle exportBundle( @WebParam(name="ExternalEntityHeaders") Collection<ExternalEntityHeader> headers ) throws MigrationException;

    @WebMethod(operationName="RetrieveMappingCandidates")
    Collection<MappingCandidate> retrieveMappingCandidates( @WebParam(name="ExternalEntityHeaders") Collection<ExternalEntityHeader> mappables,
                                                            @WebParam(name="Scope") ExternalEntityHeader scope,
                                                            @WebParam(name="Filter") String filter ) throws MigrationException;

    @WebMethod(operationName="ImportBundle")
    Collection<MigratedItem> importBundle( @WebParam(name="Bundle") MigrationBundle bundle,
                                           @WebParam(name="DryRun") boolean dryRun) throws MigrationException;

    @XmlRootElement(name="MappingCandidate", namespace="http://www.layer7tech.com/management/migration")
    final class MappingCandidate {
        private ExternalEntityHeader header;
        private EntityHeaderSet<ExternalEntityHeader> candidates;

        public MappingCandidate() {
        }

        public MappingCandidate( final ExternalEntityHeader header, final EntityHeaderSet<ExternalEntityHeader> candidates ) {
            this.header = header;
            this.candidates = candidates;
        }

        public ExternalEntityHeader getHeader() {
            return header;
        }

        public void setHeader(ExternalEntityHeader header) {
            this.header = header;
        }

        public EntityHeaderSet<ExternalEntityHeader> getCandidates() {
            return candidates;
        }

        public void setCandidates(EntityHeaderSet<ExternalEntityHeader> candidates) {
            this.candidates = candidates;
        }

        public static Collection<MappingCandidate> asCandidates( final Map<ExternalEntityHeader,EntityHeaderSet<ExternalEntityHeader>> map ) {
            Collection<MappingCandidate> candidates = new ArrayList<MappingCandidate>();

            if ( map != null ) {
                for ( Map.Entry<ExternalEntityHeader,EntityHeaderSet<ExternalEntityHeader>> entry : map.entrySet() ) {
                    candidates.add( new MappingCandidate( entry.getKey(), entry.getValue() ) );
                }
            }

            return candidates;
        }

        public static Map<ExternalEntityHeader,EntityHeaderSet<ExternalEntityHeader>> fromCandidates( final Collection<MappingCandidate> candidates ) {
            final Map<ExternalEntityHeader,EntityHeaderSet<ExternalEntityHeader>> map = new LinkedHashMap<ExternalEntityHeader,EntityHeaderSet<ExternalEntityHeader>>();

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
