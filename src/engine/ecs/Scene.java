package engine.ecs;


import engine.graphics.Disposable;
import engine.mio.IRGen;
import engine.mio.Instruction;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Scene extends Disposable {
    private List<ActorSystem> systems = new ArrayList<>();
    private Actor root;
    private String name;

    private Actor actor;
    private Object component;


    public Scene(Disposable parent, String name) {
        super(parent);
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
                    if(actor != null) actor.addActor(newActor);
                    actor = newActor;
                }

                case AddData -> {
                    switch ((String) instruction.operands()[0]) {
                        case "MeshComponent" -> {






                            break;
                        }
                        case "SpotlightComponent" -> {

                            Instruction fovDeg = iterator.next();
                            Instruction eye = iterator.next();
                            Instruction center = iterator.next();
                            Instruction up = iterator.next();
                            Instruction aspectRatio = iterator.next();
                            Instruction zNear = iterator.next();
                            Instruction zFar = iterator.next();
                            Instruction zZeroToOne = iterator.next();
                            Instruction invertY = iterator.next();
                            Instruction color = iterator.next();


                            SpotlightComponent spotlightComponent = new SpotlightComponent(
                                    this,
                                    new Matrix4f().lookAt(
                                            new Vector3f(
                                                    (float) eye.operands()[1],
                                                    (float) eye.operands()[2],
                                                    (float) eye.operands()[3]
                                            ),
                                            new Vector3f(
                                                    (float) center.operands()[1],
                                                    (float) center.operands()[2],
                                                    (float) center.operands()[3]
                                            ),
                                            new Vector3f(
                                                    (float) up.operands()[1],
                                                    (float) up.operands()[2],
                                                    (float) up.operands()[3]
                                            )
                                    ),
                                    new Matrix4f().perspective(
                                            (float) Math.toRadians((float) fovDeg.operands()[1]),
                                            ((float) aspectRatio.operands()[1]),
                                            ((float) zNear.operands()[1]),
                                            ((float) zFar.operands()[1]),
                                            (boolean) zZeroToOne.operands()[1]
                                    ),
                                    (boolean) invertY.operands()[1]
                            );
                            spotlightComponent.color = new Vector3f(
                                    (float) color.operands()[1],
                                    (float) color.operands()[2],
                                    (float) color.operands()[3]
                            );
                            actor.add(spotlightComponent);
                            break;
                        }
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


    @Override
    public void dispose() {

    }
}
