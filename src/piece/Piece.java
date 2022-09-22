package piece;

import java.util.ArrayList;
import java.util.List;

import board.Board;
import enums.PieceColor;
import enums.PieceType;
import gameutil.Position;

public abstract class Piece implements Comparable<Piece> {
	
	private Position position;
	private PieceType type;
	private Board board;
	private PieceColor color;
	private int movedTurns;
	
	public Piece(Board board, Position position, PieceType type, PieceColor color) {
		this.board = board;
		this.position = new Position(position);
		movedTurns = 0;
		setType(type);
		setColor(color);
	}
	
	public void copyPiece(Piece piece) {
		type = piece.type;
		color = piece.color;
		this.position = new Position(piece.position);
		movedTurns = piece.movedTurns;
	}

	public Position getPosition()
		{ return position; }

	/**
	 * Retorna o board associado a pedra
	 */
	public Board getBoard()
		{ return board; }

	/**
	 * Retorna a cor da pedra atual
	 */
	public PieceColor getColor()
		{ return color; }
	
	/**
	 * Define a cor das pedras adversárias
	 */
	public PieceColor getOpponentColor()
		{ return color.getOppositeColor(); }
	
	/**
	 * Define a cor da pedra atual
	 */
	public void setColor(PieceColor color)
		{ this.color = color; }
	
	/**
	 * Retorna o tipo da pedra atual
	 */
	public PieceType getType()
		{ return type; }
	
	/**
	 * Retorna o valor da pedra atual baseada no tipo dela
	 */
	public float getTypeValue()
		{ return type.getValue();	}
	
	/**
	 * Retorna o valor inteiro da pedra atual baseada no tipo dela
	 */
	public float getIntTypeValue()
		{ return type.getIntValue();	}

	/**
	 * Define o tipo da pedra atual
	 */
	public void setType(PieceType type)
		{ this.type = type; }
	
	/**
	 * Retorna o total de turnos que a pedra já se moveu
	 */
	public int getMovedTurns()
		{ return movedTurns; }

	/**
	 * Define o total de turnos que a pedra já se moveu
	 */
	public void setMovedTurns(int value)	
		{ movedTurns = value; }

	/**
	 * Incrementa no valor informado, o total de turnos que a pedra já se moveu
	 */
	public void incMovedTurns(int value)	
		{ movedTurns += value; }
	
	/**
	 * Verifica se a pedra atual já se moveu
	 */
	public Boolean wasMoved()
		{ return getMovedTurns() > 0; }
	
	/**
	 * Verifica se a pedra está presa
	 */
	public Boolean isStucked()
		{ return getPossibleMoves().isEmpty(); }
	
	/**
	 * Verifica se a pedra informada tem a mesma cor da pedra atual
	 */
	public Boolean isSameColorOf(Piece targetPiece)
		{ return targetPiece != null && getColor() == targetPiece.getColor(); }
	
	/**
	 * Verifica se a pedra atual é da cor preta
	 */
	public Boolean isBlack()
		{ return getColor() == PieceColor.BLACK; }

	/**
	 * Verifica se a pedra atual é da cor branca
	 */
	public Boolean isWhite()
		{ return getColor() == PieceColor.WHITE; }

	/**
	 * Verifica se a pedra informada tem o mesmo tipo da pedra atual
	 */
	public Boolean isSameTypeOf(Piece targetPiece)
		{ return targetPiece != null && getType() == targetPiece.getType(); }

	/**
	 * Verifica se a pedra atual é um peão da cor especificada
	 */
	public Boolean isPawn(PieceColor color)
		{ return is(PieceType.PAWN, color); }
	
	/**
	 * Verifica se a pedra atual é um peão, independente da cor
	 */
	public Boolean isPawn()
		{ return isPawn(null); }
	
	/**
	 * Verifica se a pedra atual é um peão branco
	 */
	public Boolean isWhitePawn()
		{ return isPawn(PieceColor.WHITE); }

	/**
	 * Verifica se a pedra atual é um peão preto
	 */
	public Boolean isBlackPawn()
		{ return isPawn(PieceColor.BLACK); }

	/**
	 * Verifica se a pedra atual é um rei da cor especificada
	 */
	public Boolean isKing(PieceColor color)
		{ return is(PieceType.KING, color); }
	
	/**
	 * Verifica se a pedra atual é um rei, independente da cor
	 */
	public Boolean isKing()
		{ return isKing(null); }
	
	/**
	 * Verifica se a pedra atual é um rei branco
	 */
	public Boolean isWhiteKing()
		{ return isKing(PieceColor.WHITE); }

	/**
	 * Verifica se a pedra atual é um rei preto
	 */
	public Boolean isBlackKing()
		{ return isKing(PieceColor.BLACK); }

	/**
	 * Verifica se a pedra atual é um rainha da cor especificada
	 */
	public Boolean isQueen(PieceColor color)
		{ return is(PieceType.QUEEN, color); }
	
	/**
	 * Verifica se a pedra atual é um rainha, independente da cor
	 */
	public Boolean isQueen()
		{ return isQueen(null); }
	
	/**
	 * Verifica se a pedra atual é um rainha branco
	 */
	public Boolean isWhiteQueen()
		{ return isQueen(PieceColor.WHITE); }

	/**
	 * Verifica se a pedra atual é um rainha preto
	 */
	public Boolean isBlackQueen()
		{ return isQueen(PieceColor.BLACK); }
	
	/**
	 * Verifica se a pedra atual é um cavalo da cor especificada
	 */
	public Boolean isKnight(PieceColor color)
		{ return is(PieceType.KNIGHT, color); }
	
	/**
	 * Verifica se a pedra atual é um cavalo, independente da cor
	 */
	public Boolean isKnight()
		{ return isKnight(null); }
	
	/**
	 * Verifica se a pedra atual é um cavalo branco
	 */
	public Boolean isWhiteKnight()
		{ return isKnight(PieceColor.WHITE); }

	/**
	 * Verifica se a pedra atual é um cavalo preto
	 */
	public Boolean isBlackKnight()
		{ return isKnight(PieceColor.BLACK); }

	/**
	 * Verifica se a pedra atual é um bispo da cor especificada
	 */
	public Boolean isBishop(PieceColor color)
		{ return is(PieceType.BISHOP, color); }
	
	/**
	 * Verifica se a pedra atual é um bispo, independente da cor
	 */
	public Boolean isBishop()
		{ return isBishop(null); }
	
	/**
	 * Verifica se a pedra atual é um bispo branco
	 */
	public Boolean isWhiteBishop()
		{ return isBishop(PieceColor.WHITE); }

	/**
	 * Verifica se a pedra atual é um bispo preto
	 */
	public Boolean isBlackBishop()
		{ return isBishop(PieceColor.BLACK); }
	
	/**
	 * Verifica se a pedra atual é um torre da cor especificada
	 */
	public Boolean isRook(PieceColor color)
		{ return is(PieceType.ROOK, color); }
	
	/**
	 * Verifica se a pedra atual é um torre, independente da cor
	 */
	public Boolean isRook()
		{ return isRook(null); }
	
	/**
	 * Verifica se a pedra atual é um torre branco
	 */
	public Boolean isWhiteRook()
		{ return isRook(PieceColor.WHITE); }

	/**
	 * Verifica se a pedra atual é um torre preto
	 */
	public Boolean isBlackRook()
		{ return isRook(PieceColor.BLACK); }

	/**
	 * Verifica se a pedra é do tipo informado
	 */
	public Boolean isSameTypeOf(PieceType type)
		{ return getType() == type; }
	
	/**
	 * Verifica se a pedra é da cor informada
	 */
	public Boolean isSameColorOf(PieceColor color)
		{ return getColor() == color; }
	
	/**
	 * Verifica se a pedra informada tem o mesmo tipo e cor informados
	 */
	public Boolean is(PieceType type, PieceColor color)
		{ return getType() == type && getColor() == color; }
	
	/**
	 * Verifica se a pedra informada tem o mesmo tipo e cor da pedra atual
	 */
	public Boolean isTwin(Piece piece)
		{ return piece.is(getType(), getColor()); }
	
	/**
	 * Verifica se o tipo da pedra atual é mais forte que o tipo da pedra informada
	 */
	public Boolean strongerThan(Piece piece)
		{ return getType().getIntValue() > piece.getType().getIntValue(); }
	
	/**
	 * Verifica se o tipo da pedra atual é mais forte ou similar ao tipo da pedra informada
	 */
	public Boolean strongerOrSameThan(Piece piece)
		{ return getType().getIntValue() >= piece.getType().getIntValue(); }

	/**
	 * Verifica se a pedra atual pode capturar a pedra informada
	 */
	public Boolean couldCapture(Piece targetPiece) {
		return targetPiece != null && !isSameColorOf(targetPiece) &&
			getPossibleCaptureMoves().contains(targetPiece.getPosition());
	}

	/**
	 * Verifica se a pedra atual pode ser capturada pela pedra informada
	 */
	public Boolean couldBeCapturedBy(Piece targetPiece) {
		return targetPiece != null && !isSameColorOf(targetPiece) &&
			targetPiece.getPossibleCaptureMoves().contains(getPosition());
	}
	
	public Boolean havePossibleSafeMoves()
		{ return !getPossibleSafeMoves().isEmpty(); }

	/**
	 * Verifica se a pedra pode se mover para a posição informada
	 */
	public Boolean canMoveToPosition(Position position)
		{ return getPossibleMoves().contains(position) || getPossibleCaptureMoves().contains(position); }
	
	/**
	 * Verifica se a pedra pode se mover para a posição informada sem risco de ser capturada
	 */
	public Boolean canSafeMoveToPosition(Position position) {
		if (!getBoard().isValidBoardPosition(position))
			return false;
		Board recBoard = getBoard().newClonedBoard();
		Boolean isSafe = false;
		try {
			getBoard().removePiece(this);
			Piece piece = getBoard().getPieceAt(position);
			if (piece == null || !piece.isSameColorOf(this)) {
				if (piece != null)
					getBoard().removePiece(piece);
				getBoard().addPiece(position, this);
				isSafe = getBoard().pieceIsAtSafePosition(this);
			}
		}
		catch (Exception e) {}
		Board.cloneBoard(recBoard, board);
		return isSafe;
	}

	public List<Position> getPossibleSafeMoves() {
		List<Position> list = new ArrayList<>();
		List<Position> possibleMoves = new ArrayList<>(getPossibleMoves());
		possibleMoves.addAll(getPossibleCaptureMoves());
		for (Position position : possibleMoves)
			if (canSafeMoveToPosition(position))
				list.add(position);
		return list;
	}
	
	public String getInfo()
		{ return getColor() + " " + getType().name() + " at " + getPosition(); }

	/**
	 * Retorna a lista de posições onde a pedra pode ser movida
	 */
	public abstract List<Position> getPossibleMoves();

	/**
	 * Retorna a lista de posições onde a pedra pode capturar (Só muda no peão, as outras pedras devem retornar getPossibleMoves()
	 */
	public abstract List<Position> getPossibleCaptureMoves();

	/**
	 * Retorna a letra correspondente ao tipo da pedra
	 */
	public abstract char let();
	
	@Override
	public int compareTo(Piece p)
		{ return getTypeValue() > p.getTypeValue() ? 1 : getTypeValue() < p.getTypeValue() ? -1 : 0; }

}