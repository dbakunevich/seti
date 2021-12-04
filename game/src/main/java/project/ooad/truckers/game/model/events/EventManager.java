package project.ooad.truckers.game.model.events;

import java.net.DatagramPacket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import project.ooad.truckers.game.model.announcement.IpAddress;
import project.ooad.truckers.game.model.SnakesProto.GameMessage.*;
import project.ooad.truckers.game.model.SnakesProto.GamePlayer;
import project.ooad.truckers.game.model.SnakesProto.GameState.Snake;
import project.ooad.truckers.game.model.SnakesProto.GameState.Coord;


public class EventManager {
    private final Map<String, Set<java.util.EventListener>> listeners = new HashMap<>();

    public EventManager(String... operations) {
        for (String operation : operations) {
            listeners.put(operation, ConcurrentHashMap.newKeySet());
        }
    }

    public void subscribe(java.util.EventListener listener, String... eventTypes) {
        for (String eventType : eventTypes) {
            listeners.get(eventType).add(listener);
        }
    }

    public void unsubscribe(java.util.EventListener listener, String... eventTypes) {
        for (String eventType : eventTypes) {
            listeners.get(eventType).remove(listener);
        }
    }

    public void unsubscribeAll(String... eventTypes) {
        for (String eventType : eventTypes) {
            listeners.get(eventType).clear();
        }
    }

    public void notify(String eventType,  Map<IpAddress, AnnouncementMsg> availableGames) {
        Set<java.util.EventListener> gamesListeners = listeners.get(eventType);
        for (var listener : gamesListeners) {
            listener.update(eventType, availableGames);
        }
    }

    public void notify(String eventType, List<Snake> snakes, List<Coord> food, List<GamePlayer> players) {
        Set<java.util.EventListener> gameStateListeners = listeners.get(eventType);
        for (var listener : gameStateListeners) {
            listener.update(eventType, snakes, food, players);
        }
    }

    public void notify(String eventType, DatagramPacket receivedPacket) {
        Set<java.util.EventListener> eventListeners = listeners.get(eventType);
        for (var listener : eventListeners) {
            listener.update(eventType, receivedPacket);
        }
    }

    public void notify(String eventType, String message) {
        Set<java.util.EventListener> eventListeners = listeners.get(eventType);
        for (var listener : eventListeners) {
            listener.update(eventType, message);
        }
    }

    public void notify(String eventType) {
        Set<java.util.EventListener> eventListeners = listeners.get(eventType);
        for (var listener : eventListeners) {
            listener.update(eventType);
        }
    }
}