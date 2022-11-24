package sebastien.perpignane.cardgame.webserver.contree.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import sebastien.perpignane.cardgame.card.CardSuit;
import sebastien.perpignane.cardgame.card.ClassicalCard;
import sebastien.perpignane.cardgame.game.GameTextDisplayer;
import sebastien.perpignane.cardgame.game.contree.ContreeBidValue;
import sebastien.perpignane.cardgame.game.contree.ContreeGame;
import sebastien.perpignane.cardgame.game.contree.ContreeGameFactory;
import sebastien.perpignane.cardgame.player.contree.local.thread.ThreadContreeBotPlayer;
import sebastien.perpignane.cardgame.player.contree.event.handler.ContreePlayerEventHandlerImpl;
import sebastien.perpignane.cardgame.player.contree.ContreePlayer;
import sebastien.perpignane.cardgame.webserver.contree.websocket.ContreeEventService;

import java.util.*;

@RestController
@RequestMapping("/contree/game")
public class ContreeGameController {

    @Autowired
    private ContreeEventService contreeEventService;

    Map<String, ContreeGame> games = new HashMap<>();

    private ContreePlayer sebPlayer;

    @PostMapping("/create")
    public String newGame() {
        var game = ContreeGameFactory.createGame(1500);
        var webObserver = new ContreeGameWebObserver(game.getGameId(), contreeEventService);
        game.registerAsGameObserver(GameTextDisplayer.getInstance());
        game.registerAsGameObserver(webObserver);
        game.joinGame(new ThreadContreeBotPlayer());
        game.joinGame(new ThreadContreeBotPlayer());
        game.joinGame(new ThreadContreeBotPlayer());
        games.put(game.getGameId(), game);
        return game.getGameId();
    }

    @GetMapping("/list")
    public Collection<ContreeGame> listGames() {
        return games.values();
    }

    @PostMapping("/{gameId}/join")
    public String joinGame(@PathVariable String gameId) {

        var playerHandler = new ContreePlayerWebEventHandler(contreeEventService, gameId);

        sebPlayer = new ContreePlayerEventHandlerImpl(playerHandler);
        playerHandler.setPlayer(sebPlayer);
        games.get(gameId).joinGame(sebPlayer);

        return "Seb";
    }

    @PostMapping("{gameId}/place-bid")
    public void placeBid(@PathVariable String gameId, @RequestBody BidRequest bidRequest) {
        var game = games.get(gameId);

        game.placeBid(sebPlayer, bidRequest.bidValue(), bidRequest.cardSuit());

    }

    @PostMapping("/{gameId}/play-card")
    public void playCard(@PathVariable String gameId, @RequestBody PlayCardRequest playCardRequest) {

        var game = games.get(gameId);

        game.playCard(sebPlayer, playCardRequest.card());

    }

}

record PlayCardRequest(String playerName, ClassicalCard card) {

}

record BidRequest(String playerName, ContreeBidValue bidValue, CardSuit cardSuit) {

}
