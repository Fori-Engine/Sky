package fori.physics;

import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.collision.Manifold;
import org.jbox2d.dynamics.contacts.Contact;
import org.joml.Vector2f;
import fori.Time;
import fori.graphics.Rect2D;

import java.util.ArrayList;

public class World {
    private org.jbox2d.dynamics.World world;
    private float accumulator = 0f;
    private float screen2Physics;

    private int velocityIterations = 8;
    private int positionIterations = 3;

    private ArrayList<ContactListener> contactListeners = new ArrayList<>();


    public World(float screen2PhysicsMultiplier, Vector2f gravity){
        setScreen2Physics(screen2PhysicsMultiplier);
        world = new org.jbox2d.dynamics.World(PhysicsUtil.toVec2(gravity));

        world.setContactListener(new org.jbox2d.callbacks.ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                for(ContactListener contactListener : contactListeners){
                    contactListener.beginContact(contact);
                }
            }

            @Override
            public void endContact(Contact contact) {
                for(ContactListener contactListener : contactListeners){
                    contactListener.endContact(contact);
                }
            }

            @Override
            public void preSolve(Contact contact, Manifold manifold) {

            }

            @Override
            public void postSolve(Contact contact, ContactImpulse contactImpulse) {

            }
        });
    }

    public RectBody2D newRectBody2D(Rect2D rect2D, RigidBody2D.Type bodyType, boolean canRotate){
        return new RectBody2D(screen2Physics, world, rect2D, bodyType, canRotate);
    }

    public CircleBody2D newCircleBody2D(Vector2f pos, float radius, RigidBody2D.Type bodyType, boolean canRotate) {
        return new CircleBody2D(screen2Physics, world, radius, pos, bodyType, canRotate);
    }

    public void update(float step){
        //Prevent skipping the Box2D simulation too far into the future if the
        //Frame-rate gets slowed because of the Rendering side
        float frameTime = java.lang.Math.min(Time.deltaTime, 0.25f);
        accumulator += frameTime;

        while(accumulator >= step){
            world.step(step, velocityIterations, positionIterations);
            accumulator -= step;
        }

    }

    public int getVelocityIterations() {
        return velocityIterations;
    }

    public void setVelocityIterations(int velocityIterations) {
        this.velocityIterations = velocityIterations;
    }

    public int getPositionIterations() {
        return positionIterations;
    }

    public void setPositionIterations(int positionIterations) {
        this.positionIterations = positionIterations;
    }
    public void addContactListener(ContactListener contactListener){
        contactListeners.add(contactListener);
    }

    public void removeContactListener(ContactListener contactListener){
        contactListeners.remove(contactListener);
    }

    public float getScreen2Physics() {
        return screen2Physics;
    }

    public void setScreen2Physics(float screen2Physics) {
        this.screen2Physics = screen2Physics;
    }

    public Vector2f getGravity(){
        return PhysicsUtil.toVector2f(world.getGravity());
    }

    public org.jbox2d.dynamics.World getWorld() {
        return world;
    }
}
