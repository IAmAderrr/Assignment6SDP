import java.util.List;

public record Directory(String name, List<ProjectElement> children)
        implements ProjectElement {
    public Directory {
        children = List.copyOf(children);
    }

    @Override
    public void accept(ProjectVisitor visitor) {
        visitor.visit(this);
    }
}
