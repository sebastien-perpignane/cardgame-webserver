package sebastien.perpignane.cardgame.webserver.contree;

import sebastien.perpignane.cardgame.player.contree.event.handler.ContreePlayerEventHandler;
import sebastien.perpignane.cardgame.player.contree.event.handler.ContreePlayerEventHandlerImpl;

import java.util.UUID;

public class WebContreePlayer extends ContreePlayerEventHandlerImpl {

    private final UUID uniqueId;
    private String wsSessionId;

    public WebContreePlayer(String name, ContreePlayerEventHandler playerEventHandler) {
        super(name, playerEventHandler);
        uniqueId = UUID.randomUUID();
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getWsSessionId() {
        return wsSessionId;
    }

    public void setWsSessionId(String wsSessionId) {
        this.wsSessionId = wsSessionId;
    }
}
