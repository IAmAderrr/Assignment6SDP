import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        Directory project = demoProject();
        var security = new SecurityAuditVisitor();
        var statistics = new ProjectStatisticsVisitor();
        var quality = new QualityGateVisitor();
        var compliance = new ComplianceVisitor(Set.of("example.legacy:unsafe-transport"));
        project.accept(security);
        project.accept(statistics);
        project.accept(quality);
        project.accept(compliance);

        System.out.println("Project " + project.name());
        System.out.println("\nSecurity audit");
        System.out.println("Critical issues: " + security.criticalIssues());
        for (var finding : security.findings()) {
            System.out.printf("- %s %s: %s%n", finding.severity(), finding.location(), finding.message());
        }

        var summary = statistics.summary();
        System.out.println("\nProject statistics");
        System.out.printf(Locale.ROOT,
                "Files: %d (%d source, %d test)%nLines of code: %d%nTests: %d%nDependencies: %d%n"
                        + "Average complexity: %.2f%nTest/source ratio: %.2f%n",
                summary.totalFiles(), summary.sourceFiles(), summary.testFiles(),
                summary.linesOfCode(), summary.testCount(), summary.dependencies(),
                summary.averageComplexity(), summary.testSourceRatio());

        var gate = quality.result();
        System.out.println("\nQuality gate");
        System.out.println("Security: " + status(gate.securityPass()));
        System.out.println("Complexity: " + status(gate.complexityPass()));
        System.out.println("Tests: " + status(gate.testsPass()));
        System.out.println("FINAL RESULT: " + (gate.passed() ? "PASSED" : "FAILED"));

        var violations = compliance.violations();
        System.out.println("\nCompliance");
        System.out.println("Violations: " + violations.size());
        for (var violation : violations) {
            System.out.printf("- %s %s: %s%n", violation.rule(), violation.location(), violation.detail());
        }
    }

    private static String status(boolean passed) {
        return passed ? "PASS" : "FAIL";
    }

    static Directory demoProject() {
        return new Directory("fintech-project", List.of(
                new Directory("src", List.of(
                        new SourceCodeFile("PaymentService.java", "payments", 180, 16, true, true, false),
                        new SourceCodeFile("UserService.java", "users", 120, 8, false, true, true))),
                new Directory("tests", List.of(
                        new TestFile("PaymentServiceTest.java", "payments", 90, 6))),
                new Directory("config", List.of(
                        new ConfigurationFile("application.properties", false, true))),
                new DependencyFile("pom.xml", List.of(
                        new DependencyFile.Dependency("org.example:money", "2.1.0", false),
                        new DependencyFile.Dependency("example.legacy:unsafe-transport", "", true)))));
    }
}
