package pieces;

import java.util.ArrayList;
import java.util.List;

import board.Board;
import enums.PieceColor;
import enums.PieceType;
import gameutil.Position;
import piece.Piece;

public class Pawn extends Piece {
	
	public Pawn(Board board, Position position, PieceColor color)
		{ super(board, position, PieceType.PAWN, color); }
	
	@Override
	public List<Position> getPossibleMoves()
		{ return getPossibleMoves(false);	 }
	
	@Override
	public List<Position> getPossibleCaptureMoves()
		{ return getPossibleMoves(true);	 }
	
	public List<Position> getPossibleMoves(Boolean captureMoves) {
		List<Position> moves = new ArrayList<>();
		Boolean swaped = getBoard().isSwappedSides();
		int inc = (isBlack() && !swaped) || (isWhite() && swaped) ? 1 : -1;
		Position p = new Position(getPosition());
		Position p2 = new Position(p);
		// Front check (1 or 2 steps further (2 if this piece was never moved before))
		for (int row = 1; row <= (captureMoves || wasMoved() ? 1 : 2); row++) {
			p.incY(inc);
			if (!captureMoves && getBoard().isFreeSlot(p))
				moves.add(new Position(p));
			for (int i = -1; row == 1 && i <= 1; i += 2) {
				p2.setPosition(p.getX() + i, p.getY());
				if (getBoard().isValidBoardPosition(p2)) {
					Piece piece = getBoard().getPieceAt(p2);
					// Diagonal check for capture
					if (piece != null && !isSameColorOf(piece))
						moves.add(new Position(p2));
					if (piece == null && getBoard().pieceCanDoEnPassant(this) &&
							getBoard().getEnPassantCapturePosition().equals(p2))
								moves.add(new Position(p2)); //Tile for En Passant special move
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

	@Override
	public char let()
		{ return PieceType.PAWN.getLet(); }

}