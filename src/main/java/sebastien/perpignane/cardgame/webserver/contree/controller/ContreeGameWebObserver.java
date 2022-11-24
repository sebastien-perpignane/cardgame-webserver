package sebastien.perpignane.cardgame.webserver.contree.controller;

import sebastien.perpignane.cardgame.card.CardSuit;
import sebastien.perpignane.cardgame.card.ClassicalCard;
import sebastien.perpignane.cardgame.game.GameObserver;
import sebastien.perpignane.cardgame.game.GameState;
import sebastien.perpignane.cardgame.game.Trick;
import sebastien.perpignane.cardgame.game.contree.*;
import sebastien.perpignane.cardgame.game.war.WarGame;
import sebastien.perpignane.cardgame.player.Player;
import sebastien.perpignane.cardgame.player.Team;
import sebastien.perpignane.cardgame.player.contree.ContreeTeam;
import sebastien.perpignane.cardgame.webserver.contree.websocket.ContreeEventService;

public class ContreeGameWebObserver implements ContreeDealObserver, ContreeTrickObserver, GameObserver {

    private final String gameId;

    private final ContreeEventService eventService;

    public ContreeGameWebObserver(String gameId, ContreeEventService eventService) {
        this.gameId = gameId;
        this.eventService = eventService;
    }

    @Override
    public void onStateUpdated(GameState oldState, GameState newState) {
        String message = String.format("Game state updated from %s to %s", oldState, newState);
        eventService.sendAnyGameEvent(gameId, message);
    }

    @Override
    public void onCardPlayed(Player<?, ?> player, ClassicalCard classicalCard) {
        String message = String.format("%s played %s", player, classicalCard);
        eventService.sendAnyGameEvent(gameId, message);
    }

    @Override
    public void onNextPlayer(Player<?, ?> player) {
        String message = String.format("%s plays", player);
        eventService.sendAnyGameEvent(gameId, message);
    }

    @Override
    public void onWonTrick(Trick trick) {
        String message = String.format("Trick %s won by %s", trick, trick.getWinner().orElseThrow());
        eventService.sendAnyGameEvent(gameId, message);
    }

    @Override
    public void onEndOfGame(WarGame warGame) {

    }

    @Override
    public void onEndOfGame(ContreeGame contreeGame) {
        String message = "Game is over";
        eventService.sendAnyGameEvent(gameId, message);
    }

    @Override
    public void onDealStarted(String dealId) {

        String message = String.format("Deal %s is started", dealId);
        eventService.sendAnyGameEvent(gameId, message);

    }

    @Override
    public void onEndOfDeal(String dealId, Team winnerTeam, ContreeDealScore dealScore, boolean capot) {
        String winnerText = winnerTeam == null ? "No winner." : String.format("Winner is %s.", winnerTeam);
        String message = String.format("""
************************************************************************************************************************
Deal %s is over. %s%n
Raw score :
    Team 1: %d
    Team 2: %d
%s. Score before rounding :
    Team 1: %d
    Team 2: %d
Final score :
    Team 1: %s
    Team 2: %s
************************************************************************************************************************
""",
                dealId,
                winnerText,
                dealScore.getRawTeamScore(ContreeTeam.TEAM1),
                dealScore.getRawTeamScore(ContreeTeam.TEAM2),
                dealScore.isContractReached() ? "Contract is reached" : "Contract is not reached",
                dealScore.getTeamNotRoundedScore(ContreeTeam.TEAM1),
                dealScore.getTeamNotRoundedScore(ContreeTeam.TEAM2),
                dealScore.getTeamScore(ContreeTeam.TEAM1),
                dealScore.getTeamScore(ContreeTeam.TEAM2)
        );
        eventService.sendAnyGameEvent(gameId, message);
    }

    @Override
    public void onPlacedBid(String s, Player<?, ?> player, ContreeBidValue contreeBidValue, CardSuit cardSuit) {
        String message = String.format("%s placed bid: %s %s", player, contreeBidValue, cardSuit);
        eventService.sendAnyGameEvent(gameId, message);
    }

    @Override
    public void onBidStepStarted(String dealId) {
        String message = String.format("Deal %s: bid step started", dealId);
        eventService.sendAnyGameEvent(gameId, message);
    }

    @Override
    public void onBidStepEnded(String dealId) {
        String message = String.format("Deal %s : bid step is over", dealId);
        eventService.sendAnyGameEvent(gameId, message);
    }

    @Override
    public void onPlayStepStarted(String dealId, CardSuit trumpSuit) {
        String message = String.format("Deal %s: play step started. Trump is %s", dealId, trumpSuit);
        eventService.sendAnyGameEvent(gameId, message);
    }

    @Override
    public void onPlayStepEnded(String dealId) {
        String message = String.format("Deal %s: play step over", dealId);
        eventService.sendAnyGameEvent(gameId, message);
    }

    @Override
    public void onNewTrick(String trickId, CardSuit trumpSuit) {
        String message = String.format("New Trick #%s: Trump is %s", trickId, trumpSuit);
        eventService.sendAnyGameEvent(gameId, message);
    }

    @Override
    public void onTrumpedTrick(String trickId) {
        String message = String.format("Trick #%s is trumped !", trickId);
        eventService.sendAnyGameEvent(gameId, message);
    }

}
