package pieces;

import java.util.ArrayList;
import java.util.List;

import board.Board;
import enums.PieceColor;
import enums.PieceType;
import gameutil.Position;
import piece.Piece;

public class Knight extends Piece  {

	public Knight(Board board, Position position, PieceColor color) 
		{ super(board, position, PieceType.KNIGHT, color); }

	@Override
	public List<Position> getPossibleMoves() {
		List<Position> moves = new ArrayList<>();
		int[][] inc = {{-1,1,-2,2,-1,1,-2,2}, {-2,-2,-1,-1,2,2,1,1}};
		Position p = new Position(getPosition());
		// 8 directions check
		for (int dir = 0; dir < 8; dir++) {
			p.setPosition(getPosition());
			p.incPosition(inc[0][dir], inc[1][dir]);
			if (getBoard().isValidBoardPosition(p)) {
				Piece piece = getBoard().getPieceAt(p);
				if (piece == null || !isSameColorOf(piece))
					moves.add(new Position(p));
			}
		}
		return moves;
	}

	@Override
	public List<Position> getPossibleCaptureMoves()
		{ return getPossibleMoves(); }

	@Override
	public String toString()
		{ return PieceType.KNIGHT.name(); }

	@Override
	public char let()
		{ return PieceType.KNIGHT.getLet(); }

}