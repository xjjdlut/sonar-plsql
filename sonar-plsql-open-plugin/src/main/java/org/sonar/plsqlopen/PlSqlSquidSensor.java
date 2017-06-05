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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.sonar.api.batch.fs.FilePredicates;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.config.Settings;
import org.sonar.plsqlopen.checks.CheckList;
import org.sonar.plsqlopen.checks.PlSqlCheck;
import org.sonar.plsqlopen.highlight.PlSqlHighlighterVisitor;
import org.sonar.plsqlopen.lexer.PlSqlLexer;
import org.sonar.plsqlopen.squid.PlSqlAstScanner;
import org.sonar.plsqlopen.squid.PlSqlConfiguration;
import org.sonar.plsqlopen.squid.SonarQubePlSqlFile;
import org.sonar.plsqlopen.symbols.SymbolVisitor;
import org.sonar.squidbridge.AstScanner;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.Grammar;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.impl.Lexer;

public class PlSqlSquidSensor implements Sensor {

    private final PlSqlChecks checks;

    private AstScanner<Grammar> scanner;
    private SonarComponents components;
    private SensorContext context;
    private PlSqlConfiguration configuration;
    
    public PlSqlSquidSensor(CheckFactory checkFactory, SonarComponents components, Settings settings) {
        this(checkFactory, components, settings, null);
    }

    public PlSqlSquidSensor(CheckFactory checkFactory, SonarComponents components, Settings settings,
            @Nullable CustomPlSqlRulesDefinition[] customRulesDefinition) {
        this.checks = PlSqlChecks.createPlSqlCheck(checkFactory)
                .addChecks(CheckList.REPOSITORY_KEY, CheckList.getChecks())
                .addCustomChecks(customRulesDefinition);
        this.components = components;
        this.components.loadMetadataFile(settings.getString(PlSqlPlugin.FORMS_METADATA_KEY));
        components.setChecks(checks);
    }
    
    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor
            .name("PlsqlSquidSensor")
            .onlyOnLanguage(PlSql.KEY);
    }

    @Override
    public void execute(SensorContext context) {
        this.context = context;
        List<PlSqlCheck> visitors = new ArrayList<>();
        visitors.add(new SymbolVisitor());
        visitors.add(new PlSqlHighlighterVisitor());
        visitors.addAll(checks.all());
        configuration = new PlSqlConfiguration(context.fileSystem().encoding());
        
        FilePredicates p = context.fileSystem().predicates();
        ArrayList<InputFile> inputFiles = Lists.newArrayList(context.fileSystem().inputFiles(p.and(p.hasType(InputFile.Type.MAIN), p.hasLanguage(PlSql.KEY))));
        
        PlSqlAstScanner scan = new PlSqlAstScanner(context, visitors, inputFiles, components);
        scan.scanFiles();
        
        for (InputFile file : inputFiles) {
            saveCpdTokens(file);
        }
    }

//    private void save(Collection<SourceCode> squidSourceFiles) {
//        for (SourceCode squidSourceFile : squidSourceFiles) {
//            SourceFile squidFile = (SourceFile) squidSourceFile;
//
//            InputFile inputFile = context.fileSystem().inputFile(context.fileSystem().predicates()
//                    .is(new File(squidFile.getKey())));
//
//            if (inputFile != null) {
//                saveFilesComplexityDistribution(inputFile, squidFile);
//                saveFunctionsComplexityDistribution(inputFile, squidFile);
//                saveMeasures(inputFile, squidFile);
//                saveCpdTokens(inputFile);
//            }
//        }
//    }

//    private void saveMeasures(InputFile sonarFile, SourceFile squidFile) {
//        context.<Integer>newMeasure()
//                .on(sonarFile)
//                .forMetric(CoreMetrics.FILES)
//                .withValue(squidFile.getInt(PlSqlMetric.FILES))
//                .save();
//        
//        context.<Integer>newMeasure().on(sonarFile)
//                .forMetric(CoreMetrics.NCLOC)
//                .withValue(squidFile.getInt(PlSqlMetric.LINES_OF_CODE))
//                .save();
//        
//        context.<Integer>newMeasure().on(sonarFile)
//                .forMetric(CoreMetrics.COMMENT_LINES)
//                .withValue(squidFile.getInt(PlSqlMetric.COMMENT_LINES))
//                .save();
//        
//        context.<Integer>newMeasure().on(sonarFile)
//                .forMetric(CoreMetrics.COMPLEXITY)
//                .withValue(squidFile.getInt(PlSqlMetric.COMPLEXITY))
//                .save();
//        
//        context.<Integer>newMeasure().on(sonarFile)
//                .forMetric(CoreMetrics.FUNCTIONS)
//                .withValue(squidFile.getInt(PlSqlMetric.METHODS))
//                .save();
//        
//        context.<Integer>newMeasure().on(sonarFile)
//                .forMetric(CoreMetrics.STATEMENTS)
//                .withValue(squidFile.getInt(PlSqlMetric.STATEMENTS))
//                .save();
//    }
    
//    private void saveFunctionsComplexityDistribution(InputFile sonarFile, SourceFile squidFile) {
//        Collection<SourceCode> squidFunctionsInFile = scanner.getIndex().search(new QueryByParent(squidFile),
//                new QueryByType(SourceFunction.class));
//        RangeDistributionBuilder complexityDistribution = new RangeDistributionBuilder(LIMITS_COMPLEXITY_METHODS);
//        for (SourceCode squidFunction : squidFunctionsInFile) {
//            complexityDistribution.add(squidFunction.getDouble(PlSqlMetric.COMPLEXITY));
//        }
//        
//        context.<String>newMeasure().on(sonarFile)
//                .forMetric(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION)
//                .withValue(complexityDistribution.build())
//                .save();
//    }
//
//    private void saveFilesComplexityDistribution(InputFile sonarFile, SourceFile squidFile) {
//        RangeDistributionBuilder complexityDistribution = new RangeDistributionBuilder(LIMITS_COMPLEXITY_FILES);
//        complexityDistribution.add(squidFile.getDouble(PlSqlMetric.COMPLEXITY));
//        context.<String>newMeasure().on(sonarFile)
//                .forMetric(CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION)
//                .withValue(complexityDistribution.build())
//                .save();
//    }
//    
    private void saveCpdTokens(InputFile inputFile) {
        NewCpdTokens newCpdTokens = context.newCpdTokens().onFile(inputFile);
        Lexer lexer = PlSqlLexer.create(configuration);
        PlSqlFile plSqlFile = SonarQubePlSqlFile.create(inputFile, context);
        List<Token> tokens = lexer.lex(plSqlFile.content());
        for (Token token : tokens) {
            if (token.getType() == GenericTokenType.EOF) {
                continue;
            }
            TokenLocation location = TokenLocation.from(token);
            newCpdTokens.addToken(location.line(), location.column(), location.endLine(), location.endColumn(), token.getValue());
        }
        newCpdTokens.save();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
