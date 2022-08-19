package pieces;

import java.util.ArrayList;
import java.util.List;

import board.Board;
import piece.*;

public class Rook extends Piece  {

	public Rook(Board board, Position position, PieceColor color)
		{ super(board, position, PieceType.ROOK, color); }

	@Override
	public List<Position> getPossibleMoves() {
		List<Position> moves = new ArrayList<>();
		int[][] inc = {
			{-1,0,1,0},
			{0,-1,0,1}
		};
		Position p = new Position(getPosition());
		// 4 lined directions check
		for (int dir = 0; dir < 4; dir++) {
			p.setValues(getPosition());
			while (getBoard().isValidBoardPosition(p)) {
				p.incValues(inc[0][dir], inc[1][dir]);
				if (!getBoard().isValidBoardPosition(p) ||
					(!getBoard().isFreeSlot(p) && !getBoard().isOpponentPiece(p, getColor())))
						break; 
				moves.add(new Position(p));
				if (getBoard().isOpponentPiece(p, getColor()))
					break; 
			}
		}
		return moves;
	}

	@Override
	public String toString()
		{ return "R"; }

}