package io.github.gulliby.ampwhy.base;

public final class Failure extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public Failure(String message) {
        super(message);
    }
}
