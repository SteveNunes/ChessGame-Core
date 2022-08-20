package pieces;

import java.util.ArrayList;
import java.util.List;

import board.Board;
import enums.PieceColor;
import enums.PieceType;
import piece.Piece;
import piece.Position;

public class Pawn extends Piece {
	
	public Pawn(Board board, Position position, PieceColor color)
		{ super(board, position, PieceType.PAWN, color); }
	
	@Override
	public List<Position> getPossibleMoves() {
		List<Position> moves = new ArrayList<>();
		int inc = getColor() == PieceColor.WHITE ? 1 : -1;
		Position p = new Position(getPosition());
		Position p2 = new Position(p);
		// Front check (1 or 2 steps further (2 if this piece was never moved before))
		for (int row = 1; row <= (wasMoved() ? 1 : 2); row++) {
			p.incValues(inc, 0);
			// Diagonal check for capture
			for (int i = -1; row == 1 && i <= 1; i += 2) {
				p2.setValues(p);
				p2.incColumn(i);
				if (!getBoard().isFreeSlot(p2) && getBoard().isOpponentPiece(p2, getColor())) //Tile for En Passant special move
					moves.add(new Position(p2));
			}
			if (!getBoard().isFreeSlot(p))
				break;
			moves.add(new Position(p));
		}
		if (getBoard().getSelectedPiece() == this && getBoard().checkEnPassant())
			moves.add(new Position(getBoard().getEnPassantCapturePosition()));
		return moves;
	}

	@Override
	public String toString()
		{ return PieceType.PAWN.name(); }

	@Override
	public String let()
		{ return "P"; }

}