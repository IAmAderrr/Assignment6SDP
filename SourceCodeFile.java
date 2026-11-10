public record SourceCodeFile(String name, String module, int linesOfCode, int complexity,
                             boolean hardcodedCredentials, boolean handlesSensitiveData,
                             boolean documented)
        implements ProjectElement {
    public SourceCodeFile {
        if (linesOfCode < 0 || complexity < 0) {
            throw new IllegalArgumentException("Source metrics must be nonnegative");
        }
    }

    @Override
    public void accept(ProjectVisitor visitor) {
        visitor.visit(this);
    }
}
