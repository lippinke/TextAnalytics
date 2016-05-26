package team.eight.VectorAddition;

import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Word2VecAddition;
import com.medallia.word2vec.Word2VecModel;
import org.apache.commons.collections.Buffer;
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
        Word2VecAddition adder = loadVectors("../vectors-phrase-wikipedia.bin");
        simpleAdd(adder);
    }

    public static double[] calculateDocVector(String docPath, Word2VecAddition adder){
        File file = new File(docPath);
        BufferedReader reader = null;

        String doc = "";
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = reader.readLine()) != null){
                doc += line;
            }
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                if (reader != null){
                    reader.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        String[] wordList = doc.split(" ");
        double[] vector;
        double[] docVector = new double[0];
        for(String word : wordList){
            try {
                vector = adder.getVector(word);
                docVector = adder.getSum(docVector, vector);
            } catch (Searcher.UnknownWordException e) {
                //do nothing
            }
        }

        return docVector;

    }

    public static void simpleAdd(Word2VecAddition adder){
        //get vectors:
        String string1 = null;
        String string2 = null;
        String string3 = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Please enter three words: ");
        try {
            string1 = br.readLine();
            string2 = br.readLine();
            string3 = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Getting vectors...");
        double[] vec1 = new double[0];
        double[] vec2 = new double[0];
        double[] vec3 = new double[0];
        try {
            vec1 = adder.getVector(string1);
            vec2 = adder.getVector(string2);
            vec3 = adder.getVector(string3);
        } catch (Searcher.UnknownWordException e) {
            e.printStackTrace();
        }
        System.out.println("Adding...");

        double[] sumVector;
        sumVector = adder.getSum(vec1, vec2);

        System.out.println("Calculating cosine distance.");
        double distance = adder.calculateDistance(sumVector, vec3);
        System.out.println("Cosine distance between (" + string1 + " + " + string2 + ") and " + string3 + ":" + distance);

        List<Searcher.Match> matches = adder.getMatches(sumVector, 10);
        for(Searcher.Match m : matches) {
            System.out.println(m.match() + " " + m.distance());
        }

    }

    public static Word2VecAddition loadVectors(String fileName){
        Word2VecModel model = null;
        Word2VecAddition adder = null;

        File binfile = new File(fileName);
        if (!binfile.exists()) {
            System.out.println("Error: Missing vector file. Exiting.");
            System.exit(1);
        }
        try {
            System.out.println(ANSI_PURPLE + "Initializing word2vec..." + ANSI_RESET);
            model = Word2VecModel.fromBinFile(binfile);
            adder = new Word2VecAddition(model);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return adder;
    }
}




