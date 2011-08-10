package com.l7tech.server.processcontroller.monitoring.sampling;

import com.l7tech.common.io.ProcResult;
import static com.l7tech.common.io.ProcUtils.args;
import static com.l7tech.common.io.ProcUtils.exec;
import com.l7tech.server.management.api.monitoring.BuiltinMonitorables;
import com.l7tech.server.processcontroller.ConfigService;
import com.l7tech.util.ConfigFactory;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 *
 */
class CpuTemperatureSampler extends HostPropertySampler<Integer> {
    private static final String PROP_BASE = "com.l7tech.server.processcontroller.monitoring.sampling.CpuTemperatureSampler";
    private static final String PROC_TEMP = "/proc/acpi/thermal_zone/THRM/temperature";
    private static final Pattern TEMP_MATCHER = Pattern.compile("^temperature:\\s*(\\d+)\\s*C\\s*$");
    private static final String SCRIPT_NAME = "hardware-stats.pl";

    private static final String SUDO_PATH = ConfigFactory.getProperty( PROP_BASE + ".sudoPath", "/usr/bin/sudo" );
    private static final boolean DOSUDO = ConfigFactory.getBooleanProperty( PROP_BASE + ".runWithSudo", SUDO_PATH != null );

    private final ConfigService config;

    public CpuTemperatureSampler(String componentId, ApplicationContext applicationContext) {
        super(componentId, BuiltinMonitorables.CPU_TEMPERATURE.getName());
        this.config = applicationContext.getBean("configService", ConfigService.class);
    }

    public Integer sample() throws PropertySamplingException {
        if (new File(PROC_TEMP).exists())
            return (int)matchNumberFromFile(PROC_TEMP, TEMP_MATCHER);

        String outputString = getScriptOutput();
        if (outputString != null && outputString.contains("|null"))
            throw new PropertySamplingException("Couldn't get CPU temperature", false);
        return (int)matchNumber(outputString, (DOSUDO ? "sudo " : "") + SCRIPT_NAME, Pattern.compile("^CPU\\|(\\d+)$"));
    }

    private String getScriptOutput() throws PropertySamplingException {
        try {
            File libexec = config.getApplianceLibexecDirectory();
            File script = new File(libexec, SCRIPT_NAME);
            ProcResult result = DOSUDO ? exec(new File(SUDO_PATH), args(script.getPath())) : exec(script);
            return new String(result.getOutput());
        } catch (IOException e) {
            throw new PropertySamplingException(e, false);
        }
    }
}
