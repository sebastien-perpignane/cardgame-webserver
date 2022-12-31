package sebastien.perpignane.cardgame.webserver.contree.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MyHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {

        System.err.println("beforeHandshake");
        if (request instanceof ServletServerHttpRequest servletRequest) {

            HttpSession session = servletRequest.getServletRequest().getSession();

            System.err.printf("beforeHandshake gives session id %s%n", session.getId());
            session.setAttribute("wsSessionId", session.getId());
            attributes.put("wsSessionId", session.getId());
            System.err.printf("JSESSIONID: %s%n", Arrays.stream(((ServletServerHttpRequest) request).getServletRequest().getCookies()).map(Cookie::getValue).collect(Collectors.joining(", ")));
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
