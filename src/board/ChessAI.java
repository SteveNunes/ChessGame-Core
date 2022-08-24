package board;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import enums.ChessPlayMode;
import enums.PieceColor;
import enums.PieceType;
import exceptions.GameException;
import piece.Piece;
import piece.PiecePosition;

public class ChessAI {

	private Board board = null;
	private int cpuLastChoice;
	private Piece cpuSelectedPiece;
	private PiecePosition cpuSelectedPositionToMove;

	public ChessAI(Board board) {
		this.board = board;
		reset();
	}
	
	public void reset() {
		cpuSelectedPiece = null;
		cpuSelectedPositionToMove = null;
		cpuLastChoice = -1;
	}
	
	private void validateCpuCommands() {
		if (board.getPlayMode() == ChessPlayMode.PLAYER_VS_PLAYER)
			throw new GameException("Unable to use while in a PLAYER VS PLAYER mode.");
		if (board.isGameOver())
			throw new GameException("The current game was ended");
		if (!board.isCpuTurn())
			throw new GameException("It's not the CPU turn.");
		
	}
	
	public int debugGetLastCPUchoice() {
		validateCpuCommands();
		if (cpuLastChoice == -1)
			throw new GameException("CPU did 't played yet");
		return cpuLastChoice;
	}
	
	private Boolean positionIsIgnored(Map<Piece, List<PiecePosition>> ignorePositions, Piece piece, PiecePosition position) {
		return new SecureRandom().nextInt(2) == 0 ||
			ignorePositions.containsKey(piece) && ignorePositions.get(piece).contains(position);
	}

	public PiecePosition getCpuSelectedPositionToMove() {
		validateCpuCommands();
		if (!board.pieceIsSelected())
			throw new GameException("CPU not selected a piece. Call \".doCpuSelectAPiece()\" first.");
		return cpuSelectedPositionToMove;
	}

	private void trySetCpuPositionToMove(Piece piece, PiecePosition position, Board recBoard, int cpuChoice) {
		cpuLastChoice = cpuChoice;
		Board.cloneBoard(recBoard, board);
		tryToMoveTo(piece.getPosition(), cpuSelectedPositionToMove);
		Board.cloneBoard(recBoard, board);
		board.cpuSelectedPiece(piece);
		return;
	}
	
	private void tryToMoveTo(PiecePosition sourcePos, PiecePosition targetPos) {
		cpuSelectedPiece = board.getPieceAt(sourcePos);
		cpuSelectedPositionToMove = new PiecePosition(targetPos);
		board.movePieceTo(sourcePos, targetPos, false);
	}

	public void doCpuSelectAPiece(Boolean debug) {
		validateCpuCommands();
		if (board.pieceIsSelected())
			throw new GameException("CPU already selected a piece. Call \".doCpuMoveSelectedPiece()\" for finish the CPU move.");
		Board recBoard = board.newClonedBoard();
		Map<Piece, List<PiecePosition>> ignorePositions = new HashMap<>();
		List<Piece> ignoredPieces = new ArrayList<>();
		cpuSelectedPiece = null;
		cpuSelectedPositionToMove = null;
		
		while (true)
			try {
				// Verifica se tem alguma peça em risco de captura. 
				cpuLastChoice = 1;
				for (PieceType type : PieceType.getListOfAll())
					for (Piece piece : board.getPieceList(board.getCurrentColorTurn()))
						if (piece.getType() == type && !ignoredPieces.contains(piece) && board.pieceCouldBeCaptured(piece)) {
							// Se tiver, tentar move-la para um tile seguro. 
							for (PiecePosition position : piece.getPossibleMoves())
								if (!positionIsIgnored(ignorePositions, piece, position)) {
									tryToMoveTo(piece.getPosition(), position);
									if (!board.pieceCouldBeCaptured(piece)) {
										trySetCpuPositionToMove(piece, position, recBoard, 2);
										return;
									}
									Board.cloneBoard(recBoard, board);
								}
							/* Se não puder, tentar colocar uma peça em uma posição segura, 
							 * que possa capturar a peça adversária após ela capturar a peça em perigo,
							 * caso já não exista uma pedra nessa condição.
							 */  
							cpuLastChoice = 3;
							board.removePiece(piece);
							Board recBoard2 = board.newClonedBoard();
							Boolean ok = false;
							for (Piece piece2 : board.getPieceList(board.getCurrentColorTurn()))
								if (!positionIsIgnored(ignorePositions, piece2, piece.getPosition()) &&
										piece2.canMoveToPosition(piece.getPosition())) {
											Board.cloneBoard(recBoard, board);
											ok = true;
								}
	
							if (!ok) {
								for (Piece piece2 : board.getPieceList(board.getCurrentColorTurn()))
									for (PiecePosition position : piece2.getPossibleMoves())
										if (!positionIsIgnored(ignorePositions, piece2, position)) {
											Board.cloneBoard(board, recBoard2);
											tryToMoveTo(piece2.getPosition(), position);
											if (!positionIsIgnored(ignorePositions, piece2, piece.getPosition()) &&
													piece2.canMoveToPosition(piece.getPosition())) {
												tryToMoveTo(piece2.getPosition(), piece.getPosition());
														if (board.getPiecesWhichCanCapture(piece2).size() == 1) {
															trySetCpuPositionToMove(piece2, position, recBoard, 4);
															return;
														}
											}
											Board.cloneBoard(recBoard2, board);
										}
								Board.cloneBoard(recBoard, board);
	
								/* E se não der, e for possivel bloquear a rota da peça inimiga usando uma
								 * peça de menor valor que a peça em risco, colocar a peça de menor valor
								 * no caminho da peça inimiga.
								 */
								cpuLastChoice = 5;
								for (Piece piece2 : board.getPieceList(board.getCurrentColorTurn()))
									for (PiecePosition position : piece2.getPossibleMoves())
										if (!positionIsIgnored(ignorePositions, piece2, position)) {
											tryToMoveTo(piece2.getPosition(), position);
											if (!board.pieceCouldBeCaptured(piece)) {
												trySetCpuPositionToMove(piece, position, recBoard, 6);
												return;
											}
											Board.cloneBoard(recBoard, board);
										}
							}
						}
				
				/* Tenta capturar uma peça adversária sem que a posição onde a peça vá parar,
				 * permita que ela possa ser capturada logo em seguida
				 */
				cpuLastChoice = 7;
				for (PieceType type : PieceType.getListOfAll())
					for (Piece piece : board.getPieceList(board.getCurrentColorTurn()))
						if (piece.getType() == type && !ignoredPieces.contains(piece)) {
							ignoredPieces.add(piece);
							for (Piece opponentPiece : board.getPieceList(board.opponentColor()))
								if (!positionIsIgnored(ignorePositions, piece, opponentPiece.getPosition()) &&
										piece.getPossibleMoves().contains(opponentPiece.getPosition())) {
											tryToMoveTo(piece.getPosition(), opponentPiece.getPosition());
											if (!board.pieceCouldBeCaptured(piece)) {
												trySetCpuPositionToMove(piece, opponentPiece.getPosition(), recBoard, 8);
												return;
											}
											Board.cloneBoard(recBoard, board);
								}
						}
	
				/* Tenta capturar uma peça adversária com qualquer peça se o valor da peça
				 * á ser capturada for maior ou igual ao valor da peça selecionada,
				 * mesmo que essa peça possa ser capturada logo em seguida
				 */
				cpuLastChoice = 9;
				for (PieceType type : PieceType.getListOfAll())
					for (Piece piece : board.getPieceList(board.getCurrentColorTurn()))
						if (piece.getType() == type)
							for (Piece opponentPiece : board.getPieceList(board.opponentColor()))
								if (!positionIsIgnored(ignorePositions, piece, opponentPiece.getPosition()) &&
										piece.getType().getIntValue() <= opponentPiece.getType().getIntValue() &&
										piece.getPossibleMoves().contains(opponentPiece.getPosition())) {
											trySetCpuPositionToMove(piece, opponentPiece.getPosition(), recBoard, 10);
											return;
								}
	
				// Tenta mover um peão 2 casas para frente, nas colunas do meio, se for seguro fazer isso
				cpuLastChoice = 11;
				for (Piece piece : board.getPieceList(board.getCurrentColorTurn()))
					if (piece.getType() == PieceType.PAWN && !piece.wasMoved() &&
							(piece.getColumn() == 3 || piece.getColumn() == 4)) {
								PiecePosition position = new PiecePosition(piece.getPosition());
								position.incRow(piece.getColor() == PieceColor.BLACK ? -2 : 2);
								if (!positionIsIgnored(ignorePositions, piece, position) &&
										piece.canMoveToPosition(position)) {
											tryToMoveTo(piece.getPosition(), position);
											if (!board.pieceCouldBeCaptured(position)) {
												trySetCpuPositionToMove(piece, position, recBoard, 12);
												return;
											}
											Board.cloneBoard(recBoard, board);
								}
							}
	
				/* Tenta mover um peão 2 casas para frente, (se for a primeira movimentacao
				 * do peao) sem que ele possa ser capturado logo em seguida
				 */
				cpuLastChoice = 13;
				for (Piece piece : board.getPieceList(board.getCurrentColorTurn()))
					if (piece.getType() == PieceType.PAWN) {
						for (PiecePosition position : piece.getPossibleMoves())
							if (!positionIsIgnored(ignorePositions, piece, position) &&
									Math.abs(piece.getRow() - position.getRow()) == 2) {
										tryToMoveTo(piece.getPosition(), position);
										if (!board.pieceCouldBeCaptured(position)) { 
											trySetCpuPositionToMove(piece, position, recBoard, 14);
											return;
										}
										Board.cloneBoard(recBoard, board);
							}
						}
	
				// Tenta mover a peça para um tile seguro no turno seguinte ao movimento
				cpuLastChoice = 15;
				for (PieceType type : PieceType.getListOfAll())
					for (Piece piece : board.getPieceList(board.getCurrentColorTurn()))
						if (piece.getType() == type) {
							for (PiecePosition position : piece.getPossibleMoves())
								if (!positionIsIgnored(ignorePositions, piece, position)) {
									tryToMoveTo(piece.getPosition(), position);
									if (!board.pieceCouldBeCaptured(piece)) {
										trySetCpuPositionToMove(piece, position, recBoard, 16);
										return;
									}
									Board.cloneBoard(recBoard, board);
								}
						}
		
				// Tenta capturar uma peça adversária mesmo que essa peça possa ser capturada logo em seguida
				cpuLastChoice = 17;
				for (PieceType type : PieceType.getListOfAll())
					for (Piece piece : board.getPieceList(board.getCurrentColorTurn()))
						if (piece.getType() == type)
							for (Piece opponentPiece : board.getPieceList(board.opponentColor()))
								if (!positionIsIgnored(ignorePositions, piece, opponentPiece.getPosition()) &&
										piece.getPossibleMoves().contains(opponentPiece.getPosition())) {
											trySetCpuPositionToMove(piece, opponentPiece.getPosition(), recBoard, 18);
											return;
								}
			}
			catch (Exception e) {
				if (debug) {
					System.out.println("\nAI DEBUG:\ntriedVal: " + cpuLastChoice);
					System.out.println("selectedPiece at: " + (cpuSelectedPiece == null ? null : cpuSelectedPiece.getPosition()));
					System.out.println("cpuSelectedPositionToMove: " + (cpuSelectedPositionToMove == null ? null : cpuSelectedPositionToMove));
					System.out.println(e.getMessage());
				}
				/* Se na tentativa de definir a movimentação da pedra, lançar alguma
				 * exception, repetir a verificação, porém colocando a última posição
				 * tentada na lista de ignore.
				 */
				if (!ignorePositions.containsKey(cpuSelectedPiece))
					ignorePositions.put(cpuSelectedPiece, new ArrayList<>());
				ignorePositions.get(cpuSelectedPiece).add(cpuSelectedPositionToMove);
				cpuSelectedPiece = null;
				cpuSelectedPositionToMove = null;
				Board.cloneBoard(recBoard, board);
			}
	}
	
	public void doCpuSelectAPiece()
		{ doCpuSelectAPiece(false); }

	public void doCpuMoveSelectedPiece() {
		validateCpuCommands();
		if (!board.pieceIsSelected())
			throw new GameException("CPU not selected a piece. Call \".doCpuSelectAPiece()\" first.");
		board.movePieceTo(cpuSelectedPositionToMove);
	}
	
	public Boolean cpuSelectedAPiece() {
		validateCpuCommands();
		return board.isCpuTurn() && board.pieceIsSelected();
	}

}
