package io.zipcoder.originwar.model;
import java.util.List;
public record TurnResult(boolean accepted, int cost, List<GameEvent> events) {
    public TurnResult { events = List.copyOf(events); }
    public static TurnResult rejected() { return new TurnResult(false, 0, List.of()); }
}
