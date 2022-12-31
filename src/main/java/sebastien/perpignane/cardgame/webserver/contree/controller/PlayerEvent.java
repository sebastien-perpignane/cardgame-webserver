package sebastien.perpignane.cardgame.webserver.contree.controller;

public record PlayerEvent(String gameId, String playerId, PlayerEventType type, Object eventData) { }
