// src/engine/Board.java
package Jogo;

import ai.MoveGenerator;
import java.util.HashMap;
import java.util.Map;

public class Board {
    public enum GameResult { ONGOING, DRAW, WHITE_WINS, BLACK_WINS }

    private Piece[][] grid;
    private int enPassantRow, enPassantCol;
    private boolean whiteCastleKing, whiteCastleQueen;
    private boolean blackCastleKing, blackCastleQueen;

    // —— new fields for draw/mate detection ——
    private boolean whiteToMove;
    private int halfmoveClock;            // half-moves since last pawn move or capture
    private Map<String,Integer> repetitionCounts;

    public Board() {
        grid = new Piece[8][8];
        setupStartPosition();
        whiteCastleKing = whiteCastleQueen = true;
        blackCastleKing = blackCastleQueen = true;
        enPassantRow = enPassantCol = -1;

        whiteToMove = true;
        halfmoveClock = 0;
        repetitionCounts = new HashMap<>();
        repetitionCounts.put(generateFEN(), 1);
    }

    /** Construtor de cópia */
    public Board(Board other) {
        grid = new Piece[8][8];
        for (int r = 0; r < 8; r++) {
            System.arraycopy(other.grid[r], 0, grid[r], 0, 8);
        }
        whiteCastleKing  = other.whiteCastleKing;
        whiteCastleQueen = other.whiteCastleQueen;
        blackCastleKing  = other.blackCastleKing;
        blackCastleQueen = other.blackCastleQueen;
        enPassantRow     = other.enPassantRow;
        enPassantCol     = other.enPassantCol;

        whiteToMove       = other.whiteToMove;
        halfmoveClock     = other.halfmoveClock;
        repetitionCounts  = new HashMap<>(other.repetitionCounts);
    }

    // ——— getters ———
    public int getEnPassantRow()      { return enPassantRow; }
    public int getEnPassantCol()      { return enPassantCol; }
    public boolean canWhiteCastleKing()  { return whiteCastleKing;  }
    public boolean canWhiteCastleQueen() { return whiteCastleQueen; }
    public boolean canBlackCastleKing()  { return blackCastleKing;  }
    public boolean canBlackCastleQueen() { return blackCastleQueen; }
    public boolean isWhiteToMove()       { return whiteToMove;     }

    /** Inicializa posição inicial de xadrez */
    private void setupStartPosition() {
        grid[0] = new Piece[]{
                Piece.WHITE_ROOK, Piece.WHITE_KNIGHT, Piece.WHITE_BISHOP, Piece.WHITE_QUEEN,
                Piece.WHITE_KING, Piece.WHITE_BISHOP, Piece.WHITE_KNIGHT, Piece.WHITE_ROOK
        };
        for (int c = 0; c < 8; c++) grid[1][c] = Piece.WHITE_PAWN;
        for (int r = 2; r < 6; r++) {
            for (int c = 0; c < 8; c++) grid[r][c] = null;
        }
        for (int c = 0; c < 8; c++) grid[6][c] = Piece.BLACK_PAWN;
        grid[7] = new Piece[]{
                Piece.BLACK_ROOK, Piece.BLACK_KNIGHT, Piece.BLACK_BISHOP, Piece.BLACK_QUEEN,
                Piece.BLACK_KING, Piece.BLACK_BISHOP, Piece.BLACK_KNIGHT, Piece.BLACK_ROOK
        };
    }

    public Piece getPiece(int row, int col) {
        return grid[row][col];
    }

    /**
     * Applies a move: updates board, castling rights, en passant,
     * halfmove clock, repetition counts, flips side to move.
     */
    public void makeMove(Move m) {
        Piece p = grid[m.getFromRow()][m.getFromCol()];
        Piece captured = grid[m.getToRow()][m.getToCol()];

        // ——— update castling rights ———
        if (p == Piece.WHITE_KING) {
            whiteCastleKing = whiteCastleQueen = false;
        }
        if (p == Piece.BLACK_KING) {
            blackCastleKing = blackCastleQueen = false;
        }
        if (p == Piece.WHITE_ROOK) {
            if (m.getFromRow() == 0 && m.getFromCol() == 0) whiteCastleQueen = false;
            if (m.getFromRow() == 0 && m.getFromCol() == 7) whiteCastleKing  = false;// funcao get
        }
        if (p == Piece.BLACK_ROOK) {
            if (m.getFromRow() == 7 && m.getFromCol() == 0) blackCastleQueen = false;
            if (m.getFromRow() == 7 && m.getFromCol() == 7) blackCastleKing  = false;
        }

        // ——— en passant target ———
        if (p == Piece.WHITE_PAWN || p == Piece.BLACK_PAWN) {
            int delta = m.getToRow() - m.getFromRow();
            if (Math.abs(delta) == 2) {
                enPassantRow = (m.getFromRow() + m.getToRow()) / 2;
                enPassantCol = m.getFromCol();
            } else {
                enPassantRow = enPassantCol = -1;
            }
        } else {
            enPassantRow = enPassantCol = -1;
        }

        // ——— en passant capture ———
        if ((p == Piece.WHITE_PAWN || p == Piece.BLACK_PAWN)
                && m.getFromCol() != m.getToCol()
                && captured == null) {
            int capRow = m.getFromRow();
            int capCol = m.getToCol();
            captured = grid[capRow][capCol];
            grid[capRow][capCol] = null;
        }

        // ——— castling move ———
        if (p == Piece.WHITE_KING || p == Piece.BLACK_KING) {
            if (Math.abs(m.getToCol() - m.getFromCol()) == 2) {
                int home = m.getFromRow();
                if (m.getToCol() == 6) {
                    grid[home][5] = grid[home][7]; grid[home][7] = null;
                } else {
                    grid[home][3] = grid[home][0]; grid[home][0] = null;
                }
            }
        }

        // ——— promotion ———
        if ((p == Piece.WHITE_PAWN || p == Piece.BLACK_PAWN) && m.getPromotion() != null) {
            grid[m.getToRow()][m.getToCol()] = m.getPromotion();
            grid[m.getFromRow()][m.getFromCol()] = null;
        } else {
            grid[m.getToRow()][m.getToCol()] = p;
            grid[m.getFromRow()][m.getFromCol()] = null;
        }

        // ——— halfmove clock ———
        boolean pawnMove = (p == Piece.WHITE_PAWN || p == Piece.BLACK_PAWN)
                && m.getFromRow() != m.getToRow();
        boolean didCap   = (captured != null);
        halfmoveClock = (pawnMove || didCap) ? 0 : halfmoveClock + 1;

        // ——— toggle side ———
        whiteToMove = !whiteToMove;

        // ——— repetition ———
        String fen = generateFEN();
        repetitionCounts.put(fen, repetitionCounts.getOrDefault(fen, 0) + 1);
    }

    /** Minimal FEN: placement, side, castling, en passant. */
    public String generateFEN() {
        StringBuilder sb = new StringBuilder();
        for (int r = 7; r >= 0; r--) {
            int emp = 0;
            for (int c = 0; c < 8; c++) {
                Piece q = grid[r][c];
                if (q == null) emp++;
                else {
                    if (emp > 0) { sb.append(emp); emp = 0; }
                    sb.append(q.getSymbol());
                }
            }
            if (emp > 0) sb.append(emp);
            if (r > 0) sb.append('/');
        }
        sb.append(whiteToMove ? " w " : " b ");
        StringBuilder cr = new StringBuilder();
        if (whiteCastleKing)  cr.append('K');
        if (whiteCastleQueen) cr.append('Q');
        if (blackCastleKing)  cr.append('k');
        if (blackCastleQueen) cr.append('q');
        sb.append(cr.length() > 0 ? cr : "-");
        sb.append(' ');
        if (enPassantRow >= 0) {
            sb.append((char)('a' + enPassantCol))
                    .append((char)('1' + enPassantRow));
        } else {
            sb.append('-');
        }
        return sb.toString();
    }

    public boolean isDrawByFiftyMoves() {
        return halfmoveClock >= 100;
    }

    public boolean isDrawByRepetition() {
        return repetitionCounts.getOrDefault(generateFEN(), 0) >= 3;
    }

    /** True if side to move is in check and has no legal moves. */
    public boolean isCheckmate() {
        MoveGenerator gen = new MoveGenerator();
        return isInCheck(whiteToMove) && gen.generateLegalMoves(this, whiteToMove).isEmpty();
    }

    /** True if side to move is not in check and has no legal moves. */
    public boolean isStalemate() {
        MoveGenerator gen = new MoveGenerator();
        return !isInCheck(whiteToMove) && gen.generateLegalMoves(this, whiteToMove).isEmpty();
    }

    // … existing isSquareAttacked and isInCheck methods unchanged …

    public boolean isSquareAttacked(int row, int col, boolean byWhite) {
        // knight attacks
        int[][] knightD = {{2,1},{2,-1},{-2,1},{-2,-1},{1,2},{1,-2},{-1,2},{-1,-2}};
        for (var d : knightD) {
            int r = row + d[0], c = col + d[1];
            if (r<0||r>7||c<0||c>7) continue;
            Piece p = grid[r][c];
            if (p == (byWhite ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT))
                return true;
        }
        // pawn attacks
        int pd = byWhite ? -1 : +1;
        for (int dc : new int[]{-1, +1}) {
            int r = row + pd, c = col + dc;
            if (r<0||r>7||c<0||c>7) continue;
            Piece p = grid[r][c];
            if (p == (byWhite ? Piece.WHITE_PAWN : Piece.BLACK_PAWN))
                return true;
        }
        // orthogonal sliders (rook/queen)
        int[][] orth = {{1,0},{-1,0},{0,1},{0,-1}};
        for (var d : orth) {
            int r = row + d[0], c = col + d[1];
            while (r>=0&&r<8&&c>=0&&c<8) {
                Piece q = grid[r][c];
                if (q != null) {
                    if (q.isWhite() == byWhite &&
                            (q == Piece.WHITE_ROOK || q == Piece.BLACK_ROOK ||
                                    q == Piece.WHITE_QUEEN|| q == Piece.BLACK_QUEEN))
                        return true;
                    break;
                }
                r += d[0]; c += d[1];
            }
        }
        // diagonal sliders (bishop/queen)
        int[][] diag = {{1,1},{1,-1},{-1,1},{-1,-1}};
        for (var d : diag) {
            int r = row + d[0], c = col + d[1];
            while (r>=0&&r<8&&c>=0&&c<8) {
                Piece q = grid[r][c];
                if (q != null) {
                    if (q.isWhite() == byWhite &&
                            (q == Piece.WHITE_BISHOP|| q == Piece.BLACK_BISHOP||
                                    q == Piece.WHITE_QUEEN || q == Piece.BLACK_QUEEN))
                        return true;
                    break;
                }
                r += d[0]; c += d[1];
            }
        }
        // king adjacent
        for (int dr=-1; dr<=1; dr++) {
            for (int dc=-1; dc<=1; dc++) {
                if (dr==0 && dc==0) continue;
                int r = row+dr, c = col+dc;
                if (r<0||r>7||c<0||c>7) continue;
                Piece q = grid[r][c];
                if (q == (byWhite ? Piece.WHITE_KING : Piece.BLACK_KING))
                    return true;
            }
        }
        return false;
    }

    public boolean isInCheck(boolean white) {
        Piece king = white ? Piece.WHITE_KING : Piece.BLACK_KING;
        int kr=-1, kc=-1;
        for (int r=0;r<8;r++){
            for (int c=0;c<8;c++){
                if (grid[r][c]==king){
                    kr=r; kc=c; break;
                }
            }
            if (kr!=-1) break;
        }
        return (kr!=-1) && isSquareAttacked(kr, kc, !white);
    }

    /** Overall game state. */
    public GameResult getGameResult() {
        if (isCheckmate()) {
            return whiteToMove ? GameResult.BLACK_WINS : GameResult.WHITE_WINS;
        }
        if (isStalemate() || isDrawByFiftyMoves() || isDrawByRepetition()) {
            return GameResult.DRAW;
        }
        return GameResult.ONGOING;
    }

    public boolean isGameOver() {
        return getGameResult() != GameResult.ONGOING;
    }

    // em Jogo/Board.java, dentro da classe Board:

    /**
     * Carrega uma posição arbitrária a partir de uma string FEN (ignora clocks de meio-movimento
     * e contagem de repetições, reinicializa halfmoveClock e repetitionCounts).
     *
     * @param fen string FEN no formato padrão:
     *            \"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -\"
     */
    public void loadFromFEN(String fen) {
        String[] parts = fen.trim().split("\\s+");  // sem aspas internas
        if (parts.length < 4) {
            throw new IllegalArgumentException("FEN inválida: deve ter pelo menos 4 campos");
        }

        // 1) Peças
        String[] ranks = parts[0].split("/");      // sem aspas internas
        if (ranks.length != 8) {
            throw new IllegalArgumentException("FEN inválida: deve ter 8 filas");
        }
        grid = new Piece[8][8];
        for (int r = 0; r < 8; r++) {
            String rank = ranks[7 - r];  // FEN vai de 8ª fila pra 1ª
            int c = 0;
            for (char ch : rank.toCharArray()) {
                if (Character.isDigit(ch)) {
                    c += ch - '0';
                } else {
                    grid[r][c++] = fenCharToPiece(ch);
                }
            }
        }

        // 2) Lado a mover
        whiteToMove = parts[1].equals("w");  // sem aspas internas

        // 3) Direitos de roque
        String cast = parts[2];
        whiteCastleKing   = cast.contains("K");
        whiteCastleQueen  = cast.contains("Q");
        blackCastleKing   = cast.contains("k");
        blackCastleQueen  = cast.contains("q");

        // 4) En passant
        String ep = parts[3];
        if (ep.equals("-")) {
            enPassantRow = enPassantCol = -1;
        } else {
            enPassantCol = ep.charAt(0) - 'a';
            enPassantRow = ep.charAt(1) - '1';
        }

        // Reinicia halfmove clock e repetições
        halfmoveClock = 0;
        repetitionCounts.clear();
        repetitionCounts.put(generateFEN(), 1);
    }


    /**
 * Converte um símbolo FEN num Piece correspondente.
 */
    private static Piece fenCharToPiece(char ch) {
        return switch (ch) {
            case 'P' -> Piece.WHITE_PAWN;
            case 'N' -> Piece.WHITE_KNIGHT;
            case 'B' -> Piece.WHITE_BISHOP;
            case 'R' -> Piece.WHITE_ROOK;
            case 'Q' -> Piece.WHITE_QUEEN;
            case 'K' -> Piece.WHITE_KING;
            case 'p' -> Piece.BLACK_PAWN;
            case 'n' -> Piece.BLACK_KNIGHT;
            case 'b' -> Piece.BLACK_BISHOP;
            case 'r' -> Piece.BLACK_ROOK;
            case 'q' -> Piece.BLACK_QUEEN;
            case 'k' -> Piece.BLACK_KING;
            default  -> throw new IllegalArgumentException("FEN inválida: símbolo '" + ch + "' desconhecido");
        };
    }


// undoMove could be added later for full search support
}
