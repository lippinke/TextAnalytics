package team.eight; /**
 * Created by ice-rock on 3/1/16.
 */

import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Word2VecModel;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Word2VecKeywords {
    //TODO: later make these non static.
    static final int nMatches = 20;
    static final double threshold = 0.55;


    public static void main(String[] args) {
        File binfile = new File("vectors-phrase.bin");
        Word2VecModel model = null;
        try {
            model = Word2VecModel.fromBinFile(binfile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            List<Pair<String, Double>> bestMatches = interact(model.forSearch());
            printBest(bestMatches, 30);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addBest(List<Pair<String, Double>> bestMatches,
                                List<Searcher.Match> matches,
                                int compound,
                                double threshold)
    {
        double scale = 1 + compound * 0.5;

        for(Searcher.Match m : matches){
            double weightedDistance = scale * m.distance();
            if(weightedDistance > threshold) {
                Pair<String, Double> pair = new ImmutablePair<>(m.match(), weightedDistance);
                bestMatches.add(pair);
            }
        }
    }

    public static void printBest(List<Pair<String, Double>> bestMatches, int padding)
    {
        for(Pair<String, Double> pair : bestMatches){
            System.out.println(padRight(pair.getKey() + ": ", padding) + pair.getValue());
        }
    }

    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

    private static void searchWord(Searcher searcher, String word, int compoundLevel, List<Pair<String, Double>> bestMatches)
    {
        try {
            List<Searcher.Match> matches = searcher.getMatches(word, nMatches);
            addBest(bestMatches, matches, compoundLevel, threshold);
            //System.out.println(Strings.joinObjects("\n", matches));
        }catch(Searcher.UnknownWordException e){
            //Nothing
        }
    }

    public static List<Pair<String, Double>> interact(Searcher searcher) throws IOException, Searcher.UnknownWordException {
        System.out.println("1.5");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("Enter word (EXIT to break): ");
                String searchPhrase = br.readLine();

                if (searchPhrase.equals("EXIT")) {
                    break;
                }

                String[] words = searchPhrase.split("\\s+");
                System.out.println(searchPhrase);

                //Create a list that will usually have sufficient capacity and not need resized.
                List<Pair<String, Double>> bestMatches = new ArrayList<>((2 * words.length - 1) * nMatches);

                String w_prev = null;
                String w_2prev = null;
                for (String w : words) {
                    String wLower = w.toLowerCase();
                    searchWord(searcher, wLower, 0, bestMatches);
                    if (w_prev != null) {
                        String compoundW = w_prev + "_" + wLower;
                        searchWord(searcher, compoundW, 1, bestMatches);
                    }
                    if (w_2prev != null) {
                        String compoundW = w_2prev + "_" + w_prev + "_" + wLower;
                        searchWord(searcher, compoundW, 2, bestMatches);
                    }

                    w_2prev = w_prev;
                    w_prev = wLower;
                }
                Collections.sort(bestMatches);

                return bestMatches;
            }
        }
        return null;
    }
}


