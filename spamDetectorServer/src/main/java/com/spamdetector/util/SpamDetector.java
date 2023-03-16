package com.spamdetector.util;

import com.spamdetector.domain.TestFile;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 goal: produce an algorithm which detects spam emails
 method: find odds that a given word is in a spam email (from sample set)
            vs. find odds that a given word is in ham email (from sample set)

    we want to map words as such
    spam:
        word -> no. of times in spam
    ham:
        word -> no. of times in ham

    then create a map of words such that
    P(W -> S) / ( P(W -> S) + P(W -> H)) is conclusive; i.e suggests an email is spam/ham


 */
public class SpamDetector {

    public Map<String,Integer> createHashMap(File f){
        /*
        NOTES:
        - may need to have this take a File -> Child object?        (O) X?O
        - may need to handle IOExceptions?                          (X) X?O
        - may need to handle FNFExceptions?                         (X) X?O
         */


        // Hash Maps are efficient since there will be no duplicate words (and thus no collisions)
        // this function exists only to parse raw data (.txt) -> HashMap datatypes.

        Map<String,Integer> data = new HashMap<String,Integer>();
        BufferedReader br;
        System.out.println("Creating Hash Map..." + f.getName());
        //instantiate our file, reader, and line objs.
        // note: cmds is giving me trouble, and isn't even an email, so i'm excluding it.
            try {
                br = new BufferedReader(new FileReader(f));
                // read a line..:
                String line = br.readLine();
                while (line != null) {
                    // split the line along spaces..:
                    String[] bits = line.split(" ");
                    // whack each bit of the line into our HashMap..:
                    for (int i = 0; i < bits.length; i++) {
                        if (data.get(bits[i]) == null) {
                            // if it aint in there, whack it in..:
                            data.put(bits[i], 1);
                        } //else {
                            // otherwise, have a look and increment..:
                            // we actually don't want to increment. just leave it at 1.
                            // so i'll just comment this code.
                            /*
                            int incr = Integer.valueOf(String.valueOf(data.get(bits[i])));
                            incr++;
                            data.put(bits[i], incr);
                             */
                        //}
                    }
                    line = br.readLine();
                }
            } catch (IOException e) {
                System.out.println("IOError");
        }
        return data;
    }

    public Map<String, Integer> godAdder(Map<String,Integer> GOD, Map<String,Integer> addToGOD){
        System.out.println("CONCATONATING...");
        // godAdder takes one small Map and adds it to a GOD map, adding all the values therein.
        Map<String, Integer> tempGOD = Stream.concat(GOD.entrySet().stream(), addToGOD.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.summingInt(Map.Entry::getValue)));
        return tempGOD;
    }

    public Map<String, Integer> makeGOD(String dirpath){
        System.out.println("Making GOD...");
        // uses 2 above helper functions to create a GOD map.
        // instantiate map..:
        Map<String,Integer> givenGOD = new HashMap<String,Integer>();
        // crack open our directory..:
        File dir = new File(dirpath);
        // make a list of subdirs..:
        File[] dirlist = dir.listFiles();
        if(dirlist!= null) {
            for(File subDir : dirlist){
                // for every subdir, make a map and add it to our GOD map.
                Map temp = createHashMap(subDir);
                givenGOD = godAdder(givenGOD,temp);
            }
        }
        return givenGOD;
    }

    public Integer sumMap(Map<String,Integer> someMap){
        Integer sum = 0;
        for(Integer f: someMap.values()){
            sum += f;
        }
        return sum;
    }

    public Map<String,Float> transformer(Map<String, Integer> map, int ttl){
        // transforms a map of # of occurances to a map of odds a word occurs in ham/spam
        // i.e. producing Pr(W|S) or Pr(W|H).
        // robots in disguise...

        Map<String,Float> newmap = new HashMap<String,Float>();

        // just iterate across the entire map..:
        for(Map.Entry<String,Integer> entry: map.entrySet()){
            String key = entry.getKey();
            int val = entry.getValue();

            // transform according to formula..:
            float newval;
            newval = val / ttl;
            System.out.println(key+": :"+val+ ": : " + ttl +": :"+newval);
            // whack it in..:
            newmap.put(key,newval);
        }
        return newmap;
    }

    public Map<String,Integer> transAdd(Map<String,Integer> ws, Map<String,Integer> wh){
        // similar to above, but will do some calculations and produce the Pr(S|W) value.
        // it's a bit more tricky to index multiple maps, though - we might not find the new key
        // in the next map.
        for(Map.Entry<String,Integer> entry: ws.entrySet()){
            // grab data..:
            String key = entry.getKey();
            int val = entry.getValue();
            // i'm worried this won't always work - hopefully we won't need try/catch..:
            // note: this indeed does not work. div/0. so we'll have to add that...
            // note 2: wh.get(key) occasionally returns null, which is troublesome...
            // another conditional required! further messing up how quick this runs! yay!
            int val2;
            try{
                val2 = wh.get(key);
            }catch(Exception e){
                // we don't actually care about this exception like, at all.
                // cmds just does wierd things for no discernable reason.
                // cmds isn't even an email, so i'm very uninterested in debugging why it doesn't work.
                val2 = 0;
                System.out.println("Logging CMDS file error");
            }
            // now having an issue where this always runs the else. what fun.
            int newval;
            try{
                newval = ((val)/(val-val2));
                ws.replace(key,newval);
            }catch(ArithmeticException e){
                newval = 0;
                ws.replace(key,newval);
                System.out.println("Logging DIV0 error.");
            }
        }
        return ws;
    }

    public List<TestFile> trainAndTest(File mainDirectory) {
//        TODO: main method of loading the directories and files, training and testing the model

        // the following will create maps (Word -> Count) for each directory..:
        Map trainHamFreq = makeGOD("src/main/resources/data/train/ham");
        Map trainSpamFreq = makeGOD("src/main/resources/data/train/spam");

        // it is precisely at 11:12am, 2023-03-13 that I realize that I DID NOT need to count how many times each
        // word occurs. HOWEVER. I have already coded it, so I will continue on that route.
        // Being that the assignment asks for (# of files containing word / # of files), I can instead use
        // (# of occurances / # of words).
        // this will necessitate counting how many words are in each directory though...

        // it is currently 7:50pm, 2023-03-13 that I realize that in using individual word counts, I am getting results
        // which are too close to zero- and being rounded accordingly. Luckily, this fix isn't too lethal at this
        // point in time.

        return new ArrayList<TestFile>();
    }


}

