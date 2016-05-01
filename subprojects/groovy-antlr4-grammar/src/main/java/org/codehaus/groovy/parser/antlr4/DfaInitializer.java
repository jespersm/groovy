package org.codehaus.groovy.parser.antlr4;

import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.dfa.DFA;

/**
 * Created by Daniel on 2016/5/2.
 */
public class DfaInitializer {
    private ATN atn;

    public DfaInitializer(GroovyLangLexer lexer) {
        atn = lexer.getATN();
    }

    public DfaInitializer(GroovyLangParser parser) {
        atn = parser.getATN();
    }

    /**
     *
     * Jesper's workaround for ever-growing cache in org.codehaus.groovy.parser.antlr4.GroovyParser#_decisionToDFA
     *
     */
    public DFA[] createDecisionToDFA() {
        DFA[] decisionToDFA = new DFA[atn.getNumberOfDecisions()];

        for(int i = 0; i < decisionToDFA.length; ++i) {
            decisionToDFA[i] = new DFA(atn.getDecisionState(i), i);
        }

        return decisionToDFA;
    }
}
