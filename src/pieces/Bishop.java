package pieces;

import java.util.ArrayList;
import java.util.List;

import board.Board;
import enums.PieceColor;
import enums.PieceType;
import gameutil.Position;
import piece.Piece;

public class Bishop extends Piece  {
	
	public Bishop(Board board, Position position, PieceColor color)
		{ super(board, position, PieceType.BISHOP, color); }
	
	@Override
	public List<Position> getPossibleMoves() {
		List<Position> moves = new ArrayList<>();
		int[][] inc = {{-1,1,-1,1}, {-1,-1,1,1}};
		Position p = new Position(getPosition());
		// 4 diagonal directions check
		for (int dir = 0; dir < 4; dir++) {
			p.setPosition(getPosition());
			while (getBoard().isValidBoardPosition(p)) {
				p.incPosition(inc[0][dir], inc[1][dir]);
				if (getBoard().isValidBoardPosition(p)) {
					Piece piece = getBoard().getPieceAt(p);
					if (piece == null || !isSameColorOf(piece)) 
						moves.add(new Position(p));
					if (piece != null)
						break;
				}
			}
		}
		return moves;
	}

	@Override
	public List<Position> getPossibleCaptureMoves()
		{ return getPossibleMoves(); }

	@Override
	public String toString()
		{ return PieceType.BISHOP.name(); }

	@Override
	public char let()
		{ return PieceType.BISHOP.getLet(); }

}