// src/ui/ConsoleUI.java
package UI;

import Jogo.Board;
import Jogo.Piece;

public class ConsoleUI {
    private Board board;

    public ConsoleUI(Board board) {
        this.board = board;
    }

    public void render() {
        System.out.println("  a b c d e f g h");
        for (int row = 7; row >= 0; row--) {
            System.out.print((row + 1) + " ");
            for (int col = 0; col < 8; col++) {
                Piece p = board.getPiece(row, col);
                char symbol = p != null ? p.getSymbol() : '.';
                System.out.print(symbol + " ");
            }
            System.out.println((row + 1));
        }
        System.out.println("  a b c d e f g h");
    }
}