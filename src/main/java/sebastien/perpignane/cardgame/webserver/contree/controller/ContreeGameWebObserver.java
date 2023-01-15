package sebastien.perpignane.cardgame.webserver.contree.controller;

import sebastien.perpignane.cardgame.card.CardSuit;
import sebastien.perpignane.cardgame.card.ClassicalCard;
import sebastien.perpignane.cardgame.game.GameObserver;
import sebastien.perpignane.cardgame.game.GameStatus;
import sebastien.perpignane.cardgame.game.Trick;
import sebastien.perpignane.cardgame.game.contree.*;
import sebastien.perpignane.cardgame.game.war.WarGame;
import sebastien.perpignane.cardgame.player.Player;
import sebastien.perpignane.cardgame.player.Team;
import sebastien.perpignane.cardgame.player.contree.ContreePlayer;
import sebastien.perpignane.cardgame.player.contree.ContreePlayerState;
import sebastien.perpignane.cardgame.player.contree.ContreeTeam;
import sebastien.perpignane.cardgame.webserver.contree.websocket.ContreeEventService;

import java.util.Collection;

public class ContreeGameWebObserver implements ContreeDealObserver, ContreeTrickObserver, GameObserver {

    private final String gameId;

    private final ContreeEventService eventService;

    public ContreeGameWebObserver(String gameId, ContreeEventService eventService) {
        this.gameId = gameId;
        this.eventService = eventService;
    }

    @Override
    public void onCardPlayed(Player<?, ?> player, ClassicalCard classicalCard) {
        ContreePlayer p = (ContreePlayer) player;
        var event = new ContreeGameEvent(
            EventType.CARD_PLAYED,
            new CardPlayedEventData(
                new PlayerModel(p.getName(), p.getTeam().orElseThrow().name()),
                classicalCard
            )
        );
        eventService.sendAnyGameEvent(gameId, event);
    }

    @Override
    public void onNextPlayer(Player<?, ?> player) {
        String message = String.format("%s plays", player);
        eventService.sendAnyGameEvent(gameId, new ContreeGameEvent(EventType.NEXT_PLAYER, message));
    }

    @Override
    public void onWonTrick(Trick trick) {
        String message = String.format("Trick %s won by %s", trick, trick.getWinner().orElseThrow());
        eventService.sendAnyGameEvent(gameId, new ContreeGameEvent(EventType.TRICK_OVER, message));
    }

    @Override
    public void onEndOfGame(WarGame warGame) {

    }

    @Override
    public void onEndOfGame(ContreeGame contreeGame) {
        String message = String.format("Game is over. Winner is %s", contreeGame.getWinner().orElseThrow());
        eventService.sendAnyGameEvent(gameId, new ContreeGameEvent(EventType.GAME_OVER, message));
    }

    @Override
    public void onDealStarted(int dealNumber, String dealId) {
        var eventData = new DealStartedEventData(dealNumber, dealId);
        eventService.sendAnyGameEvent(gameId, new ContreeGameEvent(EventType.DEAL_STARTED, eventData));

    }

    @Override
    public void onEndOfDeal(String dealId, Team winnerTeam, ContreeDealScore dealScore, boolean capot) {
        var eventData = new EndOfDealEventData(dealId,
                winnerTeam,
                dealScore.getRawTeamScore(ContreeTeam.TEAM1),
                dealScore.getRawTeamScore(ContreeTeam.TEAM2),
                dealScore.isContractReached(),
                dealScore.getTeamNotRoundedScore(ContreeTeam.TEAM1),
                dealScore.getTeamNotRoundedScore(ContreeTeam.TEAM2),
                dealScore.getTeamScore(ContreeTeam.TEAM1),
                dealScore.getTeamScore(ContreeTeam.TEAM2)
        );
        eventService.sendAnyGameEvent(gameId, new ContreeGameEvent(EventType.DEAL_OVER, eventData));
    }

    @Override
    public void onPlacedBid(String s, Player<?, ?> player, ContreeBidValue contreeBidValue, CardSuit cardSuit) {

        ContreePlayer p = (ContreePlayer) player;

        var event = new ContreeGameEvent(
                EventType.PLACED_BID,
                new BidPlacedEventData(
                    new PlayerModel(
                        p.getName(),
                        p.getTeam().orElseThrow().name()
                    ),
                    contreeBidValue, cardSuit
                )
        );

        eventService.sendAnyGameEvent(gameId, event);
    }

    @Override
    public void onBidStepStarted(String dealId) {
        String message = String.format("Deal %s: bid step started", dealId);
        eventService.sendAnyGameEvent(gameId, new ContreeGameEvent(EventType.BID_STEP_STARTED, message));
    }

    @Override
    public void onBidStepEnded(String dealId) {
        String message = String.format("Deal %s : bid step is over", dealId);
        eventService.sendAnyGameEvent(gameId, new ContreeGameEvent(EventType.BID_STEP_OVER, message));
    }

    @Override
    public void onPlayStepStarted(String dealId, CardSuit trumpSuit) {
        var eventData = new PlayStepStartedEventData(dealId, trumpSuit);
        eventService.sendAnyGameEvent(gameId, new ContreeGameEvent(EventType.PLAY_STEP_STARTED, eventData));
    }

    @Override
    public void onPlayStepEnded(String dealId) {
        String message = String.format("Deal %s: play step over", dealId);
        eventService.sendAnyGameEvent(gameId, new ContreeGameEvent(EventType.PLAY_STEP_OVER, message));
    }

    @Override
    public void onNewTrick(String trickId, CardSuit trumpSuit) {
        //String message = String.format("New Trick #%s: Trump is %s", trickId, trumpSuit);
        var trickStartedEventData = new TrickStartedEventData(trickId, trumpSuit);
        eventService.sendAnyGameEvent(gameId, new ContreeGameEvent(EventType.TRICK_STARTED, trickStartedEventData));
    }

    @Override
    public void onTrumpedTrick(String trickId) {
        String message = String.format("Trick #%s is trumped !", trickId);
        eventService.sendAnyGameEvent(gameId, new ContreeGameEvent(EventType.TRUMPED_TRICK, message));
    }

    @Override
    public void onEndOfTrick(String trickId, ContreePlayer winner) {
        var trickOverEventData = new TrickOverEventData(trickId, winner.toState());
        eventService.sendAnyGameEvent(gameId, new ContreeGameEvent(EventType.TRICK_OVER, trickOverEventData));
    }

    @Override
    public void onJoinedGame(ContreeGame contreeGame, int playerIndex, ContreePlayer player) {
        var joinedGameEventData = new JoinedGameEventData(playerIndex, player.getName());
        eventService.sendAnyGameEvent(gameId, new ContreeGameEvent(EventType.JOINED_GAME, joinedGameEventData));
    }

    @Override
    public void onStateUpdated(GameStatus oldState, GameStatus newState) {

    }
}

enum EventType {
    GAME_STARTED,
    GAME_OVER,
    DEAL_STARTED,
    DEAL_OVER,
    TRICK_STARTED,
    TRICK_OVER,
    PLACED_BID,
    CARD_PLAYED,
    NEXT_PLAYER,
    BID_STEP_STARTED,
    BID_STEP_OVER,
    PLAY_STEP_STARTED,
    PLAY_STEP_OVER,
    TRUMPED_TRICK,
    GAME_STATE_UPDATED,
    JOINED_GAME
}

record TrickOverEventData(String trickId, ContreePlayerState winner) {}

record ContreeGameEvent(EventType type, Object eventData) {
}

record EndOfDealEventData(
        String dealId,
        Team winnerTeam,
        int team1RawScore,
        int team2RawScore,
        boolean contractIsReached,
        int team1NotRounderScore,
        int team2NotRounderScore,
        int team1Score,
        int team2Score
        ) {

}

record CardPlayedEventData(
        PlayerModel player,
        ClassicalCard card) {}

record BidPlacedEventData(PlayerModel player, ContreeBidValue bidValue, CardSuit cardSuit) {}

record TrickStartedEventData(String trickId, CardSuit trumpSuit) {}

record PlayerModel(String name, String team) {}

record FullPlayerModel(String name, String team, Collection<ClassicalCard> hand) {}

record PlayStepStartedEventData(String dealId, CardSuit trumpSuit) {}

record DealStartedEventData(int dealNumber, String dealId) {}

record JoinedGameEventData(int playerIndex, String playerName) {}