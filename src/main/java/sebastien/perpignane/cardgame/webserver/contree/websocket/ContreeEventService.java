package sebastien.perpignane.cardgame.webserver.contree.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import sebastien.perpignane.cardgame.webserver.contree.controller.PlayerEvent;

@Component
public class ContreeEventService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendAnyGameEvent(String gameId, Object event) {
        String destination = String.format("/topic/game/%s", gameId);
        messagingTemplate.convertAndSend(destination, event);
    }

    public void sendPlayerEvent(PlayerEvent playerEvent) {
        messagingTemplate.convertAndSendToUser(playerEvent.playerId(), String.format("/topic/game/%s", playerEvent.gameId()), playerEvent);
    }

}


