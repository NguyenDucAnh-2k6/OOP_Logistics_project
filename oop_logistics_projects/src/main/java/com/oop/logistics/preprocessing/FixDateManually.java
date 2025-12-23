package com.oop.logistics.preprocessing;

public class FixDateManually {
    public static void main(String[] args) {

        String input = "YagiComments(1).csv";
        String output = "YagiComments(1).csv";

        // Ví dụ: dòng 1 → 350 đều thuộc post ngày 07-09-2024
        DateExtract.fillDateRange(
                output,
                output,
                1,
                13,
                "08/09/2024"
        );
    }
}
