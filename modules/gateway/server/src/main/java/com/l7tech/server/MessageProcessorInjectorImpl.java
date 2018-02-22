package com.l7tech.server;

import com.ca.apim.gateway.extension.processorinjection.ServiceInjection;
import com.ca.apim.gateway.extension.processorinjection.ServiceInjectionContext;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.extension.registry.processorinjection.ServiceInjectionsRegistry;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.stream.Stream;

public class MessageProcessorInjectorImpl implements MessageProcessorInjector {

    private final ServiceInjectionsRegistry preServiceInvocationInjectionsRegistry;
    private final ServiceInjectionsRegistry postServiceInvocationInjectionsRegistry;

    public MessageProcessorInjectorImpl(
            @NotNull final ServiceInjectionsRegistry preServiceInvocationInjectionsRegistry,
            @NotNull final ServiceInjectionsRegistry postServiceInvocationInjectionsRegistry) {
        this.preServiceInvocationInjectionsRegistry = preServiceInvocationInjectionsRegistry;
        this.postServiceInvocationInjectionsRegistry = postServiceInvocationInjectionsRegistry;
    }

    @Override
    public void executePreServiceInjections(@NotNull final PolicyEnforcementContext context) {
        executeInjections(preServiceInvocationInjectionsRegistry, context);
    }

    @Override
    public void executePostServiceInjections(@NotNull final PolicyEnforcementContext context) {
        executeInjections(postServiceInvocationInjectionsRegistry, context);
    }

    /**
     * Finds the needed service injectors from the given registry by looking at tags on the service associated with the given context. Then runs those injectors
     *
     * @param serviceInjectionsRegistry The service injection registry to search
     * @param context                   The context to look up the service tags from and to inject into.
     */
    private static void executeInjections(@NotNull final ServiceInjectionsRegistry serviceInjectionsRegistry, @NotNull final PolicyEnforcementContext context) {
        final Stream<ServiceInjection> preServiceInvocationExtensions = Stream.concat(
                serviceInjectionsRegistry.getTaggedExtensions(getServiceTags(context.getService())).stream(),
                serviceInjectionsRegistry.getTaggedExtensions(ServiceInjectionsRegistry.GLOBAL_TAG).stream())
                .distinct();
        preServiceInvocationExtensions.forEach(new Consumer<ServiceInjection>() {
            @Override
            public void accept(ServiceInjection preServiceInvocationExtension) {
                preServiceInvocationExtension.execute(getServiceInjectionContext(context));
            }
        });
    }

    /**
     * Returns an injectable context in that injects into the given PEC
     *
     * @param context The context to inject into
     * @return The injectable context
     */
    @NotNull
    private static ServiceInjectionContext getServiceInjectionContext(@NotNull final PolicyEnforcementContext context) {
        return new ServiceInjectionContext() {
            @Override
            public Object getVariable(String name) {
                try {
                    return context.getVariable(name);
                } catch (NoSuchVariableException e) {
                    return null;
                }
            }

            @Override
            public void setVariable(String name, Object value) {
                context.setVariable(name, value);
            }
        };
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
