// src/ai/MoveGenerator.java
package ai;

import Jogo.Board;
import Jogo.Move;
import Jogo.Piece;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {
    private static final int[][] KNIGHT_DELTAS = {
            {2,1},{2,-1},{-2,1},{-2,-1},
            {1,2},{1,-2},{-1,2},{-1,-2}
    };
    private static final int[][] ROOK_DIRS   = {{1,0},{-1,0},{0,1},{0,-1}};
    private static final int[][] BISHOP_DIRS = {{1,1},{1,-1},{-1,1},{-1,-1}};
    private static final int[][] QUEEN_DIRS  = {
            {1,0},{-1,0},{0,1},{0,-1},
            {1,1},{1,-1},{-1,1},{-1,-1}
    };

    /**
     * Gera todos os movimentos legais para o jogador da vez (sem verificar xeque).
     */
    public List<Move> generateLegalMoves(Board board, boolean whiteTurn) {
        List<Move> pseudo = new ArrayList<>();
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) {
            Piece p = board.getPiece(r, c);
            if (p == null) continue;
            if (whiteTurn && !p.isWhite()) continue;
            if (!whiteTurn && !p.isBlack()) continue;
            switch (p) {
                case WHITE_PAWN, BLACK_PAWN   -> addPawnMoves(board, r, c, pseudo);
                case WHITE_KNIGHT, BLACK_KNIGHT -> addKnightMoves(board, r, c, pseudo);
                case WHITE_BISHOP, BLACK_BISHOP -> addSliding(board, r, c, pseudo, BISHOP_DIRS);
                case WHITE_ROOK,   BLACK_ROOK   -> addSliding(board, r, c, pseudo, ROOK_DIRS);
                case WHITE_QUEEN,  BLACK_QUEEN  -> addSliding(board, r, c, pseudo, QUEEN_DIRS);
                case WHITE_KING,   BLACK_KING   -> addKingMoves(board, r, c, pseudo);
            }
        }

        // 2) filtra os que deixam o rei em cheque
        List<Move> legal = new ArrayList<>();
        for (Move m : pseudo) {
            Board copy = new Board(board);
            copy.makeMove(m);
            // encontra posição do rei desta cor
            Piece kingPiece = whiteTurn ? Piece.WHITE_KING : Piece.BLACK_KING;
            int kr=-1, kc=-1;
            for (int rr=0; rr<8; rr++) {
                for (int cc=0; cc<8; cc++) {
                    if (copy.getPiece(rr, cc) == kingPiece) {
                        kr = rr; kc = cc;
                        break;
                    }
                }
                if (kr != -1) break;
            }
            // só é legal se a casa do rei NÃO estiver atacada pela cor adversária
            if (!copy.isSquareAttacked(kr, kc, !whiteTurn)) {
                legal.add(m);
            }
        }

        return legal;
    }

    private void addKnightMoves(Board b, int r, int c, List<Move> mvs) {
        for (var d : KNIGHT_DELTAS) {
            int r2 = r + d[0], c2 = c + d[1];
            if (r2<0 || r2>7 || c2<0 || c2>7) continue;
            Piece t = b.getPiece(r2, c2);
            if (t == null || t.isWhite() != b.getPiece(r, c).isWhite())
                mvs.add(new Move(r, c, r2, c2));
        }
    }

    private void addSliding(Board b, int r, int c, List<Move> mvs, int[][] dirs) {
        for (var d : dirs) {
            int r2 = r + d[0], c2 = c + d[1];
            while (r2>=0 && r2<8 && c2>=0 && c2<8) {
                Piece t = b.getPiece(r2, c2);
                if (t == null || t.isWhite() != b.getPiece(r, c).isWhite())
                    mvs.add(new Move(r, c, r2, c2));
                if (t != null) break;
                r2 += d[0]; c2 += d[1];
            }
        }
    }

    private void addKingMoves(Board b, int r, int c, List<Move> mvs) {
        for (int dr=-1; dr<=1; dr++) for (int dc=-1; dc<=1; dc++) {
            if (dr==0 && dc==0) continue;
            int r2 = r+dr, c2 = c+dc;
            if (r2<0||r2>7||c2<0||c2>7) continue;
            Piece t = b.getPiece(r2, c2);
            if (t == null || t.isWhite() != b.getPiece(r, c).isWhite())
                mvs.add(new Move(r, c, r2, c2));
        }

        // ——— roque ———
        boolean white = b.getPiece(r,c).isWhite();
        int homeRow = white ? 0 : 7;
        // só se estiver na casa original do rei e sem xeque atual
        if (r==homeRow && c==4 && !b.isSquareAttacked(r,c,!white)) {
            // — roque pequeno — casa f e g livres e não atacadas, e direitos intactos
            if ((white && b.canWhiteCastleKing()) || (!white && b.canBlackCastleKing())) {
                if (b.getPiece(homeRow,5)==null && b.getPiece(homeRow,6)==null
                        && !b.isSquareAttacked(homeRow,5,!white)
                        && !b.isSquareAttacked(homeRow,6,!white)) {
                    mvs.add(new Move(r,c, homeRow,6));
                }
            }
            // — roque grande — casas b, c, d livres e não atacadas, direitos intactos
            if ((white && b.canWhiteCastleQueen()) || (!white && b.canBlackCastleQueen())) {
                if (b.getPiece(homeRow,1)==null &&
                        b.getPiece(homeRow,2)==null &&
                        b.getPiece(homeRow,3)==null &&
                        !b.isSquareAttacked(homeRow,3,!white) &&
                        !b.isSquareAttacked(homeRow,2,!white)) {
                    mvs.add(new Move(r,c, homeRow,2));
                }
            }
        }

    }

    private void addPawnMoves(Board b, int r, int c, List<Move> mvs) {
        Piece pawn = b.getPiece(r, c);
        int dir   = pawn.isWhite() ? 1 : -1;
        int start = pawn.isWhite() ? 1 : 6;
        int r1 = r + dir;

        // avanço 1
        if (r1>=0 && r1<8 && b.getPiece(r1, c)==null) {
            if (r1==7 || r1==0) {
                for (Piece promo : new Piece[]{
                        pawn.isWhite() ? Piece.WHITE_QUEEN   : Piece.BLACK_QUEEN,
                        pawn.isWhite() ? Piece.WHITE_ROOK    : Piece.BLACK_ROOK,
                        pawn.isWhite() ? Piece.WHITE_BISHOP  : Piece.BLACK_BISHOP,
                        pawn.isWhite() ? Piece.WHITE_KNIGHT  : Piece.BLACK_KNIGHT
                }) {
                    mvs.add(new Move(r,c,r1,c, promo));
                }
            } else {
                mvs.add(new Move(r,c,r1,c));
            };
            // avanço duplo
            if (r==start && b.getPiece(r1+dir, c)==null)
                mvs.add(new Move(r, c, r1+dir, c));
        }
        // capturas
        for (int dc : new int[]{-1,1}) {
            int c1 = c + dc;
            if (c1<0||c1>7) continue;
            Piece target = b.getPiece(r1,c1);
            if (target!=null && target.isWhite()!=pawn.isWhite()) {
                if (r1==7||r1==0) {
                    // captura + promoção
                    for (Piece promo : new Piece[]{
                            pawn.isWhite() ? Piece.WHITE_QUEEN  : Piece.BLACK_QUEEN,
                            pawn.isWhite() ? Piece.WHITE_ROOK   : Piece.BLACK_ROOK,
                            pawn.isWhite() ? Piece.WHITE_BISHOP : Piece.BLACK_BISHOP,
                            pawn.isWhite() ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT
                    }) {
                        mvs.add(new Move(r,c,r1,c1,promo));
                    }
                } else {
                    mvs.add(new Move(r,c,r1,c1));
                }
            }
        }
        int epRow = b.getEnPassantRow(), epCol = b.getEnPassantCol();
        if (epRow != -1) {
            // apenas se o en passant estiver uma linha à frente do peão
            if (epRow == r1) {
                // verifica as duas colunas adjacentes
                for (int dc : new int[]{-1, 1}) {
                    if (c + dc == epCol) {
                        // adiciona o movimento de captura en passant
                        mvs.add(new Move(r, c, r1, epCol));
                    }
                }
            }
        }
    }
}
