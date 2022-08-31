package enums;

import java.util.ArrayList;
import java.util.List;

public enum PieceColor {
	BLACK(1),
	WHITE(2);
	
	private final int value;

	PieceColor(int value)
		{ this.value = value; }
	
	public static PieceColor getOppositeColor(PieceColor color)
		{ return color == BLACK ? WHITE : BLACK; }
	
	public PieceColor getOppositeColor()
		{ return getOppositeColor(this); }
	
	public int getValue()
		{ return value; }
	
	public static List<PieceColor> getListOfAll() {
		List<PieceColor> list = new ArrayList<>();
		list.add(BLACK);
		list.add(WHITE);
		return list;
	}
	
}
