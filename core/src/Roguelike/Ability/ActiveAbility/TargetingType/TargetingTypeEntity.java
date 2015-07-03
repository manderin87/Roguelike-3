package Roguelike.Ability.ActiveAbility.TargetingType;

import com.badlogic.gdx.utils.XmlReader.Element;

public class TargetingTypeEntity extends AbstractTargetingType
{
	private boolean notSelf;
	
	@Override
	public void parse(Element xml)
	{
		notSelf = xml.getChildByName("NotSelf") != null;
	}

}
