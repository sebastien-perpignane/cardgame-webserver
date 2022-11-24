package sebastien.perpignane.cardgame.webserver.contree.websocket;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class ContreeSocketHandler extends TextWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        TextMessage message = new TextMessage("Salut");
        session.getPrincipal();

        session.sendMessage(message);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {


        String payload = message.getPayload();

        System.out.printf("Message is %s%n", payload);

        session.sendMessage(new TextMessage(String.format("C'est pas gentil de dire \"%s\"", payload)));

    }

}
