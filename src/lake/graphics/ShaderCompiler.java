package lake.graphics;

import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.util.shaderc.Shaderc.*;

public class ShaderCompiler {
    public static ShaderBinary compile(String source, int kind){
        long compiler = shaderc_compiler_initialize();
        if(compiler == MemoryUtil.NULL){
            throw new RuntimeException("Failed to init shaderc compiler");
        }

        long result = shaderc_compile_into_spv(compiler, source, kind, "", "main", MemoryUtil.NULL);
        if(result == MemoryUtil.NULL){
            throw new RuntimeException("Shader compilation failed for " + kind);
        }
        if(shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success){

            String shaderType = getShaderType(kind);



            throw new RuntimeException(shaderType + ": " + shaderc_result_get_error_message(result));
        }

        shaderc_compiler_release(compiler);


        return new ShaderBinary(result, shaderc_result_get_bytes(result));
    }

    private static String getShaderType(int kind) {
        if(kind == shaderc_glsl_vertex_shader) return "Vertex Shader";
        if(kind == shaderc_glsl_fragment_shader) return "Fragment Shader";


        return null;
    }
}
