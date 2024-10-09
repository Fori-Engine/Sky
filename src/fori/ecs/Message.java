package fori.ecs;
import java.lang.System;

public class Message {
    public Entity sender;
    public String type;

    public String recipient;

    public long lifetimeMs;

    public long creationTime;

    public boolean immortal;

    public static Message newTimedMessage(String type, Entity sender, String recipient, long lifetimeMs){
        return new Message(type, sender, recipient, lifetimeMs);
    }
    public static Message newImmortalMessage(String type, Entity sender, String recipient){
        return new Message(type, sender, recipient);
    }

    public Message(String type, Entity sender, String recipient, long lifetimeMs) {
        this.sender = sender;
        this.recipient = recipient;
        this.type = type;
        this.lifetimeMs = lifetimeMs;
        creationTime = System.currentTimeMillis();
    }

    public Message(String type, Entity sender, String recipient) {
        this.sender = sender;
        this.recipient = recipient;
        this.type = type;
        creationTime = System.currentTimeMillis();
        this.immortal = true;
    }

    @Override
    public String toString() {
        return type;
    }
}
