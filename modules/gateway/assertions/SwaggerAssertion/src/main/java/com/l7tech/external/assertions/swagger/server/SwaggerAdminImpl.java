package com.l7tech.external.assertions.swagger.server;

import com.l7tech.console.action.DeleteEntityNodeAction;
import com.l7tech.external.assertions.swagger.SwaggerAdmin;
import com.l7tech.external.assertions.swagger.SwaggerApiMetadata;
import com.l7tech.external.assertions.swagger.SwaggerUtil;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.SwaggerParser;
import org.apache.commons.lang.WordUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import static com.l7tech.external.assertions.swagger.SwaggerAssertion.*;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SwaggerAdminImpl extends AsyncAdminMethodsImpl implements SwaggerAdmin {

    private final ApplicationContext applicationContext;

    public SwaggerAdminImpl(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public SwaggerApiMetadata retrieveApiMetadata(String url) throws InvalidSwaggerDocumentException {
        ServiceAdmin serviceAdmin = applicationContext.getBean("serviceAdmin", ServiceAdmin.class);

        String swaggerDocument;

        try {
            swaggerDocument = serviceAdmin.resolveUrlTarget(url, CPROP_SWAGGER_DOC_MAX_DOWNLOAD_SIZE);
        } catch (IOException e) {
            throw new InvalidSwaggerDocumentException(
                    WordUtils.wrap("Could not download Swagger document: " + ExceptionUtils.getMessage(e),
                                    DeleteEntityNodeAction.LINE_CHAR_LIMIT));
        }

        Swagger model = SwaggerUtil.parseSwaggerJson(swaggerDocument);

        // the parser returns null if it could not parse the document
        if (null == model) {
            throw new InvalidSwaggerDocumentException("The retrieved document could not be parsed. " +
                    "Please confirm that the URL refers to a valid Swagger 2.0 document.");
        }

        SwaggerApiMetadata metadata = new SwaggerApiMetadata();

        if (null != model.getInfo()) {
            metadata.setTitle(model.getInfo().getTitle());
        }

        metadata.setHost(model.getHost());
        metadata.setBasePath(model.getBasePath());

        return metadata;
    }

    @Override
    public JobId<SwaggerApiMetadata> retrieveApiMetadataAsync(final String url) {
        final FutureTask<SwaggerApiMetadata> task =
                new FutureTask<>(AdminInfo.find(false).wrapCallable(new Callable<SwaggerApiMetadata>() {
                    @Override
                    public SwaggerApiMetadata call() throws Exception {
                        return retrieveApiMetadata(url);
                    }
                }));

        Background.scheduleOneShot(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, 0L);

        return registerJob(task, SwaggerApiMetadata.class);
    }
}
