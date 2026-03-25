package engine.ecs;


import engine.mio.IRGen;
import engine.mio.Instruction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Scene {
    private List<ActorSystem> systems = new ArrayList<>();
    private Actor root;
    private String name;

    private Actor actor;
    private Object component;


    public Scene(String name) {
        this.name = name;
    }

    public void tick() {
        for(ActorSystem system : systems) {
            system.run(root);
        }
    }

    public Actor getRootActor() {
        return root;
    }

    public void setRootActor(Actor root) {
        this.root = root;
        this.actor = root;
    }

    public Actor newActor(String name, Object... components) {
        Actor actor = new Actor(name);
        for(Object component : components)
            actor.add(component);

        return actor;
    }

    public void exec(IRGen irGen) {

        for (Iterator<Instruction> iterator = irGen.getList().iterator(); iterator.hasNext(); ) {
            Instruction instruction = iterator.next();
            switch (instruction.opcode()) {
                case PushActor -> {
                    String actorName = (String) instruction.operands()[0];
                    Actor newActor = new Actor(actorName);
                    actor = newActor;
                    actor.addActor(newActor);
                }
                case AddData -> {
                    if (instruction.operands()[0].equals("SpotlightComponent")) {

                        Instruction fovDeg = iterator.next();
                        Instruction eye = iterator.next();
                        Instruction center = iterator.next();
                        Instruction up = iterator.next();
                        Instruction aspectRatio = iterator.next();
                        Instruction zNear = iterator.next();
                        Instruction zFar = iterator.next();
                        Instruction zZeroToOne = iterator.next();
                        Instruction invertY = iterator.next();

                        System.out.println("Creating new spotlight!");




                    }

                }
                case PopActor -> actor = actor.getParent();
            }
        }

    }

    public Actor newRootActor(String name) {
        return new Actor(name);
    }



    public void addSystem(ActorSystem... actorSystems) {
        this.systems.addAll(Arrays.asList(actorSystems));
    }


    public void removeSystem(ActorSystem actorSystem) {
        systems.remove(actorSystem);
    }


    public void close() {
        for(ActorSystem system : systems) {
            system.dispose();
        }
    }


}
