package sebastien.perpignane.cardgame.webserver.contree;

import sebastien.perpignane.cardgame.player.contree.ContreePlayerEventHandler;
import sebastien.perpignane.cardgame.player.contree.ContreePlayerImpl;

public class WebContreePlayer extends ContreePlayerImpl {
    private String wsSessionId;

    public WebContreePlayer(String name, ContreePlayerEventHandler playerEventHandler) {
        super(name, playerEventHandler);
    }

    public String getWsSessionId() {
        return wsSessionId;
    }

    public void setWsSessionId(String wsSessionId) {
        this.wsSessionId = wsSessionId;
    }
}
