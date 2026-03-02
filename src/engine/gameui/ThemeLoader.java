package engine.gameui;

import com.google.gson.GsonBuilder;

public class ThemeLoader {
    public static Theme loadTheme(String json) {
        return new GsonBuilder().create().fromJson(json, Theme.class);
    }
}
