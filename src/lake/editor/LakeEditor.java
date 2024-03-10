package lake.editor;

import imgui.type.ImBoolean;
import lake.FileReader;
import lake.Time;
import lake.Utils;
import lake.graphics.*;
import lake.script.EditorUI;
import imgui.*;
import imgui.callback.ImStrConsumer;
import imgui.callback.ImStrSupplier;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class LakeEditor {

    private final int[] fbWidth = new int[1];
    private final int[] fbHeight = new int[1];
    private final long[] mouseCursors = new long[ImGuiMouseCursor.COUNT];
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private ArrayList<Panel> panels = new ArrayList<>();
    private SceneViewport sceneViewport;
    private CodeInspector codeInspector;
    private GPUDriverOutput gpuDriverOutput;
    private boolean restartRequested = false;
    private StandaloneWindow window;
    private String platform, arch;
    private String[] launchArgs;
    
    

    public LakeEditor(StandaloneWindow window) {
        this.window = window;
        
        
        //Collect OS data
        {
            platform = System.getProperty("os.name");
            arch = System.getProperty("os.arch");
        }
        
        
        
        sceneViewport = new SceneViewport(1000, 600, window);
        codeInspector = new CodeInspector(sceneViewport);
        gpuDriverOutput = new GPUDriverOutput(window);





        panels.add(sceneViewport);
        panels.add(codeInspector);
        panels.add(gpuDriverOutput);


        //ImGui
        {

            ImGui.createContext();
            ImGuiTheme.setup(true, 1f);

            // Initialize ImGuiIO config
            final ImGuiIO io = ImGui.getIO();

            io.setIniFilename(null); // We don't want to save .ini file
            io.setConfigFlags(ImGuiConfigFlags.NavEnableKeyboard | ImGuiConfigFlags.DockingEnable); // Navigation with keyboard
            io.setBackendFlags(ImGuiBackendFlags.HasMouseCursors); // Mouse cursors to display while resizing windows etc.
            io.setBackendPlatformName("imgui_java_impl_glfw"); // For clarity reasons
            io.setBackendRendererName("imgui_java_impl_lwjgl"); // For clarity reasons


            // Keyboard mapping. ImGui will use those indices to peek into the io.KeysDown[] array.
            final int[] keyMap = new int[ImGuiKey.COUNT];
            keyMap[ImGuiKey.Tab] = GLFW_KEY_TAB;
            keyMap[ImGuiKey.LeftArrow] = GLFW_KEY_LEFT;
            keyMap[ImGuiKey.RightArrow] = GLFW_KEY_RIGHT;
            keyMap[ImGuiKey.UpArrow] = GLFW_KEY_UP;
            keyMap[ImGuiKey.DownArrow] = GLFW_KEY_DOWN;
            keyMap[ImGuiKey.PageUp] = GLFW_KEY_PAGE_UP;
            keyMap[ImGuiKey.PageDown] = GLFW_KEY_PAGE_DOWN;
            keyMap[ImGuiKey.Home] = GLFW_KEY_HOME;
            keyMap[ImGuiKey.End] = GLFW_KEY_END;
            keyMap[ImGuiKey.Insert] = GLFW_KEY_INSERT;
            keyMap[ImGuiKey.Delete] = GLFW_KEY_DELETE;
            keyMap[ImGuiKey.Backspace] = GLFW_KEY_BACKSPACE;
            keyMap[ImGuiKey.Space] = GLFW_KEY_SPACE;
            keyMap[ImGuiKey.Enter] = GLFW_KEY_ENTER;
            keyMap[ImGuiKey.Escape] = GLFW_KEY_ESCAPE;
            keyMap[ImGuiKey.KeyPadEnter] = GLFW_KEY_KP_ENTER;
            keyMap[ImGuiKey.A] = GLFW_KEY_A;
            keyMap[ImGuiKey.C] = GLFW_KEY_C;
            keyMap[ImGuiKey.V] = GLFW_KEY_V;
            keyMap[ImGuiKey.X] = GLFW_KEY_X;
            keyMap[ImGuiKey.Y] = GLFW_KEY_Y;
            keyMap[ImGuiKey.Z] = GLFW_KEY_Z;
            io.setKeyMap(keyMap);


            // Mouse cursors mapping
            mouseCursors[ImGuiMouseCursor.Arrow] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
            mouseCursors[ImGuiMouseCursor.TextInput] = glfwCreateStandardCursor(GLFW_IBEAM_CURSOR);
            mouseCursors[ImGuiMouseCursor.ResizeAll] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
            mouseCursors[ImGuiMouseCursor.ResizeNS] = glfwCreateStandardCursor(GLFW_VRESIZE_CURSOR);
            mouseCursors[ImGuiMouseCursor.ResizeEW] = glfwCreateStandardCursor(GLFW_HRESIZE_CURSOR);
            mouseCursors[ImGuiMouseCursor.ResizeNESW] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
            mouseCursors[ImGuiMouseCursor.ResizeNWSE] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
            mouseCursors[ImGuiMouseCursor.Hand] = glfwCreateStandardCursor(GLFW_HAND_CURSOR);
            mouseCursors[ImGuiMouseCursor.NotAllowed] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);

            // ------------------------------------------------------------
            // Here goes GLFW callbacks to update user input in Dear ImGui

            glfwSetKeyCallback(window.getGLFWHandle(), (w, key, scancode, action, mods) -> {
                if (action == GLFW_PRESS) {
                    io.setKeysDown(key, true);
                } else if (action == GLFW_RELEASE) {
                    io.setKeysDown(key, false);
                }

                io.setKeyCtrl(io.getKeysDown(GLFW_KEY_LEFT_CONTROL) || io.getKeysDown(GLFW_KEY_RIGHT_CONTROL));
                io.setKeyShift(io.getKeysDown(GLFW_KEY_LEFT_SHIFT) || io.getKeysDown(GLFW_KEY_RIGHT_SHIFT));
                io.setKeyAlt(io.getKeysDown(GLFW_KEY_LEFT_ALT) || io.getKeysDown(GLFW_KEY_RIGHT_ALT));
                io.setKeySuper(io.getKeysDown(GLFW_KEY_LEFT_SUPER) || io.getKeysDown(GLFW_KEY_RIGHT_SUPER));
            });

            glfwSetCharCallback(window.getGLFWHandle(), (w, c) -> {
                if (c != GLFW_KEY_DELETE) {
                    io.addInputCharacter(c);
                }
            });

            glfwSetMouseButtonCallback(window.getGLFWHandle(), (w, button, action, mods) -> {
                final boolean[] mouseDown = new boolean[5];

                mouseDown[0] = button == GLFW_MOUSE_BUTTON_1 && action != GLFW_RELEASE;
                mouseDown[1] = button == GLFW_MOUSE_BUTTON_2 && action != GLFW_RELEASE;
                mouseDown[2] = button == GLFW_MOUSE_BUTTON_3 && action != GLFW_RELEASE;
                mouseDown[3] = button == GLFW_MOUSE_BUTTON_4 && action != GLFW_RELEASE;
                mouseDown[4] = button == GLFW_MOUSE_BUTTON_5 && action != GLFW_RELEASE;

                io.setMouseDown(mouseDown);

                if (!io.getWantCaptureMouse() && mouseDown[1]) {
                    ImGui.setWindowFocus(null);
                }
            });

            glfwSetScrollCallback(window.getGLFWHandle(), (w, xOffset, yOffset) -> {
                io.setMouseWheelH(io.getMouseWheelH() + (float) xOffset);
                io.setMouseWheel(io.getMouseWheel() + (float) yOffset);
            });

            io.setSetClipboardTextFn(new ImStrConsumer() {
                @Override
                public void accept(final String s) {
                    glfwSetClipboardString(window.getGLFWHandle(), s);
                }
            });

            io.setGetClipboardTextFn(new ImStrSupplier() {
                @Override
                public String get() {
                    return glfwGetClipboardString(window.getGLFWHandle());
                }
            });
            io.setIniFilename("imgui.ini");





            final ImFontAtlas fontAtlas = io.getFonts();

            // First of all we add a default font, which is 'ProggyClean.ttf, 13px'
            //fontAtlas.addFontDefault();

            final ImFontConfig fontConfig = new ImFontConfig(); // Keep in mind that creation of the ImFontConfig will allocate native memory
            fontConfig.setMergeMode(true); // All fonts added while this mode is turned on will be merged with the previously added font
            fontConfig.setPixelSnapH(true);
            fontConfig.setGlyphRanges(fontAtlas.getGlyphRangesCyrillic()); // Additional glyphs could be added like this or in addFontFrom*() methods

            // We merge font loaded from resources with the default one. Thus we will get an absent cyrillic glyphs
            //fontAtlas.addFontFromMemoryTTF(loadFromResources("basis33.ttf"), 16, fontConfig);

            // Disable merged mode and add all other fonts normally
            fontConfig.setMergeMode(false);
            fontConfig.setPixelSnapH(false);

            // ------------------------------
            // Fonts from file/memory example

            fontConfig.setRasterizerMultiply(1.2f); // This will make fonts a bit more readable

            // We can add new fonts directly from file
            //fontAtlas.addFontFromFileTTF("src/test/resources/DroidSans.ttf", 13, fontConfig);
            //fontAtlas.addFontFromFileTTF("src/test/resources/DroidSans.ttf", 14, fontConfig);

            // Or directly from memory
            fontConfig.setName("Inter-Regular.ttf, 20px"); // This name will be displayed in Style Editor
            fontAtlas.addFontFromFileTTF("thirdparty/inter/static/Inter-Regular.ttf", 20);


            //fontAtlas.addFontFromMemoryTTF("", 13, fontConfig);
            //fontConfig.setName("Roboto-Regular.ttf, 14px"); // We can apply a new config value every time we add a new font
            //fontAtlas.addFontFromMemoryTTF(loadFromResources("Roboto-Regular.ttf"), 14, fontConfig);

            fontConfig.destroy(); // After all fonts were added we don't need this config more














            imGuiGl3.init();
        }
    }

    public void update(){


        ProjectRef projectRef = ProjectManager.getProjectRef();

        if(projectRef.isProjectOpened()) {
            projectRef.invokeUpdate();

            if(projectRef.isCrashed()){
                sceneViewport.disconnect();
            }
        }







        //ImGui
        {
            {
                glClearColor(0, 0, 0, 0.0f);
                glClear(GL_COLOR_BUFFER_BIT);

                glfwGetFramebufferSize(window.getGLFWHandle(), fbWidth, fbHeight);


                final ImGuiIO io = ImGui.getIO();
                io.setDisplaySize(window.getWidth(), window.getHeight());
                io.setDisplayFramebufferScale((float) fbWidth[0] / window.getWidth(), (float) fbHeight[0] / window.getHeight());
                io.setMousePos(window.getMouseX(), window.getMouseY());
                io.setDeltaTime(Time.deltaTime);

                final int imguiCursor = ImGui.getMouseCursor();
                glfwSetCursor(window.getGLFWHandle(), mouseCursors[imguiCursor]);
                glfwSetInputMode(window.getGLFWHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);

            }
            ImGui.newFrame();



            if(projectRef.getThrowableLog() != null){
                ImGui.openPopup("The project crashed!");
                if (ImGui.beginPopupModal("The project crashed!", new ImBoolean(true)))
                {
                    ImGui.text(Utils.exceptionToString(projectRef.getThrowableLog()));
                    if (ImGui.button("Close")) {
                        ImGui.closeCurrentPopup();

                        projectRef.clearThrowableLog();
                    }
                    ImGui.endPopup();
                }
            }



            //MenuBar
            {
                if(ImGui.beginMainMenuBar()){
                    if(ImGui.beginMenu("File")){


                        if(ImGui.menuItem("Open Project", "CTRL+O")){
                            String projectFilePath =
                                    TinyFileDialogs.tinyfd_openFileDialog(
                                            "Open Project",
                                            FileSystemView.getFileSystemView().getHomeDirectory().getPath(),
                                            null,
                                            null,
                                            false
                                    );

                            if(projectFilePath != null) {

                                String outputPath = projectFilePath.strip().replace("project.lake", "") + FileReader.readFile(projectFilePath).strip();

                                if(projectRef.openProject(new File(outputPath))){
                                    sceneViewport.useFramebuffer2D(projectRef.getViewportTextureID());
                                }


                            }

                        }
                        if(ImGui.menuItem("Restart Editor", "CTRL+R")){
                            setRestartRequested(true);
                        }

                        ImGui.endMenu();
                    }
                    ImGui.endMainMenuBar();
                }
            }





            {




                int windowFlags = ImGuiWindowFlags.MenuBar | ImGuiWindowFlags.NoDocking;

                ImGuiViewport mainViewport = ImGui.getMainViewport();
                ImGui.setNextWindowPos(mainViewport.getWorkPosX(), mainViewport.getWorkPosY());
                ImGui.setNextWindowSize(mainViewport.getWorkSizeX(), mainViewport.getWorkSizeY());
                ImGui.setNextWindowViewport(mainViewport.getID());
                ImGui.setNextWindowPos(0.0f, 0.0f);
                ImGui.setNextWindowSize(window.getWidth(), window.getHeight());
                ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
                ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f);
                windowFlags |= ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse |
                        ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove |
                        ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus;

                ImGui.begin("Dockspace Demo", new ImBoolean(true), windowFlags);
                ImGui.popStyleVar(2);

                // Dockspace
                ImGui.dockSpace(ImGui.getID("Dockspace"));

                for(Panel panel : panels){
                    panel.render();
                }

                ImGui.end();




            }



            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());
        }




    }

    public boolean isRestartRequested() {
        return restartRequested;
    }

    public void setRestartRequested(boolean restartRequested) {
        this.restartRequested = restartRequested;
    }

    public void destroy(){

        for(Panel panel : panels){
            panel.dispose();
        }



        if(ProjectManager.getProjectRef().isProjectOpened())
            ProjectManager.getProjectRef().dispose();

        EditorUI.getRegistry().clear();
        imGuiGl3.dispose();
        ImGui.destroyContext();
    }

}
