package sebastien.perpignane.cardgame.webserver.contree.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.web.bind.annotation.*;
import sebastien.perpignane.cardgame.card.CardSuit;
import sebastien.perpignane.cardgame.card.ClassicalCard;
import sebastien.perpignane.cardgame.game.GameTextDisplayer;
import sebastien.perpignane.cardgame.game.contree.*;
import sebastien.perpignane.cardgame.player.contree.ContreePlayer;
import sebastien.perpignane.cardgame.player.contree.ContreePlayerImpl;
import sebastien.perpignane.cardgame.player.contree.handlers.ContreeBotPlayerEventHandler;
import sebastien.perpignane.cardgame.webserver.contree.WebContreePlayer;
import sebastien.perpignane.cardgame.webserver.contree.websocket.ContreeEventService;

import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.System.err;

record GameState(
        String gameId,
        List<String> players,
        int team1Score,
        int team2Score,
        int maxScore) {

}

record CreateGameRequest(String playerName) {}

record JoinGameRequest(String playerName) {}

record StartGameResponse(GameState gameState) {}

record JoinGameResponse (String playerId, ContreeGameState gameState, FullPlayerModel playerModel) {}

record HelloResponse(String message) {}

@RestController
@RequestMapping("/contree/game")
public class ContreeGameController {

    @Autowired
    private ContreeEventService contreeEventService;

    Map<String, ContreeGame> games = new HashMap<>();

    private final ConcurrentHashMap<String, WebContreePlayer> playersById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebContreePlayer> playersBySessionId = new ConcurrentHashMap<>();

    @PostMapping("/create")
    public NewGameResponse createGame(@RequestBody CreateGameRequest createGameRequest, HttpServletRequest r) {
        var game = ContreeGameFactory.createGame(new ContreeGameConfig() {});

        String gameId = game.getGameId();

        var webObserver = new ContreeGameWebObserver(game.getGameId(), contreeEventService);
        game.registerAsGameObserver(GameTextDisplayer.getInstance());
        game.registerAsGameObserver(webObserver);

        String sessionId = r.getSession().getId();
        var playerHandler = new ContreePlayerWebEventHandler(contreeEventService, gameId);
        var player = new WebContreePlayer(createGameRequest.playerName(), playerHandler);
        playerHandler.setPlayer(player);

        String playerId = player.getId();

        playersBySessionId.put(sessionId, player);
        playersById.put(playerId, player);
        game.joinGame(player);
        games.put(game.getGameId(), game);
        return new NewGameResponse(gameId, playerId);
    }

    @SubscribeMapping("/create")
    public NewGameResponse subscribeCreateNewGame(Principal principal, @Header("playerName") String playerName) {

        var game = ContreeGameFactory.createGame(new ContreeGameConfig() {});

        String gameId = game.getGameId();

        var webObserver = new ContreeGameWebObserver(game.getGameId(), contreeEventService);
        game.registerAsGameObserver(GameTextDisplayer.getInstance());
        game.registerAsGameObserver(webObserver);
        var playerHandler = new ContreePlayerWebEventHandler(contreeEventService, gameId);
        var player = new WebContreePlayer(playerName, playerHandler);
        player.setWsSessionId(principal.getName());
        playerHandler.setPlayer(player);

        String playerId = player.getId();

        playersById.put(playerId, player);
        game.joinGame(player);
        games.put(game.getGameId(), game);
        return new NewGameResponse(gameId, playerId);

    }

    @GetMapping("/list")
    public Collection<String> listGames() {
        return games.values().stream().map(ContreeGame::getGameId).collect(Collectors.toSet());
    }

    private int getGameNbPlayers(ContreeGame game) {
        return game.getPlayers().stream().mapToInt(p -> p == null ? 0 : 1).sum();
    }

    private ContreePlayer botPlayer(int playerIndex) {
        int playerNumber = playerIndex + 1;
        return new ContreePlayerImpl("Player " + playerNumber, new ContreeBotPlayerEventHandler());
    }

    @PostMapping("/{gameId}/start")
    public StartGameResponse startGame(@PathVariable String gameId) {
        final ContreeGame game = games.get(gameId);

        int nbPlayers = getGameNbPlayers(game);

        int missingPlayers = ContreePlayers.NB_PLAYERS - nbPlayers;

        IntStream.range(0, missingPlayers).forEach(i -> game.joinGame(botPlayer(i)) );

        var gs = new GameState(
                gameId,
                game.getPlayers().stream().map(ContreePlayer::getName).toList(),
                0,
                0,
                1500
        );

        return new StartGameResponse(gs);

    }


    @PostMapping("/{gameId}/join")
    public JoinGameResponse joinGame(@PathVariable String gameId, @RequestBody JoinGameRequest joinGameRequest, HttpServletRequest r) {

        var game = games.get(gameId);


        String sessionId = r.getSession().getId();

        var playerHandler = new ContreePlayerWebEventHandler(contreeEventService, gameId);
        var player = new WebContreePlayer(joinGameRequest.playerName(), playerHandler);
        playerHandler.setPlayer(player);
        playersBySessionId.put(sessionId, player);
        playersById.put(player.getId(), player);

        game.joinGame(player);

        var gs = game.toState();

        FullPlayerModel playerModel = new FullPlayerModel(player.getName(), player.getTeam().orElseThrow().name(), player.getHand());
        return new JoinGameResponse(player.getId(), gs, playerModel);
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

    @SubscribeMapping(value = {"/topic/game/{gameId}", "/user/topic/game/{gameId}"})
    public HelloResponse subscribeToGame(Principal principal, @DestinationVariable String gameId, @Header("playerId") String playerId) {

        err.printf("Receiving hello message from user with id %s%n", playerId);
        var player = playersById.get(playerId);
        player.setWsSessionId(principal.getName());

        return new HelloResponse("hello ok for gameId " + gameId);

    }

}

record PlayCardRequest(String playerName, ClassicalCard card) {}

record BidRequest(String playerName, ContreeBidValue bidValue, CardSuit cardSuit) {}

record NewGameResponse(String gameId, String playerId) {}
