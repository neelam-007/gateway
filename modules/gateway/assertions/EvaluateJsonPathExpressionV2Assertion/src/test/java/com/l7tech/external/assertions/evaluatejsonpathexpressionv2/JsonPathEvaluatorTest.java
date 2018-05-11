package com.l7tech.external.assertions.evaluatejsonpathexpressionv2;

import com.l7tech.test.BugId;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class JsonPathEvaluatorTest {

    /**
     * Test to ensure that an expression that evaluates to a list is returned as a properly formatted json array.
     */
    @BugId("DE361214")
    @Test
    public void evaluateList() throws Exception{
        final String source = "{\n" +
                "  \"Data\": {\n" +
                "    \"Emails\": [\n" +
                "      {\n" +
                "        \"Email\": {\n" +
                "          \"Address\": \"a******z@v*****.com\",\n" +
                "          \"CommunicationToken\": \"00886211b91a67b938f3408bc770b1d67054bd57c6ec9d300e63bac2bd750ed4\",\n" +
                "          \"PrimaryContactIndicator\": \"Y\"\n" +
                "        }\n" +
                "      }\n" +
                "    ],\n" +
                "    \"Phones\": [\n" +
                "      {\n" +
                "        \"Phone\": {\n" +
                "          \"Number\": \"*******111\",\n" +
                "          \"CommunicationToken\": \"fb1a9fbae643bc59ef144dd56c9b235ff5ff10298d1eafc3181e3fab6e771022\",\n" +
                "          \"PrimaryContactIndicator\": \"Y\"\n" +
                "        }\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"Response\": {\n" +
                "    \"Tracing\": {\n" +
                "      \"TraceID\": \"trace_1524754512\",\n" +
                "      \"TraceTimestamp\": \"2018-04-26T10:55:12.707Z\"\n" +
                "    },\n" +
                "    \"ResponseCode\": \"2000\",\n" +
                "    \"ResponseStatus\": \"Success\",\n" +
                "    \"ResponseDescription\": \"Successful Execution\"\n" +
                "  }\n" +
                "}";

        final JsonPathExpressionResult result = JsonPathEvaluator.evaluators.get(EvaluateJsonPathExpressionV2Assertion.JSONPATH_EVALUATOR).evaluate(source, "$..Emails");
        assertEquals(1, result.getCount());
        assertEquals("[{\"Email\":{\"Address\":\"a******z@v*****.com\",\"CommunicationToken\":\"00886211b91a67b938f3408bc770b1d67054bd57c6ec9d300e63bac2bd750ed4\",\"PrimaryContactIndicator\":\"Y\"}}]", result.getResults().get(0));
    }
}
