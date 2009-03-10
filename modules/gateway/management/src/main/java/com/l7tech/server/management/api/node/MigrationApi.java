package com.l7tech.server.management.api.node;

import com.l7tech.objectmodel.EntityHeaderSet;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.objectmodel.JaxbMapType;
import com.l7tech.server.management.migration.bundle.MigrationMetadata;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.management.migration.bundle.MigratedItem;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.*;

/**
 * API for Policy Migration.
 *
 * @author jbufu
 */
@WebService(name="Migration", targetNamespace="http://www.layer7tech.com/management/migration")
public interface MigrationApi {

    /**
     * Retrieves entity headers from a Cluster.
     *
     * @param clazz  The entity type for which headers are to be retrieved. If null, any entity type is considered.
     * @return       A collection of entity headers matching the specified {@link @param clazz} parameter.
     */
    @WebMethod(operationName="ListEntities")
    Collection<ExternalEntityHeader> listEntities(  @WebParam(name="EntityClass") Class<? extends Entity> clazz ) throws MigrationException;

    /**
     * Verifies if the entities corresponding to a list of headers still exist on a Cluster.
     *
     * @param headers  A collection of headers to be checked.
     * @return         A collection of headers for which entities exist on the Cluster.
     *                 If an entity found on the Cluster is of a newer version than the header in the request,
     *                 the updated header is returned for it.
     */
    @WebMethod(operationName="CheckHeaders")
    Collection<ExternalEntityHeader> checkHeaders( @WebParam(name="ExternalEntityHeaders") Collection<ExternalEntityHeader> headers);

    /**
     * Performs dependency analysis on a initial collection of entity headers.
     *
     * @param headers       The collection of entity headers for which dependencies need to be determined.
     * @return              A MigrationMetadata instance that encapsulates the initial collection of headers
     *                      and the dependencies discovered from them.
     * @see                 com.l7tech.server.management.migration.bundle.MigrationMetadata,
     *                      com.l7tech.objectmodel.migration.MigrationDependency
     * @throws MigrationException if the dependency analysis cannot be completed for any reason.
     */
    @WebMethod(operationName="FindDependencies")
    MigrationMetadata findDependencies( @WebParam(name="ExternalEntityHeaders") Collection<ExternalEntityHeader> headers ) throws MigrationException;

    /**
     * Exports a MigrationBundle for a given initial collection of entity headers.
     *
     * @param headers       The initial collection of entity headers for which a MigrationBundle is exported.
     * @return              A MigrationBundle for the given collection of headers, obtained by performing
     *                      dependency analysis on it and then loading all entities that need to be exported
     *                      for the purposes of Migration.
     * @see                 com.l7tech.server.management.migration.bundle.MigrationBundle,
     *                      com.l7tech.server.management.migration.bundle.MigrationMetadata,
     *                      com.l7tech.objectmodel.migration.Migration#export()
     */
    @WebMethod(operationName="ExportBundle")
    MigrationBundle exportBundle( @WebParam(name="ExternalEntityHeaders") Collection<ExternalEntityHeader> headers ) throws MigrationException;

    /**
     * For each entry in a given collection of entity headers (presumably obtained from a different Cluster),
     * matching mapping candidate entities from the current Cluster are identified and their entity headers
     * are returned.
     *
     * @param mappables     Collection of entity headers for which mapping candidates are wanted.
     * @param scope         Hack for allowing the retrieval of entity (headers) that are in a 'child' relationship,
     *                      i.e. belonging to some other entity. Needed for Identity Providers -> Users initially.
     * @param filters       Filters to be applied before the result is returned.
     * @return              A Map keyed on the requested entity headers; each entry value is a set of qualified
     *                      entity header candidates from the current Cluster.
     */
    @WebMethod(operationName="RetrieveMappingCandidates")
    Collection<MappingCandidate> retrieveMappingCandidates( @WebParam(name="ExternalEntityHeaders") Collection<ExternalEntityHeader> mappables,
                                                            @WebParam(name="Scope") ExternalEntityHeader scope,
                                                            @WebParam(name="Filter") @XmlJavaTypeAdapter(JaxbMapType.JaxbMapTypeAdapter.class) Map<String,String> filters ) throws MigrationException;

    /**
     * Imports the given MigrationBundle into the current Cluster.
     *
     * @param bundle        The MigrationBundle to be imported.
     * @param dryRun        If true only an import simulation is performed, with no modifications to the target Cluster.
     *                      This allows for some errors to be detected early and for a summary with the operations
     *                      about to be performed to be presented to the user for confirmation.
     * @return              A collection of MigratedItem's, representing the summary of the operations that were performed.
     *                      For each entity header from the import request that resulted in a modification on the target Cluster,
     *                      a target entity header is returned along with the actual operation performed.
     *
     * @see com.l7tech.server.management.migration.bundle.MigratedItem,
     *      com.l7tech.server.management.migration.bundle.MigratedItem.ImportOperation
     *
     * @throws MigrationException if the import operation failed for any reason. If the dryRun flag was set to false,
     *                            the target Cluster is not modified. If the dryRun flag was set to true and more than
     *                            one error was detected, the MigrationException will contain all of them. 
     *
     * @see com.l7tech.server.management.api.node.MigrationApi.MigrationException#getErrors()
     */
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
