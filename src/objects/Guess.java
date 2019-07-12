package objects;

import java.io.Serializable;

public class Guess implements Serializable {
    private String guess;

    public Guess(String guess) {
        this.guess = guess;
    }

    public String getGuess() {
        return guess;
    }
}
