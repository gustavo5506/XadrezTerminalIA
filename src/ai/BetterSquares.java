package ai;

import Jogo.Board;
import Jogo.Move;
import Jogo.Piece;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BetterSquares {
    private static final int PAWN   = 100;
    private static final int KNIGHT = 320;
    private static final int BISHOP = 330;
    private static final int ROOK   = 500;
    private static final int QUEEN  = 900;
    private static final int KING   = 20000;
    private static final int MATE_SCORE = 100_000_000;

    private static final int[][] PST_PAWN = {
            {  0,   0,   0,   0,   0,   0,   0,   0},
            { 50,  50,  50,  50,  50,  50,  50,  50},
            { 10,  10,  20,  30,  30,  20,  10,  10},
            {  15,   15,  15,  25,  25,  10,   5,   5},
            {  0,   0,   0,  20,  20,   0,   0,   0},
            {  5,   5,   10,   0,   0, -10,  -5,   5},
            {  5,  10,  10, -20, -20,  10,  10,   5},
            {  0,   0,   0,   0,   0,   0,   0,   0}
    };

    private static final int[][] PST_KNIGHT = {
            {-50, -40, -30, -30, -30, -30, -40, -50},
            {-40, -20,   0,   0,   0,   0, -20, -40},
            {-30,   0,  10,  15,  15,  10,   0, -30},
            {-30,   5,  15,  20,  20,  15,   5, -30},
            {-30,   0,  15,  20,  20,  15,   0, -30},
            {-30,   5,  10,  15,  15,  10,   5, -30},
            {-40, -20,   0,   5,   5,   0, -20, -40},
            {-50, -40, -30, -30, -30, -30, -40, -50}
    };

    private static final int[][] PST_BISHOP = {
            { -20, -10, -10, -10, -10, -10, -10, -20 },
            { -10,   0,   0,    0,    0,    0,    0, -10 },
            { -10,   0,   5,   10,   10,    5,    0, -10 },
            { -10,   5,   5,   10,   10,    5,    5, -10 },
            { -10,   0,  10,   10,   10,   10,    0, -10 },
            { -10,  10,  10,   10,   10,   10,   10, -10 },
            { -10,   5,   0,    0,    0,    0,    5, -10 },
            { -20, -10, -10,  -10,  -10,  -10,  -10, -20 }
    };

    // Torre
    private static final int[][] PST_ROOK = {
            {   0,   0,   0,   0,   0,   0,   0,   0 },
            {   5,  10,  10,  10,  10,  10,  10,   5 },
            {  -5,   0,   0,   0,   0,   0,   0,  -5 },
            {  -5,   0,   0,   0,   0,   0,   0,  -5 },
            {  -5,   0,   0,   0,   0,   0,   0,  -5 },
            {  -5,   0,   0,   0,   0,   0,   0,  -5 },
            {  -5,   0,   0,   0,   0,   0,   0,  -5 },
            {   0,   0,   0,   5,   5,   0,   0,   0 }
    };

    // Dama
    private static final int[][] PST_QUEEN = {
            { -20, -10, -10,  -5,  -5, -10, -10, -20 },
            { -10,   0,   0,   0,   0,   0,   0, -10 },
            { -10,   0,   5,   5,   5,   5,   0, -10 },
            {  -5,   0,   5,   5,   5,   5,   0,  -5 },
            {   0,   0,   5,   5,   5,   5,   0,  -5 },
            { -10,   5,   5,   5,   5,   5,   0, -10 },
            { -10,   0,   5,   0,   0,   0,   0, -10 },
            { -20, -10, -10,  -5,  -5, -10, -10, -20 }
    };

    private static final int[][] PST_KING_MID = {
            { -30, -40, -40, -50, -50, -40, -40, -30 },
            { -30, -40, -40, -50, -50, -40, -40, -30 },
            { -30, -40, -40, -50, -50, -40, -40, -30 },
            { -30, -40, -40, -50, -50, -40, -40, -30 },
            { -20, -30, -30, -40, -40, -30, -30, -20 },
            { -10, -20, -20, -20, -20, -20, -20, -10 },
            {  20,  20,   0,   0,   0,   0,   20,  20 },
            {  20,  30,  10,   0,   0,  10,   30,  20 }
    };

    // Rei (final de jogo)
    private static final int[][] PST_KING_END = {
            { -50, -40, -30, -20, -20, -30, -40, -50 },
            { -40, -20, -10,   0,   0, -10, -20, -40 },
            { -30, -10,   5,  10,  10,   5, -10, -30 },
            { -20,   0,  10,  20,  20,  10,   0, -20 },
            { -20,   0,  10,  20,  20,  10,   0, -20 },
            { -30, -10,   5,  10,  10,   5, -10, -30 },
            { -40, -20, -10,   0,   0, -10, -20, -40 },
            { -50, -40, -30, -20, -20, -30, -40, -50 }
    };


    private final MoveGenerator gen;

    public BetterSquares() {
        this.gen = new MoveGenerator();
    }

    /**
     * Encapsula score e lista de movimentos (PV).
     */
    private static class SearchResult {
        int score;
        List<Move> pv;

        SearchResult(int score, List<Move> pv) {
            this.score = score;
            this.pv = pv;
        }
    }

    /**
     * Encontra o melhor movimento com logs e PV.
     */
    public Move findBestMoveAlphaBetaTimed(Board board, boolean engineWhite, long timeLimitMs) {
        List<Move> moves = gen.generateLegalMoves(board, engineWhite);
        if (moves.isEmpty()) return null;

        long start = System.nanoTime();
        long limit = timeLimitMs * 1_000_000L;
        Move bestMove = moves.get(0);
        List<Move> bestPV = new ArrayList<>();

        try {
            for (int depth = 1; ; depth+=2) {
                if ((System.nanoTime() - start > limit) && depth >3) {
                    System.out.println(depth);
                    System.out.println("Tempo esgotou antes de depth=" + depth + " → parada.");
                    break;
                }

                int alpha = engineWhite ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                Move currentBest = null;
                List<Move> currentPV = new ArrayList<>();

                for (Move m : moves) {
                    Board next = new Board(board);
                    next.makeMove(m);

                    SearchResult res = next.isInCheck(!engineWhite)
                            ? forcedCheckSequenceWithPV(next, engineWhite, 3)
                            : alphabetaTimedWithPV(next, depth - 1, !engineWhite,
                            Integer.MIN_VALUE, Integer.MAX_VALUE);

                    System.out.printf("Depth %d | Move %s → Eval %d | PV: %s%n",
                            depth, m, res.score, res.pv);

                    if (engineWhite ? (res.score > alpha) : (res.score < alpha)) {
                        alpha = res.score;
                        currentBest = m;
                        currentPV.clear();
                        currentPV.add(m);
                        currentPV.addAll(res.pv);
                    }
                    if ((engineWhite && res.score >= MATE_SCORE) ||
                            (!engineWhite && res.score <= -MATE_SCORE)) {
                        break;
                    }
                }

                if (currentBest != null) {
                    bestMove = currentBest;
                    bestPV   = new ArrayList<>(currentPV);
                }

                System.out.printf("→ Depth %d: Best %s | Score %d | PV: %s%n",
                        depth, bestMove, alpha, bestPV);

                if ((engineWhite && alpha >= MATE_SCORE) ||
                        (!engineWhite && alpha <= -MATE_SCORE)) {
                    break;
                }
            }
        } catch (SearchTimeoutException e) {
            // timeout
        }
        return bestMove;
    }

    /**
     * Alpha-beta com PV.
     */
    private SearchResult alphabetaTimedWithPV(Board board, int depth,
                                              boolean maxPlayer, int alpha, int beta) {
        if (depth == 0 || board.isGameOver()) {
            int v = board.isGameOver()
                    ? scoreTerminal(board, depth)
                    : quiescence(board, alpha, beta, maxPlayer);
            return new SearchResult(v, new ArrayList<>());
        }

        List<Move> moves = gen.generateLegalMoves(board, maxPlayer);
        if (moves.isEmpty()) {
            return new SearchResult(scoreTerminal(board, depth), new ArrayList<>());
        }

        orderMoves(board, moves, maxPlayer);
        SearchResult best = new SearchResult(
                maxPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE,
                new ArrayList<>());

        for (Move m : moves) {
            Board next = new Board(board);
            next.makeMove(m);
            SearchResult child = alphabetaTimedWithPV(next, depth - 1, !maxPlayer, alpha, beta);

            if (maxPlayer ? child.score > best.score : child.score < best.score) {
                best.score = child.score;
                best.pv = new ArrayList<>();
                best.pv.add(m);
                best.pv.addAll(child.pv);
            }

            if (maxPlayer) {
                alpha = Math.max(alpha, best.score);
                if (alpha >= beta || best.score >= MATE_SCORE) break;
            } else {
                beta = Math.min(beta, best.score);
                if (beta <= alpha || best.score <= -MATE_SCORE) break;
            }
        }
        return best;
    }

    /**
     * Forced check sequence com PV.
     */
    /**
     * Forced check sequence com PV, agora respeitando empates em qualquer nó.
     */
    private SearchResult forcedCheckSequenceWithPV(Board board,
                                                   boolean engineWhite,
                                                   int remChecks) {
        // folha: sem mais checks forçados, sem cheque ou posição terminada
        if (remChecks == 0 || !board.isInCheck(!engineWhite) || board.isGameOver()) {
            int v = board.isGameOver()
                    ? scoreTerminal(board, 0)
                    : evaluate(board);
            return new SearchResult(v, new ArrayList<>());
        }

        List<Move> oppMoves = gen.generateLegalMoves(board, !engineWhite);
        // se não há resposta (Xeque-mate ou stalemate), devolve leaf
        if (oppMoves.isEmpty()) {
            return new SearchResult(scoreTerminal(board, 0), new ArrayList<>());
        }

        SearchResult worst = new SearchResult(Integer.MAX_VALUE, new ArrayList<>());
        for (Move opp : oppMoves) {
            Board bOpp = new Board(board);
            bOpp.makeMove(opp);

            // **1) se a defesa já gera posição terminada (empate ou mate)**
            if (bOpp.isGameOver()) {
                int vOpp = scoreTerminal(bOpp, 0);
                if (vOpp < worst.score) {
                    worst.score = vOpp;
                    worst.pv.clear();
                    worst.pv.add(opp);
                }
                continue;
            }

            List<Move> replies = gen.generateLegalMoves(bOpp, engineWhite);
            // 2) se mate/sem movimentos (stalemate), devolve leaf também
            if (replies.isEmpty()) {
                int vStale = scoreTerminal(bOpp, 0);
                if (vStale < worst.score) {
                    worst.score = vStale;
                    worst.pv.clear();
                    worst.pv.add(opp);
                }
                continue;
            }

            // 3) para cada resposta, avalia (detectando check subsequente)
            SearchResult bestReply = new SearchResult(Integer.MIN_VALUE, new ArrayList<>());
            for (Move my : replies) {
                Board bMy = new Board(bOpp);
                bMy.makeMove(my);

                // **3a) se após minha resposta a posição terminou**
                if (bMy.isGameOver()) {
                    int vMy = scoreTerminal(bMy, 0);
                    if (vMy > bestReply.score) {
                        bestReply.score = vMy;
                        bestReply.pv.clear();
                        bestReply.pv.add(my);
                    }
                    continue;
                }

                // 3b) senão, segue forçando checks ou avaliando estático
                SearchResult sr = bMy.isInCheck(!engineWhite)
                        ? forcedCheckSequenceWithPV(bMy, engineWhite, remChecks - 1)
                        : new SearchResult(evaluate(bMy), new ArrayList<>());

                if (sr.score > bestReply.score) {
                    bestReply.score = sr.score;
                    bestReply.pv.clear();
                    bestReply.pv.add(my);
                    bestReply.pv.addAll(sr.pv);
                }
            }

            // 4) escolhe pior defesa (para engineWhite) = menor score
            if (bestReply.score < worst.score) {
                worst.score = bestReply.score;
                worst.pv.clear();
                worst.pv.add(opp);
                worst.pv.addAll(bestReply.pv);
            }
        }

        return worst;
    }


    /**
     * Alpha-beta padrão (sem PV).
     */
    private int alphabetaTimed(Board board, int depth,
                               boolean maxPlayer, int alpha, int beta) {
        if (depth == 0 || board.isGameOver()) {
            if (board.isGameOver()) return scoreTerminal(board, depth);
            return quiescence(board, alpha, beta, maxPlayer);
        }
        List<Move> moves = gen.generateLegalMoves(board, maxPlayer);
        if (moves.isEmpty()) return scoreTerminal(board, depth);
        orderMoves(board, moves, maxPlayer);

        if (maxPlayer) {
            int value = Integer.MIN_VALUE;
            for (Move m : moves) {
                Board next = new Board(board);
                next.makeMove(m);
                value = Math.max(value, alphabetaTimed(next, depth - 1, false, alpha, beta));
                alpha = Math.max(alpha, value);
                if (alpha >= beta || value >= MATE_SCORE) break;
            }
            return value;
        } else {
            int value = Integer.MAX_VALUE;
            for (Move m : moves) {
                Board next = new Board(board);
                next.makeMove(m);
                value = Math.min(value, alphabetaTimed(next, depth - 1, true, alpha, beta));
                beta = Math.min(beta, value);
                if (beta <= alpha || value <= -MATE_SCORE) break;
            }
            return value;
        }
    }

    /**
     * Quiescence search, detecta game over primeiro.
     */
    private int quiescence(Board board, int alpha, int beta, boolean maxPlayer) {
        if (board.isGameOver()) {
            return scoreTerminal(board, 0);
        }
        int standPat = evaluate(board);
        if (maxPlayer) {
            if (standPat >= beta) return beta;
            alpha = Math.max(alpha, standPat);
        } else {
            if (standPat <= alpha) return alpha;
            beta = Math.min(beta, standPat);
        }

        List<Move> tac = new ArrayList<>();
        for (Move m : gen.generateLegalMoves(board, maxPlayer)) {
            if (isCapture(board, m)) tac.add(m);
            else {
                Board n = new Board(board);
                n.makeMove(m);
                if (n.isInCheck(!maxPlayer)) tac.add(m);
            }
        }
        orderMoves(board, tac, maxPlayer);
        for (Move m : tac) {
            Board n = new Board(board);
            n.makeMove(m);
            if (maxPlayer) {
                int sc = quiescence(n, alpha, beta, false);
                alpha = Math.max(alpha, sc);
                if (alpha >= beta) return beta;
            } else {
                int sc = quiescence(n, alpha, beta, true);
                beta = Math.min(beta, sc);
                if (beta <= alpha) return alpha;
            }
        }
        return maxPlayer ? alpha : beta;
    }

    /** Ordena por checks e MVV-LVA. */
    private void orderMoves(Board board, List<Move> moves, boolean maxPlayer) {
        Collections.sort(moves, Comparator.comparingInt((Move m) -> {
            Board nxt = new Board(board);
            nxt.makeMove(m);

            // 1) promoções em primeiro
            if (m.getPromotion() != null) {
                return Integer.MAX_VALUE;
            }
            // 2) capturas (MVV-LVA)
            if (isCapture(board, m)) {
                return 1000 + mvvLvaScore(board, m);
            }
            // 3) avanço de peão “quiet”
            Piece p = board.getPiece(m.getFromRow(), m.getFromCol());
            if ((p == Piece.WHITE_PAWN || p == Piece.BLACK_PAWN)
                    && m.getFromCol() == m.getToCol()) {
                return 200;   // pequeno bônus para empurrar peão
            }
            // 4) cheque
            if (nxt.isInCheck(!maxPlayer)) {
                return 50;
            }
            // 5) todo o resto
            return 0;
        }).reversed());
    }


    private static boolean isCapture(Board board, Move m) {
        if (board.getPiece(m.getToRow(), m.getToCol()) != null) return true;
        Piece p = board.getPiece(m.getFromRow(), m.getFromCol());
        if ((p == Piece.WHITE_PAWN || p == Piece.BLACK_PAWN) &&
                m.getFromCol() != m.getToCol() &&
                board.getEnPassantRow() == m.getToRow() &&
                board.getEnPassantCol() == m.getToCol()) {
            return true;
        }
        return m.getPromotion() != null;
    }

    private static int mvvLvaScore(Board board, Move m) {
        Piece vic = board.getPiece(m.getToRow(), m.getToCol());
        Piece mov = board.getPiece(m.getFromRow(), m.getFromCol());
        int vv = (vic != null) ? pieceValue(vic) : 0;
        int mv = (mov != null) ? pieceValue(mov) : 0;
        return vv - mv;
    }

    private static int pieceValue(Piece p) {
        switch (p) {
            case WHITE_PAWN:
            case BLACK_PAWN:
                return PAWN;
            case WHITE_KNIGHT:
            case BLACK_KNIGHT:
                return KNIGHT;
            case WHITE_BISHOP:
            case BLACK_BISHOP:
                return BISHOP;
            case WHITE_ROOK:
            case BLACK_ROOK:
                return ROOK;
            case WHITE_QUEEN:
            case BLACK_QUEEN:
                return QUEEN;
            default:
                return 0;
        }
    }


    /** Converte resultado de jogo em score. */
    private int scoreTerminal(Board b, int depth) {
        switch (b.getGameResult()) {
            case DRAW:
                return 0;
            case WHITE_WINS:
                return MATE_SCORE + depth;
            case BLACK_WINS:
                return -MATE_SCORE - depth;
            default:
                return 0;
        }
    }

    /** Avaliação material simples. */
    /**
     * Avalia a posição somando valor material + bônus posicional (PST),
     * escolhendo tabela de fim de jogo para o rei quando não há damas em tabuleiro.
     */
    private int evaluate(Board board) {
        int score = 0;

        // 1) Detecta fim de jogo: se não houver damas no tabuleiro
        boolean endgame = true;
        for (int r = 0; r < 8 && endgame; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p == Piece.WHITE_QUEEN || p == Piece.BLACK_QUEEN) {
                    endgame = false;
                    break;
                }
            }
        }

        // 2) Soma material + PST de cada peça
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p == null) continue;
                boolean white = p.isWhite();

                // Valor material
                int base = pieceValue(p);

                // Bônus posicional
                int pst = 0;
                switch (p) {
                    case WHITE_PAWN, BLACK_PAWN ->
                            pst = PST_PAWN[ white ? r : 7 - r ][ c ];
                    case WHITE_KNIGHT, BLACK_KNIGHT ->
                            pst = PST_KNIGHT[ white ? r : 7 - r ][ c ];
                    case WHITE_BISHOP, BLACK_BISHOP ->
                            pst = PST_BISHOP[ white ? r : 7 - r ][ c ];
                    case WHITE_ROOK, BLACK_ROOK ->
                            pst = PST_ROOK[ white ? r : 7 - r ][ c ];
                    case WHITE_QUEEN, BLACK_QUEEN ->
                            pst = PST_QUEEN[ white ? r : 7 - r ][ c ];
                    case WHITE_KING, BLACK_KING -> {
                        if (endgame) {
                            pst = PST_KING_END[ white ? r : 7 - r ][ c ];
                        } else {
                            pst = PST_KING_MID[ white ? r : 7 - r ][ c ];
                        }
                    }
                }

                int pieceScore = base + pst;
                score += white ? pieceScore : -pieceScore;
            }
        }

        return score;
    }


    private static class SearchTimeoutException extends RuntimeException {}
}
