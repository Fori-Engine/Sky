package fori.ecs;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.System;

public class MessageQueue {
    private final ArrayList<Message> messages = new ArrayList<>();

    private final ArrayList<Message> queuedMessages = new ArrayList<>();

    public MessageQueue() {
    }

    public void post(Message message){
        messages.add(message);
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void process(MessageHandler m){

        for (ListIterator<Message> iterator = messages.listIterator(); iterator.hasNext(); ) {
            Message message = iterator.next();




            if(m.handleMessage(iterator, message)) {
                iterator.remove();
                break;
            }



            if(!message.immortal && ((System.currentTimeMillis() - message.creationTime) >= message.lifetimeMs))
                iterator.remove();


        }

        messages.addAll(queuedMessages);
        queuedMessages.clear();

    }



    public void postLater(Message message) {
        queuedMessages.add(message);
    }

    public Message messageExists(boolean b, String... types){
        AtomicReference<Message> m = new AtomicReference<>();

        process((iterator, message) -> {

            for(String messageType : types){
                if(message.type.equals(messageType)){
                    m.set(message);
                    return b;
                }

            }


            return false;
        });

        return m.get();
    }
}
