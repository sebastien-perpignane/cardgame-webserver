package sebastien.perpignane.cardgame.webserver;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import sebastien.perpignane.cardgame.card.CardSuit;
import sebastien.perpignane.cardgame.card.ClassicalCard;
import sebastien.perpignane.cardgame.game.contree.ContreeBidValue;
import sebastien.perpignane.cardgame.webserver.contree.websocket.ContreeGameHandshakeHandler;
import sebastien.perpignane.cardgame.webserver.contree.websocket.MyHandshakeInterceptor;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements  WebSocketMessageBrokerConfigurer { // WebSocketConfigurer {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setUserDestinationPrefix("/user");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/stomp").setAllowedOrigins("http://localhost:3000").setHandshakeHandler(new ContreeGameHandshakeHandler()).addInterceptors(new MyHandshakeInterceptor()).withSockJS();
        registry.addEndpoint("/stomp").setAllowedOrigins("http://localhost:3000").setHandshakeHandler(new ContreeGameHandshakeHandler()).addInterceptors(new MyHandshakeInterceptor());
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {

        SimpleModule cardModule = new SimpleModule("CardModule");
        cardModule.addSerializer(ClassicalCard.class, new CardSerializer());

        SimpleModule bidValueModule = new SimpleModule("BidValueModule");
        bidValueModule.addSerializer(ContreeBidValue.class, new ContreeBidValueSerializer());

        SimpleModule cardSuitModule = new SimpleModule("CardSuitModule");
        cardSuitModule.addSerializer(CardSuit.class, new CardSuitSerializer());
        //module.addDeserializer(ClassicalCard.class, new CardDeserializer());

        objectMapper.registerModule(cardModule);
        objectMapper.registerModule(bidValueModule);
        objectMapper.registerModule(cardSuitModule);

        DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
        resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        converter.setContentTypeResolver(resolver);
        messageConverters.add(converter);
        return false;
    }
}

class CardSerializer extends JsonSerializer<ClassicalCard> {

    @Override
    public void serialize(ClassicalCard card, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("rank", card.getRank().toString());
        gen.writeStringField("suit", card.getSuit().name());
        gen.writeStringField("display", String.format( "%s%s", card.getRank().toString(), card.getSuit().toString()  ) );
        gen.writeStringField("name", card.name());
        gen.writeEndObject();
    }

}

class ContreeBidValueSerializer extends JsonSerializer<ContreeBidValue> {

    @Override
    public void serialize(ContreeBidValue bidValue, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        if (bidValue.getExpectedScore() == null) {
            gen.writeNullField("expectedScore");
        }
        else {
            gen.writeNumberField("expectedScore", bidValue.getExpectedScore());
        }

        gen.writeBooleanField("suitRequired", bidValue.isCardSuitRequired());
        gen.writeStringField("display", bidValue.getLabel());
        gen.writeStringField("name", bidValue.name());
        gen.writeEndObject();
    }
}

class CardSuitSerializer extends JsonSerializer<CardSuit> {
    @Override
    public void serialize(CardSuit suit, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("name", suit.name());
        gen.writeStringField("display", suit.toString());
        gen.writeEndObject();
    }
}

/*
class CardDeserializer extends JsonDeserializer<ClassicalCard> {
    @Override
    public ClassicalCard deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode t = p.getCodec().readTree(p);
        String enumName = t.get("name").asText();
        return ClassicalCard.valueOf(enumName);
    }
}
*/
