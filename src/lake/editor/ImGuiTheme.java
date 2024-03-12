package lake.editor;

import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.ImVec4;
import imgui.flag.ImGuiCol;
import lake.graphics.Color;

import static imgui.flag.ImGuiCol.*;

public class ImGuiTheme {


    private static float[] colorConvertRGBtoHSV(float[] rgb){
        float[] hsv = new float[]{0, 0, 0};
        ImGui.colorConvertRGBtoHSV(rgb, hsv);

        return hsv;
    }


    private static void setStyleColor(ImGuiStyle style, int name, Color color){
        style.setColor(name, color.r, color.g, color.b, color.a);

    }

    public static void setup(boolean dark, float alpha){
        ImGui.styleColorsDark();
    }
}
