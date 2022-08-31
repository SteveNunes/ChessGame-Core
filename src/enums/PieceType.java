package enums;

import java.util.ArrayList;
import java.util.List;

public enum PieceType {
	PAWN(1),
	BISHOP(2.1f),
	KNIGHT(2.2f),
	ROOK(3),
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

	public static List<PieceType> getListOfAllSortedByRank(Boolean reversed) {
		List<PieceType> list = getListOfAll();
		list.sort((t1, t2) -> t1.getValue() > t2.getValue() ? (!reversed ? -1 : 1) : (!reversed ? 1 : -1));
		return list;
	}
	
	public static List<PieceType> getListOfAllSortedByRank()
		{ return getListOfAllSortedByRank(false);	}
	
	public static char getLet(PieceType type, PieceColor color) {
		final Character let;
		if (type == QUEEN)
			let = 'Q';
		else if (type == KING)
			let = 'K';
		else if (type == BISHOP)
			let = 'B';
		else if (type == KNIGHT)
			let = 'N';
		else if (type == ROOK)
			let = 'R';
		else
			let = 'P';
		return color == PieceColor.BLACK ? Character.toLowerCase(let) : let;
	}
	
	public static char getLet(PieceType type)
		{ return getLet(type, PieceColor.WHITE); }
	
	public char getLet(PieceColor color)
		{ return getLet(this, color); }

	public char getLet()
		{ return getLet(this); }
}