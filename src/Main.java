package app;

import Jogo.Board;
import Jogo.Board.GameResult;
import Jogo.Move;
import ai.*;

public class Main {
    private static final long MOVE_DELAY_MS = 2000;  // time per move

    private static class Opening {
        final String name;
        final String fen;
        Opening(String name, String fen) { this.name = name; this.fen = fen; }
    }

    private static final Opening[] OPENINGS = new Opening[]{
            new Opening("Defend Mate",      "5rq1/8/8/1p6/1P6/8/k7/7K w - - 0 1"),
            new Opening("TACTIC",      "3r1k2/1q3p2/4p2p/3pQ3/7P/p1P1PR2/6P1/6K1 w - - 0 1"),
            new Opening("Defend Mate",      "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"),
            new Opening("Ruy Lopez, Morphy","r1bqkbnr/1pp1pppp/p1n5/1B1p4/4P3/5N2/PPPP1PPP/RN1QKB1R w KQkq - 0 4"),
            new Opening("OP2",              "rn1qkb1r/ppp1pppp/5n2/3p4/3P2b1/2P2N2/PP2PPPP/RNBQKB1R w KQkq - 1 4"),
            new Opening("OP3",              "rn1qk2r/p1pp1ppp/bp2pn2/8/1bPP4/1P3NP1/P3PP1P/RNBQKB1R w KQkq - 1 6"),
            new Opening("OP5",              "r1bqk2r/pppp1ppp/2n2n2/2b1p1N1/2B1P3/8/PPPP1PPP/RNBQK2R w KQkq - 6 5"),
            new Opening("OP6",    "rnbqkb1r/1p2pppp/p2p1n2/8/3NP3/2N2P2/PPP3PP/R1BQKB1R b KQkq - 0 6"),
            new Opening("OP7",    "rnbqk1nr/ppp1ppbp/3p2p1/8/2P2P2/2N5/PP1PP1PP/R1BQKBNR w KQkq - 0 4"),
            new Opening("OP8",    "rnbqkbnr/pp2pp1p/2p3p1/3p4/2P5/2N2N2/PP1PPPPP/R1BQKB1R w KQkq - 0 4"),
            new Opening("OP9",    "rnbqkb1r/p2ppppp/2p2n2/1p6/P7/5NP1/1PPPPP1P/RNBQKB1R w KQkq - 0 4"),
            new Opening("OP10",    "rnbqkb1r/pp3ppp/2p2n2/3p2B1/2PPp3/6P1/PP2PPBP/RN1QK1NR w KQkq - 0 6"),
            new Opening("OP11",    "rn1qkbnr/pbpp1ppp/1p2p3/8/3PP3/P7/1PP2PPP/RNBQKBNR w KQkq - 1 4"),
            new Opening("OP12",    "r2q4/ppp1kpp1/2nbpnp1/6B1/3PN3/3B4/PPP1KPP1/R6Q w - - 3 13"),
            new Opening("OP13",    "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/2N2N2/PPPP1PPP/R1BQK2R w KQkq - 6 5"),
            new Opening("OP14",    "r1bqkb1r/pp1npppp/2p2n2/8/2B1N3/5Q2/PPPP1PPP/R1B1K1NR w KQkq - 3 6"),
            new Opening("OP15",    "r3kb1r/pp1b1ppp/1qn1p2n/2ppP3/5PP1/2PP1N2/PP2B2P/RNBQ1RK1 w kq - 1 10")
    };

    public static void main(String[] args) {
        AlphaBetaC2 abC2AI = new AlphaBetaC2();
        BetterSquares BQ      = new BetterSquares();

        int winsMinimax = 0, winsAB = 0, draws = 0;
        int idx = 1;

        for (Opening op : OPENINGS) {
            // vamos rodar duas partidas: (AB vs Minimax) e (Minimax vs AB)
            for (int role = 0; role < 2; role++) {
                boolean abPlaysWhite = (role == 0);

                clearConsole();
                System.out.println("=== Opening " + idx + ": " + op.name
                        + "   [Partida " + (role+1)
                        + ": " + (abPlaysWhite ? "BQ ♔ vs ABC2 ♚"
                        : "ABC2 ♔ vs BQ ♚")
                        + "] ===");

                Board board = new Board();
                board.loadFromFEN(op.fen);
                printBoard(board);

                // joga até o fim
                while (!board.isGameOver()) {
                    boolean whiteToMove = board.isWhiteToMove();
                    Move chosen;
                    if (whiteToMove == abPlaysWhite) {
                        // turno do AlphaBeta
                        chosen = BQ.findBestMoveAlphaBetaTimed(board, whiteToMove, MOVE_DELAY_MS);
                        System.out.println((whiteToMove ? "White(BQ)" : "Black(BQ)") + " plays: " + chosen);
                    } else {
                        // turno do Minimax
                        chosen = abC2AI.findBestMoveAlphaBetaTimed(board, whiteToMove, MOVE_DELAY_MS);
                        System.out.println((whiteToMove ? "White(ABC2)" : "Black(ABC2)") + " plays: " + chosen);
                    }
                    if (chosen == null) break;
                    board.makeMove(chosen);
                    clearConsole();
                    printBoard(board);
                }

                // quem venceu?
                GameResult result = board.getGameResult();
                switch (result) {
                    case WHITE_WINS:
                        if (abPlaysWhite) {
                            winsAB++;
                            System.out.println("Result: BQ wins");
                        } else {
                            winsMinimax++;
                            System.out.println("Result: ABC2 wins");
                        }
                        break;
                    case BLACK_WINS:
                        if (!abPlaysWhite) {
                            winsAB++;
                            System.out.println("Result: BQ wins");
                        } else {
                            winsMinimax++;
                            System.out.println("Result: ABC2 wins");
                        }
                        break;
                    case DRAW:
                        draws++;
                        System.out.println("Result: Draw");
                        break;
                    default:
                        System.out.println("Result: Unknown");
                }
                System.out.println();
            }

            idx++;
        }

        // Estatísticas agregadas
        System.out.println("=== Aggregate Results ===");
        System.out.println("AlphaBetaC2 wins: " + winsAB);
        System.out.println("BQ    wins: " + winsMinimax);
        System.out.println("Draws          : " + draws);
    }

    /** Limpa a console (ANSI). */
    private static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /** Desenha o tabuleiro no terminal. */
    private static void printBoard(Board board) {
        System.out.println();
        for (int r = 7; r >= 0; r--) {
            System.out.print((r + 1) + " | ");
            for (int c = 0; c < 8; c++) {
                var p = board.getPiece(r, c);
                char sym = (p != null) ? p.getSymbol() : '.';
                System.out.print(sym + " ");
            }
            System.out.println();
        }
        System.out.println("   ----------------");
        System.out.println("    a b c d e f g h");
        System.out.println();
    }
}
