package piece;

import java.util.ArrayList;
import java.util.List;

import board.Board;
import enums.PieceColor;
import enums.PieceType;

public abstract class Piece implements Comparable<Piece> {
	
	private PieceType type;
	private Board board;
	private PieceColor color;
	private PiecePosition position;
	private int movedTurns;
	
	public Piece(Board board, PiecePosition position, PieceType type, PieceColor color) {
		this.board = board;
		setPosition(position);
		movedTurns = 0;
		setType(type);
		setColor(color);
	}

	public void copyPiece(Piece piece) {
		type = piece.type;
		color = piece.color;
		position.setValues(piece.position);
		movedTurns = piece.movedTurns;
	}

	/**
	 * Retorna o board associado a pedra
	 */
	public Board getBoard()
		{ return board; }

	/**
	 * Retorna a linha do tabuleiro onde a pedra atual está
	 */
	public int getRow()
		{ return position.getRow(); }
	
	/**
	 * Incrementa a linha da pedra
	 */
	public void incRow(int value)
		{ position.incRow(value); }

	/**
	 * Retorna a coluna do tabuleiro onde a pedra atual está
	 */
	public int getColumn()
		{ return position.getColumn(); }

	/**
	 * Incrementa a coluna da pedra
	 */
	public void incColumn(int value)
		{ position.incColumn(value); }

	/**
	 * Retorna a posição do tabuleiro onde a pedra atual está
	 */
	public PiecePosition getPosition()
		{ return position; }
	
	/**
	 * Define a posição do tabuleiro onde a pedra atual está
	 */
	public void setPosition(PiecePosition position)
		{ this.position = position == null ? null : new PiecePosition(position); }
	
	/**
	 * Define a linha e coluna do tabuleiro onde a pedra atual está
	 */
	public void setPosition(int row, int column)
		{ this.position.setValues(row, column); }

	/**
	 * Retorna a cor da pedra atual
	 */
	public PieceColor getColor()
		{ return color; }
	
	/**
	 * Define a cor das pedras adversárias
	 */
	public PieceColor getOpponentColor()
		{ return color == PieceColor.BLACK ? PieceColor.WHITE : PieceColor.BLACK; }
	
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
	 * Verifica se a pedra pode se mover para a posição informada
	 */
	public Boolean canMoveToPosition(PiecePosition position) 
		{ return getPossibleMoves().contains(position); }
	
	/**
	 * Verifica se a pedra informada tem a mesma cor da pedra atual
	 */
	public Boolean isSameColorOf(Piece targetPiece)
		{ return targetPiece != null && getColor() == targetPiece.getColor(); }
	
	/**
	 * Verifica se a cor da pedra atual é o mesmo da cor informada
	 */
	public Boolean isSameColorOf(PieceColor color)
		{ return getColor() == color; }

	/**
	 * Verifica se a pedra informada tem o mesmo tipo da pedra atual
	 */
	public Boolean isSameTypeOf(Piece targetPiece)
		{ return targetPiece != null && getType() == targetPiece.getType(); }

	/**
	 * Verifica se o tipo da pedra atual é o mesmo do tipo informado
	 */
	public Boolean isSameTypeOf(PieceType type)
		{ return getType() == type; }
	
	/**
	 * Verifica se a pedra informada tem o mesmo tipo e cor do tipo e cor informados
	 */
	public Boolean isSameTypeAndColorOf(PieceType type, PieceColor color)
		{ return isSameTypeOf(type) && isSameColorOf(color); }

	/**
	 * Verifica se a pedra informada tem o mesmo tipo e cor da pedra atual
	 */
	public Boolean isTwin(Piece piece)
		{ return isSameColorOf(piece) && isSameTypeOf(piece); }
	
	/**
	 * Verifica se a pedra está na posição informada
	 */
	public Boolean isAt(PiecePosition position)
		{ return getPosition().equals(position); }
	
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
				getPossibleMoves().contains(targetPiece.getPosition());
	}

	/**
	 * Verifica se a pedra atual pode ser capturada pela pedra informada
	 */
	public Boolean couldBeCapturedBy(Piece targetPiece) {
		return targetPiece != null && !isSameColorOf(targetPiece) &&
			targetPiece.getPossibleMoves().contains(getPosition());
	}
	
	public List<PiecePosition> getPossibleSafeMoves() {
		List<PiecePosition> list = new ArrayList<>();
		for (PiecePosition position : getPossibleMoves())
			if (getBoard().pieceCanGoToASafePosition(this, position))
				list.add(position);
		return list;
	}
	
	public String getInfo()
		{ return getColor() + " " + getType().name() + " at " + getPosition(); }

	/**
	 * Retorna a lista de posições onde a pedra pode ser movida
	 */
	public abstract List<PiecePosition> getPossibleMoves();

	/**
	 * Retorna a letra correspondente ao tipo da pedra
	 */
	public abstract char let();
	
	@Override
	public int compareTo(Piece p)
		{ return getTypeValue() > p.getTypeValue() ? 1 : getTypeValue() < p.getTypeValue() ? -1 : 0; }

}