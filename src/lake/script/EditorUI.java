package lake.script;

import java.util.HashMap;
import java.util.LinkedList;

public class EditorUI {

    private static HashMap<String, Object> registry = new HashMap<>();
    private EditorUI(){}

    public static void present(String id, Object o){
        registry.put(id, o);
    }
    public static void remove(String id){
        registry.remove(id);
    }

    public static HashMap<String, Object> getRegistry(){
        return registry;
    }

}
