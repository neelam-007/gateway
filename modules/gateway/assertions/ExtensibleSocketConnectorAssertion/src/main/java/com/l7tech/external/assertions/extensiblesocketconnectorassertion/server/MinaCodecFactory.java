package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.CodecModule;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.codecconfigurations.CodecConfiguration;

import java.lang.reflect.Method;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 29/03/12
 * Time: 4:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class MinaCodecFactory {
    private static Vector<CodecModule> codecModules = new Vector<CodecModule>();

    public static void addCodecModule(CodecModule codecModule) {
        codecModules.add(codecModule);
    }

    public static Vector<CodecModule> getCodecModules() {
        return codecModules;
    }

    public static Object createCodec(CodecConfiguration codecConfig, ExtensibleSocketConnectorClassLoader classLoader) {
        for (CodecModule codecModule : codecModules) {
            if (codecModule.getConfigurationClassName().equals(codecConfig.getClass().getName())) {
                try {
                    //create codec
                    Class codecClass = Class.forName(codecModule.getCodecClassName(), true, classLoader);
                    Method codecConfigMethod = codecClass.getMethod("configureCodec", Object.class);
                    Object codec = codecClass.newInstance();

                    //configure codec
                    codecConfigMethod.invoke(codec, codecConfig);

                    return codec;
                } catch (Exception ex) {
                    return null;
                }
            }
        }

        return null;
    }
}
