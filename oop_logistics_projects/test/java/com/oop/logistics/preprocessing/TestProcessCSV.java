package com.oop.logistics.preprocessing;
import com.oop.logistics.preprocessing.ProcessCSV;
public class TestProcessCSV {
    public static void main(String[] args) {
        // Input and Output are the same file as requested (processed in place via temp file)
        String inputFile = "YagiComments_fixed.csv";
        String outputFile = "YagiComments_fixed.csv";
        
        processFile(inputFile, outputFile);
    }
}
