package org.codehaus.groovy.parser.antlr4;

import org.codehaus.groovy.control.ParserPlugin;
import org.codehaus.groovy.control.ParserPluginFactory;

public class Antlrv4PluginFactory extends ParserPluginFactory {
    @java.lang.Override
    public ParserPlugin createParserPlugin() {
        return new org.codehaus.groovy.parser.antlr4.Antlrv4ParserPlugin();
    }

}
