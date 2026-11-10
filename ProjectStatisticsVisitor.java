public final class ProjectStatisticsVisitor implements ProjectVisitor {
    public record Summary(int totalFiles, int sourceFiles, int testFiles, long linesOfCode,
                          long testCount, long dependencies, double averageComplexity,
                          double testSourceRatio) {}

    private int totalFiles;
    private int sourceFiles;
    private int testFiles;
    private long linesOfCode;
    private long testCount;
    private long dependencies;
    private long totalComplexity;

    @Override
    public void visit(SourceCodeFile file) {
        totalFiles++;
        sourceFiles++;
        linesOfCode += file.linesOfCode();
        totalComplexity += file.complexity();
    }

    @Override
    public void visit(TestFile file) {
        totalFiles++;
        testFiles++;
        linesOfCode += file.linesOfCode();
        testCount += file.testCount();
    }

    @Override
    public void visit(ConfigurationFile file) {
        totalFiles++;
    }

    @Override
    public void visit(DependencyFile file) {
        totalFiles++;
        dependencies += file.dependencies().size();
    }

    public Summary summary() {
        double average = sourceFiles == 0 ? 0.0 : (double) totalComplexity / sourceFiles;
        double ratio = sourceFiles == 0 ? 0.0 : (double) testFiles / sourceFiles;
        return new Summary(totalFiles, sourceFiles, testFiles, linesOfCode, testCount,
                dependencies, average, ratio);
    }
}
