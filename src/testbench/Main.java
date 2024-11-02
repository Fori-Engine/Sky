package testbench;

import fori.Stage;

public class Main {
    public static void main(String[] args) {


        Stage noirCitizens = new NoirCitizens();

        noirCitizens.launch(args);


        while(true){
            boolean success = noirCitizens.update();

            if(!success) break;
        }

        noirCitizens.close();


    }
}
