package team.eight;

import java.io.IOException;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;

import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.VectorWritable;

/**
 * Created by ice-rock on 3/8/16.
 */
public class Search {

    public static void main(String[] args) {
        Configuration conf = new Configuration();
        conf.set("io.serializations",
                "org.apache.hadoop.io.serializer.JavaSerialization,"
                        + "org.apache.hadoop.io.serializer.WritableSerialization");
        try {
            FileSystem fs = FileSystem.get(conf);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String dictPathString = args[0];
        Path dictPath = new Path(dictPathString);

        SequenceFile.Reader dictReader = null;
        try {
            dictReader = new SequenceFile.Reader(conf, SequenceFile.Reader.file(dictPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        IntWritable dicKey = new IntWritable();
        Text text = new Text();
        HashMap<String, Integer> dictionaryMap = new HashMap<String, Integer>();
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

        String wordString = args[2];
        Integer wordIndex = dictionaryMap.get(wordString);

        if(wordIndex == null) {
            System.out.println("Word not in dictionary :(");
            return;
        }

        String vectorsPath = args[1];
        Path path = new Path(vectorsPath);

        SequenceFile.Reader vectorReader = null;
        try {
            vectorReader = new SequenceFile.Reader(conf, SequenceFile.Reader.file(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Text key = new Text();
        VectorWritable value = new VectorWritable();
        try {
            assert vectorReader != null;
            while (vectorReader.next(key, value)) {
                RandomAccessSparseVector vector = (RandomAccessSparseVector) value.get();
                //NamedVector namedVector = (NamedVector)value.get();
                //RandomAccessSparseVector vector = (RandomAccessSparseVector)(namedVector.getDelegate());

                double weight = vector.getQuick(wordIndex);
                if(weight != 0) {
                    System.out.println("Document: " + key + " TF-IDF weight: " + weight);
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
    }

}
