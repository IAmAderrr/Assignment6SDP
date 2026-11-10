import java.util.ArrayList;
import java.util.List;

public final class DocumentationVisitor implements ProjectVisitor {
    public record Report(int sourceFiles, int documentedSources, double coverage,
                         List<String> undocumentedSources) {}

    private final List<String> undocumentedSources = new ArrayList<>();
    private int documentedSources;

    @Override
    public void visit(SourceCodeFile file) {
        if (file.documented()) {
            documentedSources++;
        } else {
            undocumentedSources.add(file.name());
        }
    }

    @Override
    public void visit(TestFile file) {}

    @Override
    public void visit(ConfigurationFile file) {}

    @Override
    public void visit(DependencyFile file) {}

    public Report report() {
        int total = documentedSources + undocumentedSources.size();
        double coverage = total == 0 ? 0.0 : (double) documentedSources / total;
        return new Report(total, documentedSources, coverage, List.copyOf(undocumentedSources));
    }
}
