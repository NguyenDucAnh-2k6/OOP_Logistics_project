package com.oop.logistics.preprocessing;

public class FixDateManually {
    public static void main(String[] args) {

        String input = "YagiComments.csv";
        String output = "YagiComments.csv";

        // Ví dụ: dòng 1 → 350 đều thuộc post ngày 07-09-2024
        DateExtract.fillDateRange(
                output,
                output,
                845,
                1029,
                "07/09/2024"
        );
    }
}
