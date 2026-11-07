package io.github.arkosammy12.ibb.cli;

public class Main {

    static void main() {
        try (Ibb ibb = new Ibb()) {
            ibb.start();
        }
    }

}