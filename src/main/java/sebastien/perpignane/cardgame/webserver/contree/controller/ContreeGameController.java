package sebastien.perpignane.cardgame.webserver.contree.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;
import sebastien.perpignane.cardgame.card.CardSuit;
import sebastien.perpignane.cardgame.card.ClassicalCard;
import sebastien.perpignane.cardgame.game.GameTextDisplayer;
import sebastien.perpignane.cardgame.game.contree.ContreeBidValue;
import sebastien.perpignane.cardgame.game.contree.ContreeGame;
import sebastien.perpignane.cardgame.game.contree.ContreeGameFactory;
import sebastien.perpignane.cardgame.player.contree.ContreePlayer;
import sebastien.perpignane.cardgame.player.contree.local.thread.ThreadContreeBotPlayer;
import sebastien.perpignane.cardgame.webserver.contree.WebContreePlayer;
import sebastien.perpignane.cardgame.webserver.contree.websocket.ContreeEventService;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

record GameState(
        String gameId,
        List<String> players,
        int team1Score,
        int team2Score,
        int maxScore) {

}

record JoinGameRequest(String playerName) {}

record JoinGameResponse (String playerId, GameState gameState) {}

@RestController
@RequestMapping("/contree/game")
public class ContreeGameController {

    @Autowired
    private ContreeEventService contreeEventService;

    Map<String, ContreeGame> games = new HashMap<>();

    private final ConcurrentHashMap<String, WebContreePlayer> playersById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebContreePlayer> playersBySessionId = new ConcurrentHashMap<>();

    @PostMapping("/create")
    public NewGameResponse createGame() {
        var game = ContreeGameFactory.createGame(1500);
        var webObserver = new ContreeGameWebObserver(game.getGameId(), contreeEventService);
        game.registerAsGameObserver(GameTextDisplayer.getInstance());
        game.registerAsGameObserver(webObserver);
        game.joinGame(new ThreadContreeBotPlayer());
        game.joinGame(new ThreadContreeBotPlayer());
        game.joinGame(new ThreadContreeBotPlayer());
        games.put(game.getGameId(), game);
        return new NewGameResponse(game.getGameId());
    }

    @GetMapping("/list")
    public Collection<ContreeGame> listGames() {
        return games.values();
    }

    @PostMapping("/{gameId}/join")
    public JoinGameResponse joinGame(@PathVariable String gameId, @RequestBody JoinGameRequest joinGameRequest, HttpServletRequest r) {

        var game = games.get(gameId);


        String sessionId = r.getSession().getId();

        String wsSessionId = (String) r.getSession().getAttribute("wsSessionId");
        System.err.printf("wsSessionId is: %s%n", wsSessionId);
        System.err.printf("http session id is: %s%n", sessionId);


        var playerHandler = new ContreePlayerWebEventHandler(contreeEventService, gameId);
        var player = new WebContreePlayer(joinGameRequest.playerName(), playerHandler);
        playerHandler.setPlayer(player);
        playersBySessionId.put(sessionId, player);
        playersById.put(player.getUniqueId().toString(), player);

        /*var sebPlayerHandler = new ContreePlayerWebEventHandler(contreeEventService, gameId);
        sebPlayer = new WebContreePlayer(joinGameRequest.playerName(), sebPlayerHandler);
        sebPlayer.setHttpSessionId(r.getSession().getId());
        sebPlayerHandler.setPlayer(sebPlayer);
        playersById.put(sebPlayer.getUniqueId().toString(), sebPlayer);*/

        game.joinGame(player);

        var gs = new GameState(
                gameId,
                game.getPlayers().stream().map(ContreePlayer::getName).toList(),
                0,
                0,
                1000
        );
        return new JoinGameResponse(player.getUniqueId().toString(), gs);
    }

    @PostMapping("{gameId}/place-bid")
    public void placeBid(@PathVariable String gameId, @RequestBody BidRequest bidRequest, HttpServletRequest r) {

        var player = getCurrentPlayer(r);

        var game = games.get(gameId);

        game.placeBid(player, bidRequest.bidValue(), bidRequest.cardSuit());

    }

    @PostMapping("/{gameId}/play-card")
    public void playCard(@PathVariable String gameId, @RequestBody PlayCardRequest playCardRequest, HttpServletRequest r) {

        var player = getCurrentPlayer(r);

        var game = games.get(gameId);

        game.playCard(player, playCardRequest.card());

    }

    private ContreePlayer getCurrentPlayer(HttpServletRequest r) {
        return playersBySessionId.get(r.getSession().getId());
    }

    @MessageMapping("/hello")
    public void subscribeToGame(Principal principal, HelloMessage helloMessage) {

        System.err.printf("Receiving hello message from user with id %s%n", helloMessage.playerId());
        var player = playersById.get(helloMessage.playerId());
        player.setWsSessionId(principal.getName());

        /*sebPlayer.setWsSessionId(principal.getName());

        //player.setWsSessionId(wsSessionId);

        System.err.printf("wsSessionId: %s%n", wsSessionId);
        System.err.printf("HTTPSessionId: %s%n", sebPlayer.getHttpSessionId());
        System.err.printf("Message: %s%n", helloMessage.message());
        System.err.println("All headers ->");*/

    }

}

record HelloMessage(String message, String playerId) {}

record PlayCardRequest(String playerName, ClassicalCard card) {}

record BidRequest(String playerName, ContreeBidValue bidValue, CardSuit cardSuit) {}

record NewGameResponse(String gameId) {}
