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

import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.parser.antlr4.GroovyParser;
import org.codehaus.groovy.parser.antlr4.builders.ASTBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.asBoolean;

/**
 * Created by Daniel on 2016/4/9.
 */
public class BuildableSwitchStatementContext extends BuildableBaseNode {
    private GroovyParser.SwitchStatementContext ctx;

    public BuildableSwitchStatementContext(ASTBuilder astBuilder, GroovyParser.SwitchStatementContext ctx) {
        super(astBuilder);

        this.ctx = ctx;
    }

    @Override
    public Statement build() {
        List<CaseStatement> caseStatements = new ArrayList<CaseStatement>();
        for (GroovyParser.CaseStatementContext caseStmt : ctx.caseStatement()) {
            BlockStatement stmt = new BlockStatement();// #BSC
            for (GroovyParser.StatementContext st : caseStmt.statement()) {
                astBuilder.unpackStatement (stmt, astBuilder.parseStatement(st));
            }
            caseStatements.add(astBuilder.setupNodeLocation(new CaseStatement(astBuilder.parseExpression(caseStmt.expression()), stmt), caseStmt.KW_CASE().getSymbol()));// There only 'case' kw was highlighted in parser old version.
        }


        Statement defaultStatement;
        if (asBoolean(ctx.KW_DEFAULT())) {
            defaultStatement = new BlockStatement();// #BSC
            for (GroovyParser.StatementContext stmt : ctx.statement())
                astBuilder.unpackStatement((BlockStatement)defaultStatement, astBuilder.parseStatement(stmt));
        } else defaultStatement = EmptyStatement.INSTANCE;// TODO Refactor empty stataements and expressions.

        return new SwitchStatement(astBuilder.parseExpression(ctx.expression()), caseStatements, defaultStatement);
    }
}
