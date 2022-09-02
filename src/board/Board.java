package board;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import enums.ChessPlayMode;
import enums.PieceColor;
import enums.PieceType;
import exceptions.BoardException;
import exceptions.CancelSelectionException;
import exceptions.CheckException;
import exceptions.GameException;
import exceptions.InvalidMoveException;
import exceptions.InvalidPositionException;
import exceptions.PieceMoveException;
import exceptions.PieceSelectionException;
import exceptions.PromotionException;
import piece.Piece;
import piece.PiecePosition;
import pieces.Bishop;
import pieces.King;
import pieces.Knight;
import pieces.Pawn;
import pieces.Queen;
import pieces.Rook;

public class Board {

	private static List<Board> undoBoards = new ArrayList<>();
	private static int undoIndex = -1;

	private ChessPlayMode playMode;
	private Map<Piece, Integer> movedTurns;
	private Boolean lastMoveWasEnPassant;
	private Boolean lastMoveWasCastling;
	private int turns;
	private List<Piece> capturedPieces;
	private Piece[][] board;
	private Piece selectedPiece;
	private Piece enPassantPiece;
	private Piece lastCapturedPiece;
	private Piece lastMovedPiece;
	private Piece promotedPiece;
	private PieceColor currentColorTurn;
	private Boolean boardWasValidated;
	private Boolean drawGame;
	private ChessAI chessAI;
	private List<String> lastBoards;
	private int repeats;

	/**
	 * Construtor padrão 
	 */
	public Board()
		{ this(8,8); }

		/**
	 * Construtor que aceita como parâmetro o tamanho do tabuleiro
	 */
	public Board(int rows, int columns) {
		board = new Piece[rows][columns];
		capturedPieces = new ArrayList<>();
		movedTurns = new HashMap<>();
		playMode = ChessPlayMode.PLAYER_VS_PLAYER;
		reset(); 
	}
	
	/**
	 * Reseta o tabuleiro atual
	 */
	public void reset() {
		turns = 0;
		repeats = 0;
		boardWasValidated = false;
		lastMoveWasEnPassant = false;
		lastMoveWasCastling = false;
		drawGame = false;
		lastCapturedPiece = null;
		lastMovedPiece = null;
		selectedPiece = null;
		enPassantPiece = null;
		promotedPiece = null;
		currentColorTurn = PieceColor.WHITE;
		capturedPieces.clear();
		movedTurns.clear();
		lastBoards = new ArrayList<>();
		resetBoard(board);
	}
	
	/**
	 * Retorna o modo de jogo atual
	 */
	public ChessPlayMode getPlayMode()
		{ return playMode; }

	/**
	 * Define o modo de jogo
	 */
	public void setPlayMode(ChessPlayMode mode) {
		if (!boardWasValidated)
			throw new BoardException("You can't do it after validated the board. Call this metod before validating the board.");
		this.playMode = mode;
	}

	static void cloneBoard(Board sourceBoard, Board targetBoard) {
		validateNullVar(sourceBoard, "sourceBoard");
		validateNullVar(targetBoard, "targetBoard");
		for (int y = 0; y < sourceBoard.board.length; y++)
			for (int x = 0; x < sourceBoard.board[y].length; x++)
				if ((targetBoard.board[y][x] = sourceBoard.board[y][x]) != null)
					targetBoard.board[y][x].setPosition(y, x);
		targetBoard.lastBoards = new ArrayList<>(sourceBoard.lastBoards);
		targetBoard.drawGame = sourceBoard.drawGame;
		targetBoard.lastMovedPiece = sourceBoard.lastMovedPiece;
		targetBoard.promotedPiece = sourceBoard.promotedPiece;
		targetBoard.boardWasValidated = sourceBoard.boardWasValidated;
		targetBoard.lastCapturedPiece = sourceBoard.lastCapturedPiece;
		targetBoard.lastMoveWasEnPassant = sourceBoard.lastMoveWasEnPassant;
		targetBoard.lastMoveWasCastling = sourceBoard.lastMoveWasCastling;
		targetBoard.selectedPiece = sourceBoard.selectedPiece;
		targetBoard.enPassantPiece = sourceBoard.enPassantPiece;
		targetBoard.currentColorTurn = sourceBoard.currentColorTurn;
		targetBoard.capturedPieces = new ArrayList<>(sourceBoard.capturedPieces);
		targetBoard.turns = sourceBoard.turns;
		targetBoard.repeats = sourceBoard.repeats;
		targetBoard.movedTurns.clear();
		for (Piece piece : sourceBoard.movedTurns.keySet()) {
			piece.setMovedTurns(sourceBoard.movedTurns.get(piece));
			targetBoard.movedTurns.put(piece, sourceBoard.movedTurns.get(piece));
		}
	}

	Board newClonedBoard() {
		Board board = new Board();
		cloneBoard(this, board);
		return board;
	}
	
	/**
	 * Retorna a instância atual da classe responsável por cuidar das jogadas da CPU
	 */
	public ChessAI getChessAI()
		{ return chessAI; }
	
	/**
	 * Verifica se alguma pedra foi capturada após a última jogada
	 */
	public Boolean pieceWasCaptured()
		{ return lastCapturedPiece != null; }

	/**
	 * Retorna a última pedra movida
	 */
	public Piece getLastMovedPiece()
		{ return lastMovedPiece; }

	/**
	 * Retorna a última pedra capturada
	 */
	public Piece getLastCapturedPiece()
		{ return lastCapturedPiece; }

	/**
	 * Verifica se a última jogada foi uma captura usando o movimento especial "En Passant"
	 */
	public Boolean lastMoveWasEnPassant()
		{ return lastMoveWasEnPassant; }

	/**
	 * Verifica se a última jogada foi o movimento especial "Castling"
	 */
	public Boolean lastMoveWasCastling()
		{ return lastMoveWasCastling; }

	/**
	 * Verifica se é possivel usar o método {@code undoMove()}
	 */
	public Boolean canUndoMove()
		{ return playMode == ChessPlayMode.PLAYER_VS_PLAYER && undoIndex > 0; }
	
	/**
	 * Verifica se é possivel usar o método {@code redoMove()}
	 */
	public Boolean canRedoMove()
		{ return playMode == ChessPlayMode.PLAYER_VS_PLAYER && undoIndex + 1 < undoBoards.size(); }

	private void saveBoardForUndo() {
		Board saveBoard = newClonedBoard();
		undoIndex = undoBoards.size();
		undoBoards.add(undoIndex, saveBoard);
	}

	/**
	 * Volta o tabuleiro atual no total de jogadas informadas
	 */
	public void undoMoves(int totalUndoMoves) {
		boardWasValidated();
		if (totalUndoMoves < 1)
			throw new GameException("totalUndoMoves must be higher than 0");
		if (undoIndex == 0)
			throw new GameException("No available undo moves");
		while (--totalUndoMoves >= 0 && undoIndex - 1 >= 0) {
			if (--undoIndex == 0 || totalUndoMoves == 0)
				cloneBoard(undoBoards.get(undoIndex), this);
		}
	}
	
	/**
	 * Volta o tabuleiro atual em 1 jogada
	 */
	public void undoMove() {
		boardWasValidated();
		undoMoves(1);
	}
	
	/**
	 * Desfaz o "Voltar o tabuleiro atual" no total de jogadas informadas
	 */
	public void redoMoves(int totalRedoMoves) {
		boardWasValidated();
		if (totalRedoMoves < 1)
			throw new GameException("totalRedoMoves must be higher than 0");
		if (undoIndex + 1 == undoBoards.size())
			throw new GameException("No available redo moves");
		while (--totalRedoMoves >= 0 && undoIndex + 1 < undoBoards.size()) {
			if (++undoIndex == undoBoards.size() - 1 || totalRedoMoves == 0)
				cloneBoard(undoBoards.get(undoIndex), this);
		}
	}
	
	/**
	 * Desfaz o "Voltar o tabuleiro atual" em 1 jogada
	 */
	public void redoMove() { 
		boardWasValidated();
		redoMoves(1);
	}
	
	/**
	 * Verifica se o turno atual é da CPU, se a CPU estiver jogando
	 */
	public Boolean isCpuTurn() {
		return playMode != ChessPlayMode.PLAYER_VS_PLAYER &&
			(playMode == ChessPlayMode.CPU_VS_CPU || currentColorTurn == PieceColor.BLACK); 
	}
	
	private Boolean checkPieceCount(PieceType type, PieceColor color, Predicate<Integer> predicate)
		{ return predicate.test(getPieceList(color, p -> type == null || p.getType() == type).size()); }

	private Boolean checkPieceCount(PieceType type, Predicate<Integer> predicate)
		{ return checkPieceCount(type, null, predicate); }

	private Boolean checkPieceCount(PieceColor color, Predicate<Integer> predicate)
		{ return checkPieceCount(null, color, predicate); }

	private Boolean checkPieceCount(Predicate<Integer> predicate)
		{ return checkPieceCount(null, null, predicate); }

	/**
	 * Verifica se há alguma coisa errada com a formação atual das pedras.
	 * Se houver, lança uma exception com o erro encontrado.
	 */
	public void validateBoard() {
		if (chessAI == null)
			chessAI = new ChessAI(this);
		if (checkPieceCount(PieceType.KING, e -> e < 2))
			throw new BoardException("You must add one King of each color on the board");
		if (checkPieceCount(PieceColor.WHITE, e -> e < 2) || checkPieceCount(PieceColor.BLACK, e -> e < 2))
			throw new BoardException("You must add at least 2 pieces of each color on the board");
		if (checkPieceCount(e -> e == 64))
			throw new BoardException("The board must have at lest one free slot");
		boardWasValidated = true;
		if (allPiecesAreStucked(PieceColor.WHITE)) {
			boardWasValidated = false;
			throw new BoardException("This piece formation are unplayable because all WHITE pieces are stucked");
		}
		if (allPiecesAreStucked(PieceColor.BLACK)) {
			boardWasValidated = false;
			throw new BoardException("This piece formation are unplayable because all BLACK pieces are stucked");
		}
		if (allPiecesAreStucked(PieceColor.BLACK) || allPiecesAreStucked(PieceColor.WHITE))
			throw new BoardException((allPiecesAreStucked(PieceColor.BLACK) ? PieceColor.BLACK : PieceColor.WHITE).name() + " pieces are all stucked at the starting");
		boardWasValidated = true;
		undoBoards.clear();
		undoIndex = -1;
		if (playMode == ChessPlayMode.PLAYER_VS_PLAYER)
			saveBoardForUndo();
	}
	
	private void boardWasValidated() {
		if (!boardWasValidated)
			throw new BoardException("You must call the \".validateBoard()\" method before start playing");
	}

	private Boolean allPiecesAreStucked(PieceColor color)
		{ return getPieceList(color, p -> !p.isStucked()).isEmpty(); }

	private void validatePosition(PiecePosition position, String varName) {
		validateNullVar(position, varName);
		if (!isValidBoardPosition(position))
			throw new InvalidPositionException(varName + " " + position + " - Invalid board position");
	}
	
	private static <T> void validateNullVar(T var, String varName) {
		if (var == null)
			throw new NullPointerException("\"" + varName + "\" is null");
	}

	private void resetBoard(Piece[][] board) { 
		for (Piece[] b : board)
			Arrays.fill(b, null);
	}

	/**
	 * Retorna o total de turnos jogados até o momento
	 */
	public int getTurns()
		{ return turns; }
	
	/**
	 * Retorna o peão promovido na última rodada (se houver)
	 */
	public Piece getPromotedPawn()
		{ return promotedPiece; }
	
	/**
	 * Verifica se algum peão foi promovido na última rodada
	 */
	public Boolean pawnWasPromoted()
		{ return getPromotedPawn() != null; }
	
	/**
	 * Promove o peão que chegou ao lado oposto do tabuleiro para um novo tipo de pedra
	 */
	public void promotePawnTo(PieceType newType) throws PromotionException {
		boardWasValidated();
		if (!pawnWasPromoted())
			throw new PromotionException("There is no promoted piece");
		validateNullVar(newType, "newType");
		if (newType == PieceType.PAWN || newType == PieceType.KING)
			throw new PromotionException("You can't promote a PAWN to a " + newType.name());
		PiecePosition pos = new PiecePosition(getPromotedPawn().getPosition());
		PieceColor color = getPromotedPawn().getColor();
		removePiece(getPromotedPawn().getPosition());
		addNewPiece(pos, newType, color);
		promotedPiece = null;
		changeTurn();
	}
	
	/**
	 * Retorna a pedra marcada atualmente como En Passant (se houver)
	 */
	public Piece getEnPassantPawn()
		{ return enPassantPiece; }
	
	/**
	 * Se a pedra informada for um peão que possa realizar o movimento "En Passant", retorna a posição onde ele pode capturar usando esse movimento
	 */
	public PiecePosition getEnPassantCapturePosition() {
		if (getEnPassantPawn() == null)
			throw new GameException("There's no \"En Passant\" pawn");
		PiecePosition position = new PiecePosition(getEnPassantPawn().getPosition());
		position.incRow(getEnPassantPawn().getRow() == 3 ? -1 : 1);
		return position;
	}

	/**
	 * Verifica se a pedra informada é um peão que possa realizar o movimento "En Passant" em cima do peão marcado atualmente como tal
	 */
	public Boolean pieceCanDoEnPassant(Piece piece) {
		if (piece == null ||
				piece.getType() != PieceType.PAWN || 
				getEnPassantPawn() == null ||
				getEnPassantPawn().getColor() == piece.getColor())
					return false;
		for (int x = -1; x <= 1; x += 2) {
			PiecePosition p = new PiecePosition(piece.getPosition());
			p.incColumn(x);
			if (getEnPassantPawn().getPosition().equals(p))
				return true;
		}
		return false;
	}

	private Rook checkCastling(PiecePosition kingPositionSource, PiecePosition kingPositionTarget) {
		validateNullVar(kingPositionSource, "kingPositionSource");
		validateNullVar(kingPositionTarget, "kingPositionTarget");

		King king;
		Rook rook = null;
		PiecePosition kingPosition = new PiecePosition(kingPositionSource);
		PiecePosition rookPosition = new PiecePosition(kingPositionSource);
		Boolean toLeft = kingPosition.getColumn() > kingPositionTarget.getColumn();
		if (Math.abs(kingPositionSource.getColumn() - kingPositionTarget.getColumn()) != 2)
			return null;

		try {
			king = (King) getPieceAt(kingPositionSource);
			validateNullVar(king, "");
			rookPosition.setColumn(toLeft ? 0 : 7);
			rook = (Rook) getPieceAt(rookPosition);

			if (!king.isSameColorOf(rook))
				throw new GameException("king and rook must be from the same color");
			if (king.wasMoved() || rook.wasMoved() || isChecked())
				return null;
		}
		catch (Exception e)
			{ return null; }

		Board recBoard = newClonedBoard();
		
		try {
			while (true) {
				removePiece(kingPosition);
				kingPosition.incColumn(toLeft ? -1 : 1);
				if (!isFreeSlot(kingPosition)) {
					cloneBoard(recBoard, this);
					return null;
				}
				addPiece(kingPosition, king);
				if (isChecked()) {
					cloneBoard(recBoard, this);
					return null;
				}
				else if (kingPosition.getColumn() == (toLeft ? 1 : 6)) {
					cloneBoard(recBoard, this);
					return rook;
				}
			}
		}
		catch (Exception e) {
			cloneBoard(this, recBoard);
			return null;
		}
		
	}
	
	/**
	 * Verifica se a posição informada é uma posição válida no tabuleiro
	 */
	public Boolean isValidBoardPosition(PiecePosition position) {
		validateNullVar(position, "position");
		return position.getColumn() >= 0 && position.getColumn() < board[0].length &&
			position.getRow() >= 0 && position.getRow() < board.length;
	}
	
	/**
	 * Verifica se a pedra informada está em uma posição segura
	 */
	public Boolean pieceIsAtSafePosition(Piece piece) {
		validateNullVar(piece, "piece");
		return getPieceList(piece.getOpponentColor(), p -> p.couldCapture(piece)).isEmpty();
	}
	
	/**
	 * Verifica se a peça informada pode ir para uma posição segura
	 */
	public Boolean pieceCanGoToASafePosition(Piece piece, PiecePosition positionToGo) {
		validateNullVar(piece, "piece");
		Board recBoard = newClonedBoard();
		Boolean isSafe = false;
		try {
			removePiece(piece);
			addPiece(positionToGo, piece);
			isSafe = pieceIsAtSafePosition(piece);
		}
		catch (Exception e) {}
		cloneBoard(recBoard, this);
		return isSafe;
	}

	/**
	 * Verifica se a posição informada está vaga.
	 */
	public Boolean isFreeSlot(PiecePosition position) {
		validateNullVar(position, "position");
		return isValidBoardPosition(position) && getPieceAt(position) == null;
	}
	
	/**
	 * Verifica se há uma pedra no tile informado. Usar o método
	 * {@code isFreeSlot()} para isso não é seguro, pois ele pode
	 * retornar {@code false} indicando que o tile não está vago,
	 * porém esse retorno pode ser devido á ser um slot inválido
	 * do tabuleiro. Já esse método, só retornará {@code true} se
	 * de fato houver uma pedra na posição informada.
	 */
	public Boolean isAPiece(PiecePosition position) {
		validateNullVar(position, "position");
		return isValidBoardPosition(position) && getPieceAt(position) != null;
	}

	/**
	 * Verifica se há uma pedra adversária no tile informado.
	 */
	public Boolean isAnOpponentPiece(PiecePosition position) {
		validateNullVar(position, "position");
		return isValidBoardPosition(position) && getPieceAt(position) != null &&
			getPieceAt(position).getColor() != getCurrentColorTurn();
	}

	/**
	 * Retorna a pedra na posição especificada do tabuleiro
	 */
	public Piece getPieceAt(PiecePosition position) {
		validateNullVar(position, "position");
		return !isValidBoardPosition(position) ? null : board[position.getRow()][position.getColumn()];
	}
	
	/**
	 * Retorna a pedra selecionada no momento
	 */
	public Piece getSelectedPiece()
		{ return selectedPiece; }
	
	/**
	 * Verifica se há alguma pedra selecionada no momento
	 */
	public Boolean pieceIsSelected()
		{ return getSelectedPiece() != null; }

	void cpuSelectedPiece(Piece piece)
		{ selectedPiece = piece; }

	/**
	 * Seleciona uma pedra na posição informada
	 */
	public Piece selectPiece(PiecePosition position) throws PieceSelectionException {
		boardWasValidated();
		validatePosition(position, "position");
		if (isGameOver())
			throw new PieceSelectionException("The current game was ended");
		if (isCpuTurn())
			throw new PieceSelectionException("It's CPU turn! Wait...");
		if (checkMate())
			throw new PieceSelectionException("The current game is ended (Checkmate)");
		if (drawGame()) {
			if (isDrawByBareKings())
				throw new PieceSelectionException("The current game is ended (Draw game by Bare Kings)");
			if (isDrawByStalemate())
				throw new PieceSelectionException("The current game is ended (Draw game by Stalemate)");
			if (isDrawByThreefoldRepetition())
				throw new PieceSelectionException("The current game is ended (Draw game by Threefold-repetition)");
			throw new PieceSelectionException("The current game is ended (Draw game)");
		}
		if (isFreeSlot(position))
			throw new PieceSelectionException("There is no piece on that position");
		if (getPieceAt(position).getColor() != getCurrentColorTurn())
			throw new PieceSelectionException("This piece is not yours");
		if (getPieceAt(position).isStucked())
			throw new PieceSelectionException("This piece is stucked");
		return (selectedPiece = getPieceAt(position));
	}
	
	/**
	 * Retorna a cor da pedra vitoriosa (se houver)
	 */
	public PieceColor getWinnerColor()
		{ return checkMate() ? getOpponentColor() : null; }

	/**
	 * Retorna a cor da pedra do turno atual
	 */
	public PieceColor getCurrentColorTurn()
		{ return currentColorTurn; }
	
	/**
	 * Retorna a cor da pedra adversária
	 */
	public PieceColor getOpponentColor()
		{ return currentColorTurn.getOppositeColor(); }
	
	private void addCapturedPiece(Piece piece) {
		boardWasValidated();
		validateNullVar(piece, "piece");
		if (capturedPieces.contains(piece))
			throw new GameException("piece is already on captured pieces list");
		capturedPieces.add(piece);
	}

	public List<Piece> sortPieceListByPieceValue(List<Piece> pieceList, Boolean fromStrongestToWeakest) {
		if (!fromStrongestToWeakest)
			pieceList.sort((p1, p2) -> p1.compareTo(p2));
		else
			pieceList.sort((p1, p2) -> p2.compareTo(p1));
		return pieceList;
	}
	
	public List<Piece> sortPieceListByPieceValue(List<Piece> pieceList)
		{ return sortPieceListByPieceValue(pieceList, false); }
	
	private List<Piece> getPieceList(PieceColor color, Predicate<Piece> predicate) {
		List<Piece> pieceList = new ArrayList<>();
		for (Piece[] boardRow : board)
			for (Piece piece : boardRow)
				if (piece != null &&
						(color == null || piece.isSameColorOf(color)) &&
						(predicate == null || predicate.test(piece)))
							pieceList.add(piece);
		return pieceList;
	}
	
	/**
	 * Retorna a lista de todas as pedras em jogo no momento
	 */
	public List<Piece> getPieceList()
		{ return getPieceList(null, null); }
	
	/**
	 * Retorna a lista de todas as pedras em jogo no momento, que atendam o predicate informado
	 */
	public List<Piece> getPieceList(Predicate<Piece> predicate)
		{ return getPieceList(null, predicate); }

	/**
	 * Retorna a lista de todas as pedras em jogo no momento de uma determinada cor e que atendam o predicate informado
	 */
	public List<Piece> getPieceListByColor(PieceColor color, Predicate<Piece> predicate)
		{ return getPieceList(color, predicate); }
	
	/**
	 * Retorna a lista de todas as pedras em jogo no momento de uma determinada cor
	 */
	public List<Piece> getPieceListByColor(PieceColor color)
		{ return getPieceList(color, null); }
	
	/**
	 * Retorna a lista das pedras da cor do turno atual que atendam o predicate informado
	 */
	public List<Piece> getFriendlyPieceList(Predicate<Piece> predicate)
		{ return getPieceList(getCurrentColorTurn(), predicate); }

	/**
	 * Retorna a lista das pedras da cor do turno atual
	 */
	public List<Piece> getFriendlyPieceList()
		{ return getPieceList(getCurrentColorTurn(), null); }

	/**
	 * Retorna a lista das pedras do adversário que atendam o predicate informado
	 */
	public List<Piece> getOpponentPieceList(Predicate<Piece> predicate)
		{ return getPieceList(getOpponentColor(), predicate); }

	/**
	 * Retorna a lista das pedras do adversário
	 */
	public List<Piece> getOpponentPieceList()
		{ return getPieceList(getOpponentColor(), null); }

	/**
	 * Retorna a lista das pedras do adversário, baseado na cor informada, que atendam o predicate informado
	 */
	public List<Piece> getOpponentPieceList(PieceColor color, Predicate<Piece> predicate)
		{ return getPieceList(color.getOppositeColor(), predicate); }

	/**
	 * Retorna a lista das pedras do adversário, baseado na cor informada
	 */
	public List<Piece> getOpponentPieceList(PieceColor color)
		{ return getPieceList(color.getOppositeColor(), null); }

	/**
	 * Retorna a lista das pedras do adversário, baseado na cor da pedra informada, que atendam o predicate informado
	 */
	public List<Piece> getOpponentPieceList(Piece piece, Predicate<Piece> predicate)
		{ return getPieceList(piece.getOpponentColor(), predicate); }

	/**
	 * Retorna a lista das pedras do adversário, baseado na cor da pedra informada
	 */
	public List<Piece> getOpponentPieceList(Piece piece)
		{ return getPieceList(piece.getOpponentColor(), null); }

	/**
	 * Retorna a lista de todas as pedras brancas em jogo, que atendam o predicate informado
	 */
	public List<Piece> getWhitePieceList(Predicate<Piece> predicate)
		{ return getPieceList(PieceColor.WHITE, predicate); }

	/**
	 * Retorna a lista de todas as pedras brancas em jogo
	 */
	public List<Piece> getWhitePieceList()
		{ return getPieceList(PieceColor.WHITE, null); }

	/**
	 * Retorna a lista de todas as pedras pretas em jogo, que atendam o predicate informado
	 */
	public List<Piece> getBlackPieceList(Predicate<Piece> predicate)
		{ return getPieceList(PieceColor.BLACK, predicate); }

	/**
	 * Retorna a lista de todas as pedras pretas em jogo
	 */
	public List<Piece> getBlackPieceList()
		{ return getPieceList(PieceColor.BLACK, null); }

	/*
	 * Retorna a lista de todas as pedras da cor informada capturadas até o momento, filtradas pelo predicate
	 */
	public List<Piece> getCapturedPieces(PieceColor color, Predicate<Piece> predicate) {
		List<Piece> pieceList = new ArrayList<>();
		for (Piece piece : capturedPieces)
			if (piece != null &&
			(color == null || piece.isSameColorOf(color)) &&
			(predicate == null || predicate.test(piece)))
				pieceList.add(piece);
		return pieceList;
	}
	
	/**
	 * Retorna a lista de todas as pedras capturadas até o momento
	 */
	public List<Piece> getCapturedPieces()
		{ return getCapturedPieces(null, null); }
	
	/**
	 * Retorna a lista de todas as pedras capturadas até o momento, de uma determinada cor e que atendam o predicate informado
	 */
	public List<Piece> getCapturedPiecesByColor(PieceColor color, Predicate<Piece> predicate)
		{ return getCapturedPieces(color, predicate); }
	
	/**
	 * Retorna a lista de todas as pedras capturadas até o momento, de uma determinada cor
	 */
	public List<Piece> getCapturedPiecesByColor(PieceColor color)
		{ return getCapturedPieces(color, null); }
	
	/**
	 * Retorna a lista das pedras da cor do turno atual que atendam o predicate informado
	 */
	public List<Piece> getCapturedFriendlyPiece(Predicate<Piece> predicate)
		{ return getCapturedPieces(getCurrentColorTurn(), predicate); }

	/**
	 * Retorna a lista das pedras da cor do turno atual
	 */
	public List<Piece> getCapturedFriendlyPieces()
		{ return getCapturedPieces(getCurrentColorTurn(), null); }

	/**
	 * Retorna a lista das pedras capturadas do adversário que atendam o predicate informado
	 */
	public List<Piece> getCapturedOpponentPieces(Predicate<Piece> predicate)
		{ return getCapturedPieces(getOpponentColor(), predicate); }

	/**
	 * Retorna a lista das pedras capturadas do adversário
	 */
	public List<Piece> getCapturedOpponentPieces()
		{ return getCapturedPieces(getOpponentColor(), null); }

	/**
	 * Retorna a lista das pedras capturadas do adversário, baseado na cor informada, que atendam o predicate informado
	 */
	public List<Piece> getCapturedOpponentPieces(PieceColor color, Predicate<Piece> predicate)
		{ return getCapturedPieces(color.getOppositeColor(), predicate); }

	/**
	 * Retorna a lista das pedras capturadas do adversário, baseado na cor informada
	 */
	public List<Piece> getCapturedOpponentPieces(PieceColor color)
		{ return getCapturedPieces(color.getOppositeColor(), null); }

	/**
	 * Retorna a lista das pedras capturadas do adversário, baseado na cor da pedra informada, que atendam o predicate informado
	 */
	public List<Piece> getCapturedOpponentPieces(Piece piece, Predicate<Piece> predicate)
		{ return getCapturedPieces(piece.getOpponentColor(), predicate); }

	/**
	 * Retorna a lista das pedras capturadas do adversário, baseado na cor da pedra informada
	 */
	public List<Piece> getCapturedOpponentPieces(Piece piece)
		{ return getCapturedPieces(piece.getOpponentColor(), null); }

	/**
	 * Retorna a lista de todas as pedras brancas capturadas até o momento, que atendam o predicate informado
	 */
	public List<Piece> getCapturedWhitePieces(Predicate<Piece> predicate)
		{ return getCapturedPieces(PieceColor.WHITE, predicate); }

	/**
	 * Retorna a lista de todas as pedras brancas capturadas até o momento
	 */
	public List<Piece> getCapturedWhitePieces()
		{ return getCapturedPieces(PieceColor.WHITE, null); }

	/**
	 * Retorna a lista de todas as pedras pretas capturadas até o momento, que atendam o predicate informado
	 */
	public List<Piece> getCapturedBlackPieces(Predicate<Piece> predicate)
		{ return getCapturedPieces(PieceColor.BLACK, predicate); }

	/**
	 * Retorna a lista de todas as pedras pretas capturadas até o momento
	 */
	public List<Piece> getCapturedBlackPieces()
		{ return getCapturedPieces(PieceColor.BLACK, null); }

	private void addPiece(PiecePosition position, Piece piece) { 
		validatePosition(position, "position");
		validateNullVar(piece, "piece");
		if (!isFreeSlot(position))
			throw new InvalidPositionException("The slot at this position is not free");
		board[position.getRow()][position.getColumn()] = piece;
		piece.setPosition(position);
	}

	private void removePiece(PiecePosition position) { 
		validatePosition(position, "position");
		if (isFreeSlot(position))
			throw new InvalidPositionException("There's no piece at this slot position");
		board[position.getRow()][position.getColumn()] = null;
	}
	
	private void removePiece(Piece piece)
		{ removePiece(piece.getPosition()); }

	/**
	 * Cancela a seleção atual
	 */
	public void cancelSelection() throws CancelSelectionException {
		boardWasValidated();
		if (pawnWasPromoted())
			throw new CancelSelectionException("You must promote your pawn");
		selectedPiece = null;
	}
	
	Piece movePieceTo(PiecePosition sourcePos, PiecePosition targetPos, Boolean justTesting) {
		if (!justTesting) {
			boardWasValidated();
			validatePosition(sourcePos, "sourcePos");
			validatePosition(targetPos, "targetPos");
		}
		
		sourcePos = new PiecePosition(sourcePos);
		targetPos = new PiecePosition(targetPos);
		
		if (pawnWasPromoted()) {
			if (justTesting)
				return null;
			throw new InvalidMoveException("You must promote the pawn");
		}

		if (selectedPiece != null && targetPos.equals(selectedPiece.getPosition())) {
			//Se o slot de destino for o mesmo da pedra selecionada, desseleciona ela
			selectedPiece = null;
			return null;
		}
		
		Boolean checked = isChecked();
		Piece sourcePiece = getPieceAt(sourcePos);
		Piece targetPiece = getPieceAt(targetPos);
		lastMoveWasCastling = lastMoveWasEnPassant = false;
		lastCapturedPiece = null;
		
		if (selectedPiece != null && targetPiece != null && selectedPiece.isSameColorOf(targetPiece)) {
			//Se já houver uma pedra selecionada, e clicar em cima de outra pedra da mesma cor, cancela a seleção atual e seleciona a nova pedra
			selectedPiece = targetPiece;
			return null;
		}
		
		if (!sourcePiece.canMoveToPosition(targetPos)) {
			if (justTesting)
				return null;
			throw new InvalidMoveException("Invalid move for this piece");
		}

		Board cloneBoard = newClonedBoard();

		// Castling special move
		Rook rook;
		if (sourcePiece.getType() == PieceType.KING &&
				(rook = checkCastling(sourcePos, targetPos)) != null) {
					removePiece(rook.getPosition());
					rook.setPosition(new PiecePosition(targetPos));
					rook.incColumn(sourcePos.getColumn() > targetPos.getColumn() ? 1 : -1);
					addPiece(rook.getPosition(), rook);
					if (!justTesting)
						lastMoveWasCastling = true;
		}

		removePiece(sourcePos);
		
		if (pieceCanDoEnPassant(selectedPiece) && getEnPassantCapturePosition().equals(targetPos)) {
			if (!justTesting)
				lastMoveWasEnPassant = true;
			targetPiece = getEnPassantPawn(); // Verifica se o peão atual realizou um movimento de captura EnPassant
		}
		enPassantPiece = null;
		promotedPiece = null;

		if (sourcePiece.getType() == PieceType.PAWN &&
				Math.abs(sourcePos.getRow() - targetPos.getRow()) == 2)
					enPassantPiece = sourcePiece; // Marca peão que iniciou movendo 2 casas como EnPassant

		if (targetPiece != null) {
			removePiece(targetPiece.getPosition());
			addCapturedPiece(targetPiece);
			lastCapturedPiece = targetPiece;
		}

		addPiece(targetPos, sourcePiece);
		sourcePiece.incMovedTurns(1);
		movedTurns.put(sourcePiece, sourcePiece.getMovedTurns());

		if (sourcePiece.getType() == PieceType.PAWN &&
				(targetPos.getRow() == 0 || targetPos.getRow() == 7))
					promotedPiece = sourcePiece;
		
		if (isChecked()) {
			if (justTesting)
				return null;
			cloneBoard(cloneBoard, this);
			throw new CheckException(!checked ? "You can't put yourself in check" : "You'll still checked after this move");
		}

		if (!pawnWasPromoted())
			changeTurn();

		if (!justTesting && playMode == ChessPlayMode.PLAYER_VS_PLAYER)
			saveBoardForUndo();
		
		StringBuilder sb = new StringBuilder();
		for (Piece[] boardRow : board)
			for (Piece piece : boardRow)
				if (piece == null)
					sb.append("[null]");
				else {
					sb.append("[");
					sb.append(piece.getColor().name());
					sb.append("_");
					sb.append(piece.getType().name());
					sb.append("]");
				}
		lastBoards.add(sb.toString());
		int i = lastBoards.size() - 1;
		if (lastBoards.size() > 4) {
			if (!lastBoards.get(i).equals(lastBoards.get(i - 4))) {
				lastBoards.clear();
				repeats = 0;
			}
			else
				++repeats;
		}
		drawGame = getPieceList().size() == 2 || repeats == 3 || kingIsStalemated();				
		lastMovedPiece = sourcePiece;
		return targetPiece;
	}
	
	/**
	 * Move a pedra selecionada para a posição informada
	 */
	public Piece movePieceTo(PiecePosition targetPos) throws PieceSelectionException,PieceMoveException {
		boardWasValidated();
		if (isGameOver())
			throw new PieceMoveException("The current game was ended");
		if (!pieceIsSelected())
			throw new PieceSelectionException("There's no selected piece to move!");
		return movePieceTo(getSelectedPiece().getPosition(), targetPos, false);
	}
	
	private void changeTurn() {
		turns++;
		selectedPiece = null;
		currentColorTurn = currentColorTurn.getOppositeColor();
	}
	
	/**
	 * Verifica se a pedra informada pode capturar alguma pedra adversária
	 */
	public Boolean pieceCouldCaptureAnyOpponentPiece(Piece piece) {
		validateNullVar(piece, "piece");
		return !getPieceList(piece.getOpponentColor(), p -> piece.couldCapture(p)).isEmpty();
	}

	/**
	 * Verifica se a pedra informada pode ser capturada por alguma pedra adversária
	 */
	public Boolean pieceCouldBeCapturedByAnyOpponentPiece(Piece piece) {
		validateNullVar(piece, "piece");
		return !getPieceList(piece.getOpponentColor(), p -> p.couldCapture(piece)).isEmpty();
	}

	/**
	 * Verifica se a pedra na posição informada pode ser capturada por alguma pedra adversária
	 */
	public Boolean pieceCouldBeCapturedByAnyOpponentPiece(PiecePosition piecePosition) {
		validatePosition(piecePosition, "piecePosition");
		if (getPieceAt(piecePosition) == null)
			throw new InvalidPositionException(piecePosition + " - There is no piece");
		return pieceCouldBeCapturedByAnyOpponentPiece(getPieceAt(piecePosition));
	}

	/**
	 * Verifica se a pedra informada pode ir para alguma posição sem risco de ser capturada
	 */
	public Boolean pieceCanGoToAnySafeSpot(Piece piece) {
		validateNullVar(piece, "piece");
		return !piece.getPossibleSafeMoves().isEmpty();
	}
	
	private PiecePosition testPositionsForAResult(Piece piece, Predicate<PiecePosition> resultWanted) {
		Board b = newClonedBoard();
		for (PiecePosition position : piece.getPossibleMoves()) {
			movePieceTo(piece.getPosition(), position, true);
			if (resultWanted.test(position)) {
				cloneBoard(b, this);
				return position;
			}
			cloneBoard(b, this);
		}
		return null;
	}
	
	/**
	 * Verifica se o jogo terminou 'ou por check mate, ou por empate' 
	 */
	public Boolean isGameOver()
		{ return checkMate() || drawGame(); }

	/**
	 * Verifica se a cor do turno atual está em check 
	 */
	public Boolean isChecked(PieceColor color)
		{ return getPieceList(color, p -> p.isSameTypeOf(PieceType.KING) && pieceIsAtSafePosition(p)).isEmpty(); }

	/**
	 * Verifica se a cor do turno atual está em check 
	 */
	public Boolean isChecked()
		{ return isChecked(getCurrentColorTurn()); }

	/**
	 * Verifica se a cor informada deu check mate no adversário 
	 */
	public Boolean checkMate() {
		PieceColor color = getCurrentColorTurn();
		for (Piece piece : getFriendlyPieceList())
			if (testPositionsForAResult(piece, e -> !isChecked(color)) != null)
				return false;
		return true;
	}
	
	/**
	 * Verifica se o jogo deu empate 
	 */
	public Boolean drawGame()
		{ return drawGame; }
	
	/*
	 * Retorna {@code true} se o empate foi devido á só ter sobrado os reis no tabuleiro 
	 */
	public Boolean isDrawByBareKings()
		{ return drawGame && getPieceList().size() == 2; }
	
	/*
	 * Retorna {@code true} se o empate foi devido á repetições de movimento 
	 */
	public Boolean isDrawByThreefoldRepetition()
		{ return drawGame && repeats == 3; }

	/*
	 * Retorna {@code true} se o empate foi devido á afogamento (Rei sem possibilidade de movimento, mas não sob risco de captura) 
	 */
	public Boolean isDrawByStalemate()
		{ return drawGame && kingIsStalemated(); }
	
	private Boolean kingIsStalemated() {
		return !getFriendlyPieceList(p -> p.isSameTypeOf(PieceType.KING) &&
				((King)p).isStaleMated() && testPositionsForAResult(p, e -> ((King)p).isStaleMated()) != null).isEmpty();
	}

	/**
	 * Adiciona uma nova pedra no tabuleiro
	 * @param position - Posição do tabuleiro onde a pedra será adicionada
	 * @param type - Tipo da pedra que será adicionada
	 * @param color - Cor da pedra que será adicionada
	 */
	public void addNewPiece(PiecePosition position, PieceType type, PieceColor color) {
		validateNullVar(position, "position");
		if (!isValidBoardPosition(position))
			throw new InvalidPositionException("Invalid board position");
		validateNullVar(type, "type");
		validateNullVar(color, "color");
		if (!isFreeSlot(position))
			throw new InvalidPositionException("This board position is not free");
		
		Piece piece;
		if (type == PieceType.KING)
			piece = new King(this, position, color);
		else if (type == PieceType.QUEEN) 
			piece = new Queen(this, position, color);
		else if (type == PieceType.ROOK) 
			piece = new Rook(this, position, color);
		else if (type == PieceType.BISHOP) 
			piece = new Bishop(this, position, color);
		else if (type == PieceType.KNIGHT) 
			piece = new Knight(this, position, color);
		else 
			piece = new Pawn(this, position, color);

		if (type == PieceType.KING &&
				!getPieceListByColor(color, p -> p.isSameTypeOf(PieceType.KING)).isEmpty())
					throw new BoardException("You can't put more than 1 King of each color on the board");
				
		board[position.getRow()][position.getColumn()] = piece;
		movedTurns.put(piece, 0);
	}

	/**
	 * Sobrecarga do método {@code addNewPiece()} que recebe uma coordenada {@code row, column} ao invés de um tipo {@code PiecePosition}
	 */
	public void addNewPiece(int row, int column, PieceType type, PieceColor color)
		{ addNewPiece(new PiecePosition(row, column), type, color); }

	/**
	 * Sobrecarga do método {@code addNewPiece()} que recebe uma {@code String} com a coordenada no formato {@code a1|b2|c3|d4|e5|f6|g7|h8}
	 */
	public void addNewPiece(String position, PieceType type, PieceColor color)
		{ addNewPiece(PiecePosition.stringToPosition(position), type, color); }
	
	/**
	 * Sobrecarga do método {@code addNewPiece()} que pega o tipo e cor da nova pedra á ser adicionada, á partir de uma pedra existente.
	 */
	public void addNewPiece(PiecePosition position, Piece piece)
		{ addNewPiece(position, piece.getType(), piece.getColor()); }
	
	/**
	 * Define todas as pedras do tabuleiro á partir de uma matriz contendo caracteres referentes ás pedras em suas respectivas posições no tabuleiro.
	 */
	public void setBoard(Character[][] pieces) throws Exception {
		PieceType[] types = {PieceType.PAWN, PieceType.BISHOP, PieceType.KING, PieceType.KNIGHT, PieceType.QUEEN, PieceType.ROOK};
		String lets = "";
		for (PieceType type : types)
			lets += (!lets.isEmpty() ? "," : "") + type.getLet();
		Boolean invalidLet;
		for (int y = 0; y < pieces.length; y++)
			for (int x = 0; x < pieces[y].length; x++) {
				PieceType type = null;
				PieceColor color = null;
				if (pieces[y][x] != ' ' && pieces[y][x] != '.' && pieces[y][x] != 'x' && pieces[y][x] != 'X') {
					invalidLet = true;
					for (PieceType t : types)
						if (PieceType.getLet(t) == Character.toUpperCase(pieces[y][x])) {
							invalidLet = false;
							type = t;
							color = PieceType.getLet(t) == pieces[y][x] ? PieceColor.WHITE : PieceColor.BLACK;
							addNewPiece(y, x, type, color);
						}
					if (invalidLet)
						throw new GameException(pieces[y][x] + " - Invalid piece letter. Available letters: \"" + lets + "\"");
				}
			}
	}

}