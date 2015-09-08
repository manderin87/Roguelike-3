package Roguelike.Save;

import java.util.HashMap;

import Roguelike.Entity.EnvironmentEntity;
import Roguelike.Items.Inventory;
import Roguelike.StatusEffect.StatusEffect;
import Roguelike.Tiles.Point;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader.Element;

public final class SaveEnvironmentEntity extends SaveableObject<EnvironmentEntity>
{
	public int hp;
	public Point pos = new Point();
	public Array<StatusEffect> statuses = new Array<StatusEffect>();
	public Inventory inventory;
	public String UID;
	public Element creationData;
	public HashMap<String, Object> data;
	public String levelUID;

	@Override
	public void store( EnvironmentEntity obj )
	{
		hp = obj.HP;
		pos.set( obj.tile.x, obj.tile.y );
		for ( StatusEffect status : obj.statusEffects )
		{
			statuses.add( status );
		}
		inventory = obj.inventory;

		UID = obj.UID;

		creationData = obj.creationData;
		data = (HashMap<String, Object>) obj.data.clone();

		levelUID = obj.tile.level.UID;
	}

	@Override
	public EnvironmentEntity create()
	{
		EnvironmentEntity entity = EnvironmentEntity.load( creationData, levelUID );
		entity.data = (HashMap<String, Object>) data.clone();

		entity.HP = hp;
		for ( StatusEffect saveStatus : statuses )
		{
			entity.addStatusEffect( saveStatus );
		}
		entity.inventory = inventory;

		entity.UID = UID;

		return entity;
	}

}
