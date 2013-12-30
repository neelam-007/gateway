package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.NullAudit;
import com.l7tech.server.audit.Auditor;
import com.l7tech.test.BenchmarkRunner;
import com.l7tech.util.CollectionUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class ExpandVariablesTemplateTest {
    private static final Logger logger = Logger.getLogger(ExpandVariablesTemplateTest.class.getName());

    private Map<String, ?> vars = CollectionUtils.MapBuilder.<String, Object>builder()
            .put("foo", "qwert")
            .put("bar", "zxcv")
            .put("baz", "wjhewiueh")
            .put("quux", "wuhweuh")
            .put("asdf", "uhwuhwruh")
            .unmodifiableMap();

    private Audit audit = new Auditor(this, null, logger);
    private Audit nullAudit = new NullAudit();

    @Test
    public void expandStaticString() throws Exception {
        assertEquals("blah blah blah", new ExpandVariablesTemplate("blah blah blah").process(vars, audit));
    }

    @Test
    public void expandSingleVar() throws Exception {
        assertEquals("blah qwert blah", new ExpandVariablesTemplate("blah ${foo} blah").process(vars, audit));
    }

    @Test
    public void expandMultiVar() throws Exception {
        assertEquals("blah qwert zxcv", new ExpandVariablesTemplate("blah ${foo} ${bar}").process(vars, audit));
        assertEquals("blah qwertzxcv", new ExpandVariablesTemplate("blah ${foo}${bar}").process(vars, audit));
        assertEquals("qwertzxcv", new ExpandVariablesTemplate("${foo}${bar}").process(vars, audit));
        assertEquals("qwert zxcv baz", new ExpandVariablesTemplate("${foo} ${bar} baz").process(vars, audit));
    }

    @Test
    @Ignore("Developer benchmark")
    public void testPerformance_1k_Static() throws Exception {
        final ExpandVariablesTemplate template = new ExpandVariablesTemplate(KB);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; ++i)
                    template.process(vars, nullAudit);
            }
        };
        new BenchmarkRunner(r, 100000, 50, "static template 1kb").run();
    }

    @Test
    @Ignore("Developer benchmark")
    public void testPerformance_1k_OneVar() throws Exception {
        String str = KB.substring(0, 500) + "${foo}" + KB.substring(506);
        assertEquals(KB.length(), str.length());
        final ExpandVariablesTemplate template = new ExpandVariablesTemplate(str);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; ++i)
                    template.process(vars, nullAudit);
            }
        };
        new BenchmarkRunner(r, 1000, 50, "testPerformance_1k_OneVar").run();
    }

    @Test
    @Ignore("Developer benchmark")
    public void testPerformance_1k_TwoVars() throws Exception {
        String str = KB.substring(0, 500) + "${foo}" +
                     KB.substring(506, 600) + "${bar}" + KB.substring(606);
        assertEquals(KB.length(), str.length());
        final ExpandVariablesTemplate template = new ExpandVariablesTemplate(str);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; ++i)
                    template.process(vars, nullAudit);
            }
        };
        new BenchmarkRunner(r, 1000, 50, "testPerformance_1k_TwoVars").run();
    }

    @Test
    @Ignore("Developer benchmark")
    public void testPerformance_1k_FourVars() throws Exception {
        String str = KB.substring(0, 500) + "${foo}" +
                     KB.substring(506, 600) + "${bar}" +
                     KB.substring(606, 700) + "${baz}" +
                     KB.substring(706, 800) + "${quux}" + KB.substring(807);
        assertEquals(KB.length(), str.length());
        final ExpandVariablesTemplate template = new ExpandVariablesTemplate(str);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; ++i)
                    template.process(vars, nullAudit);
            }
        };
        new BenchmarkRunner(r, 1000, 50, "testPerformance_1k_FourVars").run();
    }

    @Test
    @Ignore("Developer benchmark")
    public void testPerformance_4k_32Vars() throws Exception {
        String str = KB.substring(0, 100) + "${foo}" +
                    KB.substring(106, 200) + "${bar}" +
                    KB.substring(206, 300) + "${baz}" +
                    KB.substring(306, 400) + "${quux}" +
                    KB.substring(407, 500) + "${bar}" +
                    KB.substring(506, 600) + "${baz}" +
                    KB.substring(606, 700) + "${quux}" +
                     KB.substring(707, 800) + "${asdf}" + KB.substring(807);
        assertEquals(KB.length(), str.length());
        str = str + str + str + str;
        final ExpandVariablesTemplate template = new ExpandVariablesTemplate(str);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; ++i)
                    template.process(vars, nullAudit);
            }
        };
        new BenchmarkRunner(r, 1000, 20, "testPerformance_4k_32Vars").run();
    }

    @Test
    @Ignore("Developer benchmark")
    public void testPerformance_64k_128Vars() throws Exception {
        String str = KB.substring(0, 100) + "${foo}" +
                    KB.substring(106, 200) + "${bar}" +
                    KB.substring(206, 300) + "${baz}" +
                    KB.substring(306, 400) + "${quux}" +
                    KB.substring(407, 500) + "${bar}" +
                    KB.substring(506, 600) + "${baz}" +
                    KB.substring(606, 700) + "${quux}" +
                     KB.substring(707, 800) + "${asdf}" + KB.substring(807);
        assertEquals(KB.length(), str.length());
        str = str + str + str + str;
        str = str + str + str + str;
        str = str + str + str + str;
        final ExpandVariablesTemplate template = new ExpandVariablesTemplate(str);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; ++i)
                    template.process(vars, nullAudit);
            }
        };
        new BenchmarkRunner(r, 1000, 20, "testPerformance_64k_128Vars").run();
    }

    public static final String KB =
            "alskdhfalsdkjfhaldksjfasdfhadkslfhalsdfhalsdfkhaldskfjhaldsfkjhalksdfjhladskfjhadsjf\n" +
            "alskdhfalsdkjfhaldksjfasdfhadkslfhalsdfhalsdfkhaldskfjhaldsfkjhalksdfjhladskfjhadsjf\n" +
            "alskdhfalsdkjfhaldksjfasdfhadkslfhalsdfhalsdfkhaldskfjhaldsfkjhalksdfjhladskfjhadsjf\n" +
            "alskdhfalsdkjfhaldksjfasdfhadkslfhalsdfhalsdfkhaldskfjhaldsfkjhalksdfjhladskfjhadsjf\n" +
            "alskdhfalsdkjfhaldksjfasdfhadkslfhalsdfhalsdfkhaldskfjhaldsfkjhalksdfjhladskfjhadsjf\n" +
            "alskdhfalsdkjfhaldksjfasdfhadkslfhalsdfhalsdfkhaldskfjhaldsfkjhalksdfjhladskfjhadsjf\n" +
            "alskdhfalsdkjfhaldksjfasdfhadkslfhalsdfhalsdfkhaldskfjhaldsfkjhalksdfjhladskfjhadsjf\n" +
            "alskdhfalsdkjfhaldksjfasdfhadkslfhalsdfhalsdfkhaldskfjhaldsfkjhalksdfjhladskfjhadsjf\n" +
            "alskdhfalsdkjfhaldksjfasdfhadkslfhalsdfhalsdfkhaldskfjhaldsfkjhalksdfjhladskfjhadsjf\n" +
            "alskdhfalsdkjfhaldksjfasdfhadkslfhalsdfhalsdfkhaldskfjhaldsfkjhalksdfjhladskfjhadsjf\n" +
            "alskdhfalsdkjfhaldksjfasdfhadkslfhalsdfhalsdfkhaldskfjhaldsfkjhalksdfjhladskfjhadsjf\n" +
            "alskdhfalsdkjfhaldksjfasdfhadkslfhalsdfhalsdfkhaldskfjhaldsfkjhalksdfjhladskfjhadsjf\n";
}
