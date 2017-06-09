package com.l7tech.external.assertions.quickstarttemplate.server.policy;

import com.google.common.annotations.VisibleForTesting;
import com.l7tech.external.assertions.quickstarttemplate.QuickStartTemplateAssertion;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.QuickStartParser;
import com.l7tech.external.assertions.quickstarttemplate.server.parser.ServiceContainer;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.Policy;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.exc.UnrecognizedPropertyException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

/**
 * Utility class for installing JSON file in the gatewa
 */
public class QuickStartJsonServiceInstaller {
    private static final Logger logger = Logger.getLogger(QuickStartJsonServiceInstaller.class.getName());

    private static final String BOOTSTRAP_FOLDER = ConfigFactory.getProperty("bootstrap.folder");
    private static final String BOOTSTRAP_QST_FOLDER_PREFIX = "qs";
    private static final String JSON_SERVICE_FILES_GLOB = "*.json";

    private final QuickStartServiceBuilder serviceBuilder;
    private final ServiceManager serviceManager;
    private final PolicyVersionManager policyVersionManager;
    private final QuickStartParser parser;

    public QuickStartJsonServiceInstaller(
            @NotNull final QuickStartServiceBuilder serviceBuilder,
            @NotNull final ServiceManager serviceManager,
            @NotNull final PolicyVersionManager policyVersionManager,
            @NotNull final QuickStartParser parser
    ) {
        this.serviceBuilder = serviceBuilder;
        this.serviceManager = serviceManager;
        this.policyVersionManager = policyVersionManager;
        this.parser = parser;
    }

    /**
     * Used for test coverage.
     * Should NOT be used elsewhere.
     */
    @VisibleForTesting
    @NotNull
    Path getBootstrapFolder() throws InvalidPathException {
        assert BOOTSTRAP_FOLDER != null;
        return Paths.get(BOOTSTRAP_FOLDER, BOOTSTRAP_QST_FOLDER_PREFIX);
    }

    /**
     * Tag installing json exception
     */
    private static abstract class InstallException extends Exception {
        InstallException(@NotNull final String message, @NotNull final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Indicates that an error happen while parsing json payload
     */
    static class ParseJsonPayload extends InstallException {
        ParseJsonPayload(@NotNull final String message, @NotNull final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Indicates that an error happen while creating {@link PublishedService service} out of {@link ServiceContainer parsed json object}.
     */
    static class CreateServiceException extends InstallException {
        CreateServiceException(@NotNull final String message, @NotNull final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Indicates that an error happen while saving the {@link PublishedService service} object.
     */
    static class SaveServiceException extends InstallException {
        SaveServiceException(@NotNull final String message, @NotNull final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Loops through all (*.json) files inside the {@code qs-bootstrap} folder and installs them one by one.
     */
    public void installJsonServices() {
        try {
            final Path jsonFolder = getBootstrapFolder();
            try (final DirectoryStream<Path> stream = Files.newDirectoryStream(jsonFolder, JSON_SERVICE_FILES_GLOB)) {
                logger.info("installing JSON services from bootstrap folder: " + jsonFolder);
                AdminInfo.find(false).wrapCallable((Callable<Void>) () -> {
                    StreamSupport.stream(stream.spliterator(), false)
                            .sorted((path1, path2) -> path1.getFileName().compareTo(path2.getFileName()))
                            .forEach(this::installJsonService);
                    return null;
                }).call();
            } catch (NotDirectoryException | NoSuchFileException e) {
                handleError(Level.FINE, "JSON services bootstrap folder \"" + jsonFolder + "\" doesn't exist", e);
            } catch (IOException e) {
                handleError("Failed to read content of JSON services bootstrap folder: \"" + jsonFolder + "\": " + ExceptionUtils.getMessage(e), e);
            } catch (Throwable e) {
                handleError("Failed to install JSON services: " + ExceptionUtils.getMessageWithCause(e), e);
            }
        } catch (InvalidPathException e) {
            handleError("Invalid JSON services bootstrap folder path : \"" + BOOTSTRAP_FOLDER + File.separator + BOOTSTRAP_QST_FOLDER_PREFIX + "\"", e);
        }
    }

    /**
     * Installs the specified {@code jsonFile}.
     * <p/>
     * Package visible for testing, should not be invoked from outside.
     *
     * @param jsonFile    a {@link Path} to the {@code json} file to install. Mandatory and cannot be {@code null}.
     */
    @VisibleForTesting
    void installJsonService(@NotNull final Path jsonFile) {
        assert !Files.isDirectory(jsonFile) && Files.isRegularFile(jsonFile);

        logger.info("installing JSON service file: " + jsonFile);
        try (final InputStream stream = new BufferedInputStream(Files.newInputStream(jsonFile, StandardOpenOption.READ))) {
            installJsonService(stream);
            logger.info("Successfully installed JSON service file: " + jsonFile);
        } catch (IOException e) {
            handleError("Error while reading JSON service file: \"" + jsonFile + "\": " + ExceptionUtils.getMessage(e), e);
        } catch (InstallException e) {
            final Throwable cause = e.getCause();
            if (cause != null) {
                handleError(ExceptionUtils.getMessage(e), cause);
            }
            handleError("Failed to install JSON service file: " + jsonFile, e);
        }
    }

    /**
     * Installs the {@code json} payload specified with the {@code fileStream}.
     *
     * @param fileStream    {@code InputStream} to the {@code json} file to install. Mandatory and cannot be {@code null}.
     *
     * @throws InstallException if an error happens while installing {@code json} payload.
     */
    private void installJsonService(@NotNull final InputStream fileStream) throws InstallException {
        final ServiceContainer serviceContainer = parseJsonPayload(fileStream);
        final PublishedService service = createService(serviceContainer);
        saveService(service);
    }

    /**
     * Parse the {@code json} payload, specified with the {@code fileStream}, and return a {@link ServiceContainer service container pojo}.
     *
     * @param fileStream    {@code InputStream} to the {@code json} file to install. Mandatory and cannot be {@code null}.
     * @return {@link ServiceContainer service container pojo}, never {@code null}.
     * @throws ParseJsonPayload if an error occurs while parsing the {@code json} payload.
     */
    @NotNull
    private ServiceContainer parseJsonPayload(@NotNull final InputStream fileStream) throws ParseJsonPayload {
        try {
            return parser.parseJson(fileStream);
        } catch (UnrecognizedPropertyException e) {
            throw new ParseJsonPayload("Unrecognized property \"" + e.getUnrecognizedPropertyName() + "\" for object \"" + e.getReferringClass().getSimpleName() + "\"", e);
        } catch (JsonMappingException e) {
            final IllegalArgumentException arg = ExceptionUtils.getCauseIfCausedBy(e, IllegalArgumentException.class);
            if (arg != null) {
                throw new ParseJsonPayload(ExceptionUtils.getMessage(arg), e);
            }
            throw new ParseJsonPayload(ExceptionUtils.getMessage(e), e);
        } catch (IOException e) {
            throw new ParseJsonPayload("Unable to parse JSON service payload: " + ExceptionUtils.getMessage(e), e);
        } catch (Exception e) {
            throw new ParseJsonPayload("Unhandled exception while parsing JSON service payload: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Create a {@link PublishedService service} from the specified {@code serviceContainer} object.
     *
     * @param serviceContainer    input {@link ServiceContainer service container pojo}. Mandatory and cannot be {@code null}.
     * @return {@link PublishedService service} object, never {@code null}.
     * @throws CreateServiceException if an error occurs while creating the {@link PublishedService service} object.
     */
    @NotNull
    private PublishedService createService(@NotNull final ServiceContainer serviceContainer) throws CreateServiceException {
        try {
            final PublishedService service = serviceBuilder.createService(serviceContainer);
            service.putProperty(QuickStartTemplateAssertion.PROPERTY_QS_CREATE_METHOD, String.valueOf(QuickStartTemplateAssertion.QsServiceCreateMethod.BOOTSTRAP));
            return service;
        } catch (QuickStartPolicyBuilderException e) {
            throw new CreateServiceException(ExceptionUtils.getMessage(e), e);
        } catch (FindException e) {
            throw new CreateServiceException("Unable to create service: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Save the specified {@link PublishedService service} into the {@code Gateway}, using {@code Gateway} api i.e. {@link ServiceManager}.
     *
     * @param service    {@link PublishedService service} object to save. Mandatory and cannot be {@code null}.
     * @throws SaveServiceException if an error occurs while saving the {@link PublishedService service} into the {@code Gateway}.
     */
    private void saveService(@NotNull final PublishedService service) throws SaveServiceException {
        final Policy policy = service.getPolicy();
        try {
            serviceManager.save(service);
            if (policy != null) {
                policyVersionManager.checkpointPolicy(policy, true, true);
            }
            serviceManager.createRoles(service);
        } catch (ObjectModelException e) {
            throw new SaveServiceException("Unable to save service: " + ExceptionUtils.getMessage(e), e);
        }
    }

    /**
     * Exception handler, which for now only loges the exception.
     * <p/>
     * Package visible for testing, should not be invoked from outside.
     *
     * @param level      level.  Mandatory and cannot be {@code null}.
     * @param message    message.  Mandatory and cannot be {@code null}.
     * @param e          exception. Optional and can be {@code null}.
     */
    @VisibleForTesting
    void handleError(@NotNull final Level level, @NotNull final String message, @Nullable final Throwable e) {
        logger.log(level, message, ExceptionUtils.getDebugException(e));
    }

    /**
     * Convenient method with {@link Level#WARNING warning} level.
     *
     * @param message    message.  Mandatory and cannot be {@code null}.
     * @param e          exception. Optional and can be {@code null}.
     *
     * @see #handleError(Level, String, Throwable)
     */
    private void handleError(@NotNull final String message, @Nullable final Throwable e) {
        handleError(Level.WARNING, message, e);
    }
}
