package sebastien.perpignane.cardgame.webserver.contree.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.web.bind.annotation.*;
import sebastien.perpignane.cardgame.card.CardSuit;
import sebastien.perpignane.cardgame.card.ClassicalCard;
import sebastien.perpignane.cardgame.game.GameTextDisplayer;
import sebastien.perpignane.cardgame.game.contree.ContreeBidValue;
import sebastien.perpignane.cardgame.game.contree.ContreeGame;
import sebastien.perpignane.cardgame.game.contree.ContreeGameFactory;
import sebastien.perpignane.cardgame.game.contree.ContreeGamePlayers;
import sebastien.perpignane.cardgame.player.contree.ContreePlayer;
import sebastien.perpignane.cardgame.player.contree.ContreePlayerImpl;
import sebastien.perpignane.cardgame.player.contree.handlers.ContreeBotPlayerEventHandler;
import sebastien.perpignane.cardgame.webserver.contree.WebContreePlayer;
import sebastien.perpignane.cardgame.webserver.contree.websocket.ContreeEventService;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

record JoinGameResponse (String playerId, GameState gameState, FullPlayerModel playerModel) {}

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
        var game = ContreeGameFactory.createGame(1500);

        String gameId = game.getGameId();

        var webObserver = new ContreeGameWebObserver(game.getGameId(), contreeEventService);
        game.registerAsGameObserver(GameTextDisplayer.getInstance());
        game.registerAsGameObserver(webObserver);

        String sessionId = r.getSession().getId();
        var playerHandler = new ContreePlayerWebEventHandler(contreeEventService, gameId);
        var player = new WebContreePlayer(createGameRequest.playerName(), playerHandler);
        playerHandler.setPlayer(player);

        String playerId = player.getUniqueId();

        playersBySessionId.put(sessionId, player);
        playersById.put(player.getUniqueId(), player);
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

    private ContreePlayer botPlayer() {
        return new ContreePlayerImpl(new ContreeBotPlayerEventHandler());
    }

    @PostMapping("/{gameId}/start")
    public StartGameResponse startGame(@PathVariable String gameId) {
        final ContreeGame game = games.get(gameId);

        int nbPlayers = getGameNbPlayers(game);

        int missingPlayers = ContreeGamePlayers.NB_PLAYERS - nbPlayers;

        IntStream.range(0, missingPlayers).forEach(i -> game.joinGame(botPlayer()) );

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
        playersById.put(player.getUniqueId(), player);

        game.joinGame(player);

        var gs = new GameState(
                gameId,
                game.getPlayers().stream().map(ContreePlayer::getName).toList(),
                0,
                0,
                1500
        );

        FullPlayerModel playerModel = new FullPlayerModel(player.getName(), player.getTeam().orElseThrow().name(), player.getHand());
        return new JoinGameResponse(player.getUniqueId(), gs, playerModel);
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

        System.err.printf("Receiving hello message from user with id %s%n", playerId);
        var player = playersById.get(playerId);
        player.setWsSessionId(principal.getName());

        return new HelloResponse("hello ok for gameId " + gameId);

    }

}

record PlayCardRequest(String playerName, ClassicalCard card) {}

record BidRequest(String playerName, ContreeBidValue bidValue, CardSuit cardSuit) {}

record NewGameResponse(String gameId, String playerId) {}
