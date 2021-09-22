package personal.dbakunevich.findCopies;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class MyHashMap {
    private static final Map<InetAddress, Long> knownCopies = new HashMap<>();

    public synchronized static void put(InetAddress address, long time) {
        knownCopies.remove(address);
        knownCopies.put(address, time);
    }

    public synchronized static void delete() {
        knownCopies.entrySet().removeIf(entry -> {
            if (entry.getValue() < System.currentTimeMillis()) {
                System.out.println("Lost: " + entry.getKey());
                return true;
            }
            return false;
        });
    }
    public synchronized static void printMap() {
       for (var entry : knownCopies.entrySet()) {
           System.out.println("Key: " + entry.getKey() + " value: " + entry.getValue());
       }
    }
}
