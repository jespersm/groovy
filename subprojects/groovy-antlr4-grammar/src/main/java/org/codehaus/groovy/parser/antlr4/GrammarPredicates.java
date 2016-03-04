package org.codehaus.groovy.parser.antlr4;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

import java.util.Arrays;

public class GrammarPredicates {
    private static final String[] primitiveClassNames = new String[] {
        "boolean", "byte", "char", "double",
        "float", "int", "long", "short", "void"
    };

    public static boolean isClassName(TokenStream nameOrPath) {
        int index = 1;
        Token token = nameOrPath.LT(index);
        while (nameOrPath.LT(index+1).getType() == GroovyParser.DOT) {
            index += 2;
            token = nameOrPath.LT(index);
        }
        String tokenText = token.getText();
        if (Arrays.binarySearch(primitiveClassNames, tokenText) >= 0) return true;
        return Character.isUpperCase(tokenText.codePointAt(0));
    }

}
