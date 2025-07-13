// src/engine/Move.java
package Jogo;

import java.util.Objects;

public class Move {
    private final int fromRow, fromCol, toRow, toCol;
    private final Piece promotion;   // null se não houver promoção

    /** Construtor para movimentos sem promoção */
    public Move(int fromRow, int fromCol, int toRow, int toCol) {
        this(fromRow, fromCol, toRow, toCol, null);
    }

    /** Construtor completo, incluindo promoção */
    public Move(int fromRow, int fromCol, int toRow, int toCol, Piece promotion) {
        this.fromRow   = fromRow;
        this.fromCol   = fromCol;
        this.toRow     = toRow;
        this.toCol     = toCol;
        this.promotion = promotion;
    }

    public int   getFromRow()   { return fromRow; }
    public int   getFromCol()   { return fromCol; }
    public int   getToRow()     { return toRow;   }
    public int   getToCol()     { return toCol;   }
    public Piece getPromotion() { return promotion; }

    /**
     * Parse de strings "e7e8Q", "e2e4", etc.
     * Se vir um 5º caractere, tenta identificar Q,R,B ou N,
     * em maiúsculo para peças brancas e minúsculo para pretas.
     */
    public static Move parse(String s) {
        int fromCol = s.charAt(0) - 'a';
        int fromRow = s.charAt(1) - '1';
        int toCol   = s.charAt(2) - 'a';
        int toRow   = s.charAt(3) - '1';
        Piece promo = null;
        if (s.length() == 5) {
            char p = s.charAt(4);
            switch (p) {
                case 'Q': promo = Piece.WHITE_QUEEN;  break;
                case 'R': promo = Piece.WHITE_ROOK;   break;
                case 'B': promo = Piece.WHITE_BISHOP; break;
                case 'N': promo = Piece.WHITE_KNIGHT; break;
                case 'q': promo = Piece.BLACK_QUEEN;  break;
                case 'r': promo = Piece.BLACK_ROOK;   break;
                case 'b': promo = Piece.BLACK_BISHOP; break;
                case 'n': promo = Piece.BLACK_KNIGHT; break;
            }
        }
        return new Move(fromRow, fromCol, toRow, toCol, promo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move)) return false;
        Move m = (Move) o;
        return fromRow   == m.fromRow
                && fromCol   == m.fromCol
                && toRow     == m.toRow
                && toCol     == m.toCol
                && Objects.equals(promotion, m.promotion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromRow, fromCol, toRow, toCol, promotion);
    }

    @Override
    public String toString() {
        String s = ""
                + (char)('a' + fromCol)
                + (char)('1' + fromRow)
                + (char)('a' + toCol)
                + (char)('1' + toRow);
        if (promotion != null) {
            // adiciona o símbolo da peça de promoção (Q,R,B,N,q,r,b,n)
            s += promotion.getSymbol();
        }
        return s;
    }
}
