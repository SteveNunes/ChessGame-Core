package board;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
import piece.Position;
import pieces.Bishop;
import pieces.King;
import pieces.Knight;
import pieces.Pawn;
import pieces.Queen;
import pieces.Rook;

public class Board {

	private static List<Board> undoBoards = new ArrayList<>();
	private static int undoIndex = -1;
	
	private Boolean blackKingWasAdded;
	private Boolean whiteKingWasAdded;
	private Boolean atLeastTwoBlackPiecesAdded;
	private Boolean atLeastTwoWhitePiecesAdded;
	private int turns;
	private List<Piece> capturedPieces;
	private Piece[][] board;
	private Piece selectedPiece;
	private Piece enPassantPiece;
	private PieceColor currentColorTurn;
	
	public Board() { 
		blackKingWasAdded = false;
		whiteKingWasAdded = false;
		atLeastTwoBlackPiecesAdded = false;
		atLeastTwoWhitePiecesAdded = false;
		board = new Piece[8][8];
		capturedPieces = new ArrayList<>();
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
		targetBoard.selectedPiece = sourceBoard.selectedPiece;
		targetBoard.enPassantPiece = sourceBoard.enPassantPiece;
		targetBoard.blackKingWasAdded = sourceBoard.blackKingWasAdded;
		targetBoard.whiteKingWasAdded = sourceBoard.whiteKingWasAdded;
		targetBoard.atLeastTwoBlackPiecesAdded = sourceBoard.atLeastTwoBlackPiecesAdded;
		targetBoard.atLeastTwoWhitePiecesAdded = sourceBoard.atLeastTwoWhitePiecesAdded;
		targetBoard.currentColorTurn = sourceBoard.currentColorTurn;
		targetBoard.capturedPieces = new ArrayList<>(sourceBoard.capturedPieces);
		targetBoard.turns = sourceBoard.turns;
	}

	public static void resetUndoMoves()
		{ undoBoards.clear(); }
	
	public Boolean canUndoMove()
		{ return undoIndex > 0; }
	
	public Boolean canRedoMove()
		{ return undoIndex + 1 < undoBoards.size(); }

	private void saveBoardForUndo() {
		Board saveBoard = new Board();
		cloneBoard(this, saveBoard);
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

	public void reset() {
		turns = 0;
		selectedPiece = null;
		enPassantPiece = null;
		currentColorTurn = new SecureRandom().nextInt(2) == 0 ? PieceColor.BLACK : PieceColor.WHITE;
		capturedPieces.clear();
		resetBoard(board);
	}

	private void validateBoard() throws BoardException {
		if (!blackKingWasAdded)
			throw new BoardException("The Black King was not found on the board. Set pieces on the board using .AddNewPiece()");
		if (!whiteKingWasAdded)
			throw new BoardException("The White King was not found on the board. Set pieces on the board using .AddNewPiece()");
		if (!atLeastTwoBlackPiecesAdded || !atLeastTwoWhitePiecesAdded)
			throw new BoardException("You must set at least 2 pieces of each color on the board, including a King for each color. Set pieces on the board using .AddNewPiece()");
		if (undoBoards.isEmpty())
			saveBoardForUndo();
	}

	private void validatePosition(Position position, String varName) throws NullPointerException,InvalidPositionException {
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
	
	public void resetBoard(Piece[][] board) { 
		for (Piece[] b : board)
			Arrays.fill(b, null);
	}

	public int getTurns() {
		validateBoard();
		return turns;
	}
	
	public PieceColor getWinnerColor() {
		validateBoard();
		return checkMate() ? opponentColor() : null;
	}
	
	public PieceColor getCurrentColorTurn() {
		validateBoard();
		return currentColorTurn;
	}
	
	public Piece getPromotedPiece() {
		validateBoard();
		for (Piece piece : board[currentColorTurn == PieceColor.BLACK ? 0 : 7])
			if (piece != null && piece.getType() == PieceType.PAWN)
				return piece;
		return null;
	}
	
	public Boolean pieceWasPromoted() {
		validateBoard();
		return getPromotedPiece() != null;
	}
	
	public void promotePieceTo(PieceType newType) throws PromotionException,NullPointerException {
		validateBoard();
		if (!pieceWasPromoted())
			throw new PromotionException("There is no promoted piece");
		validateNullVar(newType, "newType");
		if (newType == PieceType.PAWN || newType == PieceType.KING)
			throw new PromotionException("You can't promote a Pawn to a " + newType.getValue());
		Position pos = new Position(getPromotedPiece().getPosition());
		PieceColor color = getPromotedPiece().getColor();
		removePiece(getPromotedPiece().getPosition());
		addNewPiece(pos, newType, color);
		changeTurn();
	}
	
	public Piece getEnPassantPiece() {
		validateBoard();
		if (selectedPiece == null || selectedPiece.getType() != PieceType.PAWN)
			return null;
		return enPassantPiece;
	}
	
	public Position getEnPassantCapturePosition() {
		validateBoard();
		if (!checkEnPassant())
			return null;
		Position position = new Position(getEnPassantPiece().getPosition());
		position.incValues(getEnPassantPiece().getColor() == PieceColor.WHITE ? -1 : -1, 0);
		return position;
	}

	public Boolean checkEnPassant() {
		validateBoard();
		return getEnPassantPiece() != null;
	}

	public Boolean checkIfCastlingIsPossible(King king, Rook rook) {
		validateBoard();
		validateNullVar(king, "king");
		validateNullVar(rook, "rook");
		if (isOpponentPieces(king, rook))
			throw new GameException("king and rook must be from the same color");
		if (king.wasMoved() || rook.wasMoved() || currentColorIsChecked())
			return false;

		Position kingPosition = new Position(king.getPosition());
		Position rookPosition = new Position(rook.getPosition());
		Boolean toLeft = kingPosition.getColumn() > rookPosition.getColumn();
		
		while (!kingPosition.equals(rookPosition)) {
			kingPosition.incColumn(toLeft ? -1 : 1);
			if (isFreeSlot(kingPosition)) {
				addNewPiece(kingPosition, king);
				if (currentColorIsChecked()) {
					removePiece(kingPosition);
					return false;
				}
				removePiece(kingPosition);
			}
		}
		return kingPosition.equals(rookPosition);
	}

	private Boolean checkIfCastlingIsPossible(Piece king, Piece rook) {
		if (king.getType() != PieceType.KING || rook.getType() != PieceType.ROOK)
			return false;
		return checkIfCastlingIsPossible((King) king, (Rook) rook);
	}
	
	public Boolean isValidBoardPosition(Position position) {
		validateNullVar(position, "position");
		return position.getColumn() >= 0 && position.getColumn() < 8 &&
			position.getRow() >= 0 && position.getRow() < 8;
	}
	
	public Boolean isFreeSlot(Position position) {
		validateNullVar(position, "position");
		return isValidBoardPosition(position) && getPieceAt(position) == null;
	}
	
	public Piece getPieceAt(Position position) {
		validateNullVar(position, "position");
		return !isValidBoardPosition(position) ? null : board[position.getRow()][position.getColumn()];
	}
	
	public Boolean isOpponentPieces(Piece p1, Piece p2) {
		validateNullVar(p1, "p1");
		validateNullVar(p2, "p2");
		return p1.getColor() != p2.getColor();
	}
	
	public Boolean isOpponentPiece(Position position, PieceColor color) { 
		validateBoard();
		validateNullVar(color, "color");
		return isValidBoardPosition(position) && !isFreeSlot(position) && getPieceAt(position).getColor() != color;
	}
	
	public Boolean isOpponentPiece(Position position) {
		validateBoard();
		return getPieceAt(position) != null && isOpponentPiece(position, getCurrentColorTurn());
	}

	public Piece getSelectedPiece() {
		validateBoard();
		return selectedPiece;
	}
	
	public Boolean pieceIsSelected() {
		validateBoard();
		return getSelectedPiece() != null;
	}

	public Piece selectPiece(Position position) throws BoardException,InvalidPositionException,PieceSelectionException {
		validateBoard();
		validatePosition(position, "position");
		if (isFreeSlot(position))
			throw new PieceSelectionException("There is no piece on that position");
		if (getPieceAt(position).getColor() != getCurrentColorTurn())
			throw new PieceSelectionException("This piece is not yours");
		if (getPieceAt(position).isStucked())
			throw new PieceSelectionException("This piece is stucked");
		return (selectedPiece = getPieceAt(position));
	}
	
	public PieceColor opponentColor(PieceColor color) { 
		validateBoard();
		validateNullVar(color, "color");
		return color == PieceColor.BLACK ? PieceColor.WHITE : PieceColor.BLACK;
	}
	
	public PieceColor opponentColor() {
		validateBoard();
		return opponentColor(getCurrentColorTurn());
	}
	
	public void addCapturedPiece(Piece piece) {
		validateBoard();
		validateNullVar(piece, "piece");
		if (capturedPieces.contains(piece))
			throw new GameException("piece is already on captured pieces list");
		capturedPieces.add(piece);
	}

	public void removeCapturedPiece(Piece piece) {
		validateBoard();
		validateNullVar(piece, "piece");
		if (!capturedPieces.contains(piece))
			throw new GameException("piece is not on captured pieces list");
		capturedPieces.remove(piece);
	}
	
	public List<Piece> getCapturedPieces(PieceColor color) {
		validateBoard();
		return capturedPieces.stream()
			.filter(c -> color == null || c.getColor() == color)
			.collect(Collectors.toList());
	}

	public List<Piece> getCapturedPieces()
		{ return getCapturedPieces(null); }
	
	private void addPiece(Position position, Piece piece) { 
		validateBoard();
		validatePosition(position, "position");
		validateNullVar(piece, "piece");
		if (!isFreeSlot(position))
			throw new InvalidPositionException("The slot at this position is not free");
		board[position.getRow()][position.getColumn()] = piece;
		piece.setPosition(position);
	}

	private void removePiece(Position position) { 
		validateBoard();
		validatePosition(position, "position");
		if (isFreeSlot(position))
			throw new InvalidPositionException("There's no piece at this slot position");
		board[position.getRow()][position.getColumn()] = null;
	}

	public void cancelSelection() {
		validateBoard();
		if (pieceWasPromoted())
			throw new BoardException("You must promote your pawn");
		selectedPiece = null;
	}
	
	private Piece movePieceTo(Position sourcePos, Position targetPos, Boolean justTesting) throws InvalidMoveException {
		if (!justTesting) {
			validateBoard();
			validatePosition(sourcePos, "sourcePos");
			validatePosition(targetPos, "targetPos");
		}
		
		sourcePos = new Position(sourcePos);
		targetPos = new Position(targetPos);

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
		
		if (!sourcePiece.canMoveToPosition(targetPos)) {
			if (justTesting)
				return null;
			throw new InvalidMoveException("Invalid move for this piece");
		}

		Board cloneBoard = new Board();
		cloneBoard(this, cloneBoard);

		// Castling special move
		Position rookPosition = new Position(sourcePos);
		Boolean rookAtLeft = targetPos.getColumn() < sourcePos.getColumn();
		rookPosition.setColumn(rookAtLeft ? 0 : 7);
		if (targetPiece != null && checkIfCastlingIsPossible(sourcePiece, targetPiece)) {
			removePiece(rookPosition);
			rookPosition = new Position(targetPos);
			rookPosition.incColumn(rookAtLeft ? 1 : -1);
			addNewPiece(rookPosition, (Rook) getPieceAt(rookPosition));
		}

		removePiece(sourcePos);
		
		if (checkEnPassant() && getEnPassantCapturePosition().equals(targetPos))
			targetPiece = getEnPassantPiece(); // Verifica se o peão atual realizou um movimento de captura EnPassant
		enPassantPiece = null;

		if (sourcePiece.getType() == PieceType.PAWN &&
				Math.abs(sourcePos.getRow() - targetPos.getRow()) == 2)
					enPassantPiece = sourcePiece; // Marca peão que iniciou movendo 2 casas como EnPassant

		if (targetPiece != null) {
			removePiece(targetPiece.getPosition());
			addCapturedPiece(targetPiece);
		}

		addPiece(targetPos, sourcePiece);
		sourcePiece.incMovedTurns(1);

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

	private void changeTurn() {
		validateBoard();
		if (pieceWasPromoted())
			throw new GameException("You must promote the pawn");
		turns++;
		selectedPiece = null;
		currentColorTurn = opponentColor();
	}

	public Piece movePieceTo(Position targetPos) throws BoardException 
		{ return movePieceTo(getSelectedPiece().getPosition(), targetPos, false); }
	
	public Boolean pieceColdBeCaptured(Position position, PieceColor color) {
		validateBoard();
		validatePosition(position, "position");
		List<Piece> opponentPieceList = getPieceList(color == PieceColor.BLACK ? PieceColor.WHITE : PieceColor.BLACK);
		for (Piece opponentPiece : opponentPieceList)
			if (opponentPiece.getPossibleMoves().contains(position))
				return true;
		return false;
	}
	
	public Boolean pieceColdBeCaptured(Piece piece)
		{ return pieceColdBeCaptured(piece.getPosition(), piece.getColor()); }
	
	public Boolean pieceCanDoSafeMove(Piece piece) {
		validateBoard();
		validateNullVar(piece, "piece");
		Board backupBoard = new Board();
		Position fromPos = new Position(piece.getPosition());
		cloneBoard(this, backupBoard);
		for (Position myPos : piece.getPossibleMoves()) {
			movePieceTo(fromPos, myPos, true);
			if (!pieceColdBeCaptured(piece) && !isChecked(piece.getColor())) {
				cloneBoard(backupBoard, this);
				return true;
			}
			cloneBoard(backupBoard, this);
		}			
		return false;
	}

	public Boolean isChecked(PieceColor color) {
		validateBoard();
		validateNullVar(color, "color");
		List<Piece> pieceList = getPieceList(color);
		for (Piece piece : pieceList)
			if (piece.getType() == PieceType.KING && pieceColdBeCaptured(piece))
				return true;
		return false;
	}
	
	public Boolean currentColorIsChecked()
		{ return isChecked(getCurrentColorTurn()); }
	
	public Boolean checkMate(PieceColor color) {
		validateBoard();
		validateNullVar(color, "color");
		Piece king = null;
		List<Piece> pieceList = getPieceList(color);
		for (Piece piece : pieceList)
			if (piece.getType() == PieceType.KING && pieceCanDoSafeMove(king = piece))
				return false;

		Board backupBoard1 = new Board();
		Board backupBoard2 = new Board();
		Position kingPos = new Position(king.getPosition());
		for (Piece p : pieceList)
			if (p.getType() != PieceType.KING)
				for (Piece p2 : pieceList) {
					Position fromPos = new Position(p2.getPosition());
					for (Position pos : p2.getPossibleMoves()) {
						cloneBoard(this, backupBoard1);
						movePieceTo(fromPos, pos, true);
						for (Position pos2 : king.getPossibleMoves()) {
							cloneBoard(this, backupBoard2);
							movePieceTo(kingPos, pos2, true);
							if (pieceCanDoSafeMove(king)) {
								cloneBoard(backupBoard1, this);
								return false;
							}
							cloneBoard(backupBoard2, this);
						}
						cloneBoard(backupBoard1, this);
					}
				}
		return true;
	}
	
	public Boolean checkMate()
		{ return checkMate(getCurrentColorTurn()); }
	
	public Piece getNewPieceInstance(Board board, Position position, PieceType type, PieceColor color) {
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
	
	public Piece getNewPieceInstance(Position position, PieceType type, PieceColor color)
		{ return getNewPieceInstance(this, position, type, color); }
	
	public Piece getNewPieceInstance(Board board, Piece piece)
		{ return getNewPieceInstance(board, piece.getPosition(), piece.getType(), piece.getColor()); }

	public Piece getNewPieceInstance(Piece piece)
		{ return getNewPieceInstance(this, piece); }

	public void addNewPiece(Position position, PieceType type, PieceColor color) throws NullPointerException,InvalidPositionException {
		validateNullVar(position, "position");
		if (!isValidBoardPosition(position))
			throw new InvalidPositionException("Invalid board position");
		validateNullVar(type, "type");
		validateNullVar(color, "color");
		if (!isFreeSlot(position))
			throw new InvalidPositionException("This board position is not free");
		Piece piece = getNewPieceInstance(position, type, color);
		if (type == PieceType.KING) {
			if (color == PieceColor.BLACK)
				blackKingWasAdded = true;
			else
				whiteKingWasAdded = true;
		}
		board[position.getRow()][position.getColumn()] = piece;
		if (getPieceList(color).size() == 2) {
			if (color == PieceColor.BLACK)
				atLeastTwoBlackPiecesAdded = true;
			else
				atLeastTwoWhitePiecesAdded = true;
		}
	}

	public void addNewPiece(int row, int column, PieceType type, PieceColor color) throws GameException,BoardException
		{ addNewPiece(new Position(row, column), type, color); }

	public void addNewPiece(String position, PieceType type, PieceColor color) throws GameException,BoardException
		{ addNewPiece(Position.stringToPosition(position), type, color); }
	
	public void addNewPiece(Position position, Piece piece)
		{ addNewPiece(position, piece.getType(), piece.getColor()); }

}