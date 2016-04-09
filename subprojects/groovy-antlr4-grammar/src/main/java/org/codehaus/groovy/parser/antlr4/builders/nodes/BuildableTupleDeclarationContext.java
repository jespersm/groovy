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

import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.EmptyExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.InvalidSyntaxException;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;
import org.codehaus.groovy.syntax.Types;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Daniel on 2016/4/10.
 */
public class BuildableTupleDeclarationContext extends BuildableBaseNode {
    private GroovyParser.TupleDeclarationContext ctx;

    public BuildableTupleDeclarationContext(ASTBuilder astBuilder, GroovyParser.TupleDeclarationContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public DeclarationExpression build() {
        // tuple must have an initial value.
        if (null == ctx.expression()) {
            throw new CompilationFailedException(CompilePhase.PARSING.getPhaseNumber(), astBuilder.sourceUnit,
                    new InvalidSyntaxException("tuple declaration must have an initial value.", ctx));
        }

        List<Expression> variables = new LinkedList<Expression>();

        for (GroovyParser.TupleVariableDeclarationContext tupleVariableDeclarationContext : ctx.tupleVariableDeclaration()) {
            variables.add(astBuilder.parseTupleVariableDeclaration(tupleVariableDeclarationContext));
        }

        ArgumentListExpression argumentListExpression = new ArgumentListExpression(variables);
        org.codehaus.groovy.syntax.Token token = astBuilder.createGroovyToken(ctx.ASSIGN().getSymbol(), Types.ASSIGN);

        Expression initialValue = (ctx != null) ? astBuilder.parseExpression(ctx.expression())
                : astBuilder.setupNodeLocation(new EmptyExpression(),ctx);

        DeclarationExpression declarationExpression  = new DeclarationExpression(argumentListExpression, token, initialValue);

        return astBuilder.setupNodeLocation(declarationExpression, ctx);
    }
}
