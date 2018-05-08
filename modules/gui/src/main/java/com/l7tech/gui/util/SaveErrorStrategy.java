package com.l7tech.gui.util;

import com.l7tech.util.SyspropUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.ResourceBundle;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.awt.*;
import java.util.function.Supplier;

/**
 * The class specifies a save-error strategy to save error into a file.
 *
 * @auther: ghuang
 */
public abstract class SaveErrorStrategy {
    protected final static ResourceBundle resources =
            ResourceBundle.getBundle("com/l7tech/common/resources/ErrorReport");
    protected Throwable throwable;
    protected Window errorMessageDialog;

    private final static String propertyKeys[] = {
            "java.version",
            "java.specification.version",
            "os.name",
            "os.arch",
    };

    @Nullable
    private static Supplier<String> buildInfoRetriever;

    public void setErrorMessageDialog(Window errorMessageDialog) {
        this.errorMessageDialog = errorMessageDialog;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public abstract void saveErrorReportFile() throws UnableToSaveException;

    /**
     * Make the content of an error report
     * @return  a string of the content
     */
    public String getReportContent() {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd 'at' HH:mm:ss z");

        sb.append(MessageFormat.format(resources.getString("date.time"), dateFormat.format(new Date())));

        // TODO: This is really gross and ugly but unless the entire error reporting system in the PM is redone there is no other way to do this.
        final String buildInfo;
        if(buildInfoRetriever != null) {
            buildInfo = buildInfoRetriever.get();
        } else {
            //This should never really happen, but in case it does...
            buildInfo = resources.getString( "build.info.fail" );
        }
        sb.append(MessageFormat.format(resources.getString("build.info"), buildInfo));

        sb.append(MessageFormat.format( resources.getString( "system.properties" ),
                SyspropUtil.getProperty( propertyKeys[0] ),
                SyspropUtil.getProperty( propertyKeys[1] ),
                SyspropUtil.getProperty( propertyKeys[2] ),
                SyspropUtil.getProperty( propertyKeys[3] ) ));
        sb.append(MessageFormat.format(resources.getString("memory.usage"),
                Runtime.getRuntime().freeMemory(),
                Runtime.getRuntime().totalMemory()));
        sb.append(MessageFormat.format(resources.getString("stack.trace"), stackTrace()));
        sb.append(resources.getString("help.centre"));
        return sb.toString().replaceAll("\n", SyspropUtil.getString("line.separator", "\n"));
    }

    /**
     * Helper method to get the full content of the stack trace.
     * @return  the content of stack trace
     */
    private String stackTrace() {
        if (throwable == null) {
            return resources.getString("no.stack.trace");
        }

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        throwable.printStackTrace(printWriter);
        printWriter.flush();
        return stringWriter.toString();
    }

    public String getSuggestedFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        return "CA_API_Gateway_Policy_Manager_Error_Report_" + sdf.format(new Date()) + ".txt";
    }

    public static class UnableToSaveException extends Exception {
        public UnableToSaveException(String message) {
            super(message);
        }
    }

    /**
     * Sets the buildInfoRetriever
     *
     * @param buildInfoRetriever The supplier that provides the build info
     */
    public static void setBuildInfoRetriever(@NotNull final Supplier<String> buildInfoRetriever) {
        SaveErrorStrategy.buildInfoRetriever = buildInfoRetriever;
    }
}


