/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.codehaus.groovy.parser.antlr4;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

import java.util.*;

public class GrammarPredicates {
    private static final Set<Integer> KW_SET = new HashSet<Integer>(Arrays.asList(GroovyParser.KW_ABSTRACT, GroovyParser.KW_AS, GroovyParser.KW_ASSERT, GroovyParser.KW_BREAK, GroovyParser.KW_CASE, GroovyParser.KW_CATCH, GroovyParser.KW_CLASS, GroovyParser.KW_CONST, GroovyParser.KW_CONTINUE, GroovyParser.KW_DEF, GroovyParser.KW_DEFAULT, GroovyParser.KW_DO, GroovyParser.KW_ELSE, GroovyParser.KW_ENUM, GroovyParser.KW_EXTENDS, GroovyParser.KW_FALSE, GroovyParser.KW_FINAL, GroovyParser.KW_FINALLY, GroovyParser.KW_FOR, GroovyParser.KW_GOTO, GroovyParser.KW_IF, GroovyParser.KW_IMPLEMENTS, GroovyParser.KW_IMPORT, GroovyParser.KW_IN, GroovyParser.KW_INSTANCEOF, GroovyParser.KW_INTERFACE, GroovyParser.KW_NATIVE, GroovyParser.KW_NEW, GroovyParser.KW_NULL, GroovyParser.KW_PACKAGE, GroovyParser.KW_RETURN, GroovyParser.KW_STATIC, GroovyParser.KW_STRICTFP, GroovyParser.KW_SUPER, GroovyParser.KW_SWITCH, GroovyParser.KW_SYNCHRONIZED, GroovyParser.KW_THREADSAFE, GroovyParser.KW_THROW, GroovyParser.KW_THROWS, GroovyParser.KW_TRANSIENT, GroovyParser.KW_TRUE, GroovyParser.KW_TRY, GroovyParser.KW_VOLATILE, GroovyParser.KW_WHILE, GroovyParser.BUILT_IN_TYPE, GroovyParser.VISIBILITY_MODIFIER));

    public static boolean isClassName(TokenStream nameOrPath) {
        int index = 1;
        Token token = nameOrPath.LT(index);

        while (nameOrPath.LT(index+1).getType() == GroovyParser.DOT) {
            index += 2;
            token = nameOrPath.LT(index);
        }

        return GroovyParser.BUILT_IN_TYPE == token.getType() || Character.isUpperCase(token.getText().codePointAt(0));
    }

    /**
     * Check if the method/closure name is followed by LPAREN
     *
     * @param tokenStream
     * @return
     */
    public static boolean isFollowedByLPAREN(TokenStream tokenStream) {
        int index = 1;
        Token token = tokenStream.LT(index);
        int tokenType = token.getType();

        if (tokenType == GroovyParser.GSTRING_START) { // gstring
            do {
                token = tokenStream.LT(++index);
                tokenType = token.getType();

                if (tokenType == GroovyParser.EOF) {
                    return false;
                }
            } while (tokenType != GroovyParser.GSTRING_END);
        } else if (tokenType == GroovyParser.LCURVE) { // closure
            Deque<Integer> braceStack = new ArrayDeque<Integer>();
            braceStack.push(tokenType);

            do {
                token = tokenStream.LT(++index);
                tokenType = token.getType();

                if (tokenType == GroovyParser.EOF) {
                    return false;
                } else if (tokenType == GroovyParser.LCURVE) {
                    braceStack.push(tokenType);
                } else if (tokenType == GroovyParser.RCURVE) {
                    braceStack.pop();
                }
            } while (!braceStack.isEmpty());
        }

        // ignore the newlines
        do {
            token = tokenStream.LT(++index);
            tokenType = token.getType();
        } while (tokenType == GroovyParser.NL);

        return tokenType == GroovyParser.LPAREN;
    }

    public static boolean isKeyword(TokenStream tokenStream) {
        return KW_SET.contains(tokenStream.LT(1).getType());
    }

    public static boolean isCurrentClassName(TokenStream tokenStream, String currentClassName) {
        return tokenStream.LT(tokenStream.LT(1).getType() == GroovyParser.VISIBILITY_MODIFIER ? 2 : 1).getText().equals(currentClassName);
    }

    public static boolean isFollowedByJavaLetterInGString(CharStream cs) {
        int c1 = cs.LA(1);
        int c2 = cs.LA(2);

        String str1 = String.valueOf((char) c1);
        String str2 = String.valueOf((char) c2);

        if (str1.matches("[a-zA-Z_{]")) {
            return true;
        }

        if (str1.matches("[^\u0000-\u007F\uD800-\uDBFF]")
                && Character.isJavaIdentifierPart(c1)) {
            return true;
        }

        if (str1.matches("[\uD800-\uDBFF]")
                && str2.matches("[\uDC00-\uDFFF]")
                && Character.isJavaIdentifierPart(Character.toCodePoint((char) c1, (char) c2))) {

            return true;
        }

        return false;
    }
}
