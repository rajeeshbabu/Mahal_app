package com.mahal.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.application.Platform;

/**
 * A simple event bus to decouple background sync services from UI controllers.
 * Allows components to subscribe to updates for specific tables/topics.
 * All subscribers are notified on the JavaFX Application Thread.
 */
public class EventBus {
    private static EventBus instance;
    private final Map<String, List<Consumer<String>>> subscribers;

    private EventBus() {
        this.subscribers = new HashMap<>();
    }

    public static synchronized EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    /**
     * Subscribe to events for a specific topic (e.g., table name).
     * 
     * @param topic      The topic to listen for (e.g., "staff", "members")
     * @param subscriber The callback to execute when an event occurs
     */
    public synchronized void subscribe(String topic, Consumer<String> subscriber) {
        subscribers.computeIfAbsent(topic, k -> new ArrayList<>()).add(subscriber);
    }

    /**
     * Unsubscribe a listener from a topic.
     * 
     * @param topic      The topic
     * @param subscriber The subscriber to remove
     */
    public synchronized void unsubscribe(String topic, Consumer<String> subscriber) {
        if (subscribers.containsKey(topic)) {
            subscribers.get(topic).remove(subscriber);
        }
    }

    /**
     * Publish an event to all subscribers of a topic.
     * The subscribers will be called on the JavaFX Application Thread.
     * 
     * @param topic   The topic (e.g., "staff")
     * @param message Optional message or payload (can be null)
     */
    public synchronized void publish(String topic, String message) {
        List<Consumer<String>> topicSubscribers = subscribers.get(topic);
        if (topicSubscribers != null && !topicSubscribers.isEmpty()) {
            // Create a copy to avoid ConcurrentModificationException if a listener
            // unsubscribes during execution
            List<Consumer<String>> copy = new ArrayList<>(topicSubscribers);

            Platform.runLater(() -> {
                for (Consumer<String> subscriber : copy) {
                    try {
                        subscriber.accept(message);
                    } catch (Exception e) {
                        System.err.println("Error in EventBus subscriber for topic '" + topic + "': " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
