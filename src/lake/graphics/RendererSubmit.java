package lake.graphics;

public class RendererSubmit {
    public ShaderProgram shaderProgram;
    public RenderCommand renderCommand;
    public int indices = 0;
    public int quads = 0;
    public int totalCount = 0;

    public RendererSubmit(ShaderProgram shaderProgram, RenderCommand renderCommand) {
        this.shaderProgram = shaderProgram;
        this.renderCommand = renderCommand;
    }

    public interface RenderCommand {
        void run();
    }

}
