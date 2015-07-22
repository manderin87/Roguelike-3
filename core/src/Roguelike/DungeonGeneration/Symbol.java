package Roguelike.DungeonGeneration;

import java.util.HashMap;
import java.util.HashSet;

import Roguelike.Entity.EnvironmentEntity;
import Roguelike.Pathfinding.PathfindingTile;
import Roguelike.Tiles.TileData;

import com.badlogic.gdx.utils.XmlReader.Element;

//----------------------------------------------------------------------
public class Symbol implements PathfindingTile
{		
	public char character;

	public Element tileData;

	public Element environmentData;
	public EnvironmentEntity environmentEntityObject;

	public Element entityData;

	public String metaValue;
	
//	public Symbol copy()
//	{
//		Symbol s = new Symbol();
//		s.character = character;
//		s.tileData = tileData;
//		s.environmentData = environmentData;
//		s.entityData = entityData;
//		s.metaValue = metaValue;
//		
//		return s;
//	}
	
	public boolean hasEnvironmentEntity()
	{
		return environmentEntityObject != null || environmentData != null;
	}
	
	public EnvironmentEntity getEnvironmentEntity()
	{
		if (environmentEntityObject != null)
		{
			return environmentEntityObject;
		}
		
		if (environmentData != null)
		{
			return EnvironmentEntity.load(environmentData);
		}
		
		return null;
	}

	public TileData getAsTileData()
	{
		return TileData.parse(tileData);
	}

	public static Symbol parse(Element xml, HashMap<Character, Symbol> sharedSymbolMap, HashMap<Character, Symbol> localSymbolMap)
	{
		Symbol symbol = new Symbol();

		// load the base symbol
		if (xml.getAttribute("Extends", null) != null)
		{
			char extendsSymbol = xml.getAttribute("Extends").charAt(0);

			Symbol rs = localSymbolMap != null ? localSymbolMap.get(extendsSymbol) : null;
			if (rs == null)
			{
				rs = sharedSymbolMap.get(extendsSymbol);
			}

			symbol.character = rs.character;
			symbol.tileData = rs.tileData;
			symbol.environmentData = rs.environmentData;
			symbol.entityData = rs.entityData;
			symbol.metaValue = rs.metaValue;
		}

		// fill in the new values
		symbol.character = xml.get("Char", ""+symbol.character).charAt(0);

		if (xml.getChildByName("TileData") != null)
		{
			symbol.tileData = xml.getChildByName("TileData");
		}

		if (xml.getChildByName("EnvironmentData") != null)
		{
			symbol.environmentData = xml.getChildByName("EnvironmentData");
		}

		if (xml.getChildByName("EntityData") != null)
		{
			symbol.entityData = xml.getChildByName("EntityData");
		}

		symbol.metaValue = xml.get("MetaValue", symbol.metaValue);

		return symbol;
	}

	public boolean isPassable()
	{
		return getAsTileData().Passable;
	}

	@Override
	public String toString()
	{
		return ""+character;
	}

	@Override
	public boolean getPassable(HashSet<String> factions)
	{
		return isPassable();
	}

	@Override
	public int getInfluence()
	{
		if (character == 'F')
		{
			if (hasEnvironmentEntity())
			{
				if (!getEnvironmentEntity().passable)
				{
					return 100;
				}
			}
		}
		
		return 0;
	}
}