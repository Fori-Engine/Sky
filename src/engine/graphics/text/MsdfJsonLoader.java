package engine.graphics.text;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class MsdfJsonLoader {


    public static MsdfData load(String json) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);


        MsdfData msdfData = new MsdfData();

        //Atlas Info
        {
            JsonObject atlasInfo = jsonObject.get("atlas").getAsJsonObject();
            msdfData.type = atlasInfo.get("type").getAsString();
            msdfData.distanceRange = atlasInfo.get("distanceRange").getAsFloat();
            msdfData.distanceRangeMiddle = atlasInfo.get("distanceRangeMiddle").getAsFloat();
            msdfData.size = atlasInfo.get("size").getAsFloat();
            msdfData.width = atlasInfo.get("width").getAsFloat();
            msdfData.height = atlasInfo.get("height").getAsFloat();
            msdfData.yOrigin = atlasInfo.get("yOrigin").getAsString();
        }

        //Glyphs
        {
            JsonArray glyphsInfo = jsonObject.get("glyphs").getAsJsonArray();
            int size = glyphsInfo.size();

            for(int i = 0; i < size; i++) {
                JsonObject glyphInfo = glyphsInfo.get(i).getAsJsonObject();


                Glyph glyph = new Glyph();

                glyph.unicode = glyphInfo.get("unicode").getAsInt();
                glyph.advance =  glyphInfo.get("advance").getAsFloat();


                if(glyphInfo.has("planeBounds") && glyphInfo.has("atlasBounds")) {
                    JsonObject planeBoundsInfo = glyphInfo.get("planeBounds").getAsJsonObject();
                    JsonObject atlasBoundsInfo = glyphInfo.get("atlasBounds").getAsJsonObject();

                    glyph.planeBounds = new Rect(
                            planeBoundsInfo.get("left").getAsFloat(),
                            planeBoundsInfo.get("bottom").getAsFloat(),
                            planeBoundsInfo.get("right").getAsFloat(),
                            planeBoundsInfo.get("top").getAsFloat()

                    );

                    glyph.atlasBounds = new Rect(
                            atlasBoundsInfo.get("left").getAsFloat(),
                            atlasBoundsInfo.get("bottom").getAsFloat(),
                            atlasBoundsInfo.get("right").getAsFloat(),
                            atlasBoundsInfo.get("top").getAsFloat()

                    );
                }

                msdfData.glyphs.add(glyph);

            }

        }


        //Metrics
        {
            JsonObject metricsInfo = jsonObject.get("metrics").getAsJsonObject();
            msdfData.emSize = metricsInfo.get("emSize").getAsFloat();
            msdfData.lineHeight = metricsInfo.get("lineHeight").getAsFloat();
            msdfData.ascender = metricsInfo.get("ascender").getAsFloat();
            msdfData.descender = metricsInfo.get("descender").getAsFloat();
            msdfData.underlineY = metricsInfo.get("underlineY").getAsFloat();
            msdfData.underlineThickness = metricsInfo.get("underlineThickness").getAsFloat();

        }






        return msdfData;
    }

    public static class Rect {
        public float left, bottom, right, top;

        public Rect(float left, float bottom, float right, float top) {
            this.left = left;
            this.bottom = bottom;
            this.right = right;
            this.top = top;
        }
    }

    public static class Glyph {
        public int unicode;
        public float advance;
        public Rect planeBounds;
        public Rect atlasBounds;
    }

    public static class MsdfData {
        public String type;
        public float distanceRange;
        public float distanceRangeMiddle;
        public float size;
        public float width;
        public float height;
        public String yOrigin;
        public List<Glyph> glyphs = new ArrayList<>();

        float emSize;
        float lineHeight;
        float ascender;
        float descender;
        float underlineY;
        float underlineThickness;



    }
}
