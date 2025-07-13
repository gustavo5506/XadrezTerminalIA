//REMOVE SEARCH OTHER SEQUENCES IF MATE FORCED MATE IS FOUND
package ai;

import Jogo.Board;
import Jogo.Move;
import Jogo.Piece;

import java.util.List;

public class AlphaBetaB {
    private static final int PAWN   = 100;
    private static final int KNIGHT = 320;
    private static final int BISHOP = 330;
    private static final int ROOK   = 500;
    private static final int QUEEN  = 900;
    private static final int KING   = 20000;

    private static final int MATE_SCORE = 100_000_000;

    private final MoveGenerator gen;

    public AlphaBetaB() {
        this.gen = new MoveGenerator();
    }



    /**
     * Find the best move within a time limit (ms) using iterative deepening with alpha-beta pruning.
     * If time expires, returns the best move found so far.
     * @param board       Current position
     * @param engineWhite True if engine plays White
     * @param timeLimitMs Time limit per move in milliseconds
     * @return Best move found within time
     */
    public Move findBestMoveAlphaBetaTimed(Board board, boolean engineWhite, long timeLimitMs) {
        List<Move> moves = gen.generateLegalMoves(board, engineWhite);
        if (moves.isEmpty()) return null;

        long start = System.nanoTime();
        long limit = timeLimitMs * 1_000_000L;
        Move bestMove = moves.get(0);

        try {
            // iterative deepening
            for (int depth = 1; ; depth++) {
                if (System.nanoTime() - start > limit) {
                    System.out.println("Tempo esgotou antes de depth=" + depth + " → parada.");
                    break;
                }
                int alpha = engineWhite ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                Move currentBest = null;
                boolean mateFoundAtThisDepth = false;

                for (Move m : moves) {

                    Board next = new Board(board);
                    next.makeMove(m);
                    int val = alphabetaTimed(next,
                            depth - 1,
                            !engineWhite,
                            Integer.MIN_VALUE,
                            Integer.MAX_VALUE);

                   // System.out.println(
                     //       String.format("Depth %d | Move %s → Score %d", depth, m, val)
                    //);

                    if (engineWhite) {
                        if (val > alpha) {
                            alpha = val;
                            currentBest = m;
                        }
                        if (val >= MATE_SCORE) {
                            // marquei que achamos pelo menos um mate
                            mateFoundAtThisDepth = true;
                        }
                    } else {
                        if (val < alpha) {
                            alpha = val;
                            System.out.print(m);
                            System.out.println(" " + val);
                            currentBest = m;
                        }
                        if (val <= -MATE_SCORE) {
                            mateFoundAtThisDepth = true;
                        }
                    }
                }

                if (currentBest != null) {
                    bestMove = currentBest;
                }

                // se houve mate, pare o iterative deepening
                if (mateFoundAtThisDepth) {
                    break;
                }
            }
        } catch (SearchTimeoutException e) {
        }
        return bestMove;
    }


    private int alphabetaTimed(Board board, int depth, boolean maxPlayer, int alpha, int beta) {
        if (depth == 0 || board.isGameOver()) {
            if (board.isGameOver()) return scoreTerminal(board, depth);
            return evaluate(board);
        }
        List<Move> moves = gen.generateLegalMoves(board, maxPlayer);
        if (moves.isEmpty()) return scoreTerminal(board, depth);

        if (maxPlayer) {
            int value = Integer.MIN_VALUE;
            for (Move m : moves) {
                Board next = new Board(board);
                next.makeMove(m);
                value = Math.max(value, alphabetaTimed(next, depth - 1, false,
                        alpha, beta));
                alpha = Math.max(alpha, value);
                if (alpha >= beta) break; // cutoff
                // **mate cutoff** para maxPlayer
                if (value >= MATE_SCORE) {
                    break;
                }
            }
            return value;
        } else {
            int value = Integer.MAX_VALUE;
            for (Move m : moves) {
                Board next = new Board(board);
                next.makeMove(m);
                value = Math.min(value, alphabetaTimed(next, depth - 1, true,
                        alpha, beta));
                beta = Math.min(beta, value);
                if (beta <= alpha) break;
                // **mate cutoff** para minPlayer
                if (value <= -MATE_SCORE) {
                    break;
                }
            }
            return value;
        }
    }

    private static class SearchTimeoutException extends RuntimeException {}



    // Custom exception for timing out search


    /**
     * Find the best move using minimax search with alpha-beta pruning.
     * @param board       Current board position
     * @param engineWhite True if the engine is playing White
     * @param depth       Total plies to search
     * @return            Best {@code Move} found, or null if none
     */
   /* public Move findBestMoveAlphaBeta(Board board, boolean engineWhite, int depth) {
        List<Move> moves = gen.generateLegalMoves(board, engineWhite);
        if (moves.isEmpty()) return null;

        Move bestMove = null;
        int alpha = Integer.MIN_VALUE;
        int beta  = Integer.MAX_VALUE;

        for (Move m : moves) {
            Board next = new Board(board);
            next.makeMove(m);
            int value = alphabeta(next, depth - 1, !engineWhite, alpha, beta);

            if (engineWhite) {
                if (value > alpha) {
                    alpha = value;
                    bestMove = m;
                }
            } else {
                if (value < beta) {
                    beta = value;
                    bestMove = m;
                }
            }
        }
        return bestMove;
    }*/

    /**
     * Recursive minimax with alpha-beta pruning.
     * @param board            Current position
     * @param depth            Remaining plies
     * @param maximizingPlayer True if this node is for the maximizing side (White)
     * @param alpha            Alpha bound (best already explored for maximizer)
     * @param beta             Beta bound (best already explored for minimizer)
     * @return Heuristic score of the position
     */
   /* private int alphabeta(Board board, int depth, boolean maximizingPlayer, int alpha, int beta) {
        if (depth == 0 || board.isGameOver()) {
            if (board.isGameOver()) {
                return scoreTerminal(board, depth);
            }
            return evaluate(board);
        }

        List<Move> moves = gen.generateLegalMoves(board, maximizingPlayer);
        if (moves.isEmpty()) {
            return scoreTerminal(board, depth);
        }

        if (maximizingPlayer) {
            int value = Integer.MIN_VALUE;
            for (Move m : moves) {
                Board next = new Board(board);
                next.makeMove(m);
                value = Math.max(value, alphabeta(next, depth - 1, false, alpha, beta));
                alpha = Math.max(alpha, value);
                if (alpha >= beta) {
                    break; // beta cutoff
                }
            }
            return value;
        } else {
            int value = Integer.MAX_VALUE;
            for (Move m : moves) {
                Board next = new Board(board);
                next.makeMove(m);
                value = Math.min(value, alphabeta(next, depth - 1, true, alpha, beta));
                beta = Math.min(beta, value);
                if (beta <= alpha) {
                    break; // alpha cutoff
                }
            }
            return value;
        }
    }*/





    /**
     * Converte uma posição terminal em valor heurístico:
     *   DRAW         →  0
     *   WHITE_WINS   → +INF (péssimo para pretas)
     *   BLACK_WINS   → −INF (ótimo para pretas)
     */
    private int scoreTerminal(Board b, int depth) {
        switch (b.getGameResult()) {
            case DRAW:
                return 0;
            case WHITE_WINS:
                return 100000000 + depth;
            case BLACK_WINS:
                return -100000000 -  depth;
            default:
                // não deveria acontecer
                return 0;
        }
    }



    /** Avaliação puramente material: + para brancas, – para pretas */
    private int evaluate(Board board) {
        int score = 0;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board.getPiece(r, c);
                if (p == null) continue;
                int v;
                switch (p) {
                    case WHITE_PAWN:   v = PAWN;   break;
                    case WHITE_KNIGHT: v = KNIGHT; break;
                    case WHITE_BISHOP: v = BISHOP; break;
                    case WHITE_ROOK:   v = ROOK;   break;
                    case WHITE_QUEEN:  v = QUEEN;  break;
                    case WHITE_KING:   v = KING;   break;
                    case BLACK_PAWN:   v = -PAWN;   break;
                    case BLACK_KNIGHT: v = -KNIGHT; break;
                    case BLACK_BISHOP: v = -BISHOP; break;
                    case BLACK_ROOK:   v = -ROOK;   break;
                    case BLACK_QUEEN:  v = -QUEEN;  break;
                    case BLACK_KING:   v = -KING;   break;
                    default:           v = 0;      break;
                }
                score += v;
            }
        }
        return score;
    }
}
