package de.werwolf2303.xpairdebug.wireObjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class Message implements Serializable {
    private final MessageType type;
    private final HashMap<String, Object> data;

    public Message(MessageType type) {
        data = new HashMap<>();
        this.type = type;
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public ArrayList<String> getKeys() {
        return new ArrayList<>(data.keySet());
    }

    public void remove(String key) {
        data.remove(key);
    }

    public MessageType getType() {
        return type;
    }
}
