import java.util.ArrayList;
import java.util.List;

public final class SecurityAuditVisitor implements ProjectVisitor {
    public enum Severity { CRITICAL, HIGH }

    public record Finding(Severity severity, String location, String message) {}

    private final List<Finding> findings = new ArrayList<>();

    @Override
    public void visit(SourceCodeFile file) {
        if (file.hardcodedCredentials()) {
            add(Severity.CRITICAL, file.name(), "Hardcoded credentials");
        }
        if (file.handlesSensitiveData() && file.complexity() >= 15) {
            add(Severity.HIGH, file.name(), "High complexity in sensitive code");
        }
    }

    @Override
    public void visit(TestFile file) {
        // The test metadata has no security indicators.
    }

    @Override
    public void visit(ConfigurationFile file) {
        if (file.exposedCredentials()) {
            add(Severity.CRITICAL, file.name(), "Exposed credentials");
        }
        if (file.plaintextTransport()) {
            add(Severity.HIGH, file.name(), "Plaintext transport enabled");
        }
    }

    @Override
    public void visit(DependencyFile file) {
        for (var dependency : file.dependencies()) {
            if (dependency.knownVulnerable()) {
                add(Severity.CRITICAL, file.name(),
                        "Known vulnerable dependency: " + dependency.coordinates());
            }
        }
    }

    private void add(Severity severity, String location, String message) {
        findings.add(new Finding(severity, location, message));
    }

    public List<Finding> findings() {
        return List.copyOf(findings);
    }

    public long criticalIssues() {
        return findings.stream().filter(finding -> finding.severity() == Severity.CRITICAL).count();
    }
}
