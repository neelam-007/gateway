package org.odata4j.producer.resources;

import org.odata4j.core.Guid;
import org.odata4j.core.ODataConstants;
import org.odata4j.producer.ODataProducer;

import javax.ws.rs.QueryParam;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.ContextResolver;
import java.io.InputStream;
import java.util.List;

import static javax.ws.rs.core.Response.status;

/**
 * Extend OData4j entities request resource provider on the same package as we need access to the private/protected method of
 *
 * @author rraquepo, 8/22/13
 */
public class ODataEntitiesRequestResource extends EntitiesRequestResource {
    public static final String BATCH_FAILED_MESSAGE = "Cannot process due to previous error(s). Transaction and/or Fast fail flag is probably to true";
    private static ODataEntitiesRequestResource instance;

    public static ODataEntitiesRequestResource getInstance() {
        if (instance == null) {
            instance = new ODataEntitiesRequestResource();
        }
        return instance;
    }

    public Response processBatch(
            @Context ContextResolver<ODataProducer> producerResolver,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfoTop,
            @Context Request request,
            @QueryParam("$format") String format,
            @QueryParam("$callback") String callback,
            InputStream payload, TransValueHolder valueHolder) throws Exception {
        ODataBatchProviderExtension oDataBatchProvider = new ODataBatchProviderExtension();
        List<BatchBodyPart> bodyParts = oDataBatchProvider.readFrom(headers, uriInfoTop, payload);
        //return super.processBatch(producerResolver, headers, null, format, callback, bodyParts);
        EntityRequestResource er = new EntityRequestResource();

        String changesetBoundary = "changesetresponse_"
                + Guid.randomGuid().toString();
        String batchBoundary = "batchresponse_" + Guid.randomGuid().toString();
        StringBuilder batchResponse = new StringBuilder("\n--");
        batchResponse.append(batchBoundary);

        batchResponse
                .append("\n").append(ODataConstants.Headers.CONTENT_TYPE).append(": multipart/mixed; boundary=")
                .append(changesetBoundary);

        batchResponse.append('\n');

        ODataProducer producer = producerResolver.getContext(ODataProducer.class);

        boolean hasError = false;
        int ctr = 0;
        for (BatchBodyPart bodyPart : bodyParts) {
            HttpHeaders httpHeaders = bodyPart.getHttpHeaders();
            UriInfo uriInfo = bodyPart.getUriInfo();
            String entitySetName = bodyPart.getEntitySetName();
            String entityId = bodyPart.getEntityKey();
            String entityString = bodyPart.getEntity();
            Response response = null;

            if (hasError && valueHolder.isFastFail()) {
                Response.ResponseBuilder b = status(Response.Status.PRECONDITION_FAILED);
                b.entity(BATCH_FAILED_MESSAGE);
                response = b.build();
            } else {
                switch (bodyPart.getHttpMethod()) {
                    case POST:
                        try {
                            response = this.createEntity(httpHeaders, uriInfo, producer,
                                    entitySetName,
                                    getRequestEntity(httpHeaders, uriInfo, entityString, producer.getMetadata(), entitySetName, null));

                        } catch (Exception e) {
                            //response = Response.serverError().build();
                            Response.ResponseBuilder b = status(Response.Status.BAD_REQUEST);
                            b.entity(e.getMessage());
                            response = b.build();
                            hasError = true;
                        }
                        break;
                    case PUT:
                        try {
                            response = er.updateEntity(httpHeaders, uriInfo, producerResolver,
                                    entitySetName, entityId, entityString);
                        } catch (Exception e) {
                            //response = Response.serverError().build();
                            Response.ResponseBuilder b = status(Response.Status.BAD_REQUEST);
                            b.entity(e.getMessage());
                            response = b.build();
                            hasError = true;
                        }
                        break;
                    case MERGE:
                        //response = er.mergeEntity(httpHeaders, uriInfo, producerResolver, entitySetName,
                        //entityId, entityString);
                        //UnsupportedOperationException("Not supported yet.");
                        Response.ResponseBuilder b2 = status(Response.Status.BAD_REQUEST);
                        b2.entity("Not Implemented");
                        response = b2.build();
                        break;
                    case DELETE:
                        try {
                            response = er.deleteEntity(httpHeaders, uriInfo, producerResolver, format, callback, entitySetName, entityId);
                        } catch (Exception e) {
                            //response = Response.serverError().build();
                            Response.ResponseBuilder b = status(Response.Status.BAD_REQUEST);
                            b.entity(e.getMessage());
                            response = b.build();
                            hasError = true;
                        }
                        break;
                    case GET:
                        Response.ResponseBuilder b3 = status(Response.Status.BAD_REQUEST);
                        b3.entity("Not Implemented");
                        response = b3.build();
                        break;
                }
            }

            batchResponse.append("\n--").append(changesetBoundary);
            batchResponse.append("\n").append(ODataConstants.Headers.CONTENT_TYPE).append(": application/http");
            batchResponse.append("\nContent-Transfer-Encoding: binary\n");

            batchResponse.append(ODataBatchProvider.createResponseBodyPart(
                    bodyPart,
                    response));

            ctr++;
            if (ctr >= bodyParts.size()) {
                valueHolder.setLastOperationBody("");
                valueHolder.setLastOperationEntityName(entitySetName);
                if (entityId != null) {
                    valueHolder.setLastOperationEntityId(parse(entityId));
                }
                valueHolder.setLastOperationMethod(bodyPart.getHttpMethod().name());
                valueHolder.setLastOperationPayload(entityString);
                if (response != null) {
                    valueHolder.setLastOperationStatus(String.valueOf(response.getStatus()));
                    if (response.getEntity() != null) {
                        valueHolder.setLastOperationBody(response.getEntity().toString());
                    }
                }
            }
        }
        valueHolder.setBatchCount(ctr);
        if (hasError) {
            valueHolder.setHasError(true);
        }

        batchResponse.append("--").append(changesetBoundary).append("--\n");
        batchResponse.append("--").append(batchBoundary).append("--\n");

        return
                status(Response.Status.ACCEPTED)
                        .type(ODataBatchProvider.MULTIPART_MIXED + ";boundary="
                                + batchBoundary).header(
                        ODataConstants.Headers.DATA_SERVICE_VERSION,
                        ODataConstants.DATA_SERVICE_VERSION_HEADER)
                        .entity(batchResponse.toString()).build();
    }

    protected String parse(final String entityId) {
        int squote_start = entityId.indexOf("'");
        if (squote_start > 0) {
            int squote_end = entityId.lastIndexOf("'");
            return entityId.substring(squote_start + 1, squote_end);
        }
        return entityId;
    }


}
