package sebastien.perpignane.cardgame.webserver.contree.controller;

import org.springframework.beans.factory.annotation.Autowired;
import sebastien.perpignane.cardgame.card.ClassicalCard;
import sebastien.perpignane.cardgame.game.contree.ContreeBidValue;
import sebastien.perpignane.cardgame.player.contree.ContreePlayer;
import sebastien.perpignane.cardgame.player.contree.ContreePlayerEventHandler;
import sebastien.perpignane.cardgame.player.contree.ContreePlayerStatus;
import sebastien.perpignane.cardgame.webserver.contree.WebContreePlayer;
import sebastien.perpignane.cardgame.webserver.contree.websocket.ContreeEventService;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ContreePlayerWebEventHandler implements ContreePlayerEventHandler {

    @Autowired
    private ContreeEventService contreeEventService;

    private WebContreePlayer contreePlayer;

    private final String gameId;

    public ContreePlayerWebEventHandler(ContreeEventService eventService, String gameId) {
        this.gameId = gameId;
        this.contreeEventService = eventService;
    }

    @Override
    public void onPlayerTurnToBid(Set<ContreeBidValue> allowedBidValues) {
        var handAsCard = ClassicalCard.sort(contreePlayer.getHand());
        var eventData = new BidTurnEventData(allowedBidValues.stream().sorted().toList(), handAsCard);
        var bidTurnEvent = new PlayerEvent(gameId, contreePlayer.getWsSessionId(), PlayerEventType.BID_TURN, eventData);
        contreeEventService.sendPlayerEvent(bidTurnEvent);
    }

    @Override
    public void onPlayerTurn(Set<ClassicalCard> allowedCards) {
        var handAsCard = ClassicalCard.sort(contreePlayer.getHand());
        var eventData = new PlayTurnEventData(ClassicalCard.sort(allowedCards), handAsCard);
        var playerTurnEvent = new PlayerEvent(gameId, contreePlayer.getWsSessionId(), PlayerEventType.PLAY_TURN, eventData);
        contreeEventService.sendPlayerEvent(playerTurnEvent);
    }

    @Override
    public void onGameOver() {
        var playerGameOverEvent = new PlayerEvent(gameId, contreePlayer.getWsSessionId(), PlayerEventType.GAME_OVER, "game is over");
        contreeEventService.sendAnyGameEvent(gameId, playerGameOverEvent);
    }

    @Override
    public void onGameStarted() {
        var playerGameStartedEvent = new PlayerEvent(gameId, contreePlayer.getWsSessionId(), PlayerEventType.GAME_STARTED, "game is started");
        contreeEventService.sendAnyGameEvent(gameId, playerGameStartedEvent);
    }

    // TODO allow anything extending ContreePlayer with generics
    @Override
    public void setPlayer(ContreePlayer player) {
        this.contreePlayer = (WebContreePlayer) player;
    }

    @Override
    public void onEjection() {
        contreeEventService.sendPlayerEvent(new PlayerEvent(gameId, contreePlayer.getWsSessionId(), PlayerEventType.EJECTED, "T'es viré"));
    }

    @Override
    public boolean isBot() {
        return false;
    }

    @Override
    public void onReceivedHand(Collection<ClassicalCard> hand) {
        contreeEventService.sendPlayerEvent(new PlayerEvent(gameId, contreePlayer.getWsSessionId(), PlayerEventType.HAND_RECEIVED, hand));
    }

    @Override
    public void onStatusUpdate(ContreePlayerStatus oldStatus, ContreePlayerStatus newStatus) {
        contreeEventService.sendPlayerEvent(new PlayerEvent(gameId, contreePlayer.getWsSessionId(), PlayerEventType.STATUS_UPDATE, new StatusUpdateEventData(oldStatus, newStatus)));
    }
}

enum PlayerEventType {
    BID_TURN,
    PLAY_TURN,
    GAME_STARTED,
    GAME_OVER,
    EJECTED,
    HAND_RECEIVED,
    STATUS_UPDATE
}

record BidTurnEventData(List<ContreeBidValue> allowedBidValues, List<ClassicalCard> hand) { }

record PlayTurnEventData(List<ClassicalCard> allowedCards, List<ClassicalCard> hand) { }

record StatusUpdateEventData(ContreePlayerStatus oldStatus, ContreePlayerStatus newStatus) {}