public final class QualityGateVisitor implements ProjectVisitor {
    public static final double MAX_AVERAGE_COMPLEXITY = 10.0;
    public static final double MIN_TEST_SOURCE_RATIO = 0.40;

    public record Result(boolean securityPass, boolean complexityPass, boolean testsPass) {
        public boolean passed() {
            return securityPass && complexityPass && testsPass;
        }
    }

    private final SecurityAuditVisitor security = new SecurityAuditVisitor();
    private final ProjectStatisticsVisitor statistics = new ProjectStatisticsVisitor();

    @Override
    public void visit(SourceCodeFile file) {
        analyze(file);
    }

    @Override
    public void visit(TestFile file) {
        analyze(file);
    }

    @Override
    public void visit(ConfigurationFile file) {
        analyze(file);
    }

    @Override
    public void visit(DependencyFile file) {
        analyze(file);
    }

    private void analyze(ProjectElement file) {
        file.accept(security);
        file.accept(statistics);
    }

    public Result result() {
        var summary = statistics.summary();
        return new Result(security.criticalIssues() == 0,
                summary.averageComplexity() <= MAX_AVERAGE_COMPLEXITY,
                summary.testSourceRatio() >= MIN_TEST_SOURCE_RATIO);
    }
}
