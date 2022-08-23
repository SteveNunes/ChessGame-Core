package board;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import enums.ChessPlayMode;
import enums.PieceColor;
import enums.PieceType;
import exceptions.BoardException;
import exceptions.CheckException;
import exceptions.GameException;
import exceptions.InvalidMoveException;
import exceptions.InvalidPositionException;
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
	private Piece promotedPiece;
	private PiecePosition cpuSelectedPositionToMove;
	private PieceColor currentColorTurn;
	private Boolean boardWasValidated;
	
	public Board() {
		board = new Piece[8][8];
		capturedPieces = new ArrayList<>();
		movedTurns = new HashMap<>();
		playMode = ChessPlayMode.PLAYER_VS_PLAYER;
		reset(); 
	}
	
	public Board(PieceColor startTurn) {
		this();
		currentColorTurn = startTurn;		
	}
	
	private static void cloneBoard(Board sourceBoard, Board targetBoard) {
		for (int y = 0; y < sourceBoard.board.length; y++)
			for (int x = 0; x < sourceBoard.board[y].length; x++)
				if ((targetBoard.board[y][x] = sourceBoard.board[y][x]) != null)
					targetBoard.board[y][x].setPosition(y, x);
		targetBoard.cpuSelectedPositionToMove = sourceBoard.cpuSelectedPositionToMove;
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
		targetBoard.movedTurns.clear();
		for (Piece piece : sourceBoard.movedTurns.keySet()) {
			piece.setMovedTurns(sourceBoard.movedTurns.get(piece));
			targetBoard.movedTurns.put(piece, sourceBoard.movedTurns.get(piece));
		}
	}

	private Board newClonedBoard() {
		Board board = new Board();
		cloneBoard(this, board);
		return board;
	}
	
	public Boolean pieceWasCaptured()
		{ return lastCapturedPiece != null; }
	
	public Piece getLastCapturedPiece()
		{ return lastCapturedPiece; }

	public Boolean lastMoveWasEnPassant()
		{ return lastMoveWasEnPassant; }

	public Boolean lastMoveWasCastling()
		{ return lastMoveWasCastling; }

	public Boolean canUndoMove()
		{ return undoIndex > 0; }
	
	public Boolean canRedoMove()
		{ return undoIndex + 1 < undoBoards.size(); }

	private void saveBoardForUndo() {
		Board saveBoard = newClonedBoard();
		undoIndex = undoBoards.size();
		undoBoards.add(undoIndex, saveBoard);
	}

	public void undoMoves(int totalUndoMoves) {
		if (totalUndoMoves < 1)
			throw new GameException("totalUndoMoves must be higher than 0");
		if (undoIndex == 0)
			throw new GameException("No available undo moves");
		while (--totalUndoMoves >= 0 && undoIndex - 1 >= 0) {
			if (--undoIndex == 0 || totalUndoMoves == 0)
				cloneBoard(undoBoards.get(undoIndex), this);
		}
	}
	
	public void undoMove()
		{ undoMoves(1); }
	
	public void redoMoves(int totalRedoMoves) {
		if (totalRedoMoves < 1)
			throw new GameException("totalRedoMoves must be higher than 0");
		if (undoIndex + 1 == undoBoards.size())
			throw new GameException("No available redo moves");
		while (--totalRedoMoves >= 0 && undoIndex + 1 < undoBoards.size()) {
			if (++undoIndex == undoBoards.size() - 1 || totalRedoMoves == 0)
				cloneBoard(undoBoards.get(undoIndex), this);
		}
	}
	
	public void redoMove()
		{ redoMoves(1); }

	public void reset() throws BoardException {
		turns = 0;
		boardWasValidated = false;
		lastMoveWasEnPassant = false;
		lastMoveWasCastling = false;
		lastCapturedPiece = null;
		cpuSelectedPositionToMove = null;
		selectedPiece = null;
		enPassantPiece = null;
		promotedPiece = null;
		currentColorTurn = new SecureRandom().nextInt(2) == 0 ? PieceColor.BLACK : PieceColor.WHITE;
		capturedPieces.clear();
		movedTurns.clear();
		resetBoard(board);
	}
	
	public void setPlayMode(ChessPlayMode mode) {
		if (boardWasValidated)
			throw new BoardException("You can't do it after validated the board. Call this metod before validating the board.");
		this.playMode = mode;
	}

	public Boolean isCpuTurn() {
		return playMode != ChessPlayMode.PLAYER_VS_PLAYER &&
			(playMode == ChessPlayMode.CPU_VS_CPU || currentColorTurn == PieceColor.WHITE); 
	}
	
	private Boolean checkPieceCount(PieceType type, PieceColor color, Predicate<Long> predicate)
		{ return predicate.test(getPieceList(color).stream().filter(p -> type == null || p.getType() == type).count()); }

	private Boolean checkPieceCount(PieceType type, Predicate<Long> predicate)
		{ return checkPieceCount(type, null, predicate); }

	private Boolean checkPieceCount(PieceColor color, Predicate<Long> predicate)
		{ return checkPieceCount(null, color, predicate); }

	private Boolean checkPieceCount(Predicate<Long> predicate)
		{ return checkPieceCount(null, null, predicate); }

	public void validateBoard() throws BoardException {
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
		boardWasValidated = true;
		undoBoards.clear();
		undoIndex = -1;
		saveBoardForUndo();
	}
	
	private void checkIfBoardWasValidated() throws BoardException {
		if (!boardWasValidated)
			throw new BoardException("You must call the \".validateBoard()\" method before start playing");
	}

	private Boolean allPiecesAreStucked(PieceColor color) {
		for (Piece piece : getPieceList(color))
			if (!piece.isStucked())
				return false;
		return true;
	}

	private void validatePosition(PiecePosition position, String varName) throws NullPointerException,InvalidPositionException {
		validateNullVar(position, varName);
		if (!isValidBoardPosition(position))
			throw new InvalidPositionException(varName + " " + position + " - Invalid board position");
	}
	
	private <T> void validateNullVar(T var, String varName) throws NullPointerException {
		if (var == null)
			throw new NullPointerException("\"" + varName + "\" is null");
	}

	public List<Piece> getPieceList(PieceColor color) {
		List<Piece> pieceList = new ArrayList<>();
		for (Piece[] boardRow : board)
			for (Piece piece : boardRow)
				if (piece != null && (color == null || piece.getColor() == color))
					pieceList.add(piece);
		return pieceList;
	}
	
	public List<Piece> getPieceList()
		{ return getPieceList(null); }
	
	private void resetBoard(Piece[][] board) { 
		for (Piece[] b : board)
			Arrays.fill(b, null);
	}

	public int getTurns() {
		checkIfBoardWasValidated();
		return turns;
	}
	
	public PieceColor getWinnerColor() {
		checkIfBoardWasValidated();
		return checkMate() ? opponentColor() : null;
	}
	
	public PieceColor getCurrentColorTurn() {
		checkIfBoardWasValidated();
		return currentColorTurn;
	}
	
	public Piece getPromotedPiece() {
		checkIfBoardWasValidated();
		return promotedPiece;
	}
	
	public Boolean pieceWasPromoted() {
		checkIfBoardWasValidated();
		return getPromotedPiece() != null;
	}
	
	public void promotePieceTo(PieceType newType) throws PromotionException,NullPointerException {
		checkIfBoardWasValidated();
		if (!pieceWasPromoted())
			throw new PromotionException("There is no promoted piece");
		validateNullVar(newType, "newType");
		if (newType == PieceType.PAWN || newType == PieceType.KING)
			throw new PromotionException("You can't promote a Pawn to a " + newType.getValue());
		PiecePosition pos = new PiecePosition(getPromotedPiece().getPosition());
		PieceColor color = getPromotedPiece().getColor();
		removePiece(getPromotedPiece().getPosition());
		addNewPiece(pos, newType, color);
		promotedPiece = null;
		changeTurn();
	}
	
	public Piece getEnPassantPiece(Piece piece) {
		checkIfBoardWasValidated();
		if (enPassantPiece == null ||
				piece == null ||
				piece.getType() != PieceType.PAWN ||
				piece.getColor() == enPassantPiece.getColor())
					return null;
		return enPassantPiece;
	}
	
	public PiecePosition getEnPassantCapturePosition(Piece piece) {
		checkIfBoardWasValidated();
		if (!checkEnPassant(piece))
			return null;
		PiecePosition position = new PiecePosition(getEnPassantPiece(piece).getPosition());
		position.incRow(getEnPassantPiece(piece).getColor() == PieceColor.WHITE ? -1 : 1);
		return position;
	}

	public Boolean checkEnPassant(Piece piece) {
		checkIfBoardWasValidated();
		if (piece == null ||
				piece.getType() != PieceType.PAWN || 
				getEnPassantPiece(piece) == null ||
				getEnPassantPiece(piece).getColor() == piece.getColor())
					return false;
		for (int x = -1; x <= 1; x += 2) {
			PiecePosition p = new PiecePosition(piece.getPosition());
			p.incColumn(x);
			if (getEnPassantPiece(piece).getPosition().equals(p))
				return true;
		}
		return false;
	}

	private Rook checkCastling(PiecePosition kingPositionSource, PiecePosition kingPositionTarget) {
		checkIfBoardWasValidated();
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

			if (isOpponentPieces(king, rook))
				throw new GameException("king and rook must be from the same color");
			if (king.wasMoved() || rook.wasMoved() || currentColorIsChecked())
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
				if (currentColorIsChecked()) {
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
	
	public Boolean isValidBoardPosition(PiecePosition position) {
		validateNullVar(position, "position");
		return position.getColumn() >= 0 && position.getColumn() < 8 &&
			position.getRow() >= 0 && position.getRow() < 8;
	}
	
	public Boolean isFreeSlot(PiecePosition position) {
		validateNullVar(position, "position");
		return isValidBoardPosition(position) && getPieceAt(position) == null;
	}
	
	public Piece getPieceAt(PiecePosition position) {
		validateNullVar(position, "position");
		return !isValidBoardPosition(position) ? null : board[position.getRow()][position.getColumn()];
	}
	
	public Boolean isOpponentPieces(Piece p1, Piece p2) {
		validateNullVar(p1, "p1");
		validateNullVar(p2, "p2");
		return p1.getColor() != p2.getColor();
	}
	
	public Boolean isOpponentPiece(PiecePosition position, PieceColor color) { 
		checkIfBoardWasValidated();
		validateNullVar(color, "color");
		return isValidBoardPosition(position) && !isFreeSlot(position) && getPieceAt(position).getColor() != color;
	}
	
	public Boolean isOpponentPiece(PiecePosition position) {
		checkIfBoardWasValidated();
		return getPieceAt(position) != null && isOpponentPiece(position, getCurrentColorTurn());
	}

	public Piece getSelectedPiece() {
		checkIfBoardWasValidated();
		return selectedPiece;
	}
	
	public Boolean pieceIsSelected() {
		checkIfBoardWasValidated();
		return getSelectedPiece() != null;
	}

	public Piece selectPiece(PiecePosition position) throws BoardException,InvalidPositionException,PieceSelectionException {
		checkIfBoardWasValidated();
		validatePosition(position, "position");
		if (isCpuTurn())
			throw new PieceSelectionException("It's CPU turn! Wait...");
		if (checkMate())
			throw new PieceSelectionException("The current game is ended (Checkmate)");
		if (drawGame())
			throw new PieceSelectionException("The current game is ended (Draw game)");
		if (isFreeSlot(position))
			throw new PieceSelectionException("There is no piece on that position");
		if (getPieceAt(position).getColor() != getCurrentColorTurn())
			throw new PieceSelectionException("This piece is not yours");
		if (getPieceAt(position).isStucked())
			throw new PieceSelectionException("This piece is stucked");
		return (selectedPiece = getPieceAt(position));
	}
	
	public PieceColor opponentColor(PieceColor color) { 
		checkIfBoardWasValidated();
		validateNullVar(color, "color");
		return color == PieceColor.BLACK ? PieceColor.WHITE : PieceColor.BLACK;
	}
	
	public PieceColor opponentColor() {
		checkIfBoardWasValidated();
		return opponentColor(getCurrentColorTurn());
	}
	
	private void addCapturedPiece(Piece piece) {
		checkIfBoardWasValidated();
		validateNullVar(piece, "piece");
		if (capturedPieces.contains(piece))
			throw new GameException("piece is already on captured pieces list");
		capturedPieces.add(piece);
	}

	public List<Piece> getCapturedPieces(PieceColor color) {
		checkIfBoardWasValidated();
		return capturedPieces.stream()
			.filter(c -> color == null || c.getColor() == color)
			.collect(Collectors.toList());
	}

	public List<Piece> getCapturedPieces()
		{ return getCapturedPieces(null); }
	
	private void addPiece(PiecePosition position, Piece piece) { 
		checkIfBoardWasValidated();
		validatePosition(position, "position");
		validateNullVar(piece, "piece");
		if (!isFreeSlot(position))
			throw new InvalidPositionException("The slot at this position is not free");
		board[position.getRow()][position.getColumn()] = piece;
		piece.setPosition(position);
	}

	private void removePiece(PiecePosition position) { 
		checkIfBoardWasValidated();
		validatePosition(position, "position");
		if (isFreeSlot(position))
			throw new InvalidPositionException("There's no piece at this slot position");
		board[position.getRow()][position.getColumn()] = null;
	}

	public void cancelSelection() {
		checkIfBoardWasValidated();
		if (pieceWasPromoted())
			throw new BoardException("You must promote your pawn");
		selectedPiece = null;
	}
	
	private Piece movePieceTo(PiecePosition sourcePos, PiecePosition targetPos, Boolean justTesting) throws InvalidMoveException {
		if (!justTesting) {
			checkIfBoardWasValidated();
			validatePosition(sourcePos, "sourcePos");
			validatePosition(targetPos, "targetPos");
		}
		
		sourcePos = new PiecePosition(sourcePos);
		targetPos = new PiecePosition(targetPos);

		if (pieceWasPromoted()) {
			if (justTesting)
				return null;
			throw new InvalidMoveException("You must promote the pawn");
		}

		if (selectedPiece != null && targetPos.equals(selectedPiece.getPosition())) {
			//Se o slot de destino for o mesmo da pedra selecionada, desseleciona ela
			selectedPiece = null;
			return null;
		}
		
		Boolean checked = currentColorIsChecked();
		Piece sourcePiece = getPieceAt(sourcePos);
		Piece targetPiece = getPieceAt(targetPos);
		lastMoveWasCastling = lastMoveWasEnPassant = false;
		lastCapturedPiece = null;
		
		if (selectedPiece != null && targetPiece != null && !isOpponentPieces(selectedPiece, targetPiece)) {
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
		
		if (checkEnPassant(selectedPiece) && getEnPassantCapturePosition(selectedPiece).equals(targetPos)) {
			if (!justTesting)
				lastMoveWasEnPassant = true;
			targetPiece = getEnPassantPiece(selectedPiece); // Verifica se o peão atual realizou um movimento de captura EnPassant
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
				targetPos.getRow() == (sourcePiece.getColor() == PieceColor.BLACK ? 0 : 7))
					promotedPiece = sourcePiece;
		
		if (currentColorIsChecked()) {
			if (justTesting)
				return null;
			cloneBoard(cloneBoard, this);
			throw new CheckException(!checked ? "You can't put yourself in check" : "You'll still checked after this move");
		}

		if (!pieceWasPromoted())
			changeTurn();

		if (!justTesting)
			saveBoardForUndo();
		
		return targetPiece;
	}
	
	public Piece movePieceTo(PiecePosition targetPos) throws BoardException {
		if (!pieceIsSelected())
			throw new PieceSelectionException("There's no selected piece to move!");
		return movePieceTo(getSelectedPiece().getPosition(), targetPos, false);
	}
	
	public void doCpuSelectAPiece() throws GameException {
		if (playMode == ChessPlayMode.PLAYER_VS_PLAYER)
			throw new GameException("Unable to use while in a PLAYER VS PLAYER mode.");
		if (!isCpuTurn())
			throw new GameException("It's not the CPU turn.");
		if (selectedPiece != null)
			throw new GameException("CPU already selected a piece. Call \".doCpuMoveSelectedPiece()\" for finish the CPU move.");
		
		Board recBoard = newClonedBoard();
		Piece lastTriedPiece = null;
		PiecePosition lastTriedPosition = null;
		Map<Piece, List<PiecePosition>> ignorePositions = new HashMap<>();
		List<Piece> ignoredPieces = new ArrayList<>();
		int triedVal = -1;
		Boolean debug = false;
		cpuSelectedPositionToMove = null;

		while (true)
			try {
				// Tenta capturar uma pedra adversária sem que a posição onde a pedra vá parar, permita que ela possa ser capturada logo em seguida
				triedVal = 1;
				for (PieceType type : PieceType.getListOfAll())
					for (Piece piece : getPieceList(getCurrentColorTurn())) {
						if (piece.getType() == type && !ignoredPieces.contains(piece)) {
							ignoredPieces.add(piece);
							for (Piece opponentPiece : getPieceList(opponentColor()))
								if ((!ignorePositions.containsKey(piece) ||
										!ignorePositions.get(piece).contains(opponentPiece.getPosition())) &&
										piece.getPossibleMoves().contains(opponentPiece.getPosition())) {
											lastTriedPiece = piece;
											lastTriedPosition = opponentPiece.getPosition();
											movePieceTo(piece.getPosition(), opponentPiece.getPosition(), true);
											if (!pieceColdBeCaptured(piece)) {
												triedVal = 2;
												cloneBoard(recBoard, this);
												cpuSelectedPositionToMove = new PiecePosition(opponentPiece.getPosition());
												selectedPiece = piece;
												return;
											}
											cloneBoard(recBoard, this);
								}
						}
					}
	
				triedVal = 5;
				// Tenta capturar uma pedra adversária com qualquer pedra se o ALVO for uma pedra que não seja um peão, mesmo que essa pedra possa ser capturada logo em seguida
				for (Piece piece : getPieceList(getCurrentColorTurn()))
					for (Piece opponentPiece : getPieceList(opponentColor()))
						if (opponentPiece.getType() != PieceType.PAWN &&
								piece.getPossibleMoves().contains(opponentPiece.getPosition()) &&
								(!ignorePositions.containsKey(piece) ||
								 !ignorePositions.get(piece).contains(opponentPiece.getPosition()))) {
									triedVal = 6;
									lastTriedPiece = piece;
									lastTriedPosition = opponentPiece.getPosition();
									cpuSelectedPositionToMove = new PiecePosition(opponentPiece.getPosition());
									selectedPiece = piece;
									return;
						}
	
				triedVal = 7;
				// Tenta capturar uma pedra adversária COM UM PEAO, mesmo que essa pedra possa ser capturada logo em seguida
				for (Piece piece : getPieceList(getCurrentColorTurn()))
					if (piece.getType() == PieceType.PAWN)
						for (Piece opponentPiece : getPieceList(opponentColor()))
							if ((!ignorePositions.containsKey(piece) ||
									!ignorePositions.get(piece).contains(opponentPiece.getPosition())) &&
									piece.getPossibleMoves().contains(opponentPiece.getPosition())) {
										triedVal = 8;
										lastTriedPiece = piece;
										lastTriedPosition = opponentPiece.getPosition();
										cpuSelectedPositionToMove = new PiecePosition(opponentPiece.getPosition());
										selectedPiece = piece;
										return;
							}
		
				triedVal = 9;
				// Tenta mover um peão 2 casas para frente, nas colunas do meio, se for seguro fazer isso
				for (Piece piece : getPieceList(getCurrentColorTurn()))
					if (piece.getType() == PieceType.PAWN && !piece.wasMoved() &&
							(piece.getColumn() == 3 || piece.getColumn() == 4)) {
								PiecePosition position = new PiecePosition(piece.getPosition());
								position.incRow(piece.getColor() == PieceColor.BLACK ? -2 : 2);
								if ((!ignorePositions.containsKey(piece) ||
										!ignorePositions.get(piece).contains(position)) &&
										piece.canMoveToPosition(position)) {
											movePieceTo(piece.getPosition(), position, true);
											if (!pieceColdBeCaptured(position)) {
												triedVal = 10;
												cloneBoard(recBoard, this);
												cpuSelectedPositionToMove = position;
												selectedPiece = piece;
												return;
											}
											cloneBoard(recBoard, this);
								}
							}
	
				triedVal = 11;
				// Tenta mover um peão 2 casas para frente, (se for a primeira movimentacao do peao) sem que ele possa ser capturado logo em seguida
				for (Piece piece : getPieceList(getCurrentColorTurn()))
					if (piece.getType() == PieceType.PAWN) {
						for (PiecePosition position : piece.getPossibleMoves())
							if ((!ignorePositions.containsKey(piece) || !ignorePositions.get(piece).contains(position)) &&
									Math.abs(piece.getRow() - position.getRow()) == 2) {
										movePieceTo(piece.getPosition(), position, true);
										if (!pieceColdBeCaptured(position)) { 
											triedVal = 12;
											lastTriedPiece = piece;
											lastTriedPosition = position;
											cloneBoard(recBoard, this);
											cpuSelectedPositionToMove = position;
											selectedPiece = piece;
											return;
										}
										cloneBoard(recBoard, this);
							}
						}
	
				triedVal = 13;
				// Tenta mover a pedra para um tile seguro no turno seguinte ao movimento
				for (PieceType type : PieceType.getListOfAll())
					for (Piece piece : getPieceList(getCurrentColorTurn()))
						if (piece.getType() == type) {
							for (PiecePosition position : piece.getPossibleMoves())
								if ((!ignorePositions.containsKey(piece) || !ignorePositions.get(piece).contains(position))) {
									lastTriedPiece = piece;
									lastTriedPosition = position;
									movePieceTo(piece.getPosition(), position, true);
									if (!pieceColdBeCaptured(piece)) {
										triedVal = 14;
										cloneBoard(recBoard, this);
										cpuSelectedPositionToMove = position;
										selectedPiece = piece;
										return;
									}
									cloneBoard(recBoard, this);
								}
						}
		
				triedVal = 15;
				// Tenta capturar uma pedra adversária mesmo que essa pedra possa ser capturada logo em seguida
				for (PieceType type : PieceType.getListOfAll())
					for (Piece piece : getPieceList(getCurrentColorTurn()))
						if (piece.getType() == type)
							for (Piece opponentPiece : getPieceList(opponentColor()))
								if ((!ignorePositions.containsKey(piece) || !ignorePositions.get(piece).contains(opponentPiece.getPosition())) &&
										piece.getPossibleMoves().contains(opponentPiece.getPosition())) {
											triedVal = 16;
											lastTriedPiece = piece;
											lastTriedPosition = opponentPiece.getPosition();
											cpuSelectedPositionToMove = new PiecePosition(opponentPiece.getPosition());
											selectedPiece = piece;
											return;
								}
			}
			catch (Exception e) {
				if (debug) {
					System.out.println("\nAI DEBUG:\ntriedVal: " + triedVal);
					System.out.println("lastTriedPiece at: " + (lastTriedPiece == null ? null : lastTriedPiece.getPosition()));
					System.out.println("lastTriedPosition: " + (lastTriedPosition == null ? null : lastTriedPosition));
					System.out.println("selectedPiece at: " + (selectedPiece == null ? null : selectedPiece.getPosition()));
					System.out.println("cpuSelectedPositionToMove: " + (cpuSelectedPositionToMove == null ? null : cpuSelectedPositionToMove));
					System.out.println(e.getMessage());
				}
				cloneBoard(recBoard, this);
				if (!ignorePositions.containsKey(lastTriedPiece))
					ignorePositions.put(lastTriedPiece, new ArrayList<>());
				ignorePositions.get(lastTriedPiece).add(lastTriedPosition);
			}
	}
	
	public void doCpuMoveSelectedPiece() {
		if (playMode == ChessPlayMode.PLAYER_VS_PLAYER)
			throw new GameException("Unable to use while in a PLAYER VS PLAYER mode.");
		if (!isCpuTurn())
			throw new GameException("It's not the CPU turn.");
		if (selectedPiece == null)
			throw new GameException("CPU not selected a piece. Call \".doCpuSelectAPiece()\" first.");
		movePieceTo(selectedPiece.getPosition(), cpuSelectedPositionToMove, false);
	}
	
	public Boolean cpuSelectedAPiece() {
		if (playMode == ChessPlayMode.PLAYER_VS_PLAYER)
			throw new GameException("Unable to use while in a PLAYER VS PLAYER mode.");
		return isCpuTurn() && selectedPiece != null;
	}
	
	public PiecePosition getCpuSelectedPositionToMove() {
		if (playMode == ChessPlayMode.PLAYER_VS_PLAYER)
			throw new GameException("Unable to use while in a PLAYER VS PLAYER mode.");
		if (!isCpuTurn())
			throw new GameException("It's not the CPU turn.");
		if (selectedPiece == null)
			throw new GameException("CPU not selected a piece. Call \".doCpuSelectAPiece()\" first.");
		return cpuSelectedPositionToMove;
	}

	private void changeTurn() {
		turns++;
		selectedPiece = null;
		currentColorTurn = opponentColor();
	}

	public Boolean pieceColdBeCaptured(Piece piece) {
		checkIfBoardWasValidated();
		validateNullVar(piece, "piece");
		for (Piece opponentPiece : getPieceList(opponentColor(piece.getColor())))
			if (opponentPiece.getPossibleMoves().contains(piece.getPosition()))
				return true;
		return false;
	}
	
	public Boolean pieceColdBeCaptured(PiecePosition position) {
		checkIfBoardWasValidated();
		validatePosition(position, "position");
		if (getPieceAt(position) == null)
			throw new InvalidPositionException(position + " - There is no piece");
		return pieceColdBeCaptured(getPieceAt(position));
	}
	
	public Boolean pieceCanDoSafeMove(Piece piece) {
		checkIfBoardWasValidated();
		validateNullVar(piece, "piece");
		Board backupBoard = newClonedBoard();
		PiecePosition fromPos = new PiecePosition(piece.getPosition());
		List<PiecePosition> possibleMoves = new ArrayList<>(piece.getPossibleMoves());
		for (PiecePosition myPos : possibleMoves) {
			movePieceTo(fromPos, myPos, true);
			if (!pieceColdBeCaptured(piece) && !isChecked(piece.getColor())) {
				cloneBoard(backupBoard, this);
				return true;
			}
			cloneBoard(backupBoard, this);
		}			
		return false;
	}
	
	public Boolean pieceCanDoSafeMove(PiecePosition position) {
		checkIfBoardWasValidated();
		validatePosition(position, "position");
		if (getPieceAt(position) == null)
			throw new InvalidPositionException(position + " - There is no piece");
		return pieceCanDoSafeMove(getPieceAt(position));
	}

	public Boolean isGameOver()
		{ return checkMate() || drawGame(); }

	public Boolean isChecked(PieceColor color) {
		checkIfBoardWasValidated();
		validateNullVar(color, "color");
		for (Piece piece : getPieceList(color))
			if (piece.getType() == PieceType.KING && pieceColdBeCaptured(piece))
				return true;
		return false;
	}
	
	public Boolean currentColorIsChecked()
		{ return isChecked(getCurrentColorTurn()); }
	
	public Boolean checkMate(PieceColor color) {
		checkIfBoardWasValidated();
		validateNullVar(color, "color");
		Board backupBoard = new Board();
		for (Piece p : getPieceList(color))
			for (PiecePosition pos : p.getPossibleMoves()) {
				cloneBoard(this, backupBoard);
				movePieceTo(p.getPosition(), pos, true);
				if (!currentColorIsChecked()) {
					cloneBoard(backupBoard, this);
					return false;
				}
				cloneBoard(backupBoard, this);
			}
		return true;
	}
	
	public Boolean checkMate()
		{ return checkMate(getCurrentColorTurn()); }
	
	public Boolean drawGame() {
		return getPieceList().size() == 2 &&
			getPieceList().get(0).getType() == PieceType.KING &&
			getPieceList().get(1).getType() == PieceType.KING;
	}
	
	private Piece getNewPieceInstance(Board board, PiecePosition position, PieceType type, PieceColor color) {
		if (type == PieceType.KING)
			return new King(board, position, color);
		else if (type == PieceType.QUEEN) 
			return new Queen(board, position, color);
		else if (type == PieceType.ROOK) 
			return new Rook(board, position, color);
		else if (type == PieceType.BISHOP) 
			return new Bishop(board, position, color);
		else if (type == PieceType.KNIGHT) 
			return new Knight(board, position, color);
		else 
			return new Pawn(board, position, color);
	}
	
	private Piece getNewPieceInstance(PiecePosition position, PieceType type, PieceColor color)
		{ return getNewPieceInstance(this, position, type, color); }
	
	private void addNewPiece(PiecePosition position, PieceType type, PieceColor color) throws NullPointerException,InvalidPositionException,BoardException {
		validateNullVar(position, "position");
		if (!isValidBoardPosition(position))
			throw new InvalidPositionException("Invalid board position");
		validateNullVar(type, "type");
		validateNullVar(color, "color");
		if (!isFreeSlot(position))
			throw new InvalidPositionException("This board position is not free");
		Piece piece = getNewPieceInstance(position, type, color);

		if (type == PieceType.KING &&
				getPieceList(color).stream().filter(p -> p.getType() == PieceType.KING).count() == 1)
					throw new BoardException("You can't put more than 1 King of each color on the board");
				
		board[position.getRow()][position.getColumn()] = piece;
		movedTurns.put(piece, 0);
	}

	public void addNewPiece(int row, int column, PieceType type, PieceColor color) throws GameException,BoardException
		{ addNewPiece(new PiecePosition(row, column), type, color); }

	public void addNewPiece(String position, PieceType type, PieceColor color) throws GameException,BoardException
		{ addNewPiece(PiecePosition.stringToPosition(position), type, color); }
	
	public void addNewPiece(PiecePosition position, Piece piece)
		{ addNewPiece(position, piece.getType(), piece.getColor()); }
	
	public void setBoard(Character[][] pieces) {
		PieceType[] types = {PieceType.PAWN, PieceType.BISHOP, PieceType.KING, PieceType.KNIGHT, PieceType.QUEEN, PieceType.ROOK};
		for (int y = 0; y < pieces.length; y++)
			for (int x = 0; x < pieces[y].length; x++) {
				PieceType type = null;
				PieceColor color = null;
				if (pieces[y][x] != ' ' && pieces[y][x] != '.' && pieces[y][x] != 'x' && pieces[y][x] != 'X')
					try {
						for (PieceType t : types)
							if (PieceType.getLet(t) == Character.toUpperCase(pieces[y][x])) {
								type = t;
								color = PieceType.getLet(t) == pieces[y][x] ? PieceColor.BLACK : PieceColor.WHITE;
							}
						addNewPiece(y, x, type, color);
					}
					catch (Exception e)
						{ throw new GameException(pieces[y][x] + " - Invalid piece let."); }
			}
	}

}