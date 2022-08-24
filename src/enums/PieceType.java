package enums;

import java.util.ArrayList;
import java.util.List;

public enum PieceType {
	PAWN(1),
	KNIGHT(2),
	ROOK(3.1f),
	BISHOP(3.2f),
	QUEEN(4),
	KING(5);
	
	private final float value;

	PieceType(float value)
		{ this.value = value; }
	
	public float getValue()
		{ return value; }
	
	public int getIntValue()
		{ return (int)value; }

	public static List<PieceType> getListOfAll() {
		List<PieceType> list = new ArrayList<>();
		list.add(PAWN);
		list.add(KNIGHT);
		list.add(BISHOP);
		list.add(ROOK);
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