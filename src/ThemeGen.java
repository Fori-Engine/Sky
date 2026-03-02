import com.google.gson.GsonBuilder;
import engine.gameui.Theme;
import engine.graphics.Color;

public class ThemeGen {
    public static void main(String[] args) {
        Theme Theme = new GsonBuilder().setPrettyPrinting().create().fromJson("{\n" +
                "  \"containerBackgroundColor\": {\n" +
                "    \"r\": 0.9843137,\n" +
                "    \"g\": 0.9607843,\n" +
                "    \"b\": 0.9372549,\n" +
                "    \"a\": 1.0\n" +
                "  },\n" +
                "  \"buttonBackgroundColor\": {\n" +
                "    \"r\": 0.54509807,\n" +
                "    \"g\": 0.42745098,\n" +
                "    \"b\": 0.6117647,\n" +
                "    \"a\": 1.0\n" +
                "  },\n" +
                "  \"buttonHoverColor\": {\n" +
                "    \"r\": 0.7764706,\n" +
                "    \"g\": 0.58431375,\n" +
                "    \"b\": 0.6862745,\n" +
                "    \"a\": 1.0\n" +
                "  },\n" +
                "  \"buttonClickColor\": {\n" +
                "    \"r\": 0.15294118,\n" +
                "    \"g\": 0.15294118,\n" +
                "    \"b\": 0.26666668,\n" +
                "    \"a\": 1.0\n" +
                "  }\n" +
                "}", Theme.class);

        System.out.println();
    }
}
