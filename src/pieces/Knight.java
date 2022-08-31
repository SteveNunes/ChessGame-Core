package pieces;

import java.util.ArrayList;
import java.util.List;

import board.Board;
import enums.*;
import piece.Piece;
import piece.PiecePosition;

public class Knight extends Piece  {

	public Knight(Board board, PiecePosition position, PieceColor color) 
		{ super(board, position, PieceType.KNIGHT, color); }

	@Override
	public List<PiecePosition> getPossibleMoves() {
		List<PiecePosition> moves = new ArrayList<>();
		int[][] inc = {{-2,-2,-1,-1,2,2,1,1},{-1,1,-2,2,-1,1,-2,2}};
		PiecePosition p = new PiecePosition(getPosition());
		// 8 directions check
		for (int dir = 0; dir < 8; dir++) {
			p.setValues(getPosition());
			p.incValues(inc[0][dir], inc[1][dir]);
			if (getBoard().isValidBoardPosition(p)) {
				Piece piece = getBoard().getPieceAt(p);
				if (piece == null || !isSameColorOf(piece))
					moves.add(new PiecePosition(p));
			}
		}
		return moves;
	}

	@Override
	public String toString()
		{ return PieceType.KNIGHT.name(); }

	public char let()
		{ return PieceType.KNIGHT.getLet(); }

}