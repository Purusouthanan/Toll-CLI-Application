package com.toll;

import com.toll.cli.Module2CLI;

/**
 * Main application entry point for the Toll Management System (FASTag).
 */
public class Main {
    public static void main(String[] args) {
        Module2CLI cli = new Module2CLI();
        cli.start();
    }
}
