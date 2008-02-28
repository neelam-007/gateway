package com.l7tech.external.assertions.ipm.server;

import com.l7tech.common.util.*;
import com.l7tech.common.xml.DomElementCursor;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.external.assertions.ipm.server.resources.CompiledTemplate;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dynamically generates a Java class that will do expansion of the specified IPM template.
 */
public class TemplateCompiler {
    private static final AtomicLong counter = new AtomicLong(100000);
    private static final String PACKAGE_NAME = CompiledTemplate.class.getPackage().getName();
    private static final String COMPILED_TEMPLATE_CLASSNAME = CompiledTemplate.class.getName();
    private static final Pattern PICTURE_PATTERN = Pattern.compile("(.+?)\\((\\d+)\\)");

    private final String templateStr;
    private final String templateSimpleName;
    private final String templateClassName;
    private final List<WorkUnit> workUnits = new ArrayList<WorkUnit>();
    private String javaSource;

    /**
     * Prepare to compile the specified IPM template.
     *
     * @param template the template to compile.  Required.
     */
    public TemplateCompiler(String template) {
        this.templateStr = template;
        this.templateSimpleName = "DynamicCompiledTemplate" + counter.incrementAndGet();
        this.templateClassName = PACKAGE_NAME + '.' + templateSimpleName;
    }

    /**
     * Attempt to compile the current IPM template.
     *
     * @return a new CompiledTemplate instance that can be used to expand IPM messages into XML.  Never null.
     * @throws TemplateCompilerException if a CompiledTemplate instance could not be produced.
     */
    public CompiledTemplate compile() throws TemplateCompilerException {
        Document doc = parseXml(templateStr);
        javaSource = generateSource(templateSimpleName, doc);
        return compileSource(templateClassName, javaSource);
    }

    String getJavaSource() {
        return javaSource;
    }

    private static CompiledTemplate compileSource(String templateClassName, String javaSource) throws TemplateCompilerException {
        Codegen codegen = new Codegen(templateClassName, javaSource);
        codegen.addJavaFile(COMPILED_TEMPLATE_CLASSNAME, getCompiledTemplateSource());
        try {
            Class templateClass = codegen.compile(CompiledTemplate.class.getClassLoader());
            Object obj = templateClass.newInstance();
            if (!(obj instanceof CompiledTemplate))
                throw new TemplateCompilerException("Compilation resulted in object of unexpected type: " + obj.getClass());
            return (CompiledTemplate)obj;
        } catch (Codegen.CompileException e) {
            throw new TemplateCompilerException(makeCompileDetailMessage(codegen.getDiagnostics()), e);
        } catch (ClassNotFoundException e) {
            throw new TemplateCompilerException("Compilation failed to produce the expected class: " + ExceptionUtils.getMessage(e), e);
        } catch (IllegalAccessException e) {
            throw new TemplateCompilerException("Compilation failed to produce a usable class: " + ExceptionUtils.getMessage(e), e);
        } catch (InstantiationException e) {
            throw new TemplateCompilerException("Compilation failed to produce a usable class: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private static String makeCompileDetailMessage(List<? extends Diagnostic> diagnostics) {
        StringBuilder sb = new StringBuilder("Compilation failed: ");

        for (Diagnostic diagnostic : diagnostics) {
            sb.append('[');
            sb.append(" code=").append(diagnostic.getCode());
            sb.append(" kind=").append(diagnostic.getKind());
            sb.append(" position=").append(diagnostic.getPosition());
            sb.append(" startPosition=").append(diagnostic.getStartPosition());
            sb.append(" endPosition=").append(diagnostic.getEndPosition());
            sb.append(" source=").append(diagnostic.getSource());
            sb.append(" message=\"").append(diagnostic.getMessage(null)).append('\"');
            sb.append("]\n");
        }

        return sb.toString();
    }

    private String generateSource(String templateSimpleName, Document doc) throws TemplateCompilerException {

        analyzeTemplate(doc);

        StringWriter writer = new StringWriter();
        PrintWriter out = null;
        try {
            out = new PrintWriter(writer);

            genHeader(out, PACKAGE_NAME, COMPILED_TEMPLATE_CLASSNAME, templateSimpleName);
            genWorkUnits(out);
            genFooter(out);

            return writer.toString();
        } finally {
            if (out != null) out.close();
        }
    }

    private void analyzeTemplate(Document doc) throws TemplateCompilerException {
        ElementCursor cursor = new DomElementCursor(doc);
        cursor.moveToDocumentElement();
        collectElement(cursor, 1, 0);
    }

    private void collectElement(ElementCursor cursor, int indent, final int depth) throws TemplateCompilerException {
        String type = cursor.getAttributeValue("type");
        Integer occurs = getIntAttr(cursor, "occurs");
        if (occurs == null) occurs = 1;

        if (occurs > 1) {
            addWorkUnit(indent, "for (int i" + depth + " = 0; i" + depth + " < " + occurs + "; i" + depth + "++) {\n");
            indent++;
        }

        String elname = cursor.getLocalName();
        addEmitOpenTag(indent, elname);
        if (type != null && type.length() > 0)
            addEmitCopiedChars(indent, type);
        try {
            final int subindent = indent;
            cursor.visitChildElements(new ElementCursor.Visitor() {
                public void visit(ElementCursor ec) throws InvalidDocumentFormatException {
                    try {
                        collectElement(ec, subindent, depth + 1);
                    } catch (TemplateCompilerException e) {
                        throw new InvalidDocumentFormatException(e);
                    }
                }
            });
        } catch (InvalidDocumentFormatException e) {
            TemplateCompilerException tce = ExceptionUtils.getCauseIfCausedBy(e, TemplateCompilerException.class);
            if (tce != null) throw tce;
            throw new TemplateCompilerException("Invalid template format: " + ExceptionUtils.getMessage(e), e);
        }
        addEmitCloseTag(indent, elname);

        if (occurs > 1) {
            indent--;
            addWorkUnit(indent, "}\n");
        }
    }

    // Add a work unit that emits an open tag
    private void addEmitOpenTag(int indent, String elname) {
        addEmitConstant(indent, '<' + elname + '>');
    }

    // Add a work unit that emits a close tag
    private void addEmitCloseTag(int indent, String elname) {
        addEmitConstant(indent, "</" + elname + ">\n");
    }

    // Add a work unit that emits a constant string
    private void addEmitConstant(int indent, String constant) {
        String escapedConstant = constant.replaceAll("\\\"", "\\\"").replaceAll("\n", "\\\\\\n"); // TODO quote other special chars etc
        addWorkUnit(indent, "write(\"" + escapedConstant + "\".toCharArray());\n");
    }

    private void addEmitCopiedChars(int indent, String picture) throws TemplateCompilerException {
        final Matcher matcher = PICTURE_PATTERN.matcher(picture);
        if (!matcher.matches())
            throw new TemplateCompilerException("Unrecognized 'type' attribute: " + picture);
        String countStr = matcher.group(2);
        int count;
        try {
            count = Integer.parseInt(countStr);
            assert count >= 0;
        } catch (NumberFormatException nfe) {
            throw new TemplateCompilerException("Unrecognized 'type' attribute: " + picture, nfe);
        }
        addWorkUnit(indent, "copy(" + count + ");\n");
    }

    // Add a work unit that passes through the specified literal Java code
    private void addWorkUnit(int indent, String code) {
        for (int i = 0; i < indent; ++i)
            code = "    " + code;
        workUnits.add(new WorkUnit(code));
    }

    private static Integer getIntAttr(ElementCursor cursor, String attrName) throws TemplateCompilerException {
        try {
            final String val = cursor.getAttributeValue(attrName);
            return val == null || val.length() < 1 ? null : Integer.parseInt(val);
        } catch (NumberFormatException nfe) {
            throw new TemplateCompilerException("Attribute " + attrName + " of element " + cursor.getLocalName() + " is not an integer: " + ExceptionUtils.getMessage(nfe), nfe);
        }
    }

    private static void genHeader(PrintWriter out, String packageName, String compiledTemplateClassName, String templateSimpleName) {
        out.printf("package %s;\n\n", packageName);
        out.printf("public class %s extends %s {\n", templateSimpleName, compiledTemplateClassName);
        out.printf("  protected void doExpand() throws java.io.IOException {\n");
        out.printf("    ip = 0;\n");
        out.printf("    op = 0;\n");
    }

    private void genWorkUnits(PrintWriter out) {
        for (WorkUnit workUnit : workUnits) {
            workUnit.generate(out);
        }
    }

    private static void genFooter(PrintWriter out) {
        out.printf("  }\n");
        out.printf("}\n");
    }

    private static Document parseXml(String template) throws TemplateCompilerException {
        try {
            return XmlUtil.stringToDocument(template);
        } catch (SAXException e) {
            throw new TemplateCompilerException("Template is not well formed XML: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public static String getCompiledTemplateSource() throws TemplateCompilerException {
        final String ctPackage = CompiledTemplate.class.getPackage().getName();
        final String ctResourceDir = ctPackage.replaceAll("\\.", "/") + '/';
        final String resourcePath = ctResourceDir + "CompiledTemplate.java";
        InputStream is = null;
        try {
            is = CompiledTemplate.class.getClassLoader().getResourceAsStream(resourcePath);
            if (is == null)
                throw new TemplateCompilerException("Unable to find CompiledTemplate resource: " + resourcePath);
            byte[] bytes = HexUtils.slurpStream(is);
            return new String(bytes);
        } catch (IOException e) {
            throw new TemplateCompilerException("Unable to read resource: " + resourcePath + ": " + ExceptionUtils.getMessage(e), e);
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

    private static class WorkUnit {
        private final String code;

        private WorkUnit(String code) {
            this.code = code;
        }

        private void generate(PrintWriter out) {
            if (code != null) out.print(code);
        }
    }
}
