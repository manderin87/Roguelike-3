package Roguelike.Pathfinding;

import Roguelike.Global.Passability;
import Roguelike.Util.EnumBitflag;

public interface PathfindingTile
{
	public boolean getPassable( EnumBitflag<Passability> travelType );

	public int getInfluence();
}
