
package com.mahal;

public class TestLoader {
    public static void main(String[] args) {
        System.out.println("Attempting to load StaffController...");
        try {
            Class.forName("com.mahal.controller.staff.StaffController");
            System.out.println("StaffController loaded successfully!");

            // Try instantiation
            try {
                // We can't easily instantiate without JavaFX toolkit due to UI components in
                // constructor
                // But loading the class verifies it exists and linkable
                System.out.println("Class found and linked.");
            } catch (Throwable t) {
                System.out.println("Instantiation error (expected if JavaFX not init): " + t);
            }

        } catch (ClassNotFoundException e) {
            System.out.println("ClassNotFoundException caught!");
            e.printStackTrace();
        } catch (NoClassDefFoundError e) {
            System.out.println("NoClassDefFoundError caught!");
            e.printStackTrace();
        } catch (Throwable t) {
            System.out.println("Other error caught!");
            t.printStackTrace();
        }
    }
}
