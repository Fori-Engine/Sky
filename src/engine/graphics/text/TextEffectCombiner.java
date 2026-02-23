package engine.graphics.text;

import org.joml.Vector2f;

public class TextEffectCombiner extends TextEffect {
    private TextEffect[] textEffects;
    private Vector2f offset = new Vector2f(0, 0);

    public TextEffectCombiner(TextEffect... textEffects) {
        this.textEffects = textEffects;
    }

    @Override
    public void update() {
        super.update();
        for(TextEffect textEffect : textEffects) textEffect.update();
    }

    @Override
    public Vector2f offset(int charIndex, int stringLength) {
        float x = 0, y = 0;

        for(TextEffect textEffect : textEffects) {
            Vector2f offset = textEffect.offset(charIndex, stringLength);
            x += offset.x;
            y += offset.y;
        }

        offset.set(x, y);


        return offset;
    }
}
