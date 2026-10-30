package com.toll;

import com.toll.cli.MainCLI;

/**
 * Main application entry point for the Toll Management System (FASTag).
 */
public class Main {
    public static void main(String[] args) {
        MainCLI cli = new MainCLI();
        cli.start();
    }
}

