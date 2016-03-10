package team.eight;

import java.io.IOException;
import java.util.*;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;

import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.VectorWritable;

/**
 * Created by ice-rock on 3/8/16.
 */
public class Search {

    public static void main(String[] args) {
        String dictPathString = args[0];
        String vectorsPath = args[1];
        List<String> searchList = Arrays.asList("hitler", "nazi", "germany");

        List<Pair<String, List<Double>>> docList = searchVectors(dictPathString, vectorsPath, searchList, 0, 12);
        for(Pair<String, List<Double>> pair : docList) {
            System.out.println("Document: " + pair.getKey() + " TFIDF: " + pair.getValue().toString());
        }
    }

    public static List<Pair<String, List<Double>>> searchVectors(String dictPathString, String vectorsPath, List<String> keywords, int pad1, int pad2){
        Configuration conf = new Configuration();
        conf.set("io.serializations",
                "org.apache.hadoop.io.serializer.JavaSerialization,"
                        + "org.apache.hadoop.io.serializer.WritableSerialization");
        try {
            FileSystem.get(conf);
        } catch (IOException e) {
            e.printStackTrace();
        }


        Path dictPath = new Path(dictPathString);

        SequenceFile.Reader dictReader = null;
        try {
            dictReader = new SequenceFile.Reader(conf, SequenceFile.Reader.file(dictPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        IntWritable dicKey = new IntWritable();
        Text text = new Text();
        HashMap<String, Integer> dictionaryMap = new HashMap<>();
        try {
            assert dictReader != null;
            while (dictReader.next(text, dicKey)) {
                dictionaryMap.put(text.toString(), Integer.parseInt(dicKey.toString()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            dictReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<Integer> wordIndices = new ArrayList<>(keywords.size());
        System.out.print(padRight(" ", pad1));
        for(String keyword : keywords){
            Integer wordIndex = dictionaryMap.get(keyword);
            if(wordIndex != null) {
                wordIndices.add(wordIndex);
                System.out.print(padRight(keyword,pad2));
            }
        }

        System.out.println();

        Path path = new Path(vectorsPath);

        SequenceFile.Reader vectorReader = null;
        try {
            vectorReader = new SequenceFile.Reader(conf, SequenceFile.Reader.file(path));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Text key = new Text();
        VectorWritable value = new VectorWritable();

        List<Pair<String, List<Double>>> docList = new ArrayList<>();

        try {
            assert vectorReader != null;
            while (vectorReader.next(key, value)) {
                RandomAccessSparseVector vector = (RandomAccessSparseVector) value.get();

                List<Double> docTFIDF = new ArrayList<>(wordIndices.size());
                Boolean allZeroes = true;
                for (Integer wordIndex : wordIndices){
                    double weight = vector.getQuick(wordIndex);
                    docTFIDF.add(weight);
                    if(weight != 0) {
                        allZeroes = false;
                    }
                }
                if(!allZeroes) {
                    docList.add(new ImmutablePair<>(key.toString(), docTFIDF));
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            vectorReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return docList;
    }
    public static String padRight(String s, int n) {
        return String.format("%1$-" + n + "s", s);
    }

}
