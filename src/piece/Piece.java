package piece;

import java.util.List;

import board.Board;
import enums.PieceColor;
import enums.PieceType;

public abstract class Piece {
	
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

	public int getRow()
		{ return position.getRow(); }
	
	public void incRow(int value)
		{ position.incRow(value); }

	public int getColumn()
		{ return position.getColumn(); }

	public void incColumn(int value)
		{ position.incColumn(value); }

	public PiecePosition getPosition()
		{ return position; }
	
	public void setPosition(PiecePosition position)
		{ this.position = position; }
	
	public void setPosition(int row, int column)
		{ this.position.setValues(row, column); }

	public PieceColor getColor()
		{ return color; }
	
	public void setColor(PieceColor color)
		{ this.color = color; }
	
	public PieceType getType()
		{ return type; }
	
	public void setType(PieceType type)
		{ this.type = type; }
	
	public int getMovedTurns()
		{ return movedTurns; }

	public void setMovedTurns(int value)	
		{ movedTurns = value; }

	public void incMovedTurns(int value)	
		{ movedTurns += value; }
	
	public Boolean wasMoved()
		{ return getMovedTurns() > 0; }
	
	public Board getBoard()
		{ return board; }
	
	public Boolean isStucked()
		{ return getPossibleMoves().size() == 0; }
	
	public Boolean canMoveToPosition(PiecePosition position) 
		{ return getPossibleMoves().contains(position); }
	
	public abstract List<PiecePosition> getPossibleMoves();

	@Override
	public String toString()
		{ return "PIECE"; }
	
	public char let()
		{ return '-'; }

}