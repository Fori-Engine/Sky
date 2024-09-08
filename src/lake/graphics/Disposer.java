package lake.graphics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/***
 * A Disposer is responsible for registering OpenGL objects like VBOs/VAOs/Shaders that have to be disposed
 * when the application is closed
 */
public class Disposer {

    private static final HashMap<String, ArrayList<Disposable>> disposables = new HashMap<>();






    public static void add(Disposable disposable){
        add("default", disposable);
    }

    public static void add(String category, Disposable disposable){
        if(!disposables.containsKey(category)){
            disposables.put(category, new ArrayList<>());
        }

        disposables.get(category).add(disposable);
    }


    public static void remove(Disposable disposable){
        remove("default", disposable);
    }

    public static void remove(String category, Disposable disposable){
        disposables.get(category).remove(disposable);
    }

    public static void disposeAll(){
        for(ArrayList<Disposable> category : disposables.values()){
            for(Disposable d : category){
                d.dispose();
            }
        }
        disposables.clear();
    }


    public static void disposeAllInCategory(String category){


    }
    public static List<Disposable> inCategory(String category){
        return null;
    }

}
