package com.l7tech.external.assertions.swagger.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.swagger.SwaggerAssertion;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import io.swagger.models.Path;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static com.l7tech.external.assertions.swagger.server.ServerSwaggerAssertion.PathDefinitionResolver;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test the SwaggerAssertion.
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerSwaggerAssertionTest {

    private static final String TEST_SWAGGER_JSON = "{\n" +
            "   \"swagger\":\"2.0\",\n" +
            "   \"info\":{\n" +
            "      \"description\":\"This is a sample server Petstore server.  You can find out more about Swagger at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/).  For this sample, you can use the api key `special-key` to test the authorization filters.\",\n" +
            "      \"version\":\"1.0.0\",\n" +
            "      \"title\":\"Swagger Petstore\",\n" +
            "      \"termsOfService\":\"http://swagger.io/terms/\",\n" +
            "      \"contact\":{\n" +
            "         \"email\":\"apiteam@swagger.io\"\n" +
            "      },\n" +
            "      \"license\":{\n" +
            "         \"name\":\"Apache 2.0\",\n" +
            "         \"url\":\"http://www.apache.org/licenses/LICENSE-2.0.html\"\n" +
            "      }\n" +
            "   },\n" +
            "   \"host\":\"petstore.swagger.io:8080\",\n" +
            "   \"basePath\":\"/v2\",\n" +
            "   \"tags\":[\n" +
            "      {\n" +
            "         \"name\":\"pet\",\n" +
            "         \"description\":\"Everything about your Pets\",\n" +
            "         \"externalDocs\":{\n" +
            "            \"description\":\"Find out more\",\n" +
            "            \"url\":\"http://swagger.io\"\n" +
            "         }\n" +
            "      },\n" +
            "      {\n" +
            "         \"name\":\"store\",\n" +
            "         \"description\":\"Access to Petstore orders\"\n" +
            "      },\n" +
            "      {\n" +
            "         \"name\":\"user\",\n" +
            "         \"description\":\"Operations about user\",\n" +
            "         \"externalDocs\":{\n" +
            "            \"description\":\"Find out more about our store\",\n" +
            "            \"url\":\"http://swagger.io\"\n" +
            "         }\n" +
            "      }\n" +
            "   ],\n" +
            "   \"schemes\":[\n" +
            "      \"http\"\n" +
            "   ],\n" +
            "   \"paths\":{\n" +
            "      \"/pet\":{\n" +
            "         \"post\":{\n" +
            "            \"tags\":[\n" +
            "               \"pet\"\n" +
            "            ],\n" +
            "            \"summary\":\"Add a new pet to the store\",\n" +
            "            \"description\":\"\",\n" +
            "            \"operationId\":\"addPet\",\n" +
            "            \"consumes\":[\n" +
            "               \"application/json\",\n" +
            "               \"application/xml\"\n" +
            "            ],\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"in\":\"body\",\n" +
            "                  \"name\":\"body\",\n" +
            "                  \"description\":\"Pet object that needs to be added to the store\",\n" +
            "                  \"required\":true,\n" +
            "                  \"schema\":{\n" +
            "                     \"$ref\":\"#/definitions/Pet\"\n" +
            "                  }\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"405\":{\n" +
            "                  \"description\":\"Invalid input\"\n" +
            "               }\n" +
            "            },\n" +
            "            \"security\":[\n" +
            "               {\n" +
            "                  \"petstore_auth\":[\n" +
            "                     \"write:pets\",\n" +
            "                     \"read:pets\"\n" +
            "                  ]\n" +
            "               }\n" +
            "            ]\n" +
            "         },\n" +
            "         \"put\":{\n" +
            "            \"tags\":[\n" +
            "               \"pet\"\n" +
            "            ],\n" +
            "            \"summary\":\"Update an existing pet\",\n" +
            "            \"description\":\"\",\n" +
            "            \"operationId\":\"updatePet\",\n" +
            "            \"consumes\":[\n" +
            "               \"application/json\",\n" +
            "               \"application/xml\"\n" +
            "            ],\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"in\":\"body\",\n" +
            "                  \"name\":\"body\",\n" +
            "                  \"description\":\"Pet object that needs to be added to the store\",\n" +
            "                  \"required\":true,\n" +
            "                  \"schema\":{\n" +
            "                     \"$ref\":\"#/definitions/Pet\"\n" +
            "                  }\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"400\":{\n" +
            "                  \"description\":\"Invalid ID supplied\"\n" +
            "               },\n" +
            "               \"404\":{\n" +
            "                  \"description\":\"Pet not found\"\n" +
            "               },\n" +
            "               \"405\":{\n" +
            "                  \"description\":\"Validation exception\"\n" +
            "               }\n" +
            "            },\n" +
            "            \"security\":[\n" +
            "               {\n" +
            "                  \"petstore_auth\":[\n" +
            "                     \"write:pets\",\n" +
            "                     \"read:pets\"\n" +
            "                  ]\n" +
            "               }\n" +
            "            ]\n" +
            "         }\n" +
            "      },\n" +
            "      \"/pet/findByStatus\":{\n" +
            "         \"get\":{\n" +
            "            \"tags\":[\n" +
            "               \"pet\"\n" +
            "            ],\n" +
            "            \"summary\":\"Finds Pets by status\",\n" +
            "            \"description\":\"Multiple status values can be provided with comma seperated strings\",\n" +
            "            \"operationId\":\"findPetsByStatus\",\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"name\":\"status\",\n" +
            "                  \"in\":\"query\",\n" +
            "                  \"description\":\"Status values that need to be considered for filter\",\n" +
            "                  \"required\":true,\n" +
            "                  \"type\":\"array\",\n" +
            "                  \"items\":{\n" +
            "                     \"type\":\"string\",\n" +
            "                     \"enum\":[\n" +
            "                        \"available\",\n" +
            "                        \"pending\",\n" +
            "                        \"sold\"\n" +
            "                     ],\n" +
            "                     \"default\":\"available\"\n" +
            "                  },\n" +
            "                  \"collectionFormat\":\"csv\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"200\":{\n" +
            "                  \"description\":\"successful operation\",\n" +
            "                  \"schema\":{\n" +
            "                     \"type\":\"array\",\n" +
            "                     \"items\":{\n" +
            "                        \"$ref\":\"#/definitions/Pet\"\n" +
            "                     }\n" +
            "                  }\n" +
            "               },\n" +
            "               \"400\":{\n" +
            "                  \"description\":\"Invalid status value\"\n" +
            "               }\n" +
            "            },\n" +
            "            \"security\":[\n" +
            "               {\n" +
            "                  \"petstore_auth\":[\n" +
            "                     \"write:pets\",\n" +
            "                     \"read:pets\"\n" +
            "                  ]\n" +
            "               }\n" +
            "            ]\n" +
            "         }\n" +
            "      },\n" +
            "      \"/pet/findByTags\":{\n" +
            "         \"get\":{\n" +
            "            \"tags\":[\n" +
            "               \"pet\"\n" +
            "            ],\n" +
            "            \"summary\":\"Finds Pets by tags\",\n" +
            "            \"description\":\"Muliple tags can be provided with comma seperated strings. Use tag1, tag2, tag3 for testing.\",\n" +
            "            \"operationId\":\"findPetsByTags\",\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"name\":\"tags\",\n" +
            "                  \"in\":\"query\",\n" +
            "                  \"description\":\"Tags to filter by\",\n" +
            "                  \"required\":true,\n" +
            "                  \"type\":\"array\",\n" +
            "                  \"items\":{\n" +
            "                     \"type\":\"string\"\n" +
            "                  },\n" +
            "                  \"collectionFormat\":\"csv\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"200\":{\n" +
            "                  \"description\":\"successful operation\",\n" +
            "                  \"schema\":{\n" +
            "                     \"type\":\"array\",\n" +
            "                     \"items\":{\n" +
            "                        \"$ref\":\"#/definitions/Pet\"\n" +
            "                     }\n" +
            "                  }\n" +
            "               },\n" +
            "               \"400\":{\n" +
            "                  \"description\":\"Invalid tag value\"\n" +
            "               }\n" +
            "            },\n" +
            "            \"security\":[\n" +
            "               {\n" +
            "                  \"petstore_auth\":[\n" +
            "                     \"write:pets\",\n" +
            "                     \"read:pets\"\n" +
            "                  ]\n" +
            "               }\n" +
            "            ]\n" +
            "         }\n" +
            "      },\n" +
            "      \"/pet/{petId}\":{\n" +
            "         \"get\":{\n" +
            "            \"tags\":[\n" +
            "               \"pet\"\n" +
            "            ],\n" +
            "            \"summary\":\"Find pet by ID\",\n" +
            "            \"description\":\"Returns a single pet\",\n" +
            "            \"operationId\":\"getPetById\",\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"name\":\"petId\",\n" +
            "                  \"in\":\"path\",\n" +
            "                  \"description\":\"ID of pet to return\",\n" +
            "                  \"required\":true,\n" +
            "                  \"type\":\"integer\",\n" +
            "                  \"format\":\"int64\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"200\":{\n" +
            "                  \"description\":\"successful operation\",\n" +
            "                  \"schema\":{\n" +
            "                     \"$ref\":\"#/definitions/Pet\"\n" +
            "                  }\n" +
            "               },\n" +
            "               \"400\":{\n" +
            "                  \"description\":\"Invalid ID supplied\"\n" +
            "               },\n" +
            "               \"404\":{\n" +
            "                  \"description\":\"Pet not found\"\n" +
            "               }\n" +
            "            },\n" +
            "            \"security\":[\n" +
            "               {\n" +
            "                  \"api_key\":[\n" +
            "\n" +
            "                  ]\n" +
            "               }\n" +
            "            ]\n" +
            "         },\n" +
            "         \"post\":{\n" +
            "            \"tags\":[\n" +
            "               \"pet\"\n" +
            "            ],\n" +
            "            \"summary\":\"Updates a pet in the store with form data\",\n" +
            "            \"description\":\"\",\n" +
            "            \"operationId\":\"updatePetWithForm\",\n" +
            "            \"consumes\":[\n" +
            "               \"application/x-www-form-urlencoded\"\n" +
            "            ],\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"name\":\"petId\",\n" +
            "                  \"in\":\"path\",\n" +
            "                  \"description\":\"ID of pet that needs to be updated\",\n" +
            "                  \"required\":true,\n" +
            "                  \"type\":\"integer\",\n" +
            "                  \"format\":\"int64\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"name\":\"name\",\n" +
            "                  \"in\":\"formData\",\n" +
            "                  \"description\":\"Updated name of the pet\",\n" +
            "                  \"required\":false,\n" +
            "                  \"type\":\"string\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"name\":\"status\",\n" +
            "                  \"in\":\"formData\",\n" +
            "                  \"description\":\"Updated status of the pet\",\n" +
            "                  \"required\":false,\n" +
            "                  \"type\":\"string\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"405\":{\n" +
            "                  \"description\":\"Invalid input\"\n" +
            "               }\n" +
            "            },\n" +
            "            \"security\":[\n" +
            "               {\n" +
            "                  \"petstore_auth\":[\n" +
            "                     \"write:pets\",\n" +
            "                     \"read:pets\"\n" +
            "                  ]\n" +
            "               }\n" +
            "            ]\n" +
            "         },\n" +
            "         \"delete\":{\n" +
            "            \"tags\":[\n" +
            "               \"pet\"\n" +
            "            ],\n" +
            "            \"summary\":\"Deletes a pet\",\n" +
            "            \"description\":\"\",\n" +
            "            \"operationId\":\"deletePet\",\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"name\":\"api_key\",\n" +
            "                  \"in\":\"header\",\n" +
            "                  \"required\":false,\n" +
            "                  \"type\":\"string\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"name\":\"petId\",\n" +
            "                  \"in\":\"path\",\n" +
            "                  \"description\":\"Pet id to delete\",\n" +
            "                  \"required\":true,\n" +
            "                  \"type\":\"integer\",\n" +
            "                  \"format\":\"int64\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"400\":{\n" +
            "                  \"description\":\"Invalid pet value\"\n" +
            "               }\n" +
            "            },\n" +
            "            \"security\":[\n" +
            "               {\n" +
            "                  \"petstore_auth\":[\n" +
            "                     \"write:pets\",\n" +
            "                     \"read:pets\"\n" +
            "                  ]\n" +
            "               }\n" +
            "            ]\n" +
            "         }\n" +
            "      },\n" +
            "      \"/pet/{petId}/uploadImage\":{\n" +
            "         \"post\":{\n" +
            "            \"tags\":[\n" +
            "               \"pet\"\n" +
            "            ],\n" +
            "            \"summary\":\"uploads an image\",\n" +
            "            \"description\":\"\",\n" +
            "            \"operationId\":\"uploadFile\",\n" +
            "            \"consumes\":[\n" +
            "               \"multipart/form-data\"\n" +
            "            ],\n" +
            "            \"produces\":[\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"name\":\"petId\",\n" +
            "                  \"in\":\"path\",\n" +
            "                  \"description\":\"ID of pet to update\",\n" +
            "                  \"required\":true,\n" +
            "                  \"type\":\"integer\",\n" +
            "                  \"format\":\"int64\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"name\":\"additionalMetadata\",\n" +
            "                  \"in\":\"formData\",\n" +
            "                  \"description\":\"Additional data to pass to server\",\n" +
            "                  \"required\":false,\n" +
            "                  \"type\":\"string\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"name\":\"file\",\n" +
            "                  \"in\":\"formData\",\n" +
            "                  \"description\":\"file to upload\",\n" +
            "                  \"required\":false,\n" +
            "                  \"type\":\"file\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"200\":{\n" +
            "                  \"description\":\"successful operation\",\n" +
            "                  \"schema\":{\n" +
            "                     \"$ref\":\"#/definitions/ApiResponse\"\n" +
            "                  }\n" +
            "               }\n" +
            "            },\n" +
            "            \"security\":[\n" +
            "               {\n" +
            "                  \"petstore_auth\":[\n" +
            "                     \"write:pets\",\n" +
            "                     \"read:pets\"\n" +
            "                  ]\n" +
            "               }\n" +
            "            ]\n" +
            "         }\n" +
            "      },\n" +
            "      \"/store/inventory\":{\n" +
            "         \"get\":{\n" +
            "            \"tags\":[\n" +
            "               \"store\"\n" +
            "            ],\n" +
            "            \"summary\":\"Returns pet inventories by status\",\n" +
            "            \"description\":\"Returns a map of status codes to quantities\",\n" +
            "            \"operationId\":\"getInventory\",\n" +
            "            \"produces\":[\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"200\":{\n" +
            "                  \"description\":\"successful operation\",\n" +
            "                  \"schema\":{\n" +
            "                     \"type\":\"object\",\n" +
            "                     \"additionalProperties\":{\n" +
            "                        \"type\":\"integer\",\n" +
            "                        \"format\":\"int32\"\n" +
            "                     }\n" +
            "                  }\n" +
            "               }\n" +
            "            },\n" +
            "            \"security\":[\n" +
            "               {\n" +
            "                  \"api_key\":[\n" +
            "\n" +
            "                  ]\n" +
            "               }\n" +
            "            ]\n" +
            "         }\n" +
            "      },\n" +
            "      \"/store/order\":{\n" +
            "         \"post\":{\n" +
            "            \"tags\":[\n" +
            "               \"store\"\n" +
            "            ],\n" +
            "            \"summary\":\"Place an order for a pet\",\n" +
            "            \"description\":\"\",\n" +
            "            \"operationId\":\"placeOrder\",\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"in\":\"body\",\n" +
            "                  \"name\":\"body\",\n" +
            "                  \"description\":\"order placed for purchasing the pet\",\n" +
            "                  \"required\":true,\n" +
            "                  \"schema\":{\n" +
            "                     \"$ref\":\"#/definitions/Order\"\n" +
            "                  }\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"200\":{\n" +
            "                  \"description\":\"successful operation\",\n" +
            "                  \"schema\":{\n" +
            "                     \"$ref\":\"#/definitions/Order\"\n" +
            "                  }\n" +
            "               },\n" +
            "               \"400\":{\n" +
            "                  \"description\":\"Invalid Order\"\n" +
            "               }\n" +
            "            }\n" +
            "         }\n" +
            "      },\n" +
            "      \"/store/order/{orderId}\":{\n" +
            "         \"get\":{\n" +
            "            \"tags\":[\n" +
            "               \"store\"\n" +
            "            ],\n" +
            "            \"summary\":\"Find purchase order by ID\",\n" +
            "            \"description\":\"For valid response try integer IDs with value <= 5 or > 10. Other values will generated exceptions\",\n" +
            "            \"operationId\":\"getOrderById\",\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"name\":\"orderId\",\n" +
            "                  \"in\":\"path\",\n" +
            "                  \"description\":\"ID of pet that needs to be fetched\",\n" +
            "                  \"required\":true,\n" +
            "                  \"type\":\"integer\",\n" +
            "                  \"maximum\":5.0,\n" +
            "                  \"minimum\":1.0,\n" +
            "                  \"format\":\"int64\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"200\":{\n" +
            "                  \"description\":\"successful operation\",\n" +
            "                  \"schema\":{\n" +
            "                     \"$ref\":\"#/definitions/Order\"\n" +
            "                  }\n" +
            "               },\n" +
            "               \"400\":{\n" +
            "                  \"description\":\"Invalid ID supplied\"\n" +
            "               },\n" +
            "               \"404\":{\n" +
            "                  \"description\":\"Order not found\"\n" +
            "               }\n" +
            "            }\n" +
            "         },\n" +
            "         \"delete\":{\n" +
            "            \"tags\":[\n" +
            "               \"store\"\n" +
            "            ],\n" +
            "            \"summary\":\"Delete purchase order by ID\",\n" +
            "            \"description\":\"For valid response try integer IDs with value < 1000. Anything above 1000 or nonintegers will generate API errors\",\n" +
            "            \"operationId\":\"deleteOrder\",\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"name\":\"orderId\",\n" +
            "                  \"in\":\"path\",\n" +
            "                  \"description\":\"ID of the order that needs to be deleted\",\n" +
            "                  \"required\":true,\n" +
            "                  \"type\":\"string\",\n" +
            "                  \"minimum\":1.0\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"400\":{\n" +
            "                  \"description\":\"Invalid ID supplied\"\n" +
            "               },\n" +
            "               \"404\":{\n" +
            "                  \"description\":\"Order not found\"\n" +
            "               }\n" +
            "            }\n" +
            "         }\n" +
            "      },\n" +
            "      \"/user\":{\n" +
            "         \"post\":{\n" +
            "            \"tags\":[\n" +
            "               \"user\"\n" +
            "            ],\n" +
            "            \"summary\":\"Create user\",\n" +
            "            \"description\":\"This can only be done by the logged in user.\",\n" +
            "            \"operationId\":\"createUser\",\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"in\":\"body\",\n" +
            "                  \"name\":\"body\",\n" +
            "                  \"description\":\"Created user object\",\n" +
            "                  \"required\":true,\n" +
            "                  \"schema\":{\n" +
            "                     \"$ref\":\"#/definitions/User\"\n" +
            "                  }\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"default\":{\n" +
            "                  \"description\":\"successful operation\"\n" +
            "               }\n" +
            "            }\n" +
            "         }\n" +
            "      },\n" +
            "      \"/user/createWithArray\":{\n" +
            "         \"post\":{\n" +
            "            \"tags\":[\n" +
            "               \"user\"\n" +
            "            ],\n" +
            "            \"summary\":\"Creates list of users with given input array\",\n" +
            "            \"description\":\"\",\n" +
            "            \"operationId\":\"createUsersWithArrayInput\",\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"in\":\"body\",\n" +
            "                  \"name\":\"body\",\n" +
            "                  \"description\":\"List of user object\",\n" +
            "                  \"required\":true,\n" +
            "                  \"schema\":{\n" +
            "                     \"type\":\"array\",\n" +
            "                     \"items\":{\n" +
            "                        \"$ref\":\"#/definitions/User\"\n" +
            "                     }\n" +
            "                  }\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"default\":{\n" +
            "                  \"description\":\"successful operation\"\n" +
            "               }\n" +
            "            }\n" +
            "         }\n" +
            "      },\n" +
            "      \"/user/createWithList\":{\n" +
            "         \"post\":{\n" +
            "            \"tags\":[\n" +
            "               \"user\"\n" +
            "            ],\n" +
            "            \"summary\":\"Creates list of users with given input array\",\n" +
            "            \"description\":\"\",\n" +
            "            \"operationId\":\"createUsersWithListInput\",\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"in\":\"body\",\n" +
            "                  \"name\":\"body\",\n" +
            "                  \"description\":\"List of user object\",\n" +
            "                  \"required\":true,\n" +
            "                  \"schema\":{\n" +
            "                     \"type\":\"array\",\n" +
            "                     \"items\":{\n" +
            "                        \"$ref\":\"#/definitions/User\"\n" +
            "                     }\n" +
            "                  }\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"default\":{\n" +
            "                  \"description\":\"successful operation\"\n" +
            "               }\n" +
            "            }\n" +
            "         }\n" +
            "      },\n" +
            "      \"/user/login\":{\n" +
            "         \"get\":{\n" +
            "            \"tags\":[\n" +
            "               \"user\"\n" +
            "            ],\n" +
            "            \"summary\":\"Logs user into the system\",\n" +
            "            \"description\":\"\",\n" +
            "            \"operationId\":\"loginUser\",\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"name\":\"username\",\n" +
            "                  \"in\":\"query\",\n" +
            "                  \"description\":\"The user name for login\",\n" +
            "                  \"required\":true,\n" +
            "                  \"type\":\"string\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"name\":\"password\",\n" +
            "                  \"in\":\"query\",\n" +
            "                  \"description\":\"The password for login in clear text\",\n" +
            "                  \"required\":true,\n" +
            "                  \"type\":\"string\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"200\":{\n" +
            "                  \"description\":\"successful operation\",\n" +
            "                  \"schema\":{\n" +
            "                     \"type\":\"string\"\n" +
            "                  },\n" +
            "                  \"headers\":{\n" +
            "                     \"X-Rate-Limit\":{\n" +
            "                        \"type\":\"integer\",\n" +
            "                        \"format\":\"int32\",\n" +
            "                        \"description\":\"calls per hour allowed by the user\"\n" +
            "                     },\n" +
            "                     \"X-Expires-After\":{\n" +
            "                        \"type\":\"string\",\n" +
            "                        \"format\":\"date-time\",\n" +
            "                        \"description\":\"date in UTC when toekn expires\"\n" +
            "                     }\n" +
            "                  }\n" +
            "               },\n" +
            "               \"400\":{\n" +
            "                  \"description\":\"Invalid username/password supplied\"\n" +
            "               }\n" +
            "            }\n" +
            "         }\n" +
            "      },\n" +
            "      \"/user/logout\":{\n" +
            "         \"get\":{\n" +
            "            \"tags\":[\n" +
            "               \"user\"\n" +
            "            ],\n" +
            "            \"summary\":\"Logs out current logged in user session\",\n" +
            "            \"description\":\"\",\n" +
            "            \"operationId\":\"logoutUser\",\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"default\":{\n" +
            "                  \"description\":\"successful operation\"\n" +
            "               }\n" +
            "            }\n" +
            "         }\n" +
            "      },\n" +
            "      \"/user/{username}\":{\n" +
            "         \"get\":{\n" +
            "            \"tags\":[\n" +
            "               \"user\"\n" +
            "            ],\n" +
            "            \"summary\":\"Get user by user name\",\n" +
            "            \"description\":\"\",\n" +
            "            \"operationId\":\"getUserByName\",\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"name\":\"username\",\n" +
            "                  \"in\":\"path\",\n" +
            "                  \"description\":\"The name that needs to be fetched. Use user1 for testing. \",\n" +
            "                  \"required\":true,\n" +
            "                  \"type\":\"string\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"200\":{\n" +
            "                  \"description\":\"successful operation\",\n" +
            "                  \"schema\":{\n" +
            "                     \"$ref\":\"#/definitions/User\"\n" +
            "                  }\n" +
            "               },\n" +
            "               \"400\":{\n" +
            "                  \"description\":\"Invalid username supplied\"\n" +
            "               },\n" +
            "               \"404\":{\n" +
            "                  \"description\":\"User not found\"\n" +
            "               }\n" +
            "            }\n" +
            "         },\n" +
            "         \"put\":{\n" +
            "            \"tags\":[\n" +
            "               \"user\"\n" +
            "            ],\n" +
            "            \"summary\":\"Updated user\",\n" +
            "            \"description\":\"This can only be done by the logged in user.\",\n" +
            "            \"operationId\":\"updateUser\",\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"name\":\"username\",\n" +
            "                  \"in\":\"path\",\n" +
            "                  \"description\":\"name that need to be deleted\",\n" +
            "                  \"required\":true,\n" +
            "                  \"type\":\"string\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"in\":\"body\",\n" +
            "                  \"name\":\"body\",\n" +
            "                  \"description\":\"Updated user object\",\n" +
            "                  \"required\":true,\n" +
            "                  \"schema\":{\n" +
            "                     \"$ref\":\"#/definitions/User\"\n" +
            "                  }\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"400\":{\n" +
            "                  \"description\":\"Invalid user supplied\"\n" +
            "               },\n" +
            "               \"404\":{\n" +
            "                  \"description\":\"User not found\"\n" +
            "               }\n" +
            "            }\n" +
            "         },\n" +
            "         \"delete\":{\n" +
            "            \"tags\":[\n" +
            "               \"user\"\n" +
            "            ],\n" +
            "            \"summary\":\"Delete user\",\n" +
            "            \"description\":\"This can only be done by the logged in user.\",\n" +
            "            \"operationId\":\"deleteUser\",\n" +
            "            \"produces\":[\n" +
            "               \"application/xml\",\n" +
            "               \"application/json\"\n" +
            "            ],\n" +
            "            \"parameters\":[\n" +
            "               {\n" +
            "                  \"name\":\"username\",\n" +
            "                  \"in\":\"path\",\n" +
            "                  \"description\":\"The name that needs to be deleted\",\n" +
            "                  \"required\":true,\n" +
            "                  \"type\":\"string\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"responses\":{\n" +
            "               \"400\":{\n" +
            "                  \"description\":\"Invalid username supplied\"\n" +
            "               },\n" +
            "               \"404\":{\n" +
            "                  \"description\":\"User not found\"\n" +
            "               }\n" +
            "            }\n" +
            "         }\n" +
            "      }\n" +
            "   },\n" +
            "   \"securityDefinitions\":{\n" +
            "      \"petstore_auth\":{\n" +
            "         \"type\":\"oauth2\",\n" +
            "         \"authorizationUrl\":\"http://petstore.swagger.io/api/oauth/dialog\",\n" +
            "         \"flow\":\"implicit\",\n" +
            "         \"scopes\":{\n" +
            "            \"write:pets\":\"modify pets in your account\",\n" +
            "            \"read:pets\":\"read your pets\"\n" +
            "         }\n" +
            "      },\n" +
            "      \"api_key\":{\n" +
            "         \"type\":\"apiKey\",\n" +
            "         \"name\":\"api_key\",\n" +
            "         \"in\":\"header\"\n" +
            "      }\n" +
            "   },\n" +
            "   \"definitions\":{\n" +
            "      \"Order\":{\n" +
            "         \"type\":\"object\",\n" +
            "         \"properties\":{\n" +
            "            \"id\":{\n" +
            "               \"type\":\"integer\",\n" +
            "               \"format\":\"int64\"\n" +
            "            },\n" +
            "            \"petId\":{\n" +
            "               \"type\":\"integer\",\n" +
            "               \"format\":\"int64\"\n" +
            "            },\n" +
            "            \"quantity\":{\n" +
            "               \"type\":\"integer\",\n" +
            "               \"format\":\"int32\"\n" +
            "            },\n" +
            "            \"shipDate\":{\n" +
            "               \"type\":\"string\",\n" +
            "               \"format\":\"date-time\"\n" +
            "            },\n" +
            "            \"status\":{\n" +
            "               \"type\":\"string\",\n" +
            "               \"description\":\"Order Status\",\n" +
            "               \"enum\":[\n" +
            "                  \"placed\",\n" +
            "                  \"approved\",\n" +
            "                  \"delivered\"\n" +
            "               ]\n" +
            "            },\n" +
            "            \"complete\":{\n" +
            "               \"type\":\"boolean\",\n" +
            "               \"default\":false\n" +
            "            }\n" +
            "         },\n" +
            "         \"xml\":{\n" +
            "            \"name\":\"Order\"\n" +
            "         }\n" +
            "      },\n" +
            "      \"User\":{\n" +
            "         \"type\":\"object\",\n" +
            "         \"properties\":{\n" +
            "            \"id\":{\n" +
            "               \"type\":\"integer\",\n" +
            "               \"format\":\"int64\"\n" +
            "            },\n" +
            "            \"username\":{\n" +
            "               \"type\":\"string\"\n" +
            "            },\n" +
            "            \"firstName\":{\n" +
            "               \"type\":\"string\"\n" +
            "            },\n" +
            "            \"lastName\":{\n" +
            "               \"type\":\"string\"\n" +
            "            },\n" +
            "            \"email\":{\n" +
            "               \"type\":\"string\"\n" +
            "            },\n" +
            "            \"password\":{\n" +
            "               \"type\":\"string\"\n" +
            "            },\n" +
            "            \"phone\":{\n" +
            "               \"type\":\"string\"\n" +
            "            },\n" +
            "            \"userStatus\":{\n" +
            "               \"type\":\"integer\",\n" +
            "               \"format\":\"int32\",\n" +
            "               \"description\":\"User Status\"\n" +
            "            }\n" +
            "         },\n" +
            "         \"xml\":{\n" +
            "            \"name\":\"User\"\n" +
            "         }\n" +
            "      },\n" +
            "      \"Category\":{\n" +
            "         \"type\":\"object\",\n" +
            "         \"properties\":{\n" +
            "            \"id\":{\n" +
            "               \"type\":\"integer\",\n" +
            "               \"format\":\"int64\"\n" +
            "            },\n" +
            "            \"name\":{\n" +
            "               \"type\":\"string\"\n" +
            "            }\n" +
            "         },\n" +
            "         \"xml\":{\n" +
            "            \"name\":\"Category\"\n" +
            "         }\n" +
            "      },\n" +
            "      \"Tag\":{\n" +
            "         \"type\":\"object\",\n" +
            "         \"properties\":{\n" +
            "            \"id\":{\n" +
            "               \"type\":\"integer\",\n" +
            "               \"format\":\"int64\"\n" +
            "            },\n" +
            "            \"name\":{\n" +
            "               \"type\":\"string\"\n" +
            "            }\n" +
            "         },\n" +
            "         \"xml\":{\n" +
            "            \"name\":\"Tag\"\n" +
            "         }\n" +
            "      },\n" +
            "      \"Pet\":{\n" +
            "         \"type\":\"object\",\n" +
            "         \"required\":[\n" +
            "            \"name\",\n" +
            "            \"photoUrls\"\n" +
            "         ],\n" +
            "         \"properties\":{\n" +
            "            \"id\":{\n" +
            "               \"type\":\"integer\",\n" +
            "               \"format\":\"int64\"\n" +
            "            },\n" +
            "            \"category\":{\n" +
            "               \"$ref\":\"#/definitions/Category\"\n" +
            "            },\n" +
            "            \"name\":{\n" +
            "               \"type\":\"string\",\n" +
            "               \"example\":\"doggie\"\n" +
            "            },\n" +
            "            \"photoUrls\":{\n" +
            "               \"type\":\"array\",\n" +
            "               \"xml\":{\n" +
            "                  \"name\":\"photoUrl\",\n" +
            "                  \"wrapped\":true\n" +
            "               },\n" +
            "               \"items\":{\n" +
            "                  \"type\":\"string\"\n" +
            "               }\n" +
            "            },\n" +
            "            \"tags\":{\n" +
            "               \"type\":\"array\",\n" +
            "               \"xml\":{\n" +
            "                  \"name\":\"tag\",\n" +
            "                  \"wrapped\":true\n" +
            "               },\n" +
            "               \"items\":{\n" +
            "                  \"$ref\":\"#/definitions/Tag\"\n" +
            "               }\n" +
            "            },\n" +
            "            \"status\":{\n" +
            "               \"type\":\"string\",\n" +
            "               \"description\":\"pet status in the store\",\n" +
            "               \"enum\":[\n" +
            "                  \"available\",\n" +
            "                  \"pending\",\n" +
            "                  \"sold\"\n" +
            "               ]\n" +
            "            }\n" +
            "         },\n" +
            "         \"xml\":{\n" +
            "            \"name\":\"Pet\"\n" +
            "         }\n" +
            "      },\n" +
            "      \"ApiResponse\":{\n" +
            "         \"type\":\"object\",\n" +
            "         \"properties\":{\n" +
            "            \"code\":{\n" +
            "               \"type\":\"integer\",\n" +
            "               \"format\":\"int32\"\n" +
            "            },\n" +
            "            \"type\":{\n" +
            "               \"type\":\"string\"\n" +
            "            },\n" +
            "            \"message\":{\n" +
            "               \"type\":\"string\"\n" +
            "            }\n" +
            "         }\n" +
            "      }\n" +
            "   },\n" +
            "   \"externalDocs\":{\n" +
            "      \"description\":\"Find out more about Swagger\",\n" +
            "      \"url\":\"http://swagger.io\"\n" +
            "   }\n" +
            "}";
    public static final String INVALID_SWAGGER_JSON = "{\"name\":\"value\"}";

    @Mock
    PolicyEnforcementContext mockContext;
    @Mock
    HttpRequestKnob mockRequestKnob;
    Message requestMsg;
    Message responseMsg;

    SwaggerAssertion assertion;
    ServerSwaggerAssertion fixture;


    @Before
    public void setUp() throws Exception {
        //Setup Context
        requestMsg = new Message();
        requestMsg.attachHttpRequestKnob(mockRequestKnob);
        responseMsg = new Message();
        responseMsg.attachHttpResponseKnob(new HttpResponseKnob() {
            @Override
            public void addChallenge(String value) {

            }

            @Override
            public void setStatus(int code) {

            }

            @Override
            public int getStatus() {
                return 0;
            }
        });


        PublishedService service = new PublishedService();
        service.setRoutingUri("/svr/*");
        service.setSoap(false);
        when(mockContext.getService()).thenReturn(service);

        assertion = new SwaggerAssertion();
        fixture = new ServerSwaggerAssertion(assertion);
    }

    @Test
    public void testInvalidSwagger() throws Exception {
        assertion.setSwaggerDoc("swaggerDoc");
        Map<String,Object> varMap = new HashMap<>();
        varMap.put("swaggerdoc", INVALID_SWAGGER_JSON);
        when(mockContext.getVariableMap(eq(assertion.getVariablesUsed()), any(Audit.class))).thenReturn(varMap);
        when(mockContext.getVariable("swaggerDoc")).thenReturn(INVALID_SWAGGER_JSON);
        fixture = new ServerSwaggerAssertion(assertion);
        assertEquals(AssertionStatus.FALSIFIED, fixture.checkRequest(mockContext));

    }

    @Test
    public void testSwaggerValidRequest_checkBaseUriAndHostContextVariables() throws  Exception {
        assertion.setSwaggerDoc("swaggerDoc");
        Map<String,Object> varMap = new HashMap<>();
        varMap.put("swaggerdoc", TEST_SWAGGER_JSON);
        when(mockRequestKnob.getRequestUri()).thenReturn("/svr/pet/findByStatus");
        when(mockRequestKnob.getMethod()).thenReturn(HttpMethod.GET);
        when(mockContext.getVariableMap(eq(assertion.getVariablesUsed()), any(Audit.class))).thenReturn(varMap);
        when(mockContext.getVariable("swaggerDoc")).thenReturn(TEST_SWAGGER_JSON);
        when(mockContext.getRequest()).thenReturn(requestMsg);
        fixture = new ServerSwaggerAssertion(assertion);
        assertEquals(AssertionStatus.NONE, fixture.checkRequest(mockContext));
        verify(mockContext,times(1)).setVariable("sw.host","petstore.swagger.io:8080");
        verify(mockContext, times(1)).setVariable("sw.baseUri", "/v2");
        verify(mockContext, times(1)).setVariable("sw.apiUri", "/pet/findByStatus");
    }

    // TODO jwilliams: test for empty requestUri, empty path definition cases - does UriTemplate still match correctly?

    @Test
    public void testPathMatching() {
        PathDefinitionResolver resolver = new PathDefinitionResolver(fixture.parseSwaggerJson(TEST_SWAGGER_JSON));

        Path path = resolver.getPathForRequestUri("/pet/1234");

        assertNotNull(path);
    }

}
