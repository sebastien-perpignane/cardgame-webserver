package sebastien.perpignane.cardgame.webserver.contree.controller;

import org.springframework.beans.factory.annotation.Autowired;
import sebastien.perpignane.cardgame.card.ClassicalCard;
import sebastien.perpignane.cardgame.game.contree.ContreeBidValue;
import sebastien.perpignane.cardgame.player.contree.ContreePlayer;
import sebastien.perpignane.cardgame.player.contree.event.handler.ContreePlayerEventHandler;
import sebastien.perpignane.cardgame.webserver.contree.websocket.ContreeEventService;

import java.util.List;
import java.util.Set;

public class ContreePlayerWebEventHandler implements ContreePlayerEventHandler {

    @Autowired
    private ContreeEventService contreeEventService;

    private ContreePlayer contreePlayer;

    private final String gameId;

    public ContreePlayerWebEventHandler(ContreeEventService eventService, String gameId) {
        this.gameId = gameId;
        this.contreeEventService = eventService;
    }

    @Override
    public void onPlayerTurnToBid(Set<ContreeBidValue> allowedBidValues) {

        var event = new BidTurnEvent(allowedBidValues.stream().sorted().toList(), ClassicalCard.sort(contreePlayer.getHand()));

        contreeEventService.sendAnyGameEvent(gameId, event);
    }

    @Override
    public void onPlayerTurn(Set<ClassicalCard> allowedCards) {
        var event = new PlayTurnEvent(ClassicalCard.sort(allowedCards), ClassicalCard.sort(contreePlayer.getHand()));
        contreeEventService.sendAnyGameEvent(gameId, event);
    }

    @Override
    public void onGameOver() {
        contreeEventService.sendAnyGameEvent(gameId, "game is over");
    }

    @Override
    public void onGameStarted() {
        contreeEventService.sendAnyGameEvent(gameId, "game is started");
    }

    @Override
    public void setPlayer(ContreePlayer player) {
        this.contreePlayer = player;
    }

    @Override
    public boolean isBot() {
        return false;
    }
}

record BidTurnEvent(List<ContreeBidValue> allowedBidValues, List<ClassicalCard> hand) {
}

record PlayTurnEvent(List<ClassicalCard> allowedCards, List<ClassicalCard> hand) {

}