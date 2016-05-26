package team.eight.VectorAddition;

import com.medallia.word2vec.Searcher;
import com.medallia.word2vec.Word2VecAddition;
import com.medallia.word2vec.Word2VecModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by ice-rock on 5/25/16.
 */
public class VectorAddition {
    public static void main(String[] args) {
        //Open bin file
        File binfile = new File("../vectors-phrase-wikipedia.bin");
        if (!binfile.exists()) {
            System.out.println("Error: Missing vector file. Exiting.");
            return;
        }

        //Create word2vec model
        Word2VecModel model = null;
        try {
            System.out.println("Initializing word2vec...");
            model = Word2VecModel.fromBinFile(binfile);
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
//            string3 = br.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Word2VecAddition adder = new Word2VecAddition(model);

        double[] vec1 = new double[0];
        double[] vec2 = new double[0];
        double[] vec3 = new double[0];
        try {
            vec1 = adder.getVector(string1);
            vec2 = adder.getVector(string2);
//            vec3 = adder.getVector(string3);
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
