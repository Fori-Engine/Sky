package lake.script;

import java.util.LinkedList;

public class EditorUI {

    private static LinkedList<Object> registry = new LinkedList<>();
    private EditorUI(){}

    public static void present(Object o){
        registry.add(o);
    }
    public static void remove(Object o){
        registry.remove(o);
    }

    public static LinkedList<Object> getRegistry(){
        return registry;
    }

}
