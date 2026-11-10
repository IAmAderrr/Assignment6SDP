import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InspectionTest {
    @Test
    void dispatchesToTheExactOverloadForEveryElement() {
        var seen = new ArrayList<String>();
        ProjectVisitor visitor = new ProjectVisitor() {
            public void visit(SourceCodeFile file) { seen.add("source"); }
            public void visit(TestFile file) { seen.add("test"); }
            public void visit(ConfigurationFile file) { seen.add("config"); }
            public void visit(DependencyFile file) { seen.add("dependency"); }
            public void visit(Directory directory) {
                seen.add("directory");
                ProjectVisitor.super.visit(directory);
            }
        };

        Main.demoProject().accept(visitor);

        assertEquals(List.of("directory", "directory", "source", "source",
                "directory", "test", "directory", "config", "dependency"), seen);
    }

    @Test
    void reportsHardcodedCredentialsAndSensitiveComplexityAtTheBoundary() {
        var visitor = new SecurityAuditVisitor();
        new SourceCodeFile("Payment.java", "payments", 50, 15, true, true, true).accept(visitor);

        assertEquals(List.of(SecurityAuditVisitor.Severity.CRITICAL,
                        SecurityAuditVisitor.Severity.HIGH),
                visitor.findings().stream().map(SecurityAuditVisitor.Finding::severity).toList());
        assertEquals(1, visitor.criticalIssues());
    }

    @Test
    void reportsBothConfigurationIssues() {
        var visitor = new SecurityAuditVisitor();
        new ConfigurationFile("app.properties", true, true).accept(visitor);

        assertEquals(List.of("Exposed credentials", "Plaintext transport enabled"),
                visitor.findings().stream().map(SecurityAuditVisitor.Finding::message).toList());
    }

    @Test
    void flagsOnlyDependenciesMarkedVulnerable() {
        var visitor = new SecurityAuditVisitor();
        new DependencyFile("pom.xml", List.of(
                new DependencyFile.Dependency("demo:safe", "1", false),
                new DependencyFile.Dependency("demo:risky", "1", true))).accept(visitor);

        assertEquals(1, visitor.findings().size());
        assertEquals(1, visitor.criticalIssues());
        assertTrue(visitor.findings().get(0).message().endsWith("demo:risky"));
    }

    @Test
    void talliesEveryDemoMetric() {
        var visitor = new ProjectStatisticsVisitor();
        Main.demoProject().accept(visitor);

        assertEquals(new ProjectStatisticsVisitor.Summary(5, 2, 1, 390, 6, 2, 12.0, 0.5),
                visitor.summary());
    }

    @Test
    void visitsLeavesAcrossSeveralDirectoryLevels() {
        ProjectElement nested = source("core", 7);
        for (int level = 0; level < 30; level++) {
            nested = new Directory("level-" + level, List.of(nested));
        }
        var visitor = new ProjectStatisticsVisitor();
        nested.accept(visitor);

        assertEquals(1, visitor.summary().totalFiles());
        assertEquals(7.0, visitor.summary().averageComplexity());
    }

    @Test
    void emptyProjectProducesZeroStatisticsAndFailsTheTestGate() {
        var empty = new Directory("empty", List.of());
        var statistics = new ProjectStatisticsVisitor();
        var quality = new QualityGateVisitor();
        empty.accept(statistics);
        empty.accept(quality);

        assertEquals(new ProjectStatisticsVisitor.Summary(0, 0, 0, 0, 0, 0, 0, 0),
                statistics.summary());
        assertEquals(new QualityGateVisitor.Result(true, true, false), quality.result());
    }

    @Test
    void passesWhenComplexityAndRatioEqualTheThresholds() {
        var visitor = new QualityGateVisitor();
        new Directory("release", List.of(source("a", 10), source("b", 10), source("c", 10),
                source("d", 10), source("e", 10), new TestFile("ATest.java", "a", 10, 1),
                new TestFile("BTest.java", "b", 10, 1))).accept(visitor);

        assertEquals(new QualityGateVisitor.Result(true, true, true), visitor.result());
        assertTrue(visitor.result().passed());
    }

    @Test
    void failsTheDemoForSecurityAndComplexityEvenWhenRatioPasses() {
        var visitor = new QualityGateVisitor();
        Main.demoProject().accept(visitor);

        assertEquals(new QualityGateVisitor.Result(false, false, true), visitor.result());
        assertFalse(visitor.result().passed());
    }

    @Test
    void failsWhenSourceHasNoTestFiles() {
        var visitor = new QualityGateVisitor();
        source("users", 5).accept(visitor);

        assertEquals(new QualityGateVisitor.Result(true, true, false), visitor.result());
        assertFalse(visitor.result().passed());
    }

    @Test
    void reportsAllFourComplianceRulesInTheDemo() {
        var visitor = new ComplianceVisitor(Set.of("example.legacy:unsafe-transport"));
        Main.demoProject().accept(visitor);

        assertEquals(List.of("NO_EXPOSED_CREDENTIALS", "PIN_DEPENDENCY_VERSION",
                        "NO_PROHIBITED_DEPENDENCY", "TEST_PRODUCTION_MODULE"),
                visitor.violations().stream().map(ComplianceVisitor.Violation::rule).toList());
        assertEquals("users", visitor.violations().get(3).location());
    }

    @Test
    void zeroCaseTestFileDoesNotCoverAModuleButLaterTestsDo() {
        var visitor = new ComplianceVisitor(Set.of());
        new TestFile("EmptyTest.java", "users", 5, 0).accept(visitor);
        source("users", 5).accept(visitor);
        assertEquals(1, visitor.violations().size());

        new TestFile("UserTest.java", "users", 20, 2).accept(visitor);
        assertEquals(List.of(), visitor.violations());
    }

    @Test
    void reportsCredentialsInConfigurationAndBothKindsOfMissingVersions() {
        var visitor = new ComplianceVisitor(Set.of());
        new ConfigurationFile("app.properties", true, false).accept(visitor);
        new DependencyFile("pom.xml", List.of(new DependencyFile.Dependency("a:b", null, false),
                new DependencyFile.Dependency("a:c", "  ", false))).accept(visitor);

        assertEquals(List.of("NO_EXPOSED_CREDENTIALS", "PIN_DEPENDENCY_VERSION",
                        "PIN_DEPENDENCY_VERSION"),
                visitor.violations().stream().map(ComplianceVisitor.Violation::rule).toList());
    }

    @Test
    void reportsDocumentationCoverageAndNamesTheMissingSource() {
        var visitor = new DocumentationVisitor();
        Main.demoProject().accept(visitor);

        assertEquals(new DocumentationVisitor.Report(2, 1, 0.5, List.of("PaymentService.java")),
                visitor.report());
    }

    @Test
    void testOnlyTreeHasFiniteRatiosAndNoSourceDocumentation() {
        var project = new Directory("tests-only", List.of(new TestFile("XTest.java", "x", 10, 2)));
        var statistics = new ProjectStatisticsVisitor();
        var documentation = new DocumentationVisitor();
        project.accept(statistics);
        project.accept(documentation);

        assertEquals(0.0, statistics.summary().testSourceRatio());
        assertEquals(0.0, statistics.summary().averageComplexity());
        assertEquals(new DocumentationVisitor.Report(0, 0, 0, List.of()), documentation.report());
    }

    @Test
    void rejectsNegativeMetrics() {
        assertThrows(IllegalArgumentException.class,
                () -> new SourceCodeFile("Bad.java", "x", -1, 2, false, false, true));
        assertThrows(IllegalArgumentException.class, () -> new TestFile("BadTest.java", "x", 1, -1));
    }

    private static SourceCodeFile source(String module, int complexity) {
        return new SourceCodeFile(module + ".java", module, 20, complexity, false, false, true);
    }
}
