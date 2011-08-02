package com.l7tech.external.assertions.jsontransformation.server;

import com.l7tech.external.assertions.jsontransformation.JsonTransformationAdmin;
import com.l7tech.external.assertions.jsontransformation.JsonTransformationAssertion;
import com.l7tech.util.ExceptionUtils;
import org.json.JSONException;

/**
 * User: wlui
 */
public class JsonTransformationAdminImpl implements JsonTransformationAdmin{
    @Override
    public String testTransform(String input, JsonTransformationAssertion.Transformation transformation, String rootTag) throws JsonTransformationTestException {
        try{
            return ServerJsonTransformationAssertion.doTransformation(input, transformation, rootTag);
        }catch (JSONException ex){
            throw new JsonTransformationTestException(ex.getMessage(), ExceptionUtils.getDebugException(ex));
        }
    }
}

