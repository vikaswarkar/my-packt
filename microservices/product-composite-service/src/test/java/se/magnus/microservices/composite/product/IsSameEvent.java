package se.magnus.microservices.composite.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import se.magnus.api.event.Event;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IsSameEvent extends TypeSafeMatcher<String> {

    private ObjectMapper objectMapper = new ObjectMapper();

    private Event expectedEvent;

    public IsSameEvent( Event expectedEvent) {
        this.expectedEvent = expectedEvent;
    }

    @Override
    protected boolean matchesSafely(String eventAsJson) {
        if (expectedEvent == null ) return false;
        Map mapEvent = convertJsonStringToMap(eventAsJson);
        mapEvent.remove("eventCreatedAt");
        Map mapExpectedEvent = getMapWithoutCreatedAt(expectedEvent);
        return mapEvent.equals(mapExpectedEvent);
    }

    @Override
    public void describeTo(Description description) {
        String expectedJson = convertObjectToJsonString(expectedEvent);
        description.appendText("Expected to look like " + expectedJson);
    }

    public static Matcher<String> sameEventExceptCreatedAt(Event expectedEvent){
        return new IsSameEvent(expectedEvent);
    }

    private Map convertObjectToMap(Object object){
        JsonNode node = objectMapper.convertValue(object, JsonNode.class);
        return objectMapper.convertValue(node,Map.class);
    }

    private Map getMapWithoutCreatedAt(Event event){
        Map mapEvent = convertObjectToMap(event);
        mapEvent.remove("eventCreatedAt");
        return mapEvent;
    }

    private String convertObjectToJsonString(Object object){
        try{
            return objectMapper.writeValueAsString(object);
        }catch(JsonProcessingException jpe){
            throw new RuntimeException(jpe);
        }
    }

    private Map convertJsonStringToMap(String eventAsJson){
        try{
            return objectMapper.readValue(eventAsJson, new TypeReference<HashMap>() { });
        }catch(IOException ioe){
            throw new RuntimeException(ioe);
        }
    }
}
