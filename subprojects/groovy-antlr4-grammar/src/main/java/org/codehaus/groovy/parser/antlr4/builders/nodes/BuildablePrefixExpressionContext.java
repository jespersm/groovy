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
package org.codehaus.groovy.parser.antlr4.builders.nodes;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.PrefixExpression;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildablePrefixExpressionContext extends BuildableBaseNode {
    private GroovyParser.PrefixExpressionContext ctx;

    public BuildablePrefixExpressionContext(ASTBuilder astBuilder, GroovyParser.PrefixExpressionContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public Expression build() {
        return astBuilder.setupNodeLocation(new PrefixExpression(astBuilder.createToken(DefaultGroovyMethods.asType(ctx.getChild(0), TerminalNode.class)), astBuilder.parseExpression(ctx.expression())), ctx);
    }
}
