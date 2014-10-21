package com.l7tech.external.assertions.gatewaymanagement.tools;

import com.l7tech.util.Functions;
import com.sun.research.ws.wadl.*;
import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * This test verifies that the wadl doesn't change from the previously released versions.
 */
public class WadlPastVersionTest {

    @Test
    public void version1_0_0Test() throws JAXBException, FileNotFoundException {
        Application generatedApplicationWadl = getApplicationWadl(WadlPastVersionTest.class.getResourceAsStream("/com/l7tech/external/assertions/gatewaymanagement/server/rest/resource/restAPI_1.0.0.wadl"));
        Application applicationWadl1_0_0 = getApplicationWadl(new FileInputStream(new File("modules/gateway/assertions/GatewayManagementAssertion/src/test/resources/com/l7tech/external/assertions/gatewaymanagement/server/restAPI_1.0.0.wadl")));

        compareWadls(applicationWadl1_0_0, generatedApplicationWadl);

    }

    private void compareWadls(Application originalWadl, Application generatedWadl) {
        //check wadl grammars
        Assert.assertEquals("Different number of grammars", originalWadl.getGrammars().getInclude().size(), generatedWadl.getGrammars().getInclude().size());
        for (final Include include : originalWadl.getGrammars().getInclude()) {
            Assert.assertTrue("Missing grammar include: " + include.getHref(), Functions.exists(generatedWadl.getGrammars().getInclude(), new Functions.Unary<Boolean, Include>() {
                @Override
                public Boolean call(Include generatedInclude) {
                    return include.getHref().equals(generatedInclude.getHref());
                }
            }));
        }

        //check resources
        Assert.assertEquals("The number of resources is expected to be one", 1, generatedWadl.getResources().size());
        Assert.assertEquals("Different number of resources", originalWadl.getResources().get(0).getResource().size(), generatedWadl.getResources().get(0).getResource().size());
        for (final Resource resource : originalWadl.getResources().get(0).getResource()) {
            //check resource
            Resource generatedResource = Functions.grepFirst(generatedWadl.getResources().get(0).getResource(), new Functions.Unary<Boolean, Resource>() {
                @Override
                public Boolean call(Resource generatedResource) {
                    return resource.getPath().equals(generatedResource.getPath());
                }
            });

            Assert.assertNotNull("Missing Resource for path: " + resource.getPath(), generatedResource);
            assertResourceEquals(null, resource, generatedResource);
        }
    }

    private void assertResourceEquals(@Nullable String owner, @NotNull Resource resource, @NotNull Resource generatedResource) {
        Assert.assertEquals(resource.getQueryType(), generatedResource.getQueryType());
        assertParametersEqual(resource.getPath(), resource.getParam(), generatedResource.getParam());

        for (final Object methodOrResource : resource.getMethodOrResource()) {
            if (methodOrResource instanceof Resource) {
                final Resource subResource = (Resource) methodOrResource;
                Resource generatedSubResource = (Resource) Functions.grepFirst(generatedResource.getMethodOrResource(), new Functions.Unary<Boolean, Object>() {
                    @Override
                    public Boolean call(Object o) {
                        return o instanceof Resource && subResource.getPath().equals(((Resource) o).getPath());
                    }
                });
                Assert.assertNotNull("Missing Resource for path: " + resource.getPath() + (owner != null ? " in '" + owner + "'" : "") + resource.getPath() + subResource.getPath(), generatedResource);
                assertResourceEquals(owner + resource.getPath() + subResource.getPath(), subResource, generatedSubResource);
            } else if (methodOrResource instanceof Method) {
                final Method method = (Method) methodOrResource;
                Method generatedMethod = (Method) Functions.grepFirst(generatedResource.getMethodOrResource(), new Functions.Unary<Boolean, Object>() {
                    @Override
                    public Boolean call(Object o) {
                        return o instanceof Method && method.getName().equals(((Method) o).getName());
                    }
                });
                Assert.assertNotNull("Missing Method: " + method.getName() + (owner != null ? " in '" + owner + "'" : "") + resource.getPath(), generatedResource);
                assertMethodEquals((owner != null ? owner : "") + resource.getPath(), method, generatedMethod);
            }
        }
    }

    private void assertMethodEquals(@Nullable String owner, @NotNull Method method, @NotNull Method generatedMethod) {
        Assert.assertEquals("Different method names for :" + owner, method.getName(), generatedMethod.getName());
        if (method.getRequest() == null) {
            Assert.assertNull("The method request was expected to be null for method: " + method.getName() + " on " + owner, generatedMethod.getRequest());
        } else {
            assertParametersEqual("method " + method.getName() + " on " + owner, method.getRequest().getParam(), generatedMethod.getRequest().getParam());
            assertRepresentationEquals("method " + method.getName() + " on " + owner, method.getRequest().getRepresentation(), generatedMethod.getRequest().getRepresentation());
            assertResponsesEqual("method " + method.getName() + " on " + owner, method.getResponse(), generatedMethod.getResponse());
        }
    }

    private void assertResponsesEqual(String responseOwner, List<Response> responses, List<Response> generatedResponses) {
        Assert.assertEquals("different number of responses for " + responseOwner, responses.size(), generatedResponses.size());
        for (final Response response : responses) {
            Response generatedResponse = Functions.grepFirst(generatedResponses, new Functions.Unary<Boolean, Response>() {
                @Override
                public Boolean call(final Response generatedResponse) {
                    return Functions.forall(response.getStatus(), new Functions.Unary<Boolean, Long>() {
                        @Override
                        public Boolean call(final Long status) {
                            return Functions.exists(generatedResponse.getStatus(), new Functions.Unary<Boolean, Long>() {
                                @Override
                                public Boolean call(Long generatedStatus) {
                                    return status.equals(generatedStatus);
                                }
                            });
                        }
                    });
                }
            });

            Assert.assertNotNull("Missing response for status " + response.getStatus() + " in " + responseOwner, generatedResponse);
            assertParametersEqual("response for status '" + response.getStatus() + " in " + responseOwner, response.getParam(), generatedResponse.getParam());
            assertRepresentationEquals("response for status '" + response.getStatus() + " in " + responseOwner, response.getRepresentation(), generatedResponse.getRepresentation());

        }
    }

    private void assertRepresentationEquals(String representationOwner, List<Representation> representations, List<Representation> generatedRepresentations) {
        Assert.assertEquals("different number of representations for " + representationOwner, representations.size(), generatedRepresentations.size());
        for (final Representation representation : representations) {
            Representation generatedRepresentation = Functions.grepFirst(generatedRepresentations, new Functions.Unary<Boolean, Representation>() {
                @Override
                public Boolean call(Representation generatedParam) {
                    return (representation.getElement() != null ? representation.getElement().equals(generatedParam.getElement()) : generatedParam.getElement() == null)
                            && StringUtils.equals(representation.getMediaType(), generatedParam.getMediaType());
                }
            });

            Assert.assertNotNull("Missing representation for element '" + representation.getElement() + "' with media type '" + representation.getMediaType() + "' in " + representationOwner, generatedRepresentation);
            assertParametersEqual("representation for element '" + representation.getElement() + "' with media type '" + representation.getMediaType() + "' on " + representationOwner, representation.getParam(), generatedRepresentation.getParam());
        }
    }

    private void assertParametersEqual(String paramsOwner, List<Param> params, List<Param> generatedParams) {
        Assert.assertEquals("different number of parameters for " + paramsOwner, params.size(), generatedParams.size());
        for (final Param param : params) {
            Param generatedParam = Functions.grepFirst(generatedParams, new Functions.Unary<Boolean, Param>() {
                @Override
                public Boolean call(Param generatedParam) {
                    return param.getName().equals(generatedParam.getName());
                }
            });

            Assert.assertNotNull("Missing parameter '" + param.getName() + "' in " + paramsOwner, generatedParam);
            Assert.assertEquals(param.getPath(), generatedParam.getPath());
            Assert.assertEquals(param.getDefault(), generatedParam.getDefault());
            Assert.assertEquals(param.getFixed(), generatedParam.getFixed());
            Assert.assertEquals(param.getStyle(), generatedParam.getStyle());
            Assert.assertEquals(param.getType(), generatedParam.getType());

            for (final Option option : param.getOption()) {
                Option generatedOption = Functions.grepFirst(generatedParam.getOption(), new Functions.Unary<Boolean, Option>() {
                    @Override
                    public Boolean call(Option generatedOption) {
                        return option.getValue().equals(generatedOption.getValue());
                    }
                });

                Assert.assertNotNull("Missing parameter option '" + option.getValue() + "' on param '" + param.getName() + "' in " + paramsOwner, generatedOption);
                Assert.assertEquals(option.getMediaType(), generatedOption.getMediaType());
            }
        }
    }

    private Application getApplicationWadl(InputStream wadlStream) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Application.class);
        StreamSource source = new StreamSource(wadlStream);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return unmarshaller.unmarshal(source, Application.class).getValue();
    }
}
