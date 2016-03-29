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
parser grammar GroovyParser;

options { tokenVocab = GroovyLexer; }

@members {
    private String currentClassName = null; // Used for correct constructor recognition.
    private boolean declarationRuleInExpressionEnabled = false;

    private boolean isDeclarationRuleInExpressionEnabled() {
        return declarationRuleInExpressionEnabled;
    }
    private void enableDeclarationRuleInExpression() {
        declarationRuleInExpressionEnabled = true;
    }
    private void disableDeclarationRuleInExpression() {
        declarationRuleInExpressionEnabled = false;
    }
}

compilationUnit: SHEBANG_COMMENT? (NL*) packageDefinition? (NL | SEMICOLON)* (importStatement (NL | SEMICOLON)*)* (NL | SEMICOLON)* (classDeclaration | enumDeclaration | NL)* (NL | SEMICOLON)* (scriptPart (NL | SEMICOLON)+)* (scriptPart)? (NL | SEMICOLON)* EOF;

scriptPart:
    statement
    | methodDeclaration
;

packageDefinition:
    (annotationClause (NL | annotationClause)*)? KW_PACKAGE (IDENTIFIER (DOT IDENTIFIER)*);
importStatement:
    (annotationClause (NL | annotationClause)*)? KW_IMPORT KW_STATIC? (IDENTIFIER (DOT IDENTIFIER)* (DOT MULT)?) (KW_AS IDENTIFIER)?;
classDeclaration:
    ((annotationClause | classModifier) (NL | annotationClause | classModifier)*)? (AT KW_INTERFACE | KW_CLASS | KW_INTERFACE) IDENTIFIER { currentClassName = $IDENTIFIER.text; } genericDeclarationList? extendsClause? implementsClause? (NL)* classBody ;
enumDeclaration:
    ((annotationClause | classModifier) (NL | annotationClause | classModifier)*)? KW_ENUM IDENTIFIER { currentClassName = $IDENTIFIER.text; } implementsClause? (NL)* LCURVE (enumMember | NL | SEMICOLON)* RCURVE ;
classMember:
    constructorDeclaration | methodDeclaration | fieldDeclaration | objectInitializer | classInitializer | classDeclaration | enumDeclaration ;
enumMember:
    IDENTIFIER (COMMA | NL)
    | classMember
;
implementsClause:  KW_IMPLEMENTS genericClassNameExpression (COMMA genericClassNameExpression)* ;
extendsClause:  KW_EXTENDS genericClassNameExpression ;

// Members // FIXME Make more strict check for def keyword. It can't repeat.
methodDeclaration:
    (
        (memberModifier | annotationClause | KW_DEF) (memberModifier | annotationClause | KW_DEF | NL)* (
            (genericDeclarationList genericClassNameExpression) | typeDeclaration
        )?
    |
        genericClassNameExpression
    )
    (IDENTIFIER | STRING) LPAREN argumentDeclarationList RPAREN throwsClause? (KW_DEFAULT annotationParameter | methodBody)?
;

methodBody:
    LCURVE blockStatement? RCURVE
;

fieldDeclaration:
    (
        (memberModifier | annotationClause | KW_DEF) (memberModifier | annotationClause | KW_DEF | NL)* genericClassNameExpression?
        | genericClassNameExpression)
    singleDeclaration ( COMMA singleDeclaration)*
;
constructorDeclaration: { _input.LT(_input.LT(1).getType() == VISIBILITY_MODIFIER ? 2 : 1).getText().equals(currentClassName) }?
    VISIBILITY_MODIFIER? IDENTIFIER LPAREN argumentDeclarationList RPAREN throwsClause? LCURVE blockStatement? RCURVE ; // Inner NL 's handling.
objectInitializer: LCURVE blockStatement? RCURVE ;
classInitializer: KW_STATIC LCURVE blockStatement? RCURVE ;

typeDeclaration:
    (genericClassNameExpression | KW_DEF)
;

annotationClause: //FIXME handle assignment expression.
    AT genericClassNameExpression ( LPAREN ((annotationElementPair (COMMA annotationElementPair)*) | annotationElement)? RPAREN )?
;
annotationElementPair: IDENTIFIER ASSIGN annotationElement ;
annotationElement: annotationParameter | annotationClause ;

genericDeclarationList:
    LT genericsDeclarationElement (COMMA genericsDeclarationElement)* GT
;

genericsDeclarationElement: genericClassNameExpression (KW_EXTENDS genericClassNameExpression (BAND genericClassNameExpression)* )? ;

throwsClause: KW_THROWS classNameExpression (COMMA classNameExpression)*;

argumentDeclarationList:
    argumentDeclaration (COMMA argumentDeclaration)* | /* EMPTY ARGUMENT LIST */ ;
argumentDeclaration:
    annotationClause* typeDeclaration? IDENTIFIER (ASSIGN expression)? ;

blockStatement:
    (NL | SEMICOLON)+ (statement (NL | SEMICOLON)+)* statement? (NL | SEMICOLON)*
    | statement ((NL | SEMICOLON)+ statement)* (NL | SEMICOLON)*;

declarationRule: annotationClause* typeDeclaration singleDeclaration ( COMMA singleDeclaration)*;
singleDeclaration: IDENTIFIER (ASSIGN expression)?;
newInstanceRule: KW_NEW (classNameExpression (LT GT)? | genericClassNameExpression) (LPAREN argumentList? RPAREN) (classBody)?;
newArrayRule: KW_NEW classNameExpression (LBRACK INTEGER RBRACK)* ;
classBody: LCURVE (classMember | NL | SEMICOLON)* RCURVE ;

statement:
    declarationRule #declarationStatement
    | newArrayRule #newArrayStatement
    | newInstanceRule #newInstanceStatement
    | KW_FOR LPAREN {enableDeclarationRuleInExpression();} (expression)? {disableDeclarationRuleInExpression();} SEMICOLON expression? SEMICOLON expression? RPAREN NL* statementBlock #classicForStatement
    | KW_FOR LPAREN {enableDeclarationRuleInExpression();} typeDeclaration? IDENTIFIER {disableDeclarationRuleInExpression();} KW_IN expression RPAREN NL* statementBlock #forInStatement
    | KW_FOR LPAREN {enableDeclarationRuleInExpression();} typeDeclaration  IDENTIFIER {disableDeclarationRuleInExpression();} COLON expression RPAREN NL* statementBlock #forColonStatement
    | KW_IF LPAREN expression RPAREN NL* statementBlock NL* (KW_ELSE NL* statementBlock)? #ifStatement
    | KW_WHILE LPAREN expression RPAREN NL* statementBlock #whileStatement
    | KW_SWITCH LPAREN expression RPAREN NL* LCURVE
        (
          (caseStatement | NL)*
          (KW_DEFAULT COLON (statement | SEMICOLON | NL)*)?
        )
      RCURVE #switchStatement
    |  tryBlock ((catchBlock+ finallyBlock?) | finallyBlock) #tryCatchFinallyStatement
    | (KW_CONTINUE | KW_BREAK) #controlStatement
    | KW_RETURN expression? #returnStatement
    | KW_THROW expression #throwStatement
    | expression #expressionStatement
    | KW_ASSERT expression ((COLON|COMMA) NL* expression)? #assertStatement
    | cmdExpressionRule #commandExpressionStatement
;

statementBlock:
    LCURVE blockStatement? RCURVE
    | statement ;

tryBlock: KW_TRY NL* LCURVE blockStatement? RCURVE NL*;
catchBlock: KW_CATCH NL* LPAREN ((classNameExpression (BOR classNameExpression)* IDENTIFIER) | IDENTIFIER) RPAREN NL* LCURVE blockStatement? RCURVE NL*;
finallyBlock: KW_FINALLY NL* LCURVE blockStatement? RCURVE;

caseStatement: (KW_CASE expression COLON (statement | SEMICOLON | NL)* );

cmdExpressionRule: pathExpression ( argumentList IDENTIFIER)* argumentList IDENTIFIER? ;
pathExpression: (IDENTIFIER DOT)* IDENTIFIER ;
gstringPathExpression: IDENTIFIER (GSTRING_PATH_PART)* ;

closureExpressionRule: LCURVE (argumentDeclarationList CLOSURE_ARG_SEPARATOR)? blockStatement? RCURVE ;
gstringExpressionBody:( gstringPathExpression
                      | LCURVE expression? RCURVE
                      | closureExpressionRule
                      );
gstring:  GSTRING_START gstringExpressionBody (GSTRING_PART  gstringExpressionBody)* GSTRING_END ;

// Special cases.
// 1. Command expression(parenthesis-less expressions)
// 2. Annotation paramenthers.. (inline constant)
// 3. Constant expressions.
// 4. class ones, for instanceof and as (type specifier)

annotationParameter:
    LBRACK (annotationParameter (COMMA annotationParameter)*)? RBRACK #annotationParamArrayExpression
    | pathExpression #annotationParamPathExpression //class, enum or constant field
    | genericClassNameExpression #annotationParamClassExpression //class
    | STRING #annotationParamStringExpression //primitive
    | DECIMAL #annotationParamDecimalExpression //primitive
    | INTEGER #annotationParamIntegerExpression //primitive
    | KW_NULL #annotationParamNullExpression //primitive
    | (KW_TRUE | KW_FALSE) #annotationParamBoolExpression //primitive
;

expression:
      {isDeclarationRuleInExpressionEnabled()}?  declarationRule #declarationExpression
    | newArrayRule #newArrayExpression
    | newInstanceRule #newInstanceExpression
    | closureExpressionRule #closureExpression
    | LBRACK (expression (COMMA expression)* COMMA?)?  RBRACK #listConstructor
    | LBRACK (COLON | (mapEntry (COMMA mapEntry)*) COMMA?) RBRACK #mapConstructor
    | KW_SUPER LPAREN argumentList? RPAREN  #constructorCallExpression
    | expression (DOT | SAFE_DOT | STAR_DOT | ATTR_DOT | MEMBER_POINTER) (selectorName | STRING | gstring) #fieldAccessExpression
    | pathExpression (LPAREN argumentList? RPAREN)? closureExpressionRule* #callExpression
    | LPAREN expression RPAREN #parenthesisExpression
    | MULT expression #spreadExpression
    | expression (DECREMENT | INCREMENT)  #postfixExpression
    | (NOT | BNOT) expression #unaryExpression
    | LPAREN genericClassNameExpression RPAREN expression #castExpression
    | (PLUS | MINUS) expression #unaryExpression
    | (DECREMENT | INCREMENT) expression #prefixExpression
    | expression LBRACK (expression (COMMA expression)*)? RBRACK #indexExpression
    | expression POWER expression #binaryExpression
    | expression MULT expression #binaryExpression
    | expression DIV expression #binaryExpression
    | expression MOD expression #binaryExpression
    | expression PLUS expression #binaryExpression
    | expression MINUS expression #binaryExpression
    | expression LSHIFT expression #binaryExpression
    | expression GT GT expression #binaryExpression
    | expression GT GT GT expression #binaryExpression
    | expression RANGE expression #binaryExpression
    | expression ORANGE expression #binaryExpression
    | expression GT expression #binaryExpression
    | expression GTE expression #binaryExpression
    | expression LT expression #binaryExpression
    | expression LTE expression #binaryExpression
    | expression KW_IN expression #binaryExpression
    | expression KW_AS genericClassNameExpression #binaryExpression
    | expression KW_INSTANCEOF genericClassNameExpression #binaryExpression
    | expression EQUAL expression #binaryExpression
    | expression UNEQUAL expression #binaryExpression
    | expression SPACESHIP expression #binaryExpression
    | expression FIND expression #binaryExpression
    | expression MATCH expression #binaryExpression
    | expression BAND expression #binaryExpression
    |<assoc=right> expression XOR expression #binaryExpression
    | expression BOR expression #binaryExpression
    | expression AND expression #binaryExpression
    | expression OR expression #binaryExpression
    |<assoc=right> expression QUESTION NL* expression NL* COLON NL* expression #ternaryExpression
    | expression ELVIS NL* expression #elvisExpression
    | expression (DOT | SAFE_DOT | STAR_DOT) (selectorName | STRING | gstring) ((LPAREN argumentList? RPAREN)| argumentList) #methodCallExpression
    |<assoc=right> expression (ASSIGN | PLUS_ASSIGN | MINUS_ASSIGN | MULT_ASSIGN | DIV_ASSIGN | MOD_ASSIGN | BAND_ASSIGN | XOR_ASSIGN | BOR_ASSIGN | LSHIFT_ASSIGN | RSHIFT_ASSIGN | RUSHIFT_ASSIGN) expression #assignmentExpression
    | STRING #constantExpression
    | gstring #gstringExpression
    | DECIMAL #constantDecimalExpression
    | INTEGER #constantIntegerExpression
    | KW_NULL #nullExpression
    | (KW_TRUE | KW_FALSE) #boolExpression
    | IDENTIFIER #variableExpression
;

classNameExpression: { GrammarPredicates.isClassName(_input) }? IDENTIFIER (DOT IDENTIFIER)* ;

genericClassNameExpression: classNameExpression (genericList | (LBRACK RBRACK))?;

genericList:
    LT genericListElement (COMMA genericListElement)* GT
;

genericListElement:
    genericClassNameExpression #genericsConcreteElement
    | QUESTION (KW_EXTENDS genericClassNameExpression | KW_SUPER genericClassNameExpression)? #genericsWildcardElement
;

mapEntry:
    STRING COLON expression
    | gstring COLON expression
    | selectorName COLON expression
    | LPAREN expression RPAREN COLON expression
    | MULT COLON expression
    | DECIMAL COLON expression
    | INTEGER COLON expression
;

classModifier: //JSL7 8.1 FIXME Now gramar allows modifier duplication. It's possible to make it more strict listing all 24 permutations.
VISIBILITY_MODIFIER | KW_STATIC | (KW_ABSTRACT | KW_FINAL) | KW_STRICTFP ;

memberModifier:
    VISIBILITY_MODIFIER | KW_STATIC | (KW_ABSTRACT | KW_FINAL) | KW_NATIVE | KW_SYNCHRONIZED | KW_TRANSIENT | KW_VOLATILE ;

argumentList: ( (closureExpressionRule)+ | argument (COMMA argument)*) ;

argument:
    mapEntry
    | expression
;

selectorName:
    IDENTIFIER | KW_ABSTRACT | KW_AS | KW_ASSERT | KW_BREAK | KW_CASE | KW_CATCH | KW_CLASS | KW_CONTINUE
     | KW_DEF | KW_DEFAULT | KW_ELSE | KW_ENUM | KW_EXTENDS | KW_FALSE | KW_FINAL | KW_FINALLY
     | KW_FOR | KW_IF | KW_IMPLEMENTS | KW_IMPORT | KW_IN | KW_INSTANCEOF | KW_INTERFACE
     | KW_NATIVE | KW_NEW | KW_NULL | KW_PACKAGE
     | KW_RETURN | KW_STATIC | KW_STRICTFP | KW_SUPER | KW_SWITCH | KW_SYNCHRONIZED | KW_THROW
     | KW_THROWS | KW_TRANSIENT | KW_TRUE | KW_TRY | KW_VOLATILE | KW_WHILE
     | VISIBILITY_MODIFIER /* in place of KW_PRIVATE | KW_PROTECTED | KW_PUBLIC */
;