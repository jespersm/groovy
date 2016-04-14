package org.codehaus.groovy.parser.antlr4;

import org.antlr.v4.runtime.CharStream;

/**
 * Created by Daniel on 2016/4/14.
 */
public class GroovyScanner extends GroovyLexer {
    public GroovyScanner(CharStream input) {
        super(input);
        _interp = new PositionAdjustingLexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }
}
