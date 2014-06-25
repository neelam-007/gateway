package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APITransformer;
import com.l7tech.gateway.api.*;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * These are utilities that are used by the Rest Entity Resources. The mostly involve creating Item objects
 */
public class RestEntityResourceUtils {

    /**
     * Creates a template item given a template resource
     *
     * @param resource      The template resource
     * @param urlAccessible The urlAccessible associated with this resource
     * @param templateUrl   The template uri used to retrieve this template
     * @param <R>           The resource type
     * @return The template item
     */
    public static <R> Item<R> createTemplateItem(@NotNull final R resource, @NotNull final URLAccessible<R> urlAccessible, @NotNull final String templateUrl) {
        return new ItemBuilder<R>(urlAccessible.getResourceType() + " Template", urlAccessible.getResourceType())
                .addLink(ManagedObjectFactory.createLink(Link.LINK_REL_SELF, templateUrl))
                .addLinks(Functions.reduce(urlAccessible.getRelatedLinks(null), new ArrayList<Link>(), new Functions.Binary<ArrayList<Link>, ArrayList<Link>, Link>() {
                    @Override
                    public ArrayList<Link> call(ArrayList<Link> links, Link link) {
                        if (Link.LINK_REL_TEMPLATE.equals(link.getRel())) {
                            return links;
                        } else {
                            links.add(link);
                            return links;
                        }
                    }
                }))
                .setContent(resource)
                .build();
    }

    /**
     * Created an items list given a list of resources
     *
     * @param resources     The list of resources to create the items list from
     * @param transformer   A transformer used to transform these resources to items
     * @param urlAccessible The url accessible for this resource type
     * @param selfUrl       The url used to create this list
     * @param <R>           The type of resource
     * @return The items list of the given resources
     */
    public static <R> ItemsList<R> createItemsList(@NotNull final List<R> resources, @NotNull final APITransformer<R, ?> transformer, @NotNull final URLAccessible<R> urlAccessible, @NotNull final String selfUrl) {
        //transform the individual items in the list.
        List<Item<R>> items = Functions.map(resources, new Functions.Unary<Item<R>, R>() {
            @Override
            public Item<R> call(R resource) {
                return new ItemBuilder<>(transformer.convertToItem(resource))
                        .addLink(urlAccessible.getLink(resource))
                        .build();
            }
        });
        return new ItemsListBuilder<R>(transformer.getResourceType() + " List", "List")
                .addLink(ManagedObjectFactory.createLink(Link.LINK_REL_SELF, selfUrl))
                .addLinks(Functions.reduce(urlAccessible.getRelatedLinks(null), new ArrayList<Link>(), new Functions.Binary<ArrayList<Link>, ArrayList<Link>, Link>() {
                    @Override
                    public ArrayList<Link> call(ArrayList<Link> links, Link link) {
                        if (Link.LINK_REL_LIST.equals(link.getRel())) {
                            return links;
                        } else {
                            links.add(link);
                            return links;
                        }
                    }
                }))
                .setContent(items)
                .build();
    }

    /**
     * Create a get response for a created resource
     *
     * @param resource      The resource that was created.
     * @param transformer   The transformer for this resource
     * @param urlAccessible The url accessible for this resource type
     * @param <R>           The type of resource
     * @return The item response.
     */
    public static <R> Item<R> createGetResponseItem(@NotNull final R resource, @NotNull final APITransformer<R, ?> transformer, @NotNull final URLAccessible<R> urlAccessible) {
        return new ItemBuilder<>(transformer.convertToItem(resource))
                .addLink(urlAccessible.getLink(resource))
                .addLinks(urlAccessible.getRelatedLinks(resource))
                .build();
    }

    /**
     * Create a create or update response for a created or updated resource
     *
     * @param resource      The resource that was created or updated.
     * @param transformer   The transformer for this resource
     * @param urlAccessible The url accessible for this resource type
     * @param created       true if the resource was created, false if it was updated
     * @param <R>           The type of resource
     * @return The item response.
     */
    public static <R> Response createCreateOrUpdatedResponseItem(R resource, APITransformer<R, ?> transformer, URLAccessible<R> urlAccessible, boolean created) {
        final Link selfLink = urlAccessible.getLink(resource);
        final Response.ResponseBuilder responseBuilder;
        if (created) {
            try {
                responseBuilder = Response.created(new URI(selfLink.getUri()));
            } catch (URISyntaxException e) {
                throw new WebApplicationException("Invalid URI built: " + selfLink, e);
            }
        } else {
            responseBuilder = Response.ok();
        }
        return responseBuilder.entity(new ItemBuilder<>(
                transformer.convertToItem(resource))
                .setContent(null)
                .addLink(selfLink)
                .addLinks(urlAccessible.getRelatedLinks(resource))
                .build())
                .build();
    }
}
