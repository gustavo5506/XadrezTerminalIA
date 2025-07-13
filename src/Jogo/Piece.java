// src/engine/Piece.java
package Jogo;

public enum Piece {
    WHITE_KING('K'), WHITE_QUEEN('Q'), WHITE_ROOK('R'), WHITE_BISHOP('B'),
    WHITE_KNIGHT('N'), WHITE_PAWN('P'),
    BLACK_KING('k'), BLACK_QUEEN('q'), BLACK_ROOK('r'), BLACK_BISHOP('b'),
    BLACK_KNIGHT('n'), BLACK_PAWN('p');

    private final char symbol;

    Piece(char symbol) {
        this.symbol = symbol;
    }

    public char getSymbol() {
        return symbol;
    }

    public boolean isWhite() {
        return this.ordinal() <= Piece.WHITE_PAWN.ordinal();
    }

    public boolean isBlack() {
        return this.ordinal() >= Piece.BLACK_KING.ordinal();
    }
}