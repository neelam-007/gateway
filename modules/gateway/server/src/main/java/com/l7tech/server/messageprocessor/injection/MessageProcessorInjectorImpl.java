package com.l7tech.server.messageprocessor.injection;

import com.ca.apim.gateway.extension.processorinjection.ServiceInjection;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.server.extension.registry.processorinjection.ServiceInjectionsRegistry;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class MessageProcessorInjectorImpl implements MessageProcessorInjector {
    private static final Logger LOGGER = Logger.getLogger(MessageProcessorInjectorImpl.class.getName());

    private final ServiceInjectionsRegistry preServiceInvocationInjectionsRegistry;
    private final ServiceInjectionsRegistry postServiceInvocationInjectionsRegistry;

    public MessageProcessorInjectorImpl(
            @NotNull final ServiceInjectionsRegistry preServiceInvocationInjectionsRegistry,
            @NotNull final ServiceInjectionsRegistry postServiceInvocationInjectionsRegistry) {
        this.preServiceInvocationInjectionsRegistry = preServiceInvocationInjectionsRegistry;
        this.postServiceInvocationInjectionsRegistry = postServiceInvocationInjectionsRegistry;
    }

    @Override
    public boolean executePreServiceInjections(@NotNull final PolicyEnforcementContext context) {
        return executeInjections(preServiceInvocationInjectionsRegistry, context);
    }

    @Override
    public boolean executePostServiceInjections(@NotNull final PolicyEnforcementContext context) {
        return executeInjections(postServiceInvocationInjectionsRegistry, context);
    }

    /**
     * Finds the needed service injectors from the given registry by looking at tags on the service associated with the given context. Then runs those injectors
     *
     * @param serviceInjectionsRegistry The service injection registry to search
     * @param context                   The context to look up the service tags from and to inject into.
     */
    static boolean executeInjections(@NotNull final ServiceInjectionsRegistry serviceInjectionsRegistry, @NotNull final PolicyEnforcementContext context) {
        final Stream<ServiceInjection> serviceInvocationExtensions = Stream.concat(
                serviceInjectionsRegistry.getTaggedExtensions(getServiceTags(context.getService())).stream(),
                serviceInjectionsRegistry.getTaggedExtensions(ServiceInjectionsRegistry.GLOBAL_TAG).stream())
                .distinct();
        final ServiceInjectionContextImpl serviceInjectionContext = new ServiceInjectionContextImpl(context);
        return serviceInvocationExtensions.reduce(Boolean.TRUE, new BiFunction<Boolean, ServiceInjection, Boolean>() {
            @Override
            public Boolean apply(Boolean shouldContinue, ServiceInjection serviceInjection) {
                try {
                    return serviceInjection.execute(serviceInjectionContext) && shouldContinue;
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Service injection failed to execute. Ignoring and continuing. Message: " + ExceptionUtils.getMessageWithCause(e), ExceptionUtils.getDebugException(e));
                }
                return shouldContinue;
            }
        }, new BinaryOperator<Boolean>() {
            @Override
            public Boolean apply(Boolean b1, Boolean b2) {
                return b1 && b2;
            }
        });
    }

    /**
     * Looks up the 'tags' property on the service and returns the array of all tags seperated by a comma
     *
     * @param service The service to look up tags on
     * @return All tags for the service or the empty array if there are non
     */
    @NotNull
    private static String[] getServiceTags(@Nullable final PublishedService service) {
        if (service != null) {
            final String tags = service.getProperty("tags");
            return tags == null ? new String[0] : tags.split(",");
        } else {
            return new String[0];
        }
    }
}
