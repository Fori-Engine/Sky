package fori.graphics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/***
 * A Disposer manages the lifetime of objects that need special handling when no longer in use (think Audio Devices or GPU Memory)
 */
public class Disposer {

    private static final HashMap<String, ArrayList<Disposable>> categories = new HashMap<>();






    public static void add(Disposable disposable){
        add("default", disposable);
    }

    public static void add(String category, Disposable disposable){
        if(!categories.containsKey(category)){
            categories.put(category, new ArrayList<>());
        }

        categories.get(category).add(disposable);
    }


    public static void remove(Disposable disposable){
        remove("default", disposable);
    }

    public static void remove(String category, Disposable disposable){
        categories.get(category).remove(disposable);
    }

    public static void disposeAll(){
        for(ArrayList<Disposable> category : categories.values()){
            for(Disposable d : category) d.dispose();
        }
        categories.clear();
    }


    public static void disposeAllInCategory(String categoryName){
        ArrayList<Disposable> category = categories.get(categoryName);
        for(Disposable d : category) d.dispose();
        category.clear();
    }
    public static List<Disposable> inCategory(String category){
        return null;
    }

}
