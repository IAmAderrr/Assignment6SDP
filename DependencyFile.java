import java.util.List;

public record DependencyFile(String name, List<Dependency> dependencies)
        implements ProjectElement {
    public DependencyFile {
        dependencies = List.copyOf(dependencies);
    }

    public record Dependency(String coordinates, String version, boolean knownVulnerable) {
        public boolean hasVersion() {
            return version != null && !version.isBlank();
        }
    }

    @Override
    public void accept(ProjectVisitor visitor) {
        visitor.visit(this);
    }
}
