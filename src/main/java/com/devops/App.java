package com.devops;

public class App {

    public static void main(String[] args) {

        System.out.println(
            "DevSecOps CI/CD Pipeline Running Successfully!"
        );

        Runtime.getRuntime().addShutdownHook(
            new Thread(() ->
                System.out.println("Application shutting down...")
            )
        );

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
