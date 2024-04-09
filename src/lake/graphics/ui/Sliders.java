package lake.graphics.ui;

public class Sliders {
    public static final int padding = 10;

    public static final int sliderKnobWidth = 20, sliderKnobHeight = 20;



    public static class SliderEvent extends UIEvent {
        public float value;

        public SliderEvent(float value) {
            this.value = value;
        }
    }

}
