package Roguelike.Ability.ActiveAbility.EffectType;

import java.util.HashMap;

import Roguelike.Ability.ActiveAbility.ActiveAbility;
import Roguelike.Entity.EnvironmentEntity;
import Roguelike.Entity.GameEntity;
import Roguelike.Tiles.GameTile;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader.Element;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;

public abstract class AbstractEffectType
{
	public abstract void update( ActiveAbility aa, float time, GameTile tile, GameEntity entity, EnvironmentEntity envEntity );

	public abstract void parse( Element xml );

	public abstract AbstractEffectType copy();

	public abstract Array<String> toString( ActiveAbility aa );

	public static AbstractEffectType load( Element xml )
	{
		Class<AbstractEffectType> c = ClassMap.get( xml.getName().toUpperCase() );
		AbstractEffectType type = null;

		try
		{
			type = ClassReflection.newInstance( c );
		}
		catch ( Exception e )
		{
			System.err.println(xml.getName());
			e.printStackTrace();
		}

		type.parse( xml );

		return type;
	}

	public static final HashMap<String, Class> ClassMap = new HashMap<String, Class>();
	static
	{
		ClassMap.put( "DAMAGE", EffectTypeDamage.class );
		ClassMap.put( "FIELD", EffectTypeField.class );
		ClassMap.put( "FIELDINTERACTION", EffectTypeFieldInteraction.class );
		ClassMap.put( "HEAL", EffectTypeHeal.class );
		ClassMap.put( "STATUS", EffectTypeStatus.class );
		ClassMap.put( "SUMMON", EffectTypeSummon.class );
		ClassMap.put( "TELEPORT", EffectTypeTeleport.class );
	}
}
