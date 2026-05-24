package org.ptrades.flairhq.service;

public class DiscordApiException extends RuntimeException {

    private final int statusCode;

    public DiscordApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

}
