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
        ImGuiStyle style = ImGui.getStyle();
        
        Color muted = Color.fromRGB(144, 140, 170, 255f).mul(0.87f);
        Color mutedHovered = muted.mul(0.91f);
        Color mutedActive = muted.mul(0.85f);


        setStyleColor(style, TabActive, muted);
        setStyleColor(style, Tab, muted);
        setStyleColor(style, TabUnfocused, muted);
        setStyleColor(style, TabHovered, muted);
        setStyleColor(style, TabUnfocusedActive, muted);
        setStyleColor(style, TitleBgActive, Color.BLACK);
        setStyleColor(style, Button, muted);
        setStyleColor(style, ButtonHovered, mutedHovered);
        setStyleColor(style, ButtonActive, mutedActive);
        setStyleColor(style, SliderGrab, muted);
        setStyleColor(style, SliderGrabActive, mutedActive);
        setStyleColor(style, DockingEmptyBg, mutedActive);
        //setStyleColor(style, Bg, mutedActive);


        style.setTabRounding(0f);




        //setStyleColor(style, HeaderActive, 1.0f, 0.0f, 0.0f, 1.0f);
        //setStyleColor(style, HeaderHovered, 1.0f, 0.0f, 0.0f, 1.0f);
        //setStyleColor(style, TabUnfocusedActive, 1.0f, 0.0f, 0.0f, 1.0f);
        //setStyleColor(style, TabActive, 1.0f, 0.0f, 0.0f, 1.0f);
        //setStyleColor(style, TitleBgActive, 1.0f, 0.0f, 0.0f, 1.0f);

    }
}
