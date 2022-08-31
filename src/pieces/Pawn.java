package pieces;

import java.util.ArrayList;
import java.util.List;

import board.Board;
import enums.PieceColor;
import enums.PieceType;
import piece.Piece;
import piece.PiecePosition;

public class Pawn extends Piece {
	
	public Pawn(Board board, PiecePosition position, PieceColor color)
		{ super(board, position, PieceType.PAWN, color); }
	
	@Override
	public List<PiecePosition> getPossibleMoves() {
		List<PiecePosition> moves = new ArrayList<>();
		int inc = getColor() == PieceColor.BLACK ? 1 : -1;
		PiecePosition p = new PiecePosition(getPosition());
		PiecePosition p2 = new PiecePosition(p);
		// Front check (1 or 2 steps further (2 if this piece was never moved before))
		for (int row = 1; row <= (wasMoved() ? 1 : 2); row++) {
			p.incRow(inc);
			// Diagonal check for capture
			if (getBoard().isFreeSlot(p))
				moves.add(new PiecePosition(p));
			for (int i = -1; row == 1 && i <= 1; i += 2) {
				p2.setValues(p);
				p2.incColumn(i);
				if (getBoard().isValidBoardPosition(p2)) {
					Piece piece = getBoard().getPieceAt(p2);
					if (piece != null && !isSameColorOf(piece))
						moves.add(new PiecePosition(p2));
					if (piece == null && getBoard().pieceCanDoEnPassant(this) &&
							getBoard().getEnPassantCapturePosition().equals(p2))
								moves.add(new PiecePosition(p2)); //Tile for En Passant special move
				}
			}
			if (!getBoard().isFreeSlot(p))
				break;
		}
		return moves;
	}
	
	@Override
	public String toString()
		{ return PieceType.PAWN.name(); }

	public char let()
		{ return PieceType.PAWN.getLet(); }

}