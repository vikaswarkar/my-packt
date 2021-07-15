package se.magnus.microservices.composite.product.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.HttpErrorInfo;

import java.io.IOException;

@Component
@Slf4j
public class ExceptionHelper {

    ObjectMapper mapper;

    public ExceptionHelper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Throwable handleException(WebClientResponseException ex, HttpStatus httpStatus){
        switch (httpStatus) {
            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(ex));

            case UNPROCESSABLE_ENTITY:
                return new InvalidInputException(getErrorMessage(ex));

            default:
                log.error("Got an unexpected HTTP error: {}, will rethrow it. ", ex.getStatusCode() );
                log.error("Error body: {}", ex.getResponseBodyAsString());
                return ex;
        }
    }

    public RuntimeException handleException(HttpClientErrorException ex, HttpStatus httpStatus){
        switch (httpStatus) {
            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(ex));

            case UNPROCESSABLE_ENTITY:
                return new InvalidInputException(getErrorMessage(ex));

            default:
                log.error("Got an unexpected HTTP error: {}, will rethrow it. ", ex.getStatusCode() );
                log.error("Error body: {}", ex.getResponseBodyAsString());
                return ex;
        }
    }

    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }

    private String getErrorMessage(HttpClientErrorException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }
}
