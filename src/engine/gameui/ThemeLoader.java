package engine.gameui;

import com.google.gson.GsonBuilder;
import engine.graphics.Color;

public class ThemeLoader {
    public static Theme loadTheme(String json) {
        return new GsonBuilder().registerTypeAdapter(Color.class, new InvSRGBColorTypeAdapter()).create().fromJson(json, Theme.class);
    }
}
