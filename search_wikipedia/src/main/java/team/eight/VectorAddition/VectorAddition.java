package team.eight.VectorAddition;

import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Word2VecAddition;
import com.medallia.word2vec.Word2VecModel;
import org.apache.commons.collections.Buffer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hadoop.util.hash.Hash;
import team.eight.Search;
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
        String dictPathString = args[0];
        String vectorsPath = args[1];

        Word2VecAddition adder = loadVectors("../vectors-phrase-wikipedia.bin");

        System.out.println("Adding words naively without tfidf.");

        System.out.println("Calculating frog vector...");
        double[] frogDoc = calculateDocVector("./frog_wiki.txt", adder);
        System.out.println("Calculating OSU vector...");
        double[] osuDoc = calculateDocVector("./oregon_state_university_wiki.txt", adder);
        System.out.println("Calculating Corvallis Vector");
        double[] corvallisDoc = calculateDocVector("./corvallis_wikipedia.txt", adder);

        double d1 = adder.calculateDistance(osuDoc, corvallisDoc);
        double d2 = adder.calculateDistance(osuDoc, frogDoc);
        System.out.println("OSU vs Corvallis: " + d1);
        System.out.println("OSU vs Frogs: " + d2);

        System.out.println("\nAdding words with tfidf weightings.");

        String[] docTitleArr = {"Corvallis, Oregon", "Oregon State University", "Frog"};
        ArrayList<String> docTitles = new ArrayList<>(Arrays.asList(docTitleArr));
        HashMap<String, double[]> docVecs = calculateDocVectorsTfidf(
                docTitles, dictPathString, vectorsPath, adder);

        //------

        System.out.println("The frog wiki is most similar to...");
        //printMatches(frogDoc, adder, 10);
        printMatches(docVecs.get("Frog"), adder, 10);

        System.out.println("The corvallis wiki is most similar to...");
        //printMatches(corvallisDoc, adder, 10);
        printMatches(docVecs.get("Corvallis, Oregon"), adder, 10);

        System.out.println("The osu wiki is most similar to...");
        //printMatches(osuDoc, adder, 10);
        printMatches(docVecs.get("Oregon State University"), adder, 10);

        d1 = adder.calculateDistance(docVecs.get("Oregon State University"), docVecs.get("Corvallis, Oregon"));
        d2 = adder.calculateDistance(docVecs.get("Oregon State University"), docVecs.get("Frog"));

        System.out.println("OSU vs Corvallis: " + d1);
        System.out.println("OSU vs Frogs: " + d2);

        //------
        //simpleAdd(adder);
    }



    public static HashMap<String, double[]> calculateDocVectorsTfidf(
            List<String> docTitles, String dictFile, String tfidfFile, Word2VecAddition adder){
        HashMap<String, ArrayList<Pair<String, Double>>> docTfidf = Search.getDocTFIDF(dictFile, tfidfFile, docTitles);
        HashMap<String, double[]> docVectors = new HashMap<>();

        double[] vector;
        double[] docVector = null;
        int tfidfSum = 0;
        for(String docTitle : docTitles){
            //System.out.println(docTitle);
            //System.out.println(docTfidf.get(docTitle).toString());
            ArrayList<Pair<String, Double>> wordList = docTfidf.get(docTitle);
            wordList.sort(new WordComparison());
            int k = wordList.size();
            if(k > 10) {
                wordList.subList(10, k).clear();
            }
            for(Pair<String, Double> word: wordList){
                try {
                    vector = adder.getVector(word.getKey());
                    tfidfSum += word.getValue(); //Wait to increment counter until we see that the word is in the dictionary
                    if (docVector == null){
                        docVector = adder.scaleVector(vector, word.getValue());
                    } else {
                        docVector = adder.getSum(docVector, adder.scaleVector(vector, word.getValue()));
                    }
                } catch (Searcher.UnknownWordException e) {
                    //System.out.println("Unknown word: " + word.getKey());
                }
            }

            for(int i = 0; i < docVector.length; ++i) {
            /* Scale down all vector dimensions */
                docVector[i] /= tfidfSum;
            }

            docVectors.put(docTitle, docVector);
        }

        return docVectors;
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
        double[] docVector = null;
        int count = 0;
        for(String word : wordList){
            try {
                vector = adder.getVector(word);
                count++; //Wait to increment counter until we see that the word is in the dictionary
                if (docVector == null){
                    docVector = vector.clone();
                } else {
                    docVector = adder.getSum(docVector, vector);
                }
            } catch (Searcher.UnknownWordException e) {
                //do nothing
            }
        }

        for(int i = 0; i < docVector.length; ++i) {
            /* Scale down all vector dimensions */
            docVector[i] /= count;
        }

        return docVector;

    }

    public static void printMatches(double[] docVector, Word2VecAddition adder, int nMatches){
        List<Searcher.Match> matches = adder.getMatches(docVector, nMatches);
        for(Searcher.Match m : matches) {
            System.out.println(m.match() + " " + m.distance());
        }
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

class WordComparison implements Comparator<Pair<String, Double>> {

    @Override
    public int compare(Pair<String, Double> p1, Pair<String, Double> p2) {
        if (p1.getValue() < p2.getValue()) return 1;
        else if (p1.getValue() > p2.getValue()) return -1;
        else return 0;
    }
}