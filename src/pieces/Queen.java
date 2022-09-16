package pieces;

import java.util.ArrayList;
import java.util.List;

import board.Board;
import enums.PieceColor;
import enums.PieceType;
import gameutil.Position;
import piece.Piece;

public class Queen extends Piece  {

	public Queen(Board board, Position position, PieceColor color)
		{ super(board, position, PieceType.QUEEN, color); }

	@Override
	public List<Position> getPossibleMoves() {
		List<Position> moves = new ArrayList<>();
		int[][] inc = {{-1,0,1,-1,1,-1,0,1}, {-1,-1,-1,0,0,1,1,1}};
		Position p = new Position(getPosition());
		// 8 directions check
		for (int dir = 0; dir < 8; dir++) {
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
		{ return PieceType.QUEEN.name(); }

	@Override
	public char let()
		{ return PieceType.QUEEN.getLet(); }

}