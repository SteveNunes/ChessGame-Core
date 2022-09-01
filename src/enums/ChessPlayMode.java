package enums;

import java.util.ArrayList;
import java.util.List;

public enum ChessPlayMode {
	PLAYER_VS_PLAYER(1),
	PLAYER_VS_CPU(2),
	CPU_VS_CPU(3);
	
	private final int value;

	ChessPlayMode(int value)
		{ this.value = value; }
	
	public int getValue()
		{ return value; }
	
	public static List<ChessPlayMode> getListOfAll() {
		List<ChessPlayMode> list = new ArrayList<>();
		list.add(PLAYER_VS_PLAYER);
		list.add(PLAYER_VS_CPU);
		list.add(CPU_VS_CPU);
		return list;
	}

}
