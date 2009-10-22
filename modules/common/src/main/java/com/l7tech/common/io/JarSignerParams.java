package com.l7tech.common.io;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Parameter argument builder for the JarSigner utility.
 *
 * @author jbufu
 */
public class JarSignerParams {

    // - PUBLIC

    public JarSignerParams(String keystore, String keystorePassword, String alias, String password) {
        // required params
        set("-keystore", keystore);
        set("-storepass", keystorePassword);
        set("-keypass", password);
        set(ALIAS_PARAM, alias);
    }

    public JarSignerParams set(String name, String value) {
        params.put(name, value);
        return this;
    }

    public String getAlias() {
        return params.get(ALIAS_PARAM);
    }

    public String[] getOptions() {
        List<String> result = new ArrayList<String>();
        for (String paramName : params.keySet()) {
            if (ALIAS_PARAM.equals(paramName)) continue;
            result.add(paramName);
            result.add(params.get(paramName));
        }
        return result.toArray(new String[result.size()]);
    }

    // - PRIVATE

    private static final String ALIAS_PARAM = "alias";

    private Map<String,String> params = new LinkedHashMap<String, String>();
}
