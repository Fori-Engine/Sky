package fori.ecs;

import java.util.ListIterator;

public interface MessageHandler {
    boolean handleMessage(ListIterator<Message> iterator, Message message);
}