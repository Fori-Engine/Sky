package engine.graphics.text;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
            JsonArray charactersInfo = jsonObject.get("glyphs").getAsJsonArray();
            int size = charactersInfo.size();

            for(int i = 0; i < size; i++) {
                JsonObject characterInfo = charactersInfo.get(i).getAsJsonObject();


                Character character = new Character();

                character.unicode = characterInfo.get("unicode").getAsInt();
                character.advance =  characterInfo.get("advance").getAsFloat();


                if(characterInfo.has("planeBounds") && characterInfo.has("atlasBounds")) {
                    JsonObject planeBoundsInfo = characterInfo.get("planeBounds").getAsJsonObject();
                    JsonObject atlasBoundsInfo = characterInfo.get("atlasBounds").getAsJsonObject();

                    character.planeBounds = new Rect(
                            planeBoundsInfo.get("left").getAsFloat(),
                            planeBoundsInfo.get("bottom").getAsFloat(),
                            planeBoundsInfo.get("right").getAsFloat(),
                            planeBoundsInfo.get("top").getAsFloat()

                    );

                    character.atlasBounds = new Rect(
                            atlasBoundsInfo.get("left").getAsFloat(),
                            atlasBoundsInfo.get("bottom").getAsFloat(),
                            atlasBoundsInfo.get("right").getAsFloat(),
                            atlasBoundsInfo.get("top").getAsFloat()

                    );
                }

                msdfData.characters[character.unicode] = character;

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

    public static class Character {
        public int unicode;
        public float advance;
        public Rect planeBounds;
        public Rect atlasBounds;
    }

    public static class MsdfData {
        public static int glyphCount = 1024;
        public String type;
        public float distanceRange;
        public float distanceRangeMiddle;
        public float size;
        public float width;
        public float height;
        public String yOrigin;
        public Character[] characters = new Character[glyphCount];

        public float emSize;
        public float lineHeight;
        public float ascender;
        public float descender;
        public float underlineY;
        public float underlineThickness;



    }
}
