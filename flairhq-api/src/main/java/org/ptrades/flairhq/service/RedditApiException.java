package org.ptrades.flairhq.service;

public class RedditApiException extends RuntimeException {

    private final int statusCode;

    public RedditApiException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

}
