package pieces;

import java.util.ArrayList;
import java.util.List;

import board.Board;
import enums.PieceColor;
import enums.PieceType;
import gameutil.Position;
import piece.Piece;

public class King extends Piece  {

	public King(Board board, Position position, PieceColor color)
		{ super(board, position, PieceType.KING, color); }

	@Override
	public List<Position> getPossibleMoves() {
		int[][] inc = {{-1,0,1,-1,0,1,-1,1,1,1}, {-1,-1,-1,1,1,1,0,0,0,0}};
		
		// Castling special move
		if (!wasMoved()) {
			Position p2 = new Position(getPosition());
			for (int c = 0; c <= 7; c += 7) {
				p2.setX(c);
				Piece piece = getBoard().getPieceAt(p2);
				if (piece != null && !piece.wasMoved() && piece.isRook(getColor())) {
					do 
						p2.incX(c == 0 ? 1 : -1);
					while (getBoard().isFreeSlot(p2));
					if (p2.equals(getPosition()))
						inc[1][c == 0 ? 8 : 9] = c == 0 ? -2 : 2;
				}
			}
		}
		
		/* 10 directions check (9th and 10th is for castling special move
		 * (these values will be changed if castling check pass, otherwise
		 * they will be just repeated directions))
		 */
		List<Position> moves = new ArrayList<>();
		Position p = new Position(getPosition());
		for (int dir = 0; dir < (!wasMoved() ? 10 : 8); dir++) {
			p.setPosition(getPosition());
			p.incPosition(inc[0][dir], inc[1][dir]);
			Piece piece = getBoard().getPieceAt(p);
			if (piece == null || !isSameColorOf(piece))
				moves.add(new Position(p));
		}
		return moves;
	}
	
	public Boolean isOpponentQueenAround()
		{ return !getBoard().getPieceListByColor(getOpponentColor(), p -> p.isQueen()).isEmpty(); }

	@Override
	public List<Position> getPossibleCaptureMoves()
		{ return getPossibleMoves(); }

	@Override
	public String toString()	
		{ return PieceType.KING.name(); }

	@Override
	public char let()
		{ return PieceType.KING.getLet(); }

}