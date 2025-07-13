package app;

import Jogo.Board;
import Jogo.Board.GameResult;
import Jogo.Move;
import ai.AlphaBetaB;
import ai.AlphaBetaC2;
import ai.MoveGenerator;

import java.util.List;
import java.util.Scanner;

public class Simulator {
    private static final long MOVE_DELAY_MS = 500;  // tempo por movimento em ms

    public static void main(String[] args) {
        AlphaBetaC2 ai = new AlphaBetaC2();
        MoveGenerator moveGen = new MoveGenerator();
        Scanner scanner = new Scanner(System.in);

        // Escolha de FEN ou posição inicial
        Board board = new Board();
        System.out.println("Iniciar de posição padrão (enter) ou usar FEN? (fen + enter)");
        String opt = scanner.nextLine().trim().toLowerCase();
        if (opt.equals("fen")) {
            System.out.print("Digite FEN: ");
            String fen = scanner.nextLine().trim();
            board.loadFromFEN(fen);
        }

        clearConsole();
        printBoard(board);
        System.out.println("Você joga com as brancas. Lance no formato longo ex: e2e4 ");

        while (!board.isGameOver()) {
            if (board.isWhiteToMove()) {
                // jogador humano
                List<Move> legal = moveGen.generateLegalMoves(board, true);
                System.out.println("Jogadas legais: " + legal);
                Move human = null;
                while (human == null) {
                    System.out.print("Seu lance: ");
                    String in = scanner.nextLine().trim();
                    try {
                        human = Move.parse(in);
                        if (!legal.contains(human)) {
                            System.out.println("Lance ilegal ou não disponível. Tente novamente.");
                            human = null;
                        }
                    } catch (Exception e) {
                        System.out.println("Formato inválido. Use ex: e2e4");
                    }
                }
                board.makeMove(human);
            } else {
                // ABB joga
                System.out.println("ABB pensando...");
                Move aiMove = ai.findBestMoveAlphaBetaTimed(board, false, MOVE_DELAY_MS);
                System.out.println("ABB joga: " + aiMove + "\n");
                board.makeMove(aiMove);
            }
            clearConsole();
            printBoard(board);
        }

        // resultado final
        GameResult result = board.getGameResult();
        switch (result) {
            case WHITE_WINS: System.out.println("Você venceu!"); break;
            case BLACK_WINS: System.out.println("ABB venceu."); break;
            case DRAW:       System.out.println("Empate."); break;
            default:         System.out.println("Fim de jogo.");
        }
        scanner.close();
    }

    private static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void printBoard(Board board) {
        System.out.println();
        for (int r = 7; r >= 0; r--) {
            System.out.print((r+1) + " | ");
            for (int c = 0; c < 8; c++) {
                var p = board.getPiece(r, c);
                char sym = (p != null) ? p.getSymbol() : '.';
                System.out.print(sym + " ");
            }
            System.out.println();
        }
        System.out.println("   ----------------");
        System.out.println("    a b c d e f g h\n");
    }
}
