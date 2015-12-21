package Roguelike.GameEvent.OnDeath;

import java.util.HashMap;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import Roguelike.Global;
import Roguelike.Entity.Entity;
import Roguelike.Fields.Field;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader.Element;

import exp4j.Helpers.EquationHelper;

public final class FieldOnDeathEvent extends AbstractOnDeathEvent
{
	private String condition;
	private String[] reliesOn;
	private String fieldName;
	private String stacksEqn;

	@Override
	public boolean handle( Entity entity, Entity killer )
	{
		HashMap<String, Integer> variableMap = entity.getVariableMap();
		for ( String name : reliesOn )
		{
			if ( !variableMap.containsKey( name.toLowerCase() ) )
			{
				variableMap.put( name.toLowerCase(), 0 );
			}
		}

		if ( condition != null )
		{
			ExpressionBuilder expB = EquationHelper.createEquationBuilder( condition );
			EquationHelper.setVariableNames( expB, variableMap, "" );

			Expression exp = EquationHelper.tryBuild( expB );
			if ( exp == null ) { return false; }

			EquationHelper.setVariableValues( exp, variableMap, "" );

			double conditionVal = exp.evaluate();

			if ( conditionVal == 0 ) { return false; }
		}

		int stacks = 1;

		if ( stacksEqn != null )
		{
			if ( Global.isNumber( stacksEqn ) )
			{
				stacks = Integer.parseInt( stacksEqn );
			}
			else
			{
				ExpressionBuilder expB = EquationHelper.createEquationBuilder( stacksEqn );
				EquationHelper.setVariableNames( expB, variableMap, "" );

				Expression exp = EquationHelper.tryBuild( expB );
				if ( exp != null )
				{
					EquationHelper.setVariableValues( exp, variableMap, "" );

					stacks = (int) Math.ceil( exp.evaluate() );
				}
			}
		}

		Field field = Field.load( fieldName );
		field.trySpawnInTile( entity.tile[0][0], stacks );

		return true;
	}

	@Override
	public void parse( Element xml )
	{
		reliesOn = xml.getAttribute( "ReliesOn", "" ).split( "," );
		condition = xml.getAttribute( "Condition", null );
		if ( condition != null )
		{
			condition = condition.toLowerCase();
		}
		stacksEqn = xml.getAttribute( "Stacks", null );
		if ( stacksEqn != null )
		{
			stacksEqn = stacksEqn.toLowerCase();
		}

		fieldName = xml.getText();
	}

	@Override
	public Array<String> toString( HashMap<String, Integer> variableMap )
	{
		Array<String> lines = new Array<String>();

		lines.add( "Has a chance to create " + fieldName );

		return lines;
	}

}
