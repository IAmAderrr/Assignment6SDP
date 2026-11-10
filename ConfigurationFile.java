public record ConfigurationFile(String name, boolean exposedCredentials, boolean plaintextTransport)
        implements ProjectElement {
    @Override
    public void accept(ProjectVisitor visitor) {
        visitor.visit(this);
    }
}
