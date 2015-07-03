package com.l7tech.gateway.standardreports;

import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.design.JRAbstractJavaCompiler;
import net.sf.jasperreports.engine.design.JRCompilationSourceCode;
import net.sf.jasperreports.engine.design.JRSourceCompileTask;
import net.sf.jasperreports.engine.design.JRCompilationUnit;
import net.sf.jasperreports.engine.design.JRClassGenerator;
import net.sf.jasperreports.engine.JRException;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.IOUtils;

import javax.tools.Diagnostic;

/**
 *
 */
public class GatewayJavaReportCompiler extends JRAbstractJavaCompiler {

    //- PUBLIC

    public GatewayJavaReportCompiler() {
        super( false );
    }

    public GatewayJavaReportCompiler(JasperReportsContext jasperReportsContext) {
        super( jasperReportsContext, false );
    }

    public static void registerClass( final Class compilationClass ) {
        classList.add( compilationClass ); 
    }

    //- PROTECTED

    @Override
    protected void checkLanguage(final String language) throws JRException {
        if ( !"java".equals(language) ) {
            throw new JRException("Compiler does not support language '"+language+"'.");    
        }
    }

    @Override
    protected JRCompilationSourceCode generateSourceCode(final JRSourceCompileTask jrSourceCompileTask) throws JRException {
        return JRClassGenerator.generateClass( jrSourceCompileTask );        
    }

    @Override
    protected String compileUnits(final JRCompilationUnit[] jrCompilationUnits, final String classpath, final File file) throws JRException {
        for ( JRCompilationUnit cu : jrCompilationUnits ) {
            Codegen codegen = new Codegen( cu.getName(), cu.getSourceCode());
            synchronized(classList) {
                for ( Class compileClass : classList ) {
                    codegen.addClassFile(compileClass.getName(), getClassSource(compileClass.getName()));
                }
            }
            try {
                byte[] reportClass = codegen.compile();
                cu.setCompileData(reportClass);
            } catch (Codegen.CompileException e) {
                return makeCompileDetailMessage(codegen.getDiagnostics());
            } catch (ClassNotFoundException e) {
                throw new JRException("Compilation failed to produce the expected class: " + ExceptionUtils.getMessage(e), e);
            }
        }

        return null; //success ...
    }

    @Override
    protected String getSourceFileName(final String unitName) {
        return unitName + ".java";
    }

    //- PRIVATE

    private static final List<Class> classList = Collections.synchronizedList( new ArrayList<Class>() );

    private static byte[] getClassSource( final String name ) {
        final String resourcePath = name.replace( '.', '/' ) + ".class";
        InputStream is = null;
        try {
            is = GatewayJavaReportCompiler.class.getClassLoader().getResourceAsStream(resourcePath);
            if (is == null)
                throw new IllegalArgumentException("Unable to find CompiledTemplate resource: " + resourcePath);
            return IOUtils.slurpStream(is);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read resource: " + resourcePath + ": " + ExceptionUtils.getMessage(e), e);
        } finally {
            ResourceUtils.closeQuietly(is);
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

    
}
