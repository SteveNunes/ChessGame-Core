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
import gameutil.Position;
import piece.Piece;
import util.Misc;

public class ChessAI {

	private Boolean debugging = false;
	private Board board = null;
	private int cpuLastChoice;
	private Position cpuSelectedPositionToMove;
	private Map<Piece, List<Position>> ignorePositions;
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
		cpuSelectedPositionToMove = null;
		cpuLastChoice = -1;
		ignorePositions = new HashMap<>();
		ignorePieces = new ArrayList<>();
	}
	
	public Position cpuSelectedTargetPosition()
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
		if (possibleMoves.isEmpty())
			return;
		// Ordena os possible moves pelo score de pecas e score de ameaças, para usar a movimentacao melhor pontuada
		possibleMoves.sort((p1, p2) -> p1.compareTo(p2));
		List<PossibleMove> possibleMoves2 = null;
		if (debugging)
			possibleMoves2 = new ArrayList<>(possibleMoves);
		for (int n = 1; n < possibleMoves.size(); n++)
			if (possibleMoves.get(n).getScore() != possibleMoves.get(0).getScore())
				possibleMoves.remove(n--);
		int pos = new SecureRandom().nextInt(possibleMoves.size());
		cpuLastChoice = cpuChoice;
		cpuSelectedPositionToMove = possibleMoves.get(pos).getTargetPosition();
		board.cpuSelectedPiece(possibleMoves.get(pos).getPiece());
		if (debugging) {
			for (PossibleMove pm : possibleMoves2)
				if (Misc.alwaysTrue() ||  pm.getPiece().isQueen()) {
					if (pm == possibleMoves.get(pos))
						System.out.println("** CURRENTLY CHOOSEN **");
					for (String s : pm.getChoicesInfo())
						System.out.println("* " + s);
					System.out.println(pm.getPiece().getInfo() + " -> " + pm.getTargetPosition() + " (Score: " + pm.getScore() + ")");
					System.out.println("--------------");
				}
			System.out.println("ACTION " + cpuLastChoice + " = " + board.getSelectedPiece().getInfo() + " -> " + possibleMoves.get(pos).getTargetPosition() + " (Score: " + possibleMoves.get(pos).getScore() + ")");
			for (String s : possibleMoves.get(pos).getChoicesInfo())
				System.out.println("- " + s);
			System.out.println();
		}
	}

	private void tryToMoveTo(Position sourcePos, Position targetPos) {
		cpuSelectedPositionToMove = new Position(targetPos);
		board.movePieceTo(sourcePos, targetPos);
	}

	private void tryToMoveTo(Piece sourcePiece, Position targetPos)
		{ tryToMoveTo(sourcePiece.getPosition(), targetPos); }

	@SuppressWarnings("unused")
	private void tryToMoveTo(Position sourcePos, Piece targetPiece)
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
		List<Position> otherKingThreatenPositions = new ArrayList<>(opponentKing.getPossibleSafeMoves());
		float opponentInsightScore = 0;
		float friendlyInsightScore = 0;
		int repeatedMovesBefore = board.getTotalRepeatedMoves();
		for (Piece piece : pieces) {
			ignorePositions.clear();
			List<Position> positions = new ArrayList<>(piece.getPossibleMoves());
			for (Position position : positions)
				if (!isIgnoredPosition(piece, position)) {
					try { // Ultimo valor usado: 134217728
						List<Piece> piecesThatCanCaptureTheMovedPiece = getListOfPiecesThatCouldCaptureThis(piece);
						Position positionBefore = new Position(piece.getPosition());
						tryToMoveTo(piece, position);
						PossibleMove possibleMove = new PossibleMove(piece, positionBefore, position);
						possibleMoves.add(possibleMove);
						if (board.getTotalRepeatedMoves() > repeatedMovesBefore )
							possibleMove.decScore(1);
						if (new SecureRandom().nextInt(2) == 0)
							possibleMove.incScore((long)piece.getIntTypeValue());
						if (predicate.test(possibleMove)) {
							Board b = board.newClonedBoard();
							Boolean checkMate = board.checkMate() || board.deadlyKissMate();
							if (board.drawGame()) {
								// Se a pedra movida resultou em um empate
								if (board.isDrawByBareKings())
									possibleMove.incScore(2097152, -Long.MAX_VALUE);
								else if (board.isDrawByFiftyMoveRule())
									possibleMove.incScore(4194304, -Long.MAX_VALUE);
								else if (board.isDrawByInsufficientMatingMaterial())
									possibleMove.incScore(8388608, -Long.MAX_VALUE);
								else if (board.isDrawByStalemate())
									possibleMove.incScore(16777216, -Long.MAX_VALUE);
								else if (board.isDrawByThreefoldRepetition())
									possibleMove.incScore(33554432, -Long.MAX_VALUE);
							}
							// Se a pedra movida resultou em um checkmate
							if (checkMate)
								possibleMove.incScore(board.deadlyKissMate() ? 1 : 64, Long.MAX_VALUE);
							else {
								if (!board.pieceIsAtSafePosition(piece)) { // Se a pedra movida está sob risco de captura...
									// Ela não realizou captura no último turno (Ficou em risco por nada)
									if (!board.pieceWasCaptured())
										possibleMove.decScore(2, (long)(Long.MAX_VALUE / 6 * piece.getTypeValue()));
									// Ela capturou uma pedra de menor valor (A troca não valeu a pena)
									else if (board.getLastCapturedPiece().getTypeValue() < piece.getTypeValue())
										possibleMove.decScore(4, (long)(Long.MAX_VALUE / 6 * (piece.getTypeValue() - board.getLastCapturedPiece().getTypeValue())));
									else // Ela capturou uma pedra de valor igual ou maior (A troca valeu a pena)
										possibleMove.incScore(8, (long)(Long.MAX_VALUE / 9 * board.getLastCapturedPiece().getTypeValue()));
								}
								else if (!safePiecesBefore.contains(piece)) {
									// Se a pedra movida não estava segura antes, e agora está...
									
									/* SE havia mais de uma pedra ameaçando a pedra movida, incrementa
									 * o score, forçando a pedra a de fato permanecer onde parou
									 */
									if (piecesThatCanCaptureTheMovedPiece.size() > 1)
										possibleMove.incScore(16, (long)(Long.MAX_VALUE / 18 * piece.getTypeValue()));
									else {
										Board.cloneBoard(recBoard, board);
										Piece opponentPiece = piecesThatCanCaptureTheMovedPiece.get(0);
										if (piece.couldCapture(opponentPiece)) {
											/* SE havia apenas 1 pedra ameaçando a pedra movida, e a pedra movida poderia
											 * ter capturado ela, testa a captura para ver se após a captura ela ficaria
											 * segura ou não
											 */
											tryToMoveTo(piece, opponentPiece.getPosition());
											/**
											 * Se ela de fato poderia ter capturado em segurança, a pedra que a ameaçava,
											 * e não o fez, decrementa o score, para evitar que ela faça isso
											 */
											if (board.pieceIsAtSafePosition(piece))
													possibleMove.decScore(32, (long)(Long.MAX_VALUE / 7 * piecesThatCanCaptureTheMovedPiece.get(0).getTypeValue()));
											/**
											 * Se ela de fato poderia ter capturado em segurança, a pedra que a ameaçava,
											 * mas poderia ser capturada logo em seguida, e a troca não valeria a pena,
											 * incrementa o score, para evitar que ela faça isso
											 */
											else if (piece.strongerThan(opponentPiece))
												possibleMove.incScore(67108864, (long)(Long.MAX_VALUE / 9 * board.getLastCapturedPiece().getTypeValue()));
											/**
											 * Se ela seria capturada após a captura, mas isso seria por uma troca
											 * justa, decrementa levemente o score
											 */
											else
												possibleMove.decScore(134217728, (long)(Long.MAX_VALUE / 6 * (piece.getTypeValue() - board.getLastCapturedPiece().getTypeValue())));
										}
										Board.cloneBoard(b, board);
									}
								}
							}
							// Se a pedra movida capturou uma pedra adversária em segurança
							if (board.pieceWasCaptured() && board.pieceIsAtSafePosition(piece))
								possibleMove.incScore(128, (long)(Long.MAX_VALUE / 6 * board.getLastCapturedPiece().getTypeValue()));
							// Se a pedra movida resultou em um check (Seguro ou não)
							if (!checkMate && board.isChecked())
								possibleMove.incScore(board.pieceIsAtSafePosition(piece) ? 256 : 512, 
										board.pieceIsAtSafePosition(piece) ? Long.MAX_VALUE / 2 : (-Long.MAX_VALUE / 2));
							// Se a pedra movida for um peão que andou 2 tiles pelas colunas no meio e está seguro
							if (piece.isSameTypeOf(PieceType.PAWN) && board.pieceIsAtSafePosition(piece) &&
									Math.abs(positionBefore.getY() - position.getY()) > 1 &&
									(piece.getPosition().getX() == 3 || piece.getPosition().getX() == 4))
										possibleMove.incScore(1024, Long.MAX_VALUE / 150);
							// Gera score negativo baseado em pedras aliadas que estão sob risco de captura
							friendlyInsightScore = 0;
							for (Piece friendlyPiece : board.getPieceListByColor(color))
								if (!board.pieceIsAtSafePosition(friendlyPiece)) {
									if (friendlyPiece != piece) {
										if (safePiecesBefore.contains(piece)) {
											// Se não for a pedra movida, e a pedra movida estava segura antes de mover
											friendlyInsightScore += friendlyPiece.getTypeValue();
											// Testa se a pedra movida não está cobrindo a pedra em risco de captura (deixou de cobrir a toa)
											friendlyPiece.setColor(color.getOppositeColor());
											if (!piece.canMoveToPosition(friendlyPiece.getPosition())) {
												board.removePiece(piece);
												board.addPiece(positionBefore, piece);
												// Se ela estava cobrindo e não está mais...
												if (piece.canMoveToPosition(friendlyPiece.getPosition()))
													possibleMove.decScore(1048576, (long)(Long.MAX_VALUE / 7 * friendlyPiece.getTypeValue()));
												Board.cloneBoard(recBoard, board);
											}
											friendlyPiece.setColor(color);
										}
									}
									/* SE a pedra movida está em risco de captura, mas não estava antes,
									 * e não houve uma captura ao mover, decrementa o score (se clocou em risco á toa)
									 */
									else if (!checkMate && !board.pieceWasCaptured())
										possibleMove.decScore(524288, (long)(Long.MAX_VALUE / 6 * piece.getTypeValue()));
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
											/* Se após a captura, a pedra capturante ficou segura, incrementa
											 * o score no valor da pedra adversária que pode ser capturada
											 */
											if (board.pieceIsAtSafePosition(piece)) 
												possibleMove.incScore(2048, (long)(Long.MAX_VALUE / 7 * opponentPiece.getTypeValue()));
											else { // Se após a captura, a pedra capturante não ficar segura...
												/* Se a pedra capturada por último for de MENOR valor que a pedra capturante,
												 * incrementa o score baseado no (VALOR DA PEDRA CAPTURADA - VALOR DA PEDRA
												 * CAPTURANTE), evitando assim que a CPU suicide pedras capturando outras de
												 * menor valor que a pedra capturante.
												 */
												if (piece.strongerThan(lastCaptured))
														possibleMove.incScore(4096, (long)(Long.MAX_VALUE / 9 * (piece.getTypeValue() - lastCaptured.getTypeValue())));
												/* Se a pedra capturada por último for de MAIOR valor que a pedra, incrementa
												 * o score baseado no (VALOR DA PEDRA CAPTURADA - VALOR DA PEDRA CAPTURANTE)
												 */
												else
													possibleMove.incScore(8192, (long)(Long.MAX_VALUE / 14 * ((lastCaptured.getTypeValue() - piece.getTypeValue()) + 0.1)));
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
											int disX = Math.abs(opponentPiece.getPosition().getX() - position.getX()); 
											int disY = Math.abs(opponentPiece.getPosition().getY() - position.getY());
											possibleMove.decScore(16384, disX > disY ? disX : disY);
										}
										if (!board.pieceWasCaptured() && !otherKingThreatenPositions.isEmpty()) { // Se o ultimo movimento não foi uma captura...
											// Se o rei adversário ficou com mais possibilidade de movimentos agora do que antes
											if (otherKingThreatenPositions.size() > opponentPiece.getPossibleSafeMoves().size())
												possibleMove.decScore(32768, Long.MAX_VALUE / 12);
											// Se o rei adversário ficou com menos possibilidade de movimentos agora do que antes
											else if (otherKingThreatenPositions.size() < opponentPiece.getPossibleSafeMoves().size())
												possibleMove.incScore(65536, Long.MAX_VALUE / 12);
										}
									}
							}
							// Se há pedras aliadas sob risco de captura, decrementa o score baseado no total de pedras aliadas que estão sob risco
							if (friendlyInsightScore > 0)
								possibleMove.decScore(131072, (long)(friendlyInsightScore * Integer.MAX_VALUE * 10));
							// Se há pedras adversárias sob risco de captura, incrementa o score baseado no total de pedras adversárias que estão sob risco
							if (opponentInsightScore > 0)
								possibleMove.incScore(262144, (long)(opponentInsightScore * Integer.MAX_VALUE));
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

	private List<Piece> getListOfPiecesThatCouldCaptureAPieceAt(Position position, PieceColor color) {
		List<Piece> pieces = board.getPieceListByColor(color.getOppositeColor(), p -> p.canMoveToPosition(position));
		return pieces.isEmpty() ? null : pieces;
	}

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
		cpuSelectedPositionToMove = null;
		ignorePositions.clear();
		ignorePieces.clear();
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
										Position originalPosition = new Position(piece3.getPosition());
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
											for (Position position : piece3.getPossibleMoves())
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
					Board.cloneBoard(recBoard, board);
					if (!possibleMoves.isEmpty()) {
						choiceAPossibleMoveToDo(possibleMoves, recBoard, 1);
						return;
					}
		}
		
		if (!(possibleMoves = testPossibleMoves()).isEmpty()) {
			choiceAPossibleMoveToDo(possibleMoves, recBoard, 1);
			return;
		}
	}	
	
	private void addIgnorePosition(Piece piece, Position position) {
		if (!ignorePositions.containsKey(piece))
			ignorePositions.put(piece, new ArrayList<>());
		ignorePositions.get(piece).add(position);
	}
	
	private Boolean isIgnoredPosition(Piece piece, Position position)
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
	private Position startPosition;
	private Position targetPosition;
	private long score;
	private int choice;
	private Map<Integer, Long> scoreByChoice;
	
	public PossibleMove(Piece piece, Position startPosition, Position targetPosition) {
		this.piece = piece;
		this.startPosition = new Position(startPosition);
		this.targetPosition = new Position(targetPosition);
		score = 0;
		choice = 0;
		scoreByChoice = new HashMap<>();
	}

	public PossibleMove(Piece piece, Position targetPosition)
		{ this(piece, piece.getPosition(), targetPosition); }

	public List<String> getChoicesInfo() {
		List<String> infos = new ArrayList<>();
		String[] s = {
				"Pedra movida resultou em mate do beijo fatal",
				"Pedra movida se colocou em risco por nada",
				"Pedra movida se colocou em risco após capturar uma pedra de menor valor",
				"Pedra movida se colocou em risco após capturar uma pedra de valor igual ou maior",
				"Pedra movida se livrou do risco de captura",
				"Pedra movida se livrou do risco de captura mesmo podendo ter capturado em segurança a pedra que a ameaçava",
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
				"Pedra movida deixou de cobrir pedra aliada em risco de captura",
				"Pedra movida resultou em empate por \"Bare Kings\"",
				"Pedra movida resultou em empate por \"Fifty-move rule\"",
				"Pedra movida resultou em empate por \"Insufficient Mating Material\"",
				"Pedra movida resultou em empate por \"Stalemate\"",
				"Pedra movida resultou em empate por \"Threefold Repetition\"",
				"Pedra movida se livrou do risco de captura, mesmo podendo capturar a pedra que a ameaçava, porque poderia ser capturada logo em seguida e a pedra capturada era de menor valor (não valeria a pena a troca)",
				"Pedra movida se livrou do risco de captura, mesmo podendo capturar a pedra que a ameaçava, porque poderia ser capturada logo em seguida, mesmo que a pedra capturada era de valor igual ou maior (valeria a pena a troca)"
		};
		for (int n = 1, i = 0; i < s.length && n <= choice; n += n, i++)
			if ((n & choice) > 0)
				infos.add(n + " - " + s[i] + " (" + (scoreByChoice.containsKey(n) ? scoreByChoice.get(n) : "???") + ")");
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
	
	public void incScore(int choice, long val) {
		incChoice(choice);
		incScore(val);
		scoreByChoice.put(choice, val);
	}

	public void decScore(int choice, long val)
		{ incScore(choice, -val); }
	
	public Piece getPiece()
		{ return piece; }
	
	public void setPiece(Piece piece)
		{ this.piece = piece; }
	
	public Position getStartPosition()
		{ return startPosition; }

	public Position getTargetPosition()
		{ return targetPosition; }

	public void setPosition(Position position) 
		{ this.targetPosition = position; }
	
	@Override
	public int compareTo(PossibleMove m)
		{ return m.getScore() < getScore() ? -1 : m.getScore() > getScore() ? 1 : 0; }
		
}