import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ComplianceVisitor implements ProjectVisitor {
    public record Violation(String rule, String location, String detail) {}

    private final Set<String> prohibitedDependencies;
    private final Set<String> productionModules = new LinkedHashSet<>();
    private final Set<String> testedModules = new LinkedHashSet<>();
    private final List<Violation> fileViolations = new ArrayList<>();

    public ComplianceVisitor(Set<String> prohibitedDependencies) {
        this.prohibitedDependencies = Set.copyOf(prohibitedDependencies);
    }

    @Override
    public void visit(SourceCodeFile file) {
        productionModules.add(file.module());
        if (file.hardcodedCredentials()) {
            fileViolations.add(new Violation("NO_EXPOSED_CREDENTIALS", file.name(),
                    "Hardcoded credentials in source"));
        }
    }

    @Override
    public void visit(TestFile file) {
        if (file.testCount() > 0) {
            testedModules.add(file.module());
        }
    }

    @Override
    public void visit(ConfigurationFile file) {
        if (file.exposedCredentials()) {
            fileViolations.add(new Violation("NO_EXPOSED_CREDENTIALS", file.name(),
                    "Credentials exposed in configuration"));
        }
    }

    @Override
    public void visit(DependencyFile file) {
        for (var dependency : file.dependencies()) {
            if (!dependency.hasVersion()) {
                fileViolations.add(new Violation("PIN_DEPENDENCY_VERSION", file.name(),
                        dependency.coordinates()));
            }
            if (prohibitedDependencies.contains(dependency.coordinates())) {
                fileViolations.add(new Violation("NO_PROHIBITED_DEPENDENCY", file.name(),
                        dependency.coordinates()));
            }
        }
    }

    public List<Violation> violations() {
        var violations = new ArrayList<>(fileViolations);
        for (String module : productionModules) {
            if (!testedModules.contains(module)) {
                violations.add(new Violation("TEST_PRODUCTION_MODULE", module,
                        "No tests recorded for production module"));
            }
        }
        return List.copyOf(violations);
    }
}
