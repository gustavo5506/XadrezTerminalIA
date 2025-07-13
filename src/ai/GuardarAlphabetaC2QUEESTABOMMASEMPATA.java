//package ai;
//
//import Jogo.Board;
//import Jogo.Move;
//import Jogo.Piece;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.List;
//
//public class AlphaBetaC2 {
//    private static final int PAWN   = 100;
//    private static final int KNIGHT = 320;
//    private static final int BISHOP = 330;
//    private static final int ROOK   = 500;
//    private static final int QUEEN  = 900;
//    private static final int KING   = 20000;
//
//    private static final int MATE_SCORE = 100_000_000;
//
//    private final MoveGenerator gen;
//
//    public AlphaBetaC2() {
//        this.gen = new MoveGenerator();
//    }
//
//    public Move findBestMoveAlphaBetaTimed(Board board, boolean engineWhite, long timeLimitMs) {
//        List<Move> moves = gen.generateLegalMoves(board, engineWhite);
//        if (moves.isEmpty()) return null;
//
//        long start = System.nanoTime();
//        long limit = timeLimitMs * 1_000_000L;
//        Move bestMove = moves.get(0);
//
//        try {
//            for (int depth = 1; ; depth++) {
//                if (System.nanoTime() - start > limit) {
//                    System.out.println("Tempo esgotou antes de depth=" + depth + " → parada.");
//                    break;
//                }
//                int alpha = engineWhite ? Integer.MIN_VALUE : Integer.MAX_VALUE;
//                Move currentBest = null;
//                boolean mateFound = false;
//
//                for (Move m : moves) {
//                    Board next = new Board(board);
//                    next.makeMove(m);
//
//                    int val;
//                    if (next.isInCheck(!engineWhite)) {
//                        val = forcedCheckSequence(next, engineWhite, 3);
//                    } else {
//                        val = alphabetaTimed(next, depth - 1, !engineWhite,
//                                Integer.MIN_VALUE, Integer.MAX_VALUE);
//                    }
//
//                    if (engineWhite ? (val > alpha) : (val < alpha)) {
//                        alpha = val;
//                        currentBest = m;
//                    }
//                    if ((engineWhite && val >= MATE_SCORE) ||
//                            (!engineWhite && val <= -MATE_SCORE)) {
//                        mateFound = true;
//                        break;
//                    }
//                }
//
//                if (currentBest != null) bestMove = currentBest;
//                if (mateFound) break;
//            }
//        } catch (SearchTimeoutException e) {
//            // ignorar
//        }
//        return bestMove;
//    }
//
//    private int forcedCheckSequence(Board board, boolean engineWhite, int remChecks) {
//        if (remChecks == 0 || !board.isInCheck(!engineWhite)) {
//            return evaluate(board);
//        }
//
//        List<Move> oppMoves = gen.generateLegalMoves(board, !engineWhite);
//        if (oppMoves.isEmpty()) {
//            // mate ou stalemate
//            return scoreTerminal(board, 0);
//        }
//
//        int worst = Integer.MAX_VALUE;
//        for (Move opp : oppMoves) {
//            Board bOpp = new Board(board);
//            bOpp.makeMove(opp);
//
//            List<Move> replies = gen.generateLegalMoves(bOpp, engineWhite);
//            if (replies.isEmpty()) {
//                worst = Math.min(worst, scoreTerminal(bOpp, 0));
//                continue;
//            }
//
//            int bestReply = Integer.MIN_VALUE;
//            for (Move my : replies) {
//                Board bMy = new Board(bOpp);
//                bMy.makeMove(my);
//                int val = bMy.isInCheck(!engineWhite)
//                        ? forcedCheckSequence(bMy, engineWhite, remChecks - 1)
//                        : evaluate(bMy);
//                bestReply = Math.max(bestReply, val);
//            }
//            worst = Math.min(worst, bestReply);
//        }
//        return worst;
//    }
//
//    private int alphabetaTimed(Board board, int depth,
//                               boolean maxPlayer, int alpha, int beta) {
//        if (depth == 0 || board.isGameOver()) {
//            if (board.isGameOver()) return scoreTerminal(board, depth);
//            return quiescence(board, alpha, beta, maxPlayer);
//        }
//
//        List<Move> moves = gen.generateLegalMoves(board, maxPlayer);
//        if (moves.isEmpty()) return scoreTerminal(board, depth);
//
//        orderMoves(board, moves, maxPlayer);
//
//        if (maxPlayer) {
//            int value = Integer.MIN_VALUE;
//            for (Move m : moves) {
//                Board next = new Board(board);
//                next.makeMove(m);
//                value = Math.max(value,
//                        alphabetaTimed(next, depth - 1, false, alpha, beta));
//                alpha = Math.max(alpha, value);
//                if (alpha >= beta || value >= MATE_SCORE) break;
//            }
//            return value;
//        } else {
//            int value = Integer.MAX_VALUE;
//            for (Move m : moves) {
//                Board next = new Board(board);
//                next.makeMove(m);
//                value = Math.min(value,
//                        alphabetaTimed(next, depth - 1, true, alpha, beta));
//                beta = Math.min(beta, value);
//                if (beta <= alpha || value <= -MATE_SCORE) break;
//            }
//            return value;
//        }
//    }
//
//    private int quiescence(Board board, int alpha, int beta, boolean maxPlayer) {
//        int standPat = evaluate(board);
//        if (maxPlayer) {
//            if (standPat >= beta) return beta;
//            alpha = Math.max(alpha, standPat);
//        } else {
//            if (standPat <= alpha) return alpha;
//            beta = Math.min(beta, standPat);
//        }
//
//        List<Move> tac = new ArrayList<>();
//        for (Move m : gen.generateLegalMoves(board, maxPlayer)) {
//            if (isCapture(board, m)) {
//                tac.add(m);
//            } else {
//                Board n = new Board(board);
//                n.makeMove(m);
//                if (n.isInCheck(!maxPlayer)) tac.add(m);
//            }
//        }
//
//        orderMoves(board, tac, maxPlayer);
//
//        for (Move m : tac) {
//            Board n = new Board(board);
//            n.makeMove(m);
//            if (maxPlayer) {
//                int sc = quiescence(n, alpha, beta, false);
//                alpha = Math.max(alpha, sc);
//                if (alpha >= beta) return beta;
//            } else {
//                int sc = quiescence(n, alpha, beta, true);
//                beta = Math.min(beta, sc);
//                if (beta <= alpha) return alpha;
//            }
//        }
//        return maxPlayer ? alpha : beta;
//    }
//
//    private void orderMoves(Board board, List<Move> moves, boolean maxPlayer) {
//        Collections.sort(moves, Comparator.comparingInt((Move m) -> {
//            Board b = new Board(board);
//            b.makeMove(m);
//            int score = (b.isInCheck(!maxPlayer) ? 10_000 : 0)
//                    + (isCapture(board, m) ? mvvLvaScore(board, m) : 0);
//            return -score;
//        }));
//    }
//
//    private static boolean isCapture(Board board, Move m) {
//        if (board.getPiece(m.getToRow(), m.getToCol()) != null) return true;
//        Piece p = board.getPiece(m.getFromRow(), m.getFromCol());
//        if ((p == Piece.WHITE_PAWN || p == Piece.BLACK_PAWN)
//                && m.getFromCol() != m.getToCol()
//                && board.getEnPassantRow() == m.getToRow()
//                && board.getEnPassantCol() == m.getToCol()) {
//            return true;
//        }
//        return m.getPromotion() != null;
//    }
//
//    private static int mvvLvaScore(Board board, Move m) {
//        Piece vic = board.getPiece(m.getToRow(), m.getToCol());
//        Piece mov = board.getPiece(m.getFromRow(), m.getFromCol());
//        int vv = (vic != null) ? pieceValue(vic) : 0;
//        int mv = (mov != null) ? pieceValue(mov) : 0;
//        return vv - mv;
//    }
//
//    private static int pieceValue(Piece p) {
//        return switch(p) {
//            case WHITE_PAWN, BLACK_PAWN     -> PAWN;
//            case WHITE_KNIGHT, BLACK_KNIGHT -> KNIGHT;
//            case WHITE_BISHOP, BLACK_BISHOP -> BISHOP;
//            case WHITE_ROOK, BLACK_ROOK     -> ROOK;
//            case WHITE_QUEEN, BLACK_QUEEN   -> QUEEN;
//            default                          -> 0;
//        };
//    }
//
//    private int scoreTerminal(Board b, int depth) {
//        switch (b.getGameResult()) {
//            case DRAW:
//                return 0;
//            case WHITE_WINS:
//                return  MATE_SCORE + depth;
//            case BLACK_WINS:
//                return -MATE_SCORE - depth;
//            default:
//                return 0;
//        }
//    }
//
//    private int evaluate(Board board) {
//        int score = 0;
//        for (int r = 0; r < 8; r++) {
//            for (int c = 0; c < 8; c++) {
//                Piece p = board.getPiece(r, c);
//                if (p == null) continue;
//                score += switch (p) {
//                    case WHITE_PAWN   ->  PAWN;
//                    case WHITE_KNIGHT ->  KNIGHT;
//                    case WHITE_BISHOP ->  BISHOP;
//                    case WHITE_ROOK   ->  ROOK;
//                    case WHITE_QUEEN  ->  QUEEN;
//                    case WHITE_KING   ->  KING;
//                    case BLACK_PAWN   -> -PAWN;
//                    case BLACK_KNIGHT -> -KNIGHT;
//                    case BLACK_BISHOP -> -BISHOP;
//                    case BLACK_ROOK   -> -ROOK;
//                    case BLACK_QUEEN  -> -QUEEN;
//                    case BLACK_KING   -> -KING;
//                    default           -> 0;
//                };
//            }
//        }
//        return score;
//    }
//
//    private static class SearchTimeoutException extends RuntimeException {}
//}
