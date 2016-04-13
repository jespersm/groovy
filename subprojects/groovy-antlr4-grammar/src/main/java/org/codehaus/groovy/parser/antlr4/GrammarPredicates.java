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

}
