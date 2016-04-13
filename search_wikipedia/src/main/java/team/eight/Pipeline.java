package team.eight;

import com.medallia.word2vec.Word2VecModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class Pipeline {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";


    public static final int NAME_LIMIT = 30;
    public static final int KEYWORD_LIMIT = 12;

    public static void main(String[] args) {
//        File binfile = new File("vectors-phrase.bin");
        File binfile = new File("../vectors-phrase-wikipedia.bin");
        if (!binfile.exists()){
            System.out.println("Error: Missing vector file. Exiting.");
            return;
        }
        Word2VecModel model = null;
        try {
            model = Word2VecModel.fromBinFile(binfile);
//            model = Medallia.fromBinFile(binfile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Pair<String, Double>> bestWords;
        System.out.println("1");
        try {
            System.out.println("1.1");
            assert model != null;
            System.out.println("1.2");
            bestWords = Word2VecKeywords.interact(model.forSearch());
            Word2VecKeywords.printBest(bestWords, NAME_LIMIT);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        System.out.println("2");
        model = null; /* Encourages garbage collecter to get rid of this */

        assert bestWords != null;
        List<String> searchList = new ArrayList<>(bestWords.size());
        String dictPathString = args[0];
        String vectorsPath = args[1];
        for(Pair<String, Double> term : bestWords) {
            searchList.add(term.getKey());
        }
        System.out.println("3");



        Pair<List<Pair<String, List<Double>>>, List<String>> searchOutput = Search.searchVectors(dictPathString, vectorsPath, searchList, NAME_LIMIT, KEYWORD_LIMIT);
        List<Pair<String, List<Double>>> docList = searchOutput.getLeft();
        List<String> usedKeywords = searchOutput.getRight();
        System.out.println("4");

        /* This is before weighting. The output can be *very* large now, so we should weight first. */
        /*for(Pair<String, List<Double>> pair : docList) {

            String docName = pair.getKey();
            if (docName.length() > NAME_LIMIT){
                docName = StringUtils.left(docName, NAME_LIMIT-3) + "...";
            }
            System.out.print(padRight(docName, NAME_LIMIT));
            for(Double weight : pair.getValue()) {
                if(weight != 0) {
                    System.out.print(ANSI_GREEN + padRight(String.format("%.2f", weight), 12) + ANSI_RESET);
                } else {
                    System.out.print(padRight("0", KEYWORD_LIMIT));
                }
            }
            System.out.println();
        }*/

        /* Calculate weights vector */
        List<Double> weightsVector = new ArrayList<>(usedKeywords.size());
        for (String keyword : usedKeywords) {
            for (Pair<String, Double> term : bestWords) {
                if(keyword.equals(term.getKey())) {
                    weightsVector.add(term.getValue());
                    break;
                }
            }
        }

        TreeSet<Pair<String, Double>> weightedDocSet = new TreeSet<>(new DocumentComparison());

        for(Pair<String, List<Double>> pair : docList) {
            double weight = 0;
            for(int i = 0; i < pair.getValue().size(); ++i) {
                /* Easy weight function for now */
                weight += pair.getValue().get(i) * weightsVector.get(i);
            }
            weightedDocSet.add(new ImmutablePair<>(pair.getKey(), weight));
        }

        int numToPrint = 50; /* print an arbitrary amount */
        for (Pair<String, Double> doc : weightedDocSet) {
            if(numToPrint-- == 0) {
                break;
            }
            String docName = doc.getKey();
            if (docName.length() > NAME_LIMIT){
                docName = StringUtils.left(docName, NAME_LIMIT-3) + "...";
            }
            System.out.print(padRight(docName, NAME_LIMIT));
            System.out.println(ANSI_GREEN + padRight(String.format("%.2f", doc.getValue()), 12) + ANSI_RESET);
        }

    }

    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }
}

class DocumentComparison implements Comparator<Pair<String, Double>> {

    @Override
    public int compare(Pair<String, Double> p1, Pair<String, Double> p2) {
        if (p1.getValue() < p2.getValue()) return 1;
        else return -1;
    }
}