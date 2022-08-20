package pieces;

import java.util.ArrayList;
import java.util.List;

import board.Board;
import enums.*;
import piece.Piece;
import piece.PiecePosition;

public class Rook extends Piece  {

	public Rook(Board board, PiecePosition position, PieceColor color)
		{ super(board, position, PieceType.ROOK, color); }

	@Override
	public List<PiecePosition> getPossibleMoves() {
		List<PiecePosition> moves = new ArrayList<>();
		int[][] inc = {
			{-1,0,1,0},
			{0,-1,0,1}
		};
		PiecePosition p = new PiecePosition(getPosition());
		// 4 lined directions check
		for (int dir = 0; dir < 4; dir++) {
			p.setValues(getPosition());
			while (getBoard().isValidBoardPosition(p)) {
				p.incValues(inc[0][dir], inc[1][dir]);
				if (!getBoard().isValidBoardPosition(p) ||
					(!getBoard().isFreeSlot(p) && !getBoard().isOpponentPiece(p, getColor())))
						break; 
				moves.add(new PiecePosition(p));
				if (!getBoard().isFreeSlot(p) && getBoard().isOpponentPiece(p, getColor()))
					break; 
			}
		}
		return moves;
	}

	@Override
	public String toString()
		{ return PieceType.ROOK.name(); }

	@Override
	public String let()
		{ return "R"; }
	
}