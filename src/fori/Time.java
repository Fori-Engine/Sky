package fori;

public class Time {
    public static float deltaTime;

    public static float deltaTime(){
        return deltaTime;
    }
    public static float framesPerSecond(){
        return (int) (1 / Time.deltaTime);
    }


}
