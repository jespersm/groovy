package org.codehaus.groovy.parser.antlr4;

import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;

/**
 * Created by Daniel on 2016/5/2.
 */
public class GroovyLangParser extends GroovyParser {
    public GroovyLangParser(TokenStream input) {
        super(input);

        this.setInterpreter(new ParserATNSimulator(this, this.getATN(), new DfaInitializer(this).createDecisionToDFA(), new PredictionContextCache()));
    }
}
