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
		List<PossibleMove> list = new ArrayList<>(); 
		Board recBoard = board.newClonedBoard();
		List<Piece> pieces = new ArrayList<>(board.sortPieceListByPieceValue(board.getPieceListByColor(color, p -> !ignorePieces.contains(p))));
		List<Piece> safePiecesBefore = new ArrayList<>();
		for (Piece piece : board.getPieceList())
			if (board.isSafeSpot(piece))
				safePiecesBefore.add(piece);
		for (Piece piece : pieces) {
			ignorePositions.clear();
			List<PiecePosition> positions = new ArrayList<>(piece.getPossibleMoves());
			PiecePosition positionBefore = new PiecePosition(piece.getPosition());
			for (PiecePosition position : positions)
				if (!isIgnoredPosition(piece, position)) {
					try {
						tryToMoveTo(piece, position);
						PossibleMove possibleMove = new PossibleMove(piece, positionBefore, position);
						if (predicate.test(possibleMove)) {
							Board b = board.newClonedBoard();
							long opponentInsightScore = 0;
							int totalOpponentInsights = 0;
							if (board.pieceWasCaptured() && board.getLastMovedPiece() == piece) { 
								// Se houve captura e a pedra capturante for a pedra atual
								if (board.isSafeSpot(piece)) { 
									// Se a pedra atual está segura, incrementa o score baseado no valor da pedra capturada
									possibleMove.incScore((long)(board.getLastCapturedPiece().getTypeValue() * 10) * Integer.MAX_VALUE * 10);
									possibleMove.incChoice(1);
								}
								else if (board.getLastCapturedPiece().strongerOrSameThan(piece)) {
									/* Se a pedra capturada era de maior valor ou igual, incrementa o score, mas
									 * não tanto como seria se a pedra capturante estivesse em segurança.
									 */
									possibleMove.incScore((long)(((piece.getTypeValue() - board.getLastCapturedPiece().getTypeValue()) + 0.1) * 10) * Integer.MAX_VALUE * 5);
									possibleMove.incChoice(2);
								}
								else {
									/* Se a pedra capturada era de menor valor que a pedra capturante,
									 * decrementa o score baseado na diferença entre os valores da pedra
									 * capturante e pedra capturada (Isso evita com que a CPU perca uma
									 * pedra de maior valor, capturando uma de menor valor)
									 */
									possibleMove.decScore(Long.MAX_VALUE - ((long)(((board.getLastCapturedPiece().getTypeValue() - piece.getTypeValue())) * 10) * Integer.MAX_VALUE * 10));
									possibleMove.incChoice(4);
								}
							}
							for (Piece opponentPiece : board.getPieceListByColor(color.getOppositeColor())) {
								if (piece.couldCapture(opponentPiece)) {
									opponentInsightScore += (long)(opponentPiece.getTypeValue() * 10);
									totalOpponentInsights++;
								}
								for (Piece friendlyPiece : board.getPieceListByColor(color))
									if (opponentPiece.couldCapture(friendlyPiece)) {
										possibleMove.decScore((long)(friendlyPiece.getTypeValue() * 10) * Integer.MAX_VALUE);
										possibleMove.incChoice(4096);
									}
								if (!board.isSafeSpot(opponentPiece) && safePiecesBefore.contains(opponentPiece)) {
									// Se no turno testado alguma pedra adversária ficou sob ameaça de captura
									if (piece.couldCapture(opponentPiece) && !opponentPiece.couldCapture(piece) && board.isSafeSpot(piece)) {
										// Se uma pedra atual pode capturar a pedra adversária em segurança...
										try {
											Piece lastCaptured = board.getLastCapturedPiece();
											tryToMoveTo(piece, opponentPiece.getPosition()); // Simula a captura para ver como vai ficar a situação da pedra capturante após a captura
											if (board.isSafeSpot(piece)) { 
												/* Se após a captura, a pedra capturante ficou segura, incrementa
												 * o score no valor da pedra adversária que pode ser capturada
												 */
												possibleMove.incScore((long)(opponentPiece.getTypeValue() * 10) * Integer.MAX_VALUE * 5);
												possibleMove.incChoice(8);
											}
											else { // Se após a captura, a pedra capturante não ficar segura...
												if (piece.strongerThan(lastCaptured)) {
													/* Se a pedra capturada por último for de MENOR valor que a pedra,
													 * decrementa o score baseado no valor da pedra capturada - pedra
													 * capturante, evitando assim que a CPU suicide pedras capturando
													 * outras de menor valor que a pedra capturante.
													 */
													possibleMove.decScore((long)(((piece.getTypeValue() - lastCaptured.getTypeValue())) * 10) * Integer.MAX_VALUE * 5);
													possibleMove.incChoice(32);
												}
												else {
													/* Se a pedra capturada por último for de MAIOR valor que a pedra,
													 * incrementa o score baseado no valor da pedra capturada - pedra
													 * capturante.
													 */
													possibleMove.incScore((long)(((lastCaptured.getTypeValue() - piece.getTypeValue()) + 0.1) * 10) * Integer.MAX_VALUE * 5);
													possibleMove.incChoice(16);
												}
											}
										}
										catch (Exception e) {}
										Board.cloneBoard(b, board);
									}
									else if (board.getLastMovedPiece() == piece && opponentPiece.couldCapture(piece)) { 
										// Se a pedra ameaçada de captura puder capturar a pedra aliada movida, decrementa o score baseado no valor da pedra aliada
										possibleMove.decScore((long)((piece.getTypeValue()) * 10) * Integer.MAX_VALUE * 10);
										possibleMove.incChoice(64);
									}
									if (opponentPiece.isSameTypeOf(PieceType.KING)) {
										if (!piece.isSameTypeOf(PieceType.PAWN) && board.isSafeSpot(piece)) {
											/* Incrementa o score baseado na distância da pedra atual para a pedra do rei,
											 * desde que essa posição nao coloque a pedra em perigo (apenas não-peôes)
											 */
											int disX = Math.abs(piece.getColumn() - position.getColumn()); 
											int disY = Math.abs(piece.getRow() - position.getRow());
											possibleMove.decScore(disX > disY ? disX : disY);
											possibleMove.incChoice(128);
										}
										/* Decrementa o score se a pedra atual estava posicionada de forma que a linha
										 * de captura dela abrangia algum dos possiveis tiles de movimento do rei
										 * adversário, e agora não esta mais, e isso não aconteceu pelo fato da pedra
										 * ter capturado uma outra pedra adversária (Tentar manter paradas as pedras
										 * que estão bloqueando o caminho do Rei)
										 */
										if (opponentPiece.canMoveToPosition(positionBefore) && board.isFreeSlot(positionBefore) &&
												!opponentPiece.canMoveToPosition(position) && board.isFreeSlot(position) &&
												(!board.pieceWasCaptured() || board.getLastMovedPiece() != piece)) {
													possibleMove.setScore(Long.MAX_VALUE / 2);
													possibleMove.incChoice(256);
										}
									}
								}
							}
							for (Piece piece2 : board.getPieceListByColor(color))
								if (!board.isSafeSpot(piece2) && safePiecesBefore.contains(piece2)) {
									/* Se pedra que estava segura anteriormente, estiver em perigo no turno atual,
									 * decrementar o score 'pouco ou muito', dependendo se a pedra movida for de
									 * valor menor que a pedra que ficou desprotegida
									 */
									if (piece2.strongerThan(board.getLastMovedPiece())) {  
										possibleMove.decScore((long)(piece2.getTypeValue() * 10));
										possibleMove.incChoice(512);
									}
									else {
										possibleMove.decScore(((long)(((piece2.getTypeValue() - board.getLastMovedPiece().getTypeValue()) + 0.1) * 10)));
										possibleMove.incChoice(1024);
									}
								}
							if (totalOpponentInsights > 0) {
								possibleMove.incScore((long)Math.pow(opponentInsightScore, totalOpponentInsights));
								possibleMove.incChoice(2048);
							}
							if (piece.isSameTypeOf(PieceType.PAWN) && board.isSafeSpot(position, color.getOppositeColor())) {
								if (Math.abs(positionBefore.getRow() - position.getRow()) > 1) {
									/* Se em alguma situacao só restar movimentos com peôes, prioriza
									 * os que podem andar 2 tiles, de preferencia pelo meio
									 */
									possibleMove.setScore(Integer.MAX_VALUE - (piece.getColumn() == 3 || piece.getColumn() == 4 ? 0 : 1));
									possibleMove.incChoice(piece.getColumn() == 3 || piece.getColumn() == 4 ? 16384 : 8192);
								}
								else {
									possibleMove.incScore(Integer.MAX_VALUE - 2);
									possibleMove.incChoice(32768);
								}
							}
							if (board.drawGame()) { // Da um score negativo caso a última jogada testada resulte em um empate
								possibleMove.setScore(-Long.MAX_VALUE);
								possibleMove.incChoice(65536);
							}
							else if (board.checkMate()) { // Incrementa o score se a última jogada testada resultou num checkMate
								possibleMove.setScore(Long.MAX_VALUE);
								possibleMove.incChoice(131072);
							}
							else if (board.isChecked()) { // Decrementa o score se a última jogada testada resultou num check seguro
								possibleMove.incScore(board.isSafeSpot(piece) ? Long.MAX_VALUE / 3 : -(Long.MAX_VALUE / 3));
								possibleMove.incChoice(board.isSafeSpot(piece) ? 262144 : 524288);
							}
							list.add(possibleMove);
						}
					}
					catch (Exception e)
						{ addIgnorePosition(piece, position); }
					Board.cloneBoard(recBoard, board);
				}
		}
			
		return list.isEmpty() ? null : list;
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
														if (board.isSafeSpot(piece3) && !board.isChecked()) {
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
																			if (board.isSafeSpot(piece3) && !board.isChecked())
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
				"Captura segura de pedra adversária",
				"Captura insegura de pedra adversária de valor MAIOR ou IGUAL",
				"Captura insegura de pedra adversária de valor MENOR",
				"Colocando pedra adversária em risco de captura, podendo capturá-la em segurança",
				"Colocando pedra adversária de valor MAIOR que a pedra atual em risco de captura, porém se capturá-la, irá ficar na mira de outra pedra adversária",
				"Colocando pedra adversária de valor MENOR que a pedra atual em risco de captura, porém se capturá-la, irá ficar na mira de outra pedra adversária",
				"Ficou sob risco de captura de pedra adversária",
				"Distância segura do Rei adversário",
				"Pedra deixou de ameaçar uma das posições possíveis do Rei adversário",
				"Desprotegeu pedra de MAIOR valor que a pedra movida",
				"Desprotegeu pedra de MENOR valor que a pedra movida",
				"Deixou pedras adversárias sob risco de captura",
				"Ficou com pedras aliadas sob risco de captura",
				"Avançando 2 tiles com peão",
				"Avançando 2 tiles com peão pelas colunas do meio",
				"Avançando com peão",
				"Jogo terminou em empate",
				"Checkmate",
				"Check seguro",
				"Check inseguro"
		};
		for (int n = 1, i = 0; i < s.length && n <= choice; n += n, i++)
			if ((n & choice) > 0)
				infos.add(n + " - " + s[i]);
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

	public void decScore(long val) {
		if (val < 0 && (score -= val) > 0)
			score = Long.MIN_VALUE;
	}

	public void incScore(long val) {
		if (val > 0 && (score += val) < 0)
			score = Long.MAX_VALUE;
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