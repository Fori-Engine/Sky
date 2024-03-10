package lake.editor;

public class ProjectManager {
    private static ProjectRef projectRef = new ProjectRef();

    public static ProjectRef getProjectRef() {
        return projectRef;
    }
}
