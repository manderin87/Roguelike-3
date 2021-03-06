package Roguelike.Quests;

import Roguelike.DungeonGeneration.DungeonFileParser;
import Roguelike.Global;
import Roguelike.Quests.Input.AbstractQuestInput;
import Roguelike.Quests.Output.AbstractQuestOutputCondition;
import Roguelike.Quests.Output.QuestOutput;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.XmlReader;

import java.io.IOException;

/**
 * Created by Philip on 23-Jan-16.
 */
public class Quest
{
	public Global.Rarity rarity;
	public String path;
	public Array<AbstractQuestInput> inputs = new Array<AbstractQuestInput>(  );
	public Array<QuestOutput> outputs = new Array<QuestOutput>(  );
	public Array<DungeonFileParser.DFPRoom> rooms = new Array<DungeonFileParser.DFPRoom>(  );
	public ObjectSet<String> allowedLevels = new ObjectSet<String>(  );

	public boolean evaluateInputs()
	{
		for (AbstractQuestInput input : inputs)
		{
			if (!input.evaluate())
			{
				return false;
			}
		}

		return true;
	}

	public void evaluateOutputs()
	{
		for (QuestOutput output : outputs)
		{
			output.evaluate();
		}
	}

	public void parse( XmlReader.Element xml )
	{
		rarity = Global.Rarity.valueOf( xml.get("Rarity", "Common").toUpperCase() );

		XmlReader.Element inputsElement = xml.getChildByName( "Inputs" );
		if (inputsElement != null)
		{
			for (int i = 0; i < inputsElement.getChildCount(); i++)
			{
				XmlReader.Element inputElement = inputsElement.getChild( i );
				AbstractQuestInput input = AbstractQuestInput.load( inputElement );
				inputs.add( input );
			}
		}

		XmlReader.Element outputsElement = xml.getChildByName( "Outputs" );
		if (outputsElement != null)
		{
			for (int i = 0; i < outputsElement.getChildCount(); i++)
			{
				XmlReader.Element outputElement = outputsElement.getChild( i );
				QuestOutput output = new QuestOutput();
				output.parse( outputElement );

				outputs.add( output );
			}
		}

		XmlReader.Element roomsElement = xml.getChildByName( "Rooms" );
		for ( int i = 0; i < roomsElement.getChildCount(); i++ )
		{
			XmlReader.Element roomElement = roomsElement.getChild( i );
			DungeonFileParser.DFPRoom room = DungeonFileParser.DFPRoom.parse( roomElement );

			rooms.add( room );
		}

		allowedLevels.addAll(xml.get( "AllowedLevels", "any" ).split( "," ));
	}

	public static Quest load(String name)
	{
		Quest quest = new Quest();
		quest.path = name;

		XmlReader reader = new XmlReader();
		XmlReader.Element xml = null;

		try
		{
			xml = reader.parse( Gdx.files.internal( "Quests/" + name + ".xml" ) );
		}
		catch ( IOException e )
		{
			e.printStackTrace();
		}

		quest.parse( xml );

		return quest;
	}
}
