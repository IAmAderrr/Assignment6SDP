public record TestFile(String name, String module, int linesOfCode, int testCount)
        implements ProjectElement {
    public TestFile {
        if (linesOfCode < 0 || testCount < 0) {
            throw new IllegalArgumentException("Test metrics must be nonnegative");
        }
    }

    @Override
    public void accept(ProjectVisitor visitor) {
        visitor.visit(this);
    }
}
