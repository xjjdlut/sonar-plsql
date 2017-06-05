/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plsqlopen;

import java.io.File;
import java.util.List;

import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.plsqlopen.checks.PlSqlVisitor;
import org.sonar.plsqlopen.metadata.FormsMetadata;
import org.sonar.plugins.plsqlopen.api.symbols.Scope;
import org.sonar.plugins.plsqlopen.api.symbols.SymbolTable;
import org.sonar.squidbridge.SquidAstVisitorContextImpl;
import org.sonar.squidbridge.api.SourceProject;

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.RecognitionException;
import com.sonar.sslr.api.Token;

public class DefaultPlSqlVisitorContext<G extends Grammar> extends SquidAstVisitorContextImpl<G> implements PlSqlVisitorContext {

    private SonarComponents components;
    private SymbolTable symbolTable;
    private Scope scope;
    private AstNode rootTree;
    private PlSqlFile plSqlFile;
    private RecognitionException parsingException;
    
    public DefaultPlSqlVisitorContext(SourceProject project, SonarComponents components) {
        super(project);
        this.components = components;
    }
    
    public DefaultPlSqlVisitorContext(AstNode rootTree, PlSqlFile plSqlFile, SonarComponents components) {
        this(rootTree, plSqlFile, null, components);
    }

    public DefaultPlSqlVisitorContext(PlSqlFile pythonFile, RecognitionException parsingException, SonarComponents components) {
        this(null, pythonFile, parsingException, components);
    }

    private DefaultPlSqlVisitorContext(AstNode rootTree, PlSqlFile plSqlFile, RecognitionException parsingException, SonarComponents components) {
        super(new SourceProject("noSquid"));
        this.rootTree = rootTree;
        this.plSqlFile = plSqlFile;
        this.parsingException = parsingException;
        this.components = components;
    }
    
    private File getFileInternal() {
        File file = getFile();
        if (file == null) {
            file = plSqlFile.file();
        }
        return file;
    }

    public AstNode rootTree() {
        return rootTree;
    }

    public PlSqlFile plSqlFile() {
        return plSqlFile;
    }

    public RecognitionException parsingException() {
        return parsingException;
    }
    
    @Override
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }

    @Override
    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }
    
    @Override
    public NewSymbolTable getSymbolizable() {
        return components.symbolizableFor(components.inputFromIOFile(getFileInternal()));
    }
    
    @Override
    public NewHighlighting getHighlighting() {
        return components.highlightingFor(components.inputFromIOFile(getFileInternal()));
    }
    
    @Override
    public void setCurrentScope(Scope scope) {
        this.scope = scope;
    }
    
    @Override
    public Scope getCurrentScope() {
        return scope;
    }

    @Override
    public FormsMetadata getFormsMetadata() {
        return components.getFormsMetadata();
    }
    
    @Override
    public void createFileViolation(PlSqlVisitor check, String message, Object... messageParameters) {
        AnalyzerMessage checkMessage = new AnalyzerMessage(check, message, null, messageParameters);
        components.reportIssue(checkMessage, components.inputFromIOFile(getFileInternal()));
        if (peekSourceCode().getName() != null) {
            log(checkMessage);
        }
    }
    
    @Override
    public void createLineViolation(PlSqlVisitor check, String message, AstNode node, Object... messageParameters) {
        createLineViolation(check, message, node.getToken(), messageParameters);
    }
    
    @Override
    public void createLineViolation(PlSqlVisitor check, String message, Token token, Object... messageParameters) {
      createLineViolation(check, message, token.getLine(), messageParameters);
    }
    
    @Override
    public void createLineViolation(PlSqlVisitor check, String message, int line, Object... messageParameters) {
        AnalyzerMessage checkMessage = new AnalyzerMessage(check, message, null, messageParameters);
        if (line > 0) {
            checkMessage.setLine(line);
        }
        components.reportIssue(checkMessage, components.inputFromIOFile(getFileInternal()));
        if (peekSourceCode().getName() != null) {
            log(checkMessage);
        }
    }
    
    @Override
    public void createViolation(PlSqlVisitor check, String message, AstNode node, Object... messageParameters) {
        createViolation(check, message, node, ImmutableList.<Location>of(), messageParameters);
    }
    
    @Override
    public void createViolation(PlSqlVisitor check, String message, AstNode node, List<Location> secondary, Object... messageParameters) {
        AnalyzerMessage checkMessage = new AnalyzerMessage(check, message, AnalyzerMessage.textSpanFor(node), messageParameters);
        for (Location location : secondary) {
            AnalyzerMessage secondaryLocation = 
                    new AnalyzerMessage(check, location.msg, AnalyzerMessage.textSpanFor(location.node), messageParameters);
            checkMessage.addSecondaryLocation(secondaryLocation);
        }
        components.reportIssue(checkMessage, components.inputFromIOFile(getFileInternal()));
        if (peekSourceCode().getName() != null) {
            log(checkMessage);
        }
    }

}
