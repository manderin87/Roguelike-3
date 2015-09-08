package Roguelike.GameEvent.OnDeath;

import java.util.HashMap;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import Roguelike.Global;
import Roguelike.Entity.Entity;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader.Element;

import exp4j.Helpers.EquationHelper;

public final class HealOnDeathEvent extends AbstractOnDeathEvent
{
	private String condition;
	private String[] reliesOn;
	private String amountEqn;

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

		int healVal = 0;
		if ( Global.isNumber( amountEqn ) )
		{
			healVal = Integer.parseInt( amountEqn );
		}
		else
		{
			ExpressionBuilder expB = EquationHelper.createEquationBuilder( amountEqn );
			EquationHelper.setVariableNames( expB, variableMap, "" );

			Expression exp = EquationHelper.tryBuild( expB );
			if ( exp == null ) { return false; }

			EquationHelper.setVariableValues( exp, variableMap, "" );

			healVal = (int) exp.evaluate();
		}

		entity.applyHealing( healVal );

		return true;
	}

	@Override
	public void parse( Element xml )
	{
		condition = xml.getAttribute( "Condition", null );
		if ( condition != null )
		{
			condition = condition.toLowerCase();
		}
		reliesOn = xml.getAttribute( "ReliesOn", "" ).split( "," );
		amountEqn = xml.get( "Amount" );
	}

	@Override
	public Array<String> toString( HashMap<String, Integer> variableMap )
	{
		Array<String> lines = new Array<String>();

		ExpressionBuilder expB = EquationHelper.createEquationBuilder( amountEqn );
		EquationHelper.setVariableNames( expB, variableMap, "" );

		Expression exp = EquationHelper.tryBuild( expB );
		if ( exp == null ) { return lines; }

		EquationHelper.setVariableValues( exp, variableMap, "" );

		int healVal = (int) exp.evaluate();

		lines.add( "Heal for " + healVal );

		return lines;
	}

}