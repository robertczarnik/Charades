package robertczarnik.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;


public class Words {
    private List<String> words;
    private Random random;

    public Words(){
        this.words=new ArrayList<>();
        this.random = new Random();
        loadWords();
    }

    private void loadWords() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        String wordsPathString = Objects.requireNonNull(classLoader.getResource("words.txt")).getPath().substring(1);

        try(BufferedReader br = new BufferedReader(new FileReader(wordsPathString))) {

            String word;
            while ((word = br.readLine()) != null) {
                this.words.add(word);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //return a random word from all chard words
    public String getRandomWord(){
        int index = random.nextInt(words.size());
        return this.words.get(index);
    }
}
