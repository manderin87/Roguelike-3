package Roguelike.Levels;

import java.util.HashSet;
import java.util.Iterator;

import Roguelike.GameEvent.GameEventHandler;
import Roguelike.Global.Passability;
import Roguelike.Global.Statistic;
import Roguelike.Global;
import Roguelike.RoguelikeGame;
import Roguelike.Pathfinding.ShadowCaster;
import Roguelike.RoguelikeGame.ScreenEnum;
import Roguelike.Ability.ActiveAbility.ActiveAbility;
import Roguelike.DungeonGeneration.DungeonFileParser.DFPRoom;
import Roguelike.Entity.Entity;
import Roguelike.Entity.EnvironmentEntity;
import Roguelike.Entity.GameEntity;
import Roguelike.Entity.Tasks.AbstractTask;
import Roguelike.Entity.Tasks.TaskAttack;
import Roguelike.Entity.Tasks.TaskMove;
import Roguelike.Entity.Tasks.TaskWait;
import Roguelike.Items.Inventory;
import Roguelike.Items.Item;
import Roguelike.Items.Item.ItemType;
import Roguelike.Items.Recipe;
import Roguelike.Lights.Light;
import Roguelike.Screens.GameScreen;
import Roguelike.Sound.RepeatingSoundEffect;
import Roguelike.Sprite.BumpAnimation;
import Roguelike.Sprite.MoveAnimation;
import Roguelike.Sprite.Sprite;
import Roguelike.Sprite.SpriteEffect;
import Roguelike.Sprite.MoveAnimation.MoveEquation;
import Roguelike.StatusEffect.StatusEffect;
import Roguelike.Tiles.GameTile;
import Roguelike.Tiles.SeenTile;
import Roguelike.Tiles.SeenTile.SeenHistoryItem;
import Roguelike.UI.MessageStack.Line;
import Roguelike.UI.MessageStack.Message;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Level
{
	private static final Array<Passability> ItemDropPassability = new Array<Passability>(new Passability[]{Passability.WALK, Passability.ENTITY});
	
	public Array<RepeatingSoundEffect> ambientSounds = new Array<RepeatingSoundEffect>();
	public String bgmName;
	
	public String fileName;
	public int depth;
	public long seed;
	public Array<DFPRoom> requiredRooms;
	public String UID;
	
	public GameEntity player;
	
	public Color Ambient = new Color(0.1f, 0.1f, 0.3f, 1.0f);
	
	public SeenTile[][] SeenGrid;
	private GameTile[][] Grid;
	public int width;
	public int height;
			
	public Level(GameTile[][] grid)
	{
		this.Grid = grid;
		this.width = grid.length;
		this.height = grid[0].length;
		
		this.SeenGrid = new SeenTile[width][height];
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				SeenGrid[x][y] = new SeenTile();
				SeenGrid[x][y].GameTile = Grid[x][y];
			}
		}
	}
	
	public GameTile[][] getGrid()
	{
		return Grid;
	}
	
	public GameTile getGameTile(int[] pos)
	{
		return getGameTile(pos[0], pos[1]);
	}
	
	public GameTile getGameTile(int x, int y)
	{
		if (x < 0 || x >= width || y < 0 || y >= height) { return null; }
		
		return Grid[x][y];
	}
	
	public SeenTile getSeenTile(int[] pos)
	{
		return getSeenTile(pos[0], pos[1]);
	}
	
	public SeenTile getSeenTile(int x, int y)
	{
		if (x < 0 || x >= width || y < 0 || y >= height) { return null; }
		
		return SeenGrid[x][y];
	}
	
	public void revealWholeLevel()
	{
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				if (!SeenGrid[x][y].seen)
				{
					SeenTile s = SeenGrid[x][y];
					s.GameTile = Grid[x][y];
					s.seen = true;
					
					s.History.clear();
					
					for (Sprite sprite : Grid[x][y].tileData.sprites)
					{
						s.History.add(new SeenHistoryItem(sprite, Grid[x][y].tileData.description));
					}
				}
			}
		}
	}
	
	public void UpdateSeenGrid()
	{
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				GameTile tile = Grid[x][y];
				if (tile.GetVisible())
				{
					SeenTile s = SeenGrid[x][y];
					s.GameTile = tile;
					s.seen = true;
					
					s.History.clear();
					
					for (Sprite sprite : tile.tileData.sprites)
					{
						s.History.add(new SeenHistoryItem(sprite, tile.tileData.description));
					}
					
					if (tile.environmentEntity != null)
					{
						SeenHistoryItem shi = new SeenHistoryItem(tile.environmentEntity.sprite, "");
						shi.location = tile.environmentEntity.location;
						s.History.add(shi);
					}
										
					if (tile.entity != null)
					{						
						s.History.add(new SeenHistoryItem(tile.entity.sprite, "A " + tile.entity.name));
					}
					
					for (Item i : tile.items)
					{
						s.History.add(new SeenHistoryItem(i.getIcon(), "A " + i.name));
					}
				}
			}
		}
	}
	
	public void calculateLight(float delta, Array<Light> lights)
	{
		Color acol = new Color(Ambient);
		acol.mul(acol.a);
		acol.a = 1;
		
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{	
				Grid[x][y].light = new Color(acol);
			}
		}
		
		for (Light l : lights)
		{
			l.update(delta);
			calculateSingleLight(l);
		}
	}
	
	private void calculateSingleLight(Light l)
	{
		if (Math.max(Math.abs(player.tile.x - l.lx), Math.abs(player.tile.y - l.ly)) > player.getStatistic(Statistic.RANGE)+(int)Math.ceil(l.actualIntensity))
		{
			return; // too far away
		}
		
		Array<int[]> output = new Array<int[]>();
		ShadowCaster shadow = new ShadowCaster(Grid, (int)Math.ceil(l.actualIntensity));
		shadow.ComputeFOV(l.lx, l.ly, output);
				
		for (int[] tilePos : output)
		{
			GameTile tile = getGameTile(tilePos);
			
			float dst = 1 - Vector2.dst(l.lx, l.ly, tile.x, tile.y) / l.actualIntensity;
			if (dst < 0) {dst = 0;}
			
			Color lcol = new Color(l.colour).mul(dst);
			lcol.mul(lcol.a);
			lcol.a = 1;
			
			tile.light.add(lcol);
		}
	}
	
	public Entity getEntityWithUID(String UID)
	{
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				GameTile tile = getGameTile(x, y);
				
				if (tile.environmentEntity != null)
				{
					if (tile.environmentEntity.UID.equals(UID)) { return tile.environmentEntity; }
				}
				
				if (tile.entity != null)
				{
					if (tile.entity.UID.equals(UID)) { return tile.entity; }
				}
			}
		}
		
		return null;
	}
	
	public Array<ActiveAbility> ActiveAbilities = new Array<ActiveAbility>(false, 16);
	private Array<ActiveAbility> NewActiveAbilities = new Array<ActiveAbility>(false, 16);
	public void addActiveAbility(ActiveAbility aa)
	{
		NewActiveAbilities.add(aa);
	}
	
	private Array<GameEntity> visibleList = new Array<GameEntity>(false, 16);
	private Array<GameEntity> invisibleList = new Array<GameEntity>(false, 16);
	
	private Array<Light> lightList = new Array<Light>(false, 16);
	
	private float updateDeltaStep = 0.05f;
	private float updateAccumulator;
	public void update(float delta)
	{
		updateAccumulator += delta;
		
		if (!hasActiveEffects())
		{	
			// Do player move and fill lists
			if (visibleList.size == 0 && invisibleList.size == 0 && updateAccumulator >= updateDeltaStep && !hasAbilitiesToUpdate())
			{
				processPlayer();
								
				updateAccumulator = 0;
			}			
			
			if (visibleList.size > 0 || invisibleList.size > 0)
			{
				// process invisible until empty
				processInvisibleList();
				
				// process visible
				processVisibleList();
			}
			
			if (ActiveAbilities.size > 0)
			{
				Iterator<ActiveAbility> itr = ActiveAbilities.iterator();
				while (itr.hasNext())
				{
					ActiveAbility aa = itr.next();
					boolean finished = aa.update();
					
					if (finished)
					{
						itr.remove();
					}
				}
			}
			
			if (NewActiveAbilities.size > 0)
			{
				ActiveAbilities.addAll(NewActiveAbilities, 0, NewActiveAbilities.size);
				NewActiveAbilities.clear();
			}
		}
		
		updateVisibleTiles();
		lightList.clear();
		
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				GameTile tile = Grid[x][y];
				
				updateSpritesForTile(tile, delta);
				updateSpriteEffectsForTile(tile, delta);
				getLightsForTile(tile, lightList);
				
				cleanUpDeadForTile(tile);
			}
		}
		
		for (ActiveAbility aa : ActiveAbilities)
		{
			aa.getSprite().update(delta);
		}
		
		calculateLight(delta, lightList);
		
		for (RepeatingSoundEffect sound : ambientSounds)
		{
			sound.update(delta);
		}
	}
	
	private void cleanUpDeadForTile(GameTile tile)
	{
		{
			GameEntity e = tile.entity;
			if (e != null)
			{
				if (tile.GetVisible())
				{
					if (e.hasDamage)
					{
						GameScreen.Instance.addActorDamageAction(e);
						e.hasDamage = false;
					}
					
					if (e.healingAccumulator > 0)
					{
						GameScreen.Instance.addActorHealingAction(e);
					}
					
					if (e.popup != null)
					{
						GameScreen.Instance.addPopupBubble(e);
					}
				}
				else
				{
					e.damageAccumulator = 0;
					e.healingAccumulator = 0;
				}

				if (e != player && e.HP <= 0 && !hasActiveEffects(e))
				{
					e.tile.entity = null;

					dropItems(e.getInventory(), e.tile);						
				}
				else if (e == player && e.HP <= 0 && !hasActiveEffects(e))
				{
					RoguelikeGame.Instance.switchScreen(ScreenEnum.GAMEOVER);
				}
			}
		}

		{
			Entity e = tile.environmentEntity;
			if (e != null)
			{
				if (e.damageAccumulator > 0 && tile.GetVisible())
				{
					GameScreen.Instance.addActorDamageAction(e);
				}

				if (e != player && e.HP <= 0 && !hasActiveEffects(e))
				{
					e.tile.environmentEntity = null;
					
					dropItems(e.getInventory(), e.tile);
				}
			}
		}
	}
	
	private void dropItems(Inventory inventory, GameTile source)
	{
		Array<int[]> possibleTiles = new Array<int[]>();
		ShadowCaster sc = new ShadowCaster(Grid, 5, ItemDropPassability);
		sc.ComputeFOV(source.x, source.y, possibleTiles);
		
		// remove nonpassable tiles
		Iterator<int[]> itr = possibleTiles.iterator();
		while (itr.hasNext())
		{
			int[] pos = itr.next();
			
			GameTile tile = getGameTile(pos);
			
			if (!tile.getPassable(ItemDropPassability))
			{
				itr.remove();
			}
		}
		
		float delay = 0;
		for (Item i : inventory.m_items)
		{
			if (i.canDrop && i.shouldDrop())
			{
				if (i.itemType == ItemType.MATERIAL)
				{
					i = Recipe.generateItemForMaterial(i);
				}

				int[] target = possibleTiles.get(MathUtils.random(possibleTiles.size-1));
				GameTile tile = getGameTile(target);
				
				tile.items.add(i);
				
				int[] diff = tile.getPosDiff(source);
				
				MoveAnimation anim = new MoveAnimation(0.4f, diff, MoveEquation.LEAP);
				anim.leapHeight = 6;
				i.icon.spriteAnimation = anim;				
				i.icon.renderDelay = delay;
				delay += 0.02f;
			}
		}
	}
	
	private void updateSpriteEffectsForTile(GameTile tile, float delta)
	{
		Iterator<SpriteEffect> itr = tile.spriteEffects.iterator();
		while (itr.hasNext())
		{
			SpriteEffect e = itr.next();
			boolean finished = e.Sprite.update(delta);
			
			if (finished) { itr.remove(); }
		}
		
		if (tile.entity != null)
		{
			itr = tile.entity.spriteEffects.iterator();
			while (itr.hasNext())
			{
				SpriteEffect e = itr.next();
				boolean finished = e.Sprite.update(delta);
				
				if ( finished) { itr.remove(); }
			}
		}
		
		if (tile.environmentEntity != null)
		{
			itr = tile.environmentEntity.spriteEffects.iterator();
			while (itr.hasNext())
			{
				SpriteEffect e = itr.next();
				boolean finished = e.Sprite.update(delta);
				
				if ( finished) { itr.remove(); }
			}
		}
	}
	
	private void updateSpritesForTile(GameTile tile, float delta)
	{
		for (Sprite sprite : tile.tileData.sprites)
		{
			sprite.update(delta);
		}
		
		if (tile.environmentEntity != null)
		{
			tile.environmentEntity.sprite.update(delta);
		}
		
		if (tile.entity != null)
		{
			tile.entity.sprite.update(delta);
		}
		
		for (Item i : tile.items)
		{
			i.getIcon().update(delta);
		}
	}
	
	private void getLightsForTile(GameTile tile, Array<Light> output)
	{
		int x = tile.x;
		int y = tile.y;
		
		if (tile.tileData.light != null)
		{
			Light l = tile.tileData.light.copy();
			l.lx = x;
			l.ly = y;
			output.add(l);
		}
		
		if (tile.environmentEntity != null && tile.environmentEntity.light != null)
		{
			tile.environmentEntity.light.lx = x;
			tile.environmentEntity.light.ly = y;
			output.add(tile.environmentEntity.light);
		}
		
		for (SpriteEffect se : tile.spriteEffects)
		{
			if (se.light != null)
			{
				se.light.lx = x;
				se.light.ly = y;
				output.add(se.light);
			}
		}
		
		if (tile.entity != null)
		{
			Array<Light> lights = tile.entity.getLight();
			for (Light l : lights)
			{
				l.lx = x;
				l.ly = y;
				output.add(l);
			}
			
			for (SpriteEffect se : tile.entity.spriteEffects)
			{
				if (se.light != null)
				{
					se.light.lx = x;
					se.light.ly = y;
					output.add(se.light);
				}
			}
		}
	}
	
	private boolean hasAbilitiesToUpdate()
	{
		for (ActiveAbility aa : ActiveAbilities)
		{
			if (aa.needsUpdate())
			{
				return true;
			}
		}
		
		return false;
	}
		
	public void updateVisibleTiles()
	{
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{	
				Grid[x][y].SetVisible(false);
			}
		}
		
		Array<int[]> output = new Array<int[]>();
		ShadowCaster shadow = new ShadowCaster(Grid, player.getStatistic(Statistic.RANGE));
		shadow.ComputeFOV(player.tile.x, player.tile.y, output);
		
		for (int[] tilePos : output)
		{
			getGameTile(tilePos).SetVisible(true);
		}
		
		UpdateSeenGrid();
	}
	
	private void processPlayer()
	{
		if (player.tasks.size == 0)
		{
			player.AI.update(player);
		}
		
		if (player.tasks.size > 0)
		{
			AbstractTask task = player.tasks.removeIndex(0);
			for (GameEventHandler handler : player.getAllHandlers())
			{
				if (task instanceof TaskMove)
				{
					handler.onMove(player, (TaskMove)task);
				}
				else if (task instanceof TaskAttack)
				{
					handler.onAttack(player, (TaskAttack)task);
				}
				else if (task instanceof TaskWait)
				{
					handler.onWait(player, (TaskWait)task);
				}
			}
			
			if (!task.cancel) { task.processTask(player); }
			
			float actionCost = task.cost * player.getActionDelay();
			
			Global.AUT += actionCost;
			
			Global.abilityPool.update(actionCost);
			
			player.update(actionCost);
			
			getAllEntitiesToBeProcessed(actionCost);
			
			for (EnvironmentEntity ee : getAllEnvironmentEntities())
			{
				ee.update(actionCost);
			}
			
			// check if enemy visible			
			if (enemyVisible())
			{
				// Clear pending moves
				player.AI.setData("Pos", null);
				player.AI.setData("Rest", null);
			}
			
			if (player.sprite.spriteAnimation instanceof BumpAnimation)
			{
				player.AI.setData("Pos", null);
				player.AI.setData("Rest", null);
			}
		}
	}
	
	private void processVisibleList()
	{
		Iterator<GameEntity> itr = visibleList.iterator();
		while (itr.hasNext())
		{
			GameEntity e = itr.next();
			
			if (e.HP <= 0)
			{
				itr.remove();
				continue;
			}
			
			// If entity can take action
			if (e.actionDelayAccumulator > 0)
			{
				// If no tasks queued, process the ai
				if (e.tasks.size == 0)
				{
					e.AI.update(e);
				}
				
				// If a task is queued, process it
				if (e.tasks.size > 0)
				{
					AbstractTask task = e.tasks.removeIndex(0);
					for (GameEventHandler handler : e.getAllHandlers())
					{
						if (task instanceof TaskMove)
						{
							handler.onMove(e, (TaskMove)task);
						}
					}
					
					if (!task.cancel) { task.processTask(e); }
					
					float actionCost = task.cost * e.getActionDelay();					
					e.actionDelayAccumulator -= actionCost * e.getActionDelay();
				}
				else
				{
					e.actionDelayAccumulator -= e.getActionDelay();
				}
			}
			
			if (e.actionDelayAccumulator <= 0)
			{
				itr.remove();
			}
			else if (!e.tile.GetVisible())
			{
				// If entity is now invisible, submit to the invisible list to be processed
				itr.remove();
				invisibleList.add(e);
			}
		}
	}
	
	private void processInvisibleList()
	{
		while(invisibleList.size > 0)
		{
			Iterator<GameEntity> itr = invisibleList.iterator();
			
			// Repeat full pass through list
			while (itr.hasNext())
			{
				GameEntity e = itr.next();
				
				if (e.HP <= 0)
				{
					itr.remove();
					continue;
				}
				
				// If entity can take action
				if (e.actionDelayAccumulator > 0)
				{
					// If no tasks queued, process the ai
					if (e.tasks.size == 0)
					{
						e.AI.update(e);
					}
					
					// If a task is queued, process it
					if (e.tasks.size > 0)
					{
						AbstractTask task = e.tasks.removeIndex(0);
						for (GameEventHandler handler : e.getAllHandlers())
						{
							if (task instanceof TaskMove)
							{
								handler.onMove(e, (TaskMove)task);
							}
						}
						
						if (!task.cancel) { task.processTask(e); }
						
						float actionCost = task.cost * e.getActionDelay();					
						e.actionDelayAccumulator -= actionCost * e.getActionDelay();
					}
					else
					{
						e.actionDelayAccumulator -= e.getActionDelay();
					}
				}
				
				if (e.actionDelayAccumulator <= 0)
				{
					itr.remove();
				}
				else if (e.tile.GetVisible())
				{
					// If entity is now visible, submit to the visible list to be processed
					itr.remove();
					visibleList.add(e);
				}
			}
		}
	}
	
	public boolean enemyVisible()
	{
		boolean enemy = false;
		
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{				
				if (Grid[x][y].GetVisible() && Grid[x][y].entity != null)
				{					
					if (!Grid[x][y].entity.isAllies(player))
					{
						enemy = true;
						break;
					}
				}
			}
			if (enemy) { break; }
		}
				
		return enemy;
	}
	
	public boolean hasActiveEffects()
	{
		boolean activeEffects = false;
		
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{			
				if (!Grid[x][y].GetVisible()) { continue; }
				
				if (Grid[x][y].spriteEffects.size > 0)
				{
					activeEffects = true;
					break;
							
				}
				
				{
					Entity e = Grid[x][y].entity;
					if (e != null)
					{									
						boolean active = hasActiveEffects(e);
						if (active)
						{
							activeEffects = true;
							break;
						}
					}	
				}
				
				{
					Entity e = Grid[x][y].environmentEntity;
					if (e != null)
					{									
						boolean active = hasActiveEffects(e);
						if (active)
						{
							activeEffects = true;
							break;
						}
					}	
				}			
			}
			
			if (activeEffects) { break; }
		}
		
		return activeEffects;
	}
	
	public boolean hasActiveEffects(Entity e)
	{
		boolean activeEffects = false;
		
		if (e.sprite.spriteAnimation != null)
		{
			activeEffects = true;
		}
		
		if (e.spriteEffects.size > 0)
		{
			activeEffects = true;
		}
		
		return activeEffects;
	}
	
	public void getAllEntitiesToBeProcessed(float cost)
	{		
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				boolean visible = Grid[x][y].GetVisible();
				GameEntity e = Grid[x][y].entity;
				if (e != null && e != player)
				{
					e.update(cost);
					
					if (e.actionDelayAccumulator > 0)
					{
						if (visible)
						{
							visibleList.add(e);
						}
						else
						{
							invisibleList.add(e);
						}					
					}
				}
			}
		}
		
		for (ActiveAbility aa : ActiveAbilities)
		{
			aa.updateAccumulators(cost);
		}
	}
		
	public Array<GameEntity> getAllEntities()
	{
		Array<GameEntity> list = new Array<GameEntity>();
		
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				if (Grid[x][y].entity != null)
				{
					list.add(Grid[x][y].entity);
				}
			}
		}
		
		return list;
	}
	
	public Array<EnvironmentEntity> getAllEnvironmentEntities()
	{
		Array<EnvironmentEntity> list = new Array<EnvironmentEntity>();
		
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				if (Grid[x][y].environmentEntity != null)
				{
					list.add(Grid[x][y].environmentEntity);
				}
			}
		}
		
		return list;
	}
}
