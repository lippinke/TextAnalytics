package team.eight.VectorAddition;

import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Word2VecAddition;
import com.medallia.word2vec.Word2VecModel;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import team.eight.Word2VecKeywords;

import java.io.*;
import java.util.*;

public class VectorAddition {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";


    public static final int NAME_LIMIT = 70;
    public static final int KEYWORD_LIMIT = 12;
    public static final int resultsToPrint = 50;

    public static void main(String[] args) {
        Word2VecModel model = null;
        Word2VecAddition adder = null;

        File binfile = new File("../vectors-phrase-wikipedia.bin");
        if (!binfile.exists()) {
            System.out.println("Error: Missing vector file. Exiting.");
            return;
        }
        try {
            System.out.println(ANSI_PURPLE + "Initializing word2vec..." + ANSI_RESET);
            model = Word2VecModel.fromBinFile(binfile);
            adder = new Word2VecAddition(model);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //get vectors:
        String string1 = null;
        String string2 = null;
        String string3 = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Please enter two words: ");
        try {
            string1 = br.readLine();
            string2 = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        double[] vec1 = new double[0];
        double[] vec2 = new double[0];
        //double[] vec3 = new double[0];
        try {
        vec1 = adder.getVector(string1);
        vec2 = adder.getVector(string2);
        } catch (Searcher.UnknownWordException e) {
        e.printStackTrace();
        }

        double[] sumVector;
        sumVector = adder.getSum(vec1, vec2);
        List<Searcher.Match> matches = adder.getMatches(sumVector, 10);

        for(Searcher.Match m : matches) {
            System.out.println(m.match() + " " + m.distance());
        }



    }
}




