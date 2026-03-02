package engine.gameui;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import engine.graphics.Color;

import java.io.IOException;

public class InvSRGBColorTypeAdapter extends TypeAdapter<Color> {
    @Override
    public void write(JsonWriter jsonWriter, Color color) throws IOException {
        jsonWriter.name("r").value(color.r);
        jsonWriter.name("g").value(color.g);
        jsonWriter.name("b").value(color.b);
        jsonWriter.name("a").value(color.a);
    }

    @Override
    public Color read(JsonReader jsonReader) throws IOException {
        float r = 0f, g = 0f, b = 0f, a = 1f;

        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
            switch (jsonReader.nextName()) {
                case "r": r = (float) jsonReader.nextDouble(); break;
                case "g": g = (float) jsonReader.nextDouble(); break;
                case "b": b = (float) jsonReader.nextDouble(); break;
                case "a": a = (float) jsonReader.nextDouble(); break;
                default: jsonReader.skipValue();
            }
        }
        jsonReader.endObject();

        return new Color(r, g, b, a, 2.2);
    }
}
