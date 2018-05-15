package be.carmen.toon.tests;

import org.junit.runner.*;


public class StartUp {
    public static void main(String[] args) {
        System.out.println("RUN TESTS");

        Result r = JUnitCore.runClasses(inschrijvingTest.class);

        if (r.wasSuccessful()) {
            System.out.println("-- TEST SUCCESFULL -- ");
        } else {
            System.out.println("-- TEST FAILED !! -- ");
        }
    }


}
