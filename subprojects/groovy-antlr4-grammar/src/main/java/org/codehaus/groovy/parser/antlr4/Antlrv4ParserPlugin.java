package org.codehaus.groovy.parser.antlr4;

import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.ParserPlugin;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.syntax.ParserException;
import org.codehaus.groovy.syntax.Reduction;

public class Antlrv4ParserPlugin implements ParserPlugin {
    @java.lang.Override
    public Reduction parseCST(SourceUnit sourceUnit, java.io.Reader reader) throws CompilationFailedException {
        return null;
    }

    @java.lang.Override
    public ModuleNode buildAST(SourceUnit sourceUnit, java.lang.ClassLoader classLoader, Reduction cst) throws ParserException {

        ASTBuilder builder = new ASTBuilder(sourceUnit, classLoader);
        return builder.getModuleNode();
    }

}
