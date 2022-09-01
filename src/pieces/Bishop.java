package pieces;

import java.util.ArrayList;
import java.util.List;

import board.Board;
import enums.*;
import piece.Piece;
import piece.PiecePosition;

public class Bishop extends Piece  {
	
	public Bishop(Board board, PiecePosition position, PieceColor color)
		{ super(board, position, PieceType.BISHOP, color); }
	
	@Override
	public List<PiecePosition> getPossibleMoves() {
		List<PiecePosition> moves = new ArrayList<>();
		int[][] inc = {{-1,-1,1,1},{-1,1,-1,1}};
		PiecePosition p = new PiecePosition(getPosition());
		// 4 diagonal directions check
		for (int dir = 0; dir < 4; dir++) {
			p.setValues(getPosition());
			while (getBoard().isValidBoardPosition(p)) {
				p.incValues(inc[0][dir], inc[1][dir]);
				if (getBoard().isValidBoardPosition(p)) {
					Piece piece = getBoard().getPieceAt(p);
					if (piece == null || !isSameColorOf(piece)) 
						moves.add(new PiecePosition(p));
					if (piece != null)
						break;
				}
			}
		}
		return moves;
	}

	@Override
	public String toString()
		{ return PieceType.BISHOP.name(); }
	
	public char let()
		{ return PieceType.BISHOP.getLet(); }

}