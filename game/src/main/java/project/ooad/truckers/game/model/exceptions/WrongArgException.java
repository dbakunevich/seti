package project.ooad.truckers.game.model.exceptions;

public class WrongArgException extends RuntimeException {
    public WrongArgException(String message) {
        super(message);
    }
}