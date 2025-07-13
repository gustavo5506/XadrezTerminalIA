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

public class AlphaBetaC2 {
    private static final int PAWN   = 100;
    private static final int KNIGHT = 320;
    private static final int BISHOP = 330;
    private static final int ROOK   = 500;
    private static final int QUEEN  = 900;
    private static final int KING   = 20000;
    private static final int MATE_SCORE = 100_000_000;

    private final MoveGenerator gen;

    public AlphaBetaC2() {
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
                if (System.nanoTime() - start > limit) {
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
    private SearchResult forcedCheckSequenceWithPV(Board board, boolean engineWhite, int remChecks) {
        if (remChecks == 0 || !board.isInCheck(!engineWhite) || board.isGameOver()) {
            int v = board.isGameOver() ? scoreTerminal(board, 0) : evaluate(board);
            return new SearchResult(v, new ArrayList<>());
        }

        List<Move> oppMoves = gen.generateLegalMoves(board, !engineWhite);
        if (oppMoves.isEmpty()) {
            return new SearchResult(scoreTerminal(board, 0), new ArrayList<>());
        }

        SearchResult worst = new SearchResult(Integer.MAX_VALUE, new ArrayList<>());
        for (Move opp : oppMoves) {
            Board bOpp = new Board(board);
            bOpp.makeMove(opp);
            List<Move> replies = gen.generateLegalMoves(bOpp, engineWhite);

            if (replies.isEmpty()) {
                SearchResult sr = new SearchResult(scoreTerminal(bOpp, 0), new ArrayList<>());
                if (sr.score < worst.score) worst = sr;
                continue;
            }

            SearchResult bestReply = new SearchResult(Integer.MIN_VALUE, new ArrayList<>());
            for (Move my : replies) {
                Board bMy = new Board(bOpp);
                bMy.makeMove(my);
                SearchResult sr = bMy.isInCheck(!engineWhite)
                        ? forcedCheckSequenceWithPV(bMy, engineWhite, remChecks - 1)
                        : new SearchResult(evaluate(bMy), new ArrayList<>());
                if (sr.score > bestReply.score) {
                    bestReply.score = sr.score;
                    bestReply.pv = new ArrayList<>();
                    bestReply.pv.add(my);
                    bestReply.pv.addAll(sr.pv);
                }
            }

            if (bestReply.score < worst.score) {
                worst = bestReply;
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
            Board b = new Board(board);
            b.makeMove(m);
            int score = (b.isInCheck(!maxPlayer) ? 10_000 : 0) +
                    (isCapture(board, m) ? mvvLvaScore(board, m) : 0);
            return -score;
        }));
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
    private int evaluate(Board board) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p == null) continue;
                switch (p) {
                    case WHITE_PAWN   -> score += PAWN;
                    case WHITE_KNIGHT -> score += KNIGHT;
                    case WHITE_BISHOP -> score += BISHOP;
                    case WHITE_ROOK   -> score += ROOK;
                    case WHITE_QUEEN  -> score += QUEEN;
                    case WHITE_KING   -> score += KING;
                    case BLACK_PAWN   -> score -= PAWN;
                    case BLACK_KNIGHT -> score -= KNIGHT;
                    case BLACK_BISHOP -> score -= BISHOP;
                    case BLACK_ROOK   -> score -= ROOK;
                    case BLACK_QUEEN  -> score -= QUEEN;
                    case BLACK_KING   -> score -= KING;
                }
            }
        }
        return score;
    }

    private static class SearchTimeoutException extends RuntimeException {}
}
