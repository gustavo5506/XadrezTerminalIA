// src/ai/SearchEngine.java
package ai;

import Jogo.Board;
import Jogo.Move;
import Jogo.Piece;

import java.util.List;

public class AlphaBeta {
    private static final int PAWN   = 100;
    private static final int KNIGHT = 320;
    private static final int BISHOP = 330;
    private static final int ROOK   = 500;
    private static final int QUEEN  = 900;
    private static final int KING   = 20000;

    private final MoveGenerator gen;

    public AlphaBeta() {
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

        // iterative deepening
        for (int depth = 1; ; depth++) {
            try {
                int alpha = engineWhite ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                Move currentBest = null;
                for (Move m : moves) {
                    Board next = new Board(board);
                    next.makeMove(m);
                    int val = alphabetaTimed(next, depth - 1, !engineWhite,
                            Integer.MIN_VALUE, Integer.MAX_VALUE,
                            start, limit);
                    //System.out.println("Move " + m + " -> score " + val + " (depth=" + depth + ")");

                    if (engineWhite) {
                        if (val > alpha) {
                            alpha = val;
                            currentBest = m;
                        }
                    } else {
                        if (val < alpha) {
                            alpha = val;
                            currentBest = m;
                        }
                    }
                    // check time after each root move
                    if (System.nanoTime() - start > limit) throw new SearchEngine.SearchTimeoutException();
                }
                if (currentBest != null) {
                    bestMove = currentBest;
                }
            } catch (SearchEngine.SearchTimeoutException e) {
                break; // time up, return last best
            }
        }
        return bestMove;
    }

    private int alphabetaTimed(Board board, int depth, boolean maxPlayer, int alpha, int beta, long start, long limit) {
        if (System.nanoTime() - start > limit) throw new SearchEngine.SearchTimeoutException();
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
                        alpha, beta, start, limit));
                alpha = Math.max(alpha, value);
                if (alpha >= beta) break; // cutoff
                if (System.nanoTime() - start > limit) throw new SearchEngine.SearchTimeoutException();
            }
            return value;
        } else {
            int value = Integer.MAX_VALUE;
            for (Move m : moves) {
                Board next = new Board(board);
                next.makeMove(m);
                value = Math.min(value, alphabetaTimed(next, depth - 1, true,
                        alpha, beta, start, limit));
                beta = Math.min(beta, value);
                if (beta <= alpha) break;
                if (System.nanoTime() - start > limit) throw new SearchEngine.SearchTimeoutException();
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
    public Move findBestMoveAlphaBeta(Board board, boolean engineWhite, int depth) {
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
    }

    /**
     * Recursive minimax with alpha-beta pruning.
     * @param board            Current position
     * @param depth            Remaining plies
     * @param maximizingPlayer True if this node is for the maximizing side (White)
     * @param alpha            Alpha bound (best already explored for maximizer)
     * @param beta             Beta bound (best already explored for minimizer)
     * @return Heuristic score of the position
     */
    private int alphabeta(Board board, int depth, boolean maximizingPlayer, int alpha, int beta) {
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
    }

    /**
     * Find the best move using a generic minimax search to the given depth.
     * @param board       Current board position
     * @param engineWhite True if the engine is playing White
     * @param depth       Total plies to search (e.g., 3 for 1.5 moves)
     * @return            Best {@code Move} found, or null if none
     */

    /**
     * Recursive minimax evaluation without alpha-beta pruning.
     * @param board            Current position
     * @param depth            Remaining plies
     * @param maximizingPlayer True if this node is for the maximizing side (White)
     * @return Heuristic score of the position
     */


    /**
     * Para cada lance possível do motor (engineWhite),
     * gera todas as respostas do adversário,
     * avalia cada uma (with evaluate()),
     * determina o “pior caso” (worst‐case) para o motor,
     * e depois escolhe o lance cuja pior‐caso seja o
     * melhor (mínimo para pretas, máximo para brancas).
     *
     * Imprime TODAS as combinações [raiz, resposta] = eval, + o worst-case de cada raiz,
     * e no fim o melhor lance.
     */
    public Move findBestMoveDepth2(Board board, boolean engineWhite) {
        List<Move> rootMoves = gen.generateLegalMoves(board, engineWhite);
        if (rootMoves.isEmpty()) return null;

        boolean maximizeRoot = engineWhite;
        Move bestMove = null;
        int bestWorstValue = maximizeRoot ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Move root : rootMoves) {
            Board b1 = new Board(board);
            b1.makeMove(root);

            // gerar respostas do adversário
            List<Move> replies = gen.generateLegalMoves(b1, !engineWhite);

            // definir initial worst-case:
            //   se engineWhite → adversário (pretas) minimiza → worst-case = +∞
            //   se engineBlack → adversário (brancas) maximiza → worst-case = –∞
            int worstCase = engineWhite
                    ? Integer.MAX_VALUE
                    : Integer.MIN_VALUE;

            for (Move reply : replies) {
                Board b2 = new Board(b1);
                b2.makeMove(reply);
                int val = evaluate(b2);

                // adversário escolhe max (brancas) ou min (pretas)
                if (engineWhite) {
                    // adversário preto minimiza
                    worstCase = Math.min(worstCase, val);
                } else {
                    // adversário branco maximiza
                    worstCase = Math.max(worstCase, val);
                }

            }

            // motor escolhe a raiz que maximize ou minimize esse worst-case
            if (maximizeRoot) {
                if (worstCase > bestWorstValue) {
                    bestWorstValue = worstCase;
                    bestMove = root;
                }
            } else {
                if (worstCase < bestWorstValue) {
                    bestWorstValue = worstCase;
                    bestMove = root;
                }
            }
        }
        return bestMove;
    }

    /**
     * Dentro de SearchEngine.java:
     *
     * Método especializado para as pretas a 2 plies:
     * - Raiz: cada lance das pretas
     * - Filhos: todas as respostas das brancas
     * - Pior-caso (brancas escolhem max) para cada raiz
     * - Pretas escolhem a raiz com o menor pior-caso
     */
    public Move findBestBlackMoveDepth2(Board board) {
        // gera jogadas iniciais das pretas
        List<Move> rootMoves = gen.generateLegalMoves(board, false);
        if (rootMoves.isEmpty()) return null;

        Move bestMove = null;
        int bestWorstValue = Integer.MAX_VALUE;  // pretas minimizam

        System.out.println("Avaliação a 2 plies para Pretas:");
        for (Move root : rootMoves) {
            // aplica o lance das pretas
            Board b1 = new Board(board);
            b1.makeMove(root);

            // gera todas as respostas das brancas
            List<Move> replies = gen.generateLegalMoves(b1, true);

            // pior-caso começa em +∞ (brancas MAX)
            int worstCase = Integer.MIN_VALUE;

            for (Move reply : replies) {
                Board b2 = new Board(b1);
                b2.makeMove(reply);
                int val = evaluate(b2);  // avaliação material

                // brancas escolhem max → pior-caso = min(valor, pior-caso atual)
                worstCase = Math.max(worstCase, val);

                System.out.println("  [" + root + "  /  " + reply + "] = eval " + val);
            }

            System.out.println(" → worst-case para Pretas = " + worstCase);

            // pretas escolhem a raiz com o menor worst-case
            if (worstCase < bestWorstValue) {
                bestWorstValue = worstCase;
                bestMove = root;
            }
        }

        System.out.println("Melhor movimento para Pretas: " + bestMove +
                "  (worst-case = " + bestWorstValue + ")");
        return bestMove;
    }



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
