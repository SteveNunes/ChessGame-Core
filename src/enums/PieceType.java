package enums;

import java.util.ArrayList;
import java.util.List;

public enum PieceType {
	PAWN(1),
	KNIGHT(2),
	ROOK(3),
	BISHOP(4),
	QUEEN(5),
	KING(6);
	
	private final int value;

	PieceType(int value)
		{ this.value = value; }
	
	public int getValue()
		{ return value; }
	
	public static List<PieceType> getListOfAll() {
		List<PieceType> list = new ArrayList<>();
		list.add(PAWN);
		list.add(KNIGHT);
		list.add(ROOK);
		list.add(BISHOP);
		list.add(QUEEN);
		list.add(KING);
		return list;
	}
	
	public static char getLet(PieceType type) {
		if (type == QUEEN)
			return 'Q';
		if (type == KING)
			return 'K';
		if (type == BISHOP)
			return 'B';
		if (type == KNIGHT)
			return 'N';
		if (type == ROOK)
			return 'R';
		return 'P';
	}
	
	public char getLet()
		{ return getLet(this); }

}