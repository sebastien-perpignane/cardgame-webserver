package sebastien.perpignane.cardgame.webserver.contree;

import sebastien.perpignane.cardgame.player.contree.ContreePlayerEventHandler;
import sebastien.perpignane.cardgame.player.contree.ContreePlayerImpl;

import java.util.UUID;

public class WebContreePlayer extends ContreePlayerImpl {

    private final String uniqueId;
    private String wsSessionId;

    public WebContreePlayer(String name, ContreePlayerEventHandler playerEventHandler) {
        super(name, playerEventHandler);
        uniqueId = UUID.randomUUID().toString();
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getWsSessionId() {
        return wsSessionId;
    }

    public void setWsSessionId(String wsSessionId) {
        this.wsSessionId = wsSessionId;
    }
}
