package team.eight;

import com.medallia.word2vec.Word2VecModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        File binfile = new File("vectors-phrase.bin");
        Word2VecModel model = null;
        try {
            model = Word2VecModel.fromBinFile(binfile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Pair<String, Double>> bestWords;

        try {
            assert model != null;
            bestWords = Word2VecKeywords.interact(model.forSearch());
            Word2VecKeywords.printBest(bestWords, NAME_LIMIT);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        assert bestWords != null;
        List<String> searchList = new ArrayList<>(bestWords.size());
        String dictPathString = args[0];
        String vectorsPath = args[1];
        for(Pair<String, Double> term : bestWords) {
            searchList.add(term.getKey());
        }

        List<Pair<String, List<Double>>> docList = Search.searchVectors(dictPathString, vectorsPath, searchList, NAME_LIMIT, KEYWORD_LIMIT);
        for(Pair<String, List<Double>> pair : docList) {

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
        }
    }

    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }
}
