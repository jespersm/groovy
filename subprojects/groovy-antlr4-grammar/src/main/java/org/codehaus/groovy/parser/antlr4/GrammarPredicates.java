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

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GrammarPredicates {
    public static final Set<Integer> KW_SET = new HashSet<Integer>(Arrays.asList(GroovyParser.KW_ABSTRACT, GroovyParser.KW_AS, GroovyParser.KW_ASSERT, GroovyParser.KW_BREAK, GroovyParser.KW_CASE, GroovyParser.KW_CATCH, GroovyParser.KW_CLASS, GroovyParser.KW_CONTINUE, GroovyParser.KW_DEF, GroovyParser.KW_DEFAULT, GroovyParser.KW_ELSE, GroovyParser.KW_ENUM, GroovyParser.KW_EXTENDS, GroovyParser.KW_FALSE, GroovyParser.KW_FINAL, GroovyParser.KW_FINALLY, GroovyParser.KW_FOR, GroovyParser.KW_IF, GroovyParser.KW_IMPLEMENTS, GroovyParser.KW_IMPORT, GroovyParser.KW_IN, GroovyParser.KW_INSTANCEOF, GroovyParser.KW_INTERFACE, GroovyParser.KW_NATIVE, GroovyParser.KW_NEW, GroovyParser.KW_NULL, GroovyParser.KW_PACKAGE, GroovyParser.KW_RETURN, GroovyParser.KW_STATIC, GroovyParser.KW_STRICTFP, GroovyParser.KW_SUPER, GroovyParser.KW_SWITCH, GroovyParser.KW_SYNCHRONIZED, GroovyParser.KW_THROW, GroovyParser.KW_THROWS, GroovyParser.KW_TRANSIENT, GroovyParser.KW_TRUE, GroovyParser.KW_TRY, GroovyParser.KW_VOLATILE, GroovyParser.KW_WHILE, GroovyParser.VISIBILITY_MODIFIER));
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

    /**
     * Check if the method/closure name is followed by LPAREN
     *
     * @param tokenStream
     * @return
     */
    public static boolean isFollowedByLPAREN(TokenStream tokenStream) {
        int index = 1;
        Token token = tokenStream.LT(index);

        if (token.getType() == GroovyParser.GSTRING_START) {
            do {
                index++;
            } while (tokenStream.LT(index).getType() != GroovyParser.GSTRING_END);
        }

        return tokenStream.LT(index + 1).getType() == GroovyParser.LPAREN;
    }

    public static boolean isKeyword(TokenStream tokenStream) {
        return KW_SET.contains(tokenStream.LT(1).getType());
    }

    public static boolean isCurrentClassName(TokenStream tokenStream, String currentClassName) {
        return tokenStream.LT(tokenStream.LT(1).getType() == GroovyParser.VISIBILITY_MODIFIER ? 2 : 1).getText().equals(currentClassName);
    }
}
