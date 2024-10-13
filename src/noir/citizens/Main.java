package noir.citizens;

public class Main {
    public static void main(String[] args) {

        Stage stage = new Stage();

        stage.init();

        while(true){
            boolean success = stage.update();

            if(!success) break;
        }

        stage.dispose();


    }
}
