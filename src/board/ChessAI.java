package board;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import enums.ChessPlayMode;
import enums.PieceColor;
import enums.PieceType;
import exceptions.GameException;
import exceptions.PieceMoveException;
import exceptions.PieceSelectionException;
import piece.Piece;
import piece.PiecePosition;

public class ChessAI {

	private Boolean debugging = false;
	private Board board = null;
	private int cpuLastChoice;
	private Piece cpuSelectedPiece;
	private PiecePosition cpuSelectedPositionToMove;
	private Map<Piece, List<PiecePosition>> ignorePositions;
	private List<Piece> ignorePieces;
 	
	/**
	 * Construtor que recebe o tabuleiro atual como parâmetro
	 */
	public ChessAI(Board board) {
		this.board = board;
		reset();
	}
	
	/**
	 * Reseta a seleção feita atualmente pela CPU
	 */
	public void reset() {
		cpuSelectedPiece = null;
		cpuSelectedPositionToMove = null;
		cpuLastChoice = -1;
		ignorePositions = new HashMap<>();
		ignorePieces = new ArrayList<>();
	}
	
	public PiecePosition cpuSelectedTargetPosition()
		{ return board.pieceIsSelected() ? cpuSelectedPositionToMove : null; }
	
	private void validateCpuCommands() {
		if (board.getPlayMode() == ChessPlayMode.PLAYER_VS_PLAYER)
			throw new GameException("Unable to use while in a PLAYER VS PLAYER mode.");
		if (board.isGameOver())
			throw new GameException("The current game was ended");
		if (!board.isCpuTurn())
			throw new GameException("It's not the CPU turn.");
		
	}
	
	private void choiceAPossibleMoveToDo(List<PossibleMove> possibleMoves, Board recBoard, int cpuChoice) {
		if (possibleMoves == null || possibleMoves.isEmpty())
			return;
		// Ordena os possible moves pelo score de pecas e score de ameaças, para usar a movimentacao melhor pontuada
		possibleMoves.sort((p1, p2) -> p1.compareTo(p2));
		for (int n = 1; n < possibleMoves.size(); n++)
			if (possibleMoves.get(n).getScore() != possibleMoves.get(0).getScore())
				possibleMoves.remove(n--);
		int pos = new SecureRandom().nextInt(possibleMoves.size());
		cpuLastChoice = cpuChoice;
		cpuSelectedPositionToMove = possibleMoves.get(pos).getTargetPosition();
		board.cpuSelectedPiece(possibleMoves.get(pos).getPiece());
		if (debugging) {
			System.out.println("ACTION " + cpuLastChoice + " = " + possibleMoves.get(pos).getStartPosition() + " -> " + possibleMoves.get(pos).getTargetPosition() + " (Score: " + possibleMoves.get(pos).getScore() + ")");
			for (String s : possibleMoves.get(pos).getChoicesInfo())
				System.out.println("- " + s);
			System.out.println();
		}
	}

	private void tryToMoveTo(PiecePosition sourcePos, PiecePosition targetPos) {
		cpuSelectedPiece = board.getPieceAt(sourcePos);
		cpuSelectedPositionToMove = new PiecePosition(targetPos);
		board.movePieceTo(sourcePos, targetPos, false);
	}

	private void tryToMoveTo(Piece sourcePiece, PiecePosition targetPos)
		{ tryToMoveTo(sourcePiece.getPosition(), targetPos); }

	@SuppressWarnings("unused")
	private void tryToMoveTo(PiecePosition sourcePos, Piece targetPiece)
		{ tryToMoveTo(sourcePos, targetPiece.getPosition()); }

	private void tryToMoveTo(Piece sourcePiece, Piece targetPiece)
		{ tryToMoveTo(sourcePiece.getPosition(), targetPiece.getPosition()); }

	private Boolean anyPieceCouldCapture(PieceColor color) {
		for (Piece piece : board.getPieceListByColor(color))
			for (Piece piece2 : board.getOpponentPieceList(color))
				if (piece.couldCapture(piece2))
					return true;
		return false;
	}

	private Boolean anyPieceCouldBeCaptured(PieceColor color)
		{ return anyPieceCouldCapture(color.getOppositeColor()); }
	
	private List<PossibleMove> testPossibleMoves(PieceColor color, Predicate<PossibleMove> predicate, Boolean startTriesFromStrongestPieces) {
		List<PossibleMove> possibleMoves = new ArrayList<>(); 
		Board recBoard = board.newClonedBoard();
		List<Piece> pieces = new ArrayList<>(board.sortPieceListByPieceValue(board.getPieceListByColor(color, p -> !ignorePieces.contains(p)), !startTriesFromStrongestPieces));
		List<Piece> safePiecesBefore = board.getPieceListByColor(color,
				p -> board.pieceIsAtSafePosition(p));
		Piece opponentKing = board.getPieceListByColor(color.getOppositeColor(),
				p -> p.isSameTypeOf(PieceType.KING)).get(0);
		List<PiecePosition> otherKingThreatenPositions = new ArrayList<>(opponentKing.getPossibleSafeMoves());
		float opponentInsightScore = 0;
		float friendlyInsightScore = 0;
		for (Piece piece : pieces) {
			ignorePositions.clear();
			List<PiecePosition> positions = new ArrayList<>(piece.getPossibleMoves());
			for (PiecePosition position : positions)
				if (!isIgnoredPosition(piece, position)) {
					try {
						PiecePosition positionBefore = new PiecePosition(piece.getPosition());
						tryToMoveTo(piece, position);
						PossibleMove possibleMove = new PossibleMove(piece, positionBefore, position);
						possibleMoves.add(possibleMove);
						if (new SecureRandom().nextInt(2) == 0)
							possibleMove.incScore((long)piece.getIntTypeValue());
						if (predicate.test(possibleMove)) {
							Board b = board.newClonedBoard();
							if (board.drawGame()) { // Se a pedra movida resultou em um empate
								possibleMove.incScore(-Long.MAX_VALUE);
								possibleMove.incChoice(1);
							}
							if (safePiecesBefore.contains(piece) && !board.pieceIsAtSafePosition(piece)) {
								// Se a pedra movida estava segura antes, e agora não está mais
								if (!board.pieceWasCaptured()) {
									// Ela não realizou captura no último turno (Ficou em risco por nada)
									possibleMove.decScore((long)(Long.MAX_VALUE / 6 * piece.getTypeValue()));
									possibleMove.incChoice(2);
								}
								else if (board.getLastCapturedPiece().getTypeValue() < piece.getTypeValue()) {
									// Ela capturou uma pedra de menor valor (A troca não valeu a pena)
									possibleMove.decScore((long)(Long.MAX_VALUE / 6 * (piece.getTypeValue() - board.getLastCapturedPiece().getTypeValue())));
									possibleMove.incChoice(4);
								}
								else {
									// Ela capturou uma pedra de valor igual ou maior (A troca valeu a pena)
									possibleMove.incScore((long)(Long.MAX_VALUE / 18 * board.getLastCapturedPiece().getTypeValue()));
									possibleMove.incChoice(8);
								}
							}
							else if (!safePiecesBefore.contains(piece) && board.pieceIsAtSafePosition(piece)) {
								// Se a pedra movida não estava segura antes, e agora está
								List<Piece> ps = getListOfPiecesThatCouldCaptureThis(piece);
								if (ps.size() > 1) {
									/* SE havia mais de uma pedra ameaçando a pedra movida, incrementa
									 * o score, forçando a pedra a de fato permanecer onde parou
									 */
									possibleMove.incScore((long)(Long.MAX_VALUE / 18 * piece.getTypeValue()));
									possibleMove.incChoice(16);
								}
								else {
									/* SE havia apenas 1 pedra ameaçando a pedra movida, e a pedra movida
									 * poderia ter capturado ela, decrementa o score, evitando que a pedra
									 * movida escolha essa posição de fato
									 */
									possibleMove.decScore((long)(Long.MAX_VALUE / 7 * ps.get(0).getTypeValue()));
									possibleMove.incChoice(32);
								}
							}
							if (board.checkMate()) { // Se a pedra movida resultou em um checkmate
								possibleMove.incScore(Long.MAX_VALUE);
								possibleMove.incChoice(64);
							}
							if (board.pieceWasCaptured() && board.pieceIsAtSafePosition(piece)) { 
								// Se a pedra movida capturou uma pedra adversária em segurança
								possibleMove.incScore((long)(Long.MAX_VALUE / 6 * board.getLastCapturedPiece().getTypeValue()));
								possibleMove.incChoice(128);
							}
							if (board.isChecked()) {
								// Se a pedra movida resultou em um check (Seguro ou não)
								possibleMove.incScore(board.pieceIsAtSafePosition(piece) ? Long.MAX_VALUE / 2 : (-Long.MAX_VALUE / 2));
								possibleMove.incChoice(board.pieceIsAtSafePosition(piece) ? 256 : 512);
							}
							if (piece.isSameTypeOf(PieceType.PAWN) && board.pieceIsAtSafePosition(piece) &&
									Math.abs(positionBefore.getRow() - position.getRow()) > 1 &&
									(piece.getColumn() == 3 || piece.getColumn() == 4)) {
										// Se a pedra movida for um peão que andou 2 tiles pelas colunas no meio e está seguro
										possibleMove.incScore(Long.MAX_VALUE / 150);
										possibleMove.incChoice(1024);
							}
							// Gera score negativo baseado em pedras aliadas que estão sob risco de captura
							friendlyInsightScore = 0;
							for (Piece friendlyPiece : board.getPieceListByColor(color))
								if (!board.pieceIsAtSafePosition(friendlyPiece)) {
									if (friendlyPiece != piece) {
										// Se não for a pedra movida
										friendlyInsightScore += friendlyPiece.getTypeValue();
										/* Testa se a pedra movida estava COBRINDO a pedra em risco de captura.
										 * Se sim, gera um score negativo para evitar que a pedra movida pare
										 * de cobrir a pedra ameaçada de captura.
										 */
										friendlyPiece.setColor(color.getOppositeColor());
										board.removePiece(piece);
										board.addPiece(positionBefore, piece);
										if (piece.canMoveToPosition(friendlyPiece.getPosition())) {
											possibleMove.decScore((long)(Long.MAX_VALUE / 7 * friendlyPiece.getTypeValue()));
											possibleMove.incChoice(1048576);
										}
										friendlyPiece.setColor(color);
										Board.cloneBoard(recBoard, board);
									}
									else {
										possibleMove.decScore((long)(Long.MAX_VALUE / 6 * piece.getTypeValue()));
										possibleMove.incChoice(524288);
									}
								}
							// Gera score positivo baseado em pedras adversárias que estão sob risco de captura
							opponentInsightScore = 0;
							for (Piece opponentPiece : board.getPieceListByColor(color.getOppositeColor())) {
								if (!board.pieceIsAtSafePosition(opponentPiece))
									opponentInsightScore += opponentPiece.getTypeValue();
								if (safePiecesBefore.contains(opponentPiece) && !board.pieceIsAtSafePosition(opponentPiece) &&
										piece.couldCapture(opponentPiece) && board.pieceIsAtSafePosition(piece)) {
										// Se a pedra movida está ameaçando uma pedra adversária em segurança
										try {
											Piece lastCaptured = board.getLastCapturedPiece();
											// Simula a captura para ver como vai ficar a situação da pedra aliada após a captura
											tryToMoveTo(piece, opponentPiece.getPosition());
											if (board.pieceIsAtSafePosition(piece)) { 
												/* Se após a captura, a pedra capturante ficou segura, incrementa
												 * o score no valor da pedra adversária que pode ser capturada
												 */
												possibleMove.incScore((long)(Long.MAX_VALUE / 7 * opponentPiece.getTypeValue()));
												possibleMove.incChoice(2048);
											}
											else { // Se após a captura, a pedra capturante não ficar segura...
												if (piece.strongerThan(lastCaptured)) {
														/* Se a pedra capturada por último for de MENOR valor que a pedra capturante,
														 * incrementa o score baseado no (VALOR DA PEDRA CAPTURADA - VALOR DA PEDRA
														 * CAPTURANTE), evitando assim que a CPU suicide pedras capturando outras de
														 * menor valor que a pedra capturante.
														 */
														possibleMove.incScore((long)(Long.MAX_VALUE / 9 * (piece.getTypeValue() - lastCaptured.getTypeValue())));
														possibleMove.incChoice(4096);
												}
												else {
													/* Se a pedra capturada por último for de MAIOR valor que a pedra, incrementa
													 * o score baseado no (VALOR DA PEDRA CAPTURADA - VALOR DA PEDRA CAPTURANTE)
													 */
													possibleMove.incScore((long)(Long.MAX_VALUE / 14 * ((lastCaptured.getTypeValue() - piece.getTypeValue()) + 0.1)));
													possibleMove.incChoice(8192);
												}
											}
										}
										catch (Exception e) {}
										Board.cloneBoard(b, board);
									}
									if (opponentPiece.isSameTypeOf(PieceType.KING)) {
										if (!piece.isSameTypeOf(PieceType.PAWN) && board.pieceIsAtSafePosition(piece)) {
											/* Incrementa o score baseado na distância da pedra atual para a pedra do rei,
											 * desde que essa posição nao coloque a pedra em perigo (apenas não-peôes)
											 */
											int disX = Math.abs(opponentPiece.getColumn() - position.getColumn()); 
											int disY = Math.abs(opponentPiece.getRow() - position.getRow());
											possibleMove.decScore(disX > disY ? disX : disY);
											possibleMove.incChoice(16384);
										}
										if (!board.pieceWasCaptured()) { // Se o ultimo movimento não foi uma captura...
											if (otherKingThreatenPositions.size() > opponentPiece.getPossibleSafeMoves().size()) {
												// Se o rei adversário ficou com menos possibilidade de movimentos agora do que antes
												possibleMove.incScore(Long.MAX_VALUE / 12);
												possibleMove.incChoice(32768);
											}
											else if (otherKingThreatenPositions.size() < opponentPiece.getPossibleSafeMoves().size()) {
												// Se o rei adversário ficou com mais possibilidade de movimentos agora do que antes
												possibleMove.decScore(Long.MAX_VALUE / 12);
												possibleMove.incChoice(65536);
											}
										}
									}
							}
							if (friendlyInsightScore > 0) {
								// Se há pedras aliadas sob risco de captura, decrementa o score baseado no total de pedras aliadas que estão sob risco
								possibleMove.decScore((long)(friendlyInsightScore * Integer.MAX_VALUE * 10));
								possibleMove.incChoice(131072);
							}
							if (opponentInsightScore > 0) {
								// Se há pedras adversárias sob risco de captura, incrementa o score baseado no total de pedras adversárias que estão sob risco
								possibleMove.incScore((long)(opponentInsightScore * Integer.MAX_VALUE));
								possibleMove.incChoice(262144);
							}
						}
					}
					catch (Exception e)
						{ addIgnorePosition(piece, position); }
					Board.cloneBoard(recBoard, board);
				}
		}
			
		return possibleMoves.isEmpty() ? null : possibleMoves;
	}

	@SuppressWarnings("unused")
	private List<PossibleMove> testPossibleMoves(PieceColor color, Predicate<PossibleMove> predicate)
		{ return testPossibleMoves(color, predicate, false); }
	
	@SuppressWarnings("unused")
	private List<PossibleMove> testPossibleMoves(PieceColor color)
		{ return testPossibleMoves(color, e -> true, false); }
		
	private List<PossibleMove> testPossibleMoves(Predicate<PossibleMove> predicate)
		{ return testPossibleMoves(board.getCurrentColorTurn(), predicate, false); }
	
	private List<PossibleMove> testPossibleMoves()
		{ return testPossibleMoves(board.getCurrentColorTurn(), e -> true, false); }

	private List<Piece> getListOfPiecesThatCouldCaptureAPieceAt(PiecePosition position, PieceColor color) {
		List<Piece> pieces = board.getPieceListByColor(color.getOppositeColor(), p -> p.canMoveToPosition(position));
		return pieces.isEmpty() ? null : pieces;
	}

	@SuppressWarnings("unused")
	private List<Piece> getListOfPiecesThatCouldCaptureThis(Piece piece)
		{ return getListOfPiecesThatCouldCaptureAPieceAt(piece.getPosition(), piece.getColor()); }

	@SuppressWarnings("unused")
	private List<Piece> getListOfPiecesThatCouldBeCapturedBy(Piece piece) {
		List<Piece> pieces = board.getPieceListByColor(piece.getOpponentColor(), p -> piece.couldCapture(p));
		return pieces.isEmpty() ? null : pieces;
	}

	public void doCpuSelectAPiece() {
		validateCpuCommands();
		if (board.pieceIsSelected())
			throw new GameException("CPU already selected a piece. Call \".doCpuMoveSelectedPiece()\" for finish the CPU move.");

		List<PossibleMove> possibleMoves;
		Board recBoard = board.newClonedBoard();
		Board recBoard2 = board.newClonedBoard();
		cpuSelectedPiece = null;
		cpuSelectedPositionToMove = null;
		ignorePositions.clear();
		ignorePieces.clear();
		int tries = 0;
		
		while (true) {
			try {
				/** Se uma pedra estiver sob risco de captura, e não houver movimento que tire
				 * isso, tentar posicionar uma pedra de forma que, após a pedra aliada ser capturada,
				 * essa outra pedra possa capturar a pedra adversária que a capturou, isso se após a
				 * captura ela ficar segura.
				 */
				PieceColor color = board.getCurrentColorTurn();
				if (anyPieceCouldBeCaptured(color) && !board.isChecked() &&
						testPossibleMoves(e -> !anyPieceCouldBeCaptured(color)) == null) {
							possibleMoves = new ArrayList<>();
							Boolean stop = false;
							for (Piece piece : board.getFriendlyPieceList())
								for (Piece opponentPiece : board.getPieceListByColor(color.getOppositeColor())) {
									if (opponentPiece.couldCapture(piece)) {
										try {
											tryToMoveTo(opponentPiece, piece);
											Board.cloneBoard(board, recBoard2);
											for (Piece piece3 : board.getPieceListByColor(color)) {
												PiecePosition originalPosition = new PiecePosition(piece3.getPosition());
												if (piece3.couldCapture(opponentPiece) &&
														!isIgnoredPosition(piece3, opponentPiece) &&
														ignorePieces.contains(piece3)) {
													try {
														tryToMoveTo(piece3, opponentPiece);
														if (board.pieceIsAtSafePosition(piece3) && !board.isChecked(piece3.getColor())) {
															ignorePieces.add(piece3);
															Board.cloneBoard(recBoard, board);
															stop = true;
															break;
														}
													}
													catch (Exception e)
														{ addIgnorePosition(piece, opponentPiece.getPosition()); }
													Board.cloneBoard(recBoard2, board);
												}
												else
													for (PiecePosition position : piece3.getPossibleMoves())
														if (!isIgnoredPosition(piece3, position) && ignorePieces.contains(piece3)) {
															try {
																tryToMoveTo(piece3, position);
																if (piece3.couldCapture(opponentPiece) &&
																	!isIgnoredPosition(piece3, opponentPiece) &&
																	ignorePieces.contains(piece3))
																		try {
																			tryToMoveTo(piece3, opponentPiece.getPosition());
																			if (board.pieceIsAtSafePosition(piece3) && !board.isChecked(piece3.getColor()))
																				possibleMoves.add(new PossibleMove(piece3, originalPosition, position));
																		}
																		catch (Exception e)
																			{ addIgnorePosition(piece3, opponentPiece.getPosition()); }
															}
															catch (Exception e)
																{ addIgnorePosition(piece3, position); }
															Board.cloneBoard(recBoard2, board);
														}
											}
										}
										catch (Exception e)
											{ addIgnorePosition(opponentPiece, piece.getPosition()); }
										Board.cloneBoard(recBoard, board);
									}
									if (stop)
										break;
								}
								if (stop)
									break;

							Board.cloneBoard(recBoard, board);
							if (!possibleMoves.isEmpty()) {
								choiceAPossibleMoveToDo(possibleMoves, recBoard, 1);
								return;
							}
				}
				
				if ((possibleMoves = testPossibleMoves()) != null) {
					choiceAPossibleMoveToDo(possibleMoves, recBoard, 1);
					return;
				}
			}
			catch (Exception e) {
				if (debugging)
					System.out.println("ERROR ON ACTION " + cpuLastChoice + " = " + (cpuSelectedPiece == null ? null : cpuSelectedPiece.getPosition()) + " -> " + cpuSelectedPositionToMove + " - "+ e.getMessage());
				/* Se na tentativa de definir a movimentação da pedra, lançar alguma
				 * exception, repetir a verificação, porém colocando a última posição
				 * tentada na lista de ignore.
				 */
				addIgnorePosition(cpuSelectedPiece, cpuSelectedPositionToMove);
				cpuSelectedPiece = null;
				cpuSelectedPositionToMove = null;
				Board.cloneBoard(recBoard, board);
			}
			if (++tries == 500)
				throw new GameException("Code got stucked trying to find the next move for CPU. Please, if you are able to, print the current board and send it to the developer.");
		}
	}	
	
	private void addIgnorePosition(Piece piece, PiecePosition position) {
		if (!ignorePositions.containsKey(piece))
			ignorePositions.put(piece, new ArrayList<>());
		ignorePositions.get(piece).add(position);
	}
	
	private Boolean isIgnoredPosition(Piece piece, PiecePosition position)
		{ return ignorePositions.containsKey(piece) && ignorePositions.get(piece).contains(position); }

	private Boolean isIgnoredPosition(Piece piece, Piece piece2)
		{ return ignorePositions.containsKey(piece) && ignorePositions.get(piece).contains(piece2.getPosition()); }

	/**
	 * Executa o movimento premeditado pela CPU. O método {@code doCpuSelectAPiece()} deve ser chamado para que a CPU possa selecionar uma pedra para mover
	 */
	public void doCpuMoveSelectedPiece() throws PieceSelectionException,PieceMoveException {
		validateCpuCommands();
		if (!board.pieceIsSelected())
			throw new PieceSelectionException("CPU not selected a piece. Call \".doCpuSelectAPiece()\" first.");
		board.movePieceTo(cpuSelectedPositionToMove);
	}
	
	/**
	 * Verifica se a CPU já selecionou uma pedra
	 */
	public Boolean cpuSelectedAPiece() {
		validateCpuCommands();
		return board.isCpuTurn() && board.pieceIsSelected();
	}

}

class PossibleMove implements Comparable<PossibleMove> {
	
	private Piece piece;
	private PiecePosition startPosition;
	private PiecePosition targetPosition;
	private long score;
	private int choice;
	
	public PossibleMove(Piece piece, PiecePosition startPosition, PiecePosition targetPosition) {
		this.piece = piece;
		this.startPosition = new PiecePosition(startPosition);
		this.targetPosition = new PiecePosition(targetPosition);
		score = 0;
		choice = 0;
	}

	public PossibleMove(Piece piece, PiecePosition targetPosition)
		{ this(piece, piece.getPosition(), targetPosition); }

	public List<String> getChoicesInfo() {
		List<String> infos = new ArrayList<>();
		String[] s = {
				"Pedra movida resultou em empate",
				"Pedra movida se colocou em risco por nada",
				"Pedra movida se colocou em risco após capturar uma pedra de menor valor",
				"Pedra movida se colocou em risco após capturar uma pedra de valor igual ou maior",
				"Pedra movida se livrou do risco de captura",
				"Pedra movida se livrou do risco de captura mesmo podendo ter capturado a pedra que a ameaçava",
				"Pedra movida resultou em checkmate",
				"Pedra movida resultou em captura segura",
				"Pedra movida resultou em check seguro",
				"Pedra movida resultou em check inseguro",
				"Peão movendo duas casas pelas colunas do meio",
				"Pedra movida está ameaçando pedra adversária, com a possibilidade de capturá-la em segurança",
				"Pedra movida está ameaçando pedra adversária de menor valor, sem a possibilidade de capturá-la em segurança",
				"Pedra movida está ameaçando pedra adversária de valor igual ou maior, sem a possibilidade de capturá-la em segurança",
				"Distância segura do Rei adversário",
				"Pedra movida resultou em Rei adversário com mais possibilidades de movimento",
				"Pedra movida resultou em Rei adversário com menos possibilidades de movimento",
				"Há pedras aliadas sob risco de captura",
				"Há pedras adversárias sob risco de captura",
				"A pedra movida está sob risco de captura",
				"Pedra movida deixou de cobrir pedra aliada em risco de captura"
		};
		for (int n = 1, i = 0; i < s.length && n <= choice; n += n, i++)
			if ((n & choice) > 0)
				infos.add(n + " - " + s[i] + " (" + score + ")");
		if (infos.isEmpty())
			infos.add("Nenhuma lógica retornada");
		return infos;
	}
	
	public void incChoice(int val) {
		if ((val & choice) == 0)
			choice += val;
	}

	public long getScore()
		{ return score; }
	
	public void setScore(long val)
		{ score = val; }
	
	public void decScore(long val)
		{ incScore(-val); }
	
	public void incScore(long val) {
		long l = score;
		score += val;
		if (val < 0 && score > l)
			score = -Long.MIN_VALUE;
		else if (val > 0 && score < l)
			score = Long.MIN_VALUE;
	}

	public Piece getPiece()
		{ return piece; }
	
	public void setPiece(Piece piece)
		{ this.piece = piece; }
	
	public PiecePosition getStartPosition()
		{ return startPosition; }

	public PiecePosition getTargetPosition()
		{ return targetPosition; }

	public void setPosition(PiecePosition position) 
		{ this.targetPosition = position; }
	
	@Override
	public int compareTo(PossibleMove m)
		{ return m.getScore() < getScore() ? -1 : m.getScore() > getScore() ? 1 : 0; }
		
}