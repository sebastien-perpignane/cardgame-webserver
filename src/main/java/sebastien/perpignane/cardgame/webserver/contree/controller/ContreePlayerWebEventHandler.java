package sebastien.perpignane.cardgame.webserver.contree.controller;

import org.springframework.beans.factory.annotation.Autowired;
import sebastien.perpignane.cardgame.card.ClassicalCard;
import sebastien.perpignane.cardgame.game.contree.ContreeBidValue;
import sebastien.perpignane.cardgame.player.contree.ContreePlayer;
import sebastien.perpignane.cardgame.player.contree.event.handler.ContreePlayerEventHandler;
import sebastien.perpignane.cardgame.webserver.contree.WebContreePlayer;
import sebastien.perpignane.cardgame.webserver.contree.websocket.ContreeEventService;

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
        contreeEventService.sendAnyGameEvent(gameId, bidTurnEvent);
        //contreeEventService.sendPlayerEvent(bidTurnEvent);
    }

    @Override
    public void onPlayerTurn(Set<ClassicalCard> allowedCards) {
        var handAsCard = ClassicalCard.sort(contreePlayer.getHand());
        var eventData = new PlayTurnEventData(ClassicalCard.sort(allowedCards), handAsCard);
        var playerTurnEvent = new PlayerEvent(gameId, contreePlayer.getWsSessionId(), PlayerEventType.PLAY_TURN, eventData);
        contreeEventService.sendAnyGameEvent(gameId, playerTurnEvent);
        contreeEventService.sendPlayerEvent(playerTurnEvent);
    }

    @Override
    public void onGameOver() {
        var playerGameOverEvent = new PlayerEvent(gameId, contreePlayer.getUniqueId().toString(), PlayerEventType.GAME_OVER, "game is over");
        contreeEventService.sendAnyGameEvent(gameId, playerGameOverEvent);
    }

    @Override
    public void onGameStarted() {
        var playerGameStartedEvent = new PlayerEvent(gameId, contreePlayer.getUniqueId().toString(), PlayerEventType.GAME_STARTED, "game is started");
        contreeEventService.sendAnyGameEvent(gameId, playerGameStartedEvent);
    }

    // TODO allow anything extending ContreePlayer with generics
    @Override
    public void setPlayer(ContreePlayer player) {
        this.contreePlayer = (WebContreePlayer) player;
    }

    @Override
    public void onEjection() {
        contreeEventService.sendAnyGameEvent(gameId, new PlayerEvent(gameId, contreePlayer.getUniqueId().toString(), PlayerEventType.EJECTED, "T'es viré"));
    }

    @Override
    public boolean isBot() {
        return false;
    }
}

enum PlayerEventType {
    BID_TURN,
    PLAY_TURN,
    GAME_STARTED, GAME_OVER, EJECTED
}

record BidTurnEventData(List<ContreeBidValue> allowedBidValues, List<ClassicalCard> hand) { }

record PlayTurnEventData(List<ClassicalCard> allowedCards, List<ClassicalCard> hand) { }