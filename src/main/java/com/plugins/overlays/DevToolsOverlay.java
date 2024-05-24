/*
 * Copyright (c) 2017, Kronos <https://github.com/KronosDesign>
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.plugins.overlays;

import com.plugins.DevToolsPlugin;
import com.plugins.utils.MovementFlag;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Set;

@Singleton
public
class DevToolsOverlay extends Overlay
{
	private static final Font FONT = FontManager.getRunescapeFont().deriveFont(Font.BOLD, 16);
	private static final Color RED = new Color(221, 44, 0);
	private static final Color GREEN = new Color(0, 200, 83);
	private static final Color ORANGE = new Color(255, 109, 0);
	private static final Color YELLOW = new Color(255, 214, 0);
	private static final Color CYAN = new Color(0, 184, 212);
	private static final Color BLUE = new Color(41, 98, 255);
	private static final Color DEEP_PURPLE = new Color(98, 0, 234);
	private static final Color PURPLE = new Color(170, 0, 255);
	private static final Color GRAY = new Color(158, 158, 158);

	private static final int MAX_DISTANCE = 2400;

	private final Client client;
	private final DevToolsPlugin plugin;
	private final TooltipManager toolTipManager;

	@Inject
	private DevToolsOverlay(Client client, DevToolsPlugin plugin, TooltipManager toolTipManager)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGHEST);
		this.client = client;
		this.plugin = plugin;
		this.toolTipManager = toolTipManager;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		graphics.setFont(FONT);

		if (plugin.getPlayers().isActive())
		{
			renderPlayers(graphics);
		}

		if (plugin.getNpcs().isActive())
		{
			renderNpcs(graphics);
		}

		if (plugin.getGroundItems().isActive() || plugin.getGroundObjects().isActive() || plugin.getGameObjects().isActive() || plugin.getWalls().isActive() || plugin.getDecorations().isActive() || plugin.getTileLocation().isActive() || plugin.getMovementFlags().isActive())
		{
			renderTileObjects(graphics);
		}

		if (plugin.getInventory().isActive())
		{
			renderInventory(graphics);
		}

		if (plugin.getProjectiles().isActive())
		{
			renderProjectiles(graphics);
		}

		if (plugin.getGraphicsObjects().isActive())
		{
			renderGraphicsObjects(graphics);
		}

		if (plugin.getRoofs().isActive())
		{
			renderRoofs(graphics);
		}

		return null;
	}

	private void renderRoofs(Graphics2D graphics)
	{
		Scene scene = client.getTopLevelWorldView().getScene();
		Tile[][][] tiles = scene.getTiles();
		byte[][][] settings = client.getTopLevelWorldView().getTileSettings();
		int z = client.getTopLevelWorldView().getPlane();
		String text = "R";

		for (int x = 0; x < Constants.SCENE_SIZE; ++x)
		{
			for (int y = 0; y < Constants.SCENE_SIZE; ++y)
			{
				Tile tile = tiles[z][x][y];

				if (tile == null)
				{
					continue;
				}

				int flag = settings[z][x][y];
				if ((flag & Constants.TILE_FLAG_UNDER_ROOF) == 0)
				{
					continue;
				}

				Point loc = Perspective.getCanvasTextLocation(client, graphics, tile.getLocalLocation(), text, z);
				if (loc == null)
				{
					continue;
				}

				OverlayUtil.renderTextLocation(graphics, loc, text, Color.RED);
			}
		}
	}

	private void renderPlayers(Graphics2D graphics)
	{
		IndexedObjectSet<? extends Player> players = client.getTopLevelWorldView().players();
		Player local = client.getLocalPlayer();

		for (Player p : players)
		{
			if (p != local)
			{
				StringBuilder s = new StringBuilder();
				for (ActorSpotAnim sa : p.getSpotAnims())
				{
					s.append(sa.getId()).append(",");
				}
				String g = s.length() > 0 ? s.substring(0, s.length() - 1) : "-1";
				String text = p.getName() + " (A: " + p.getAnimation() + ") (P: " + p.getPoseAnimation() + ") (G: " + g + ")";
				OverlayUtil.renderActorOverlay(graphics, p, text, BLUE);
			}
		}
		StringBuilder s = new StringBuilder();
		for (ActorSpotAnim sa : local.getSpotAnims())
		{
			s.append(sa.getId()).append(",");
		}
		String g = s.length() > 0 ? s.substring(0, s.length() - 1) : "-1";
		String text = local.getName() + " (A: " + local.getAnimation() + ") (P: " + local.getPoseAnimation() + ") (G: " + g + ")";
		OverlayUtil.renderActorOverlay(graphics, local, text, CYAN);
	}

	private void renderNpcs(Graphics2D graphics)
	{
		IndexedObjectSet<? extends NPC> npcs = client.getTopLevelWorldView().npcs();
		for (NPC npc : npcs)
		{
			NPCComposition composition = npc.getComposition();
			Color color = composition.getCombatLevel() > 1 ? YELLOW : ORANGE;
			if (composition.getConfigs() != null)
			{
				NPCComposition transformedComposition = composition.transform();
				if (transformedComposition == null)
				{
					color = GRAY;
				}
				else
				{
					composition = transformedComposition;
				}
			}

			StringBuilder s = new StringBuilder();
			for (ActorSpotAnim sa : npc.getSpotAnims())
			{
				s.append(sa.getId()).append(",");
			}
			String g = s.length() > 0 ? s.substring(0, s.length() - 1) : "-1";
			String text = composition.getName() + " (ID:" + composition.getId() + ")" +
				" (A: " + npc.getAnimation() + ") (P: " + npc.getPoseAnimation() + ") (G: " + g + ")";
			OverlayUtil.renderActorOverlay(graphics, npc, text, color);
		}
	}

	private void renderTileObjects(Graphics2D graphics)
	{
		Scene scene = client.getTopLevelWorldView().getScene();
		Tile[][][] tiles = scene.getTiles();

		int z = client.getTopLevelWorldView().getPlane();

		for (int x = 0; x < Constants.SCENE_SIZE; ++x)
		{
			for (int y = 0; y < Constants.SCENE_SIZE; ++y)
			{
				Tile tile = tiles[z][x][y];

				if (tile == null)
				{
					continue;
				}

				Player player = client.getLocalPlayer();
				if (player == null)
				{
					continue;
				}

				if (plugin.getGroundItems().isActive())
				{
					renderGroundItems(graphics, tile, player);
				}

				if (plugin.getGroundObjects().isActive())
				{
					renderTileObject(graphics, tile.getGroundObject(), player, PURPLE);
				}

				if (plugin.getGameObjects().isActive())
				{
					renderGameObjects(graphics, tile, player);
				}

				if (plugin.getWalls().isActive())
				{
					renderTileObject(graphics, tile.getWallObject(), player, GRAY);
				}

				if (plugin.getDecorations().isActive())
				{
					renderDecorObject(graphics, tile, player);
				}

				if (plugin.getTileLocation().isActive())
				{
					renderTileTooltip(graphics, tile);
				}

				if (plugin.getMovementFlags().isActive())
				{
					renderMovementInfo(graphics, tile);
				}
			}
		}
	}

	private void renderTileTooltip(Graphics2D graphics, Tile tile)
	{
		final LocalPoint tileLocalLocation = tile.getLocalLocation();
		Polygon poly = Perspective.getCanvasTilePoly(client, tileLocalLocation);
		if (poly != null && poly.contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY()))
		{
			WorldPoint worldLocation = tile.getWorldLocation();
			String tooltip = String.format("World location: %d, %d, %d</br>" +
					"Region ID: %d location: %d, %d",
				worldLocation.getX(), worldLocation.getY(), worldLocation.getPlane(),
				(client.getTopLevelWorldView().getScene().isInstance() ? WorldPoint.fromLocalInstance(client, tileLocalLocation).getRegionID() : worldLocation.getRegionID()), worldLocation.getRegionX(), worldLocation.getRegionY());
			toolTipManager.add(new Tooltip(tooltip));
			OverlayUtil.renderPolygon(graphics, poly, GREEN);
		}
	}

	private void renderMovementInfo(Graphics2D graphics, Tile tile)
	{
		Polygon poly = Perspective.getCanvasTilePoly(client, tile.getLocalLocation());

		if (poly == null || !poly.contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY()))
		{
			return;
		}

		if (client.getTopLevelWorldView().getCollisionMaps() != null)
		{
			int[][] flags = client.getTopLevelWorldView().getCollisionMaps()[client.getTopLevelWorldView().getPlane()].getFlags();
			int data = flags[tile.getSceneLocation().getX()][tile.getSceneLocation().getY()];

			Set<MovementFlag> movementFlags = MovementFlag.getSetFlags(data);

			if (movementFlags.isEmpty())
			{
				toolTipManager.add(new Tooltip("No movement flags"));
			}
			else
			{
				movementFlags.forEach(flag -> toolTipManager.add(new Tooltip(flag.toString())));
			}

			OverlayUtil.renderPolygon(graphics, poly, GREEN);
		}
	}

	private void renderGroundItems(Graphics2D graphics, Tile tile, Player player)
	{
		ItemLayer itemLayer = tile.getItemLayer();
		if (itemLayer != null)
		{
			if (player.getLocalLocation().distanceTo(itemLayer.getLocalLocation()) <= MAX_DISTANCE)
			{
				Node current = itemLayer.getTop();
				while (current instanceof TileItem)
				{
					TileItem item = (TileItem) current;
					OverlayUtil.renderTileOverlay(graphics, itemLayer, "ID: " + item.getId() + " Qty:" + item.getQuantity(), RED);
					current = current.getNext();
				}
			}
		}
	}

	private void renderGameObjects(Graphics2D graphics, Tile tile, Player player)
	{
		GameObject[] gameObjects = tile.getGameObjects();
		if (gameObjects != null)
		{
			for (GameObject gameObject : gameObjects)
			{
				if (gameObject != null && gameObject.getSceneMinLocation().equals(tile.getSceneLocation()))
				{
					if (player.getLocalLocation().distanceTo(gameObject.getLocalLocation()) <= MAX_DISTANCE)
					{
						StringBuilder stringBuilder = new StringBuilder();
						stringBuilder.append("ID: ").append(gameObject.getId());
						if (gameObject.getRenderable() instanceof DynamicObject)
						{
							Animation animation = ((DynamicObject) gameObject.getRenderable()).getAnimation();
							if (animation != null)
							{
								stringBuilder.append(" A: ").append(animation.getId());
							}
						}

						OverlayUtil.renderTileOverlay(graphics, gameObject, stringBuilder.toString(), GREEN);
					}
				}
			}
		}
	}

	private void renderTileObject(Graphics2D graphics, TileObject tileObject, Player player, Color color)
	{
		if (tileObject != null)
		{
			if (player.getLocalLocation().distanceTo(tileObject.getLocalLocation()) <= MAX_DISTANCE)
			{
				OverlayUtil.renderTileOverlay(graphics, tileObject, "ID: " + tileObject.getId(), color);
			}
		}
	}

	private void renderDecorObject(Graphics2D graphics, Tile tile, Player player)
	{
		DecorativeObject decorObject = tile.getDecorativeObject();
		if (decorObject != null)
		{
			if (player.getLocalLocation().distanceTo(decorObject.getLocalLocation()) <= MAX_DISTANCE)
			{
				OverlayUtil.renderTileOverlay(graphics, decorObject, "ID: " + decorObject.getId(), DEEP_PURPLE);
			}

			Shape p = decorObject.getConvexHull();
			if (p != null)
			{
				graphics.draw(p);
			}

			p = decorObject.getConvexHull2();
			if (p != null)
			{
				graphics.draw(p);
			}
		}
	}

	private void renderInventory(Graphics2D graphics)
	{
		Widget inventoryWidget = client.getWidget(InterfaceID.INVENTORY, 0);
		if (inventoryWidget == null || inventoryWidget.isHidden())
		{
			return;
		}

		for (Widget item : inventoryWidget.getDynamicChildren())
		{
			Rectangle slotBounds = item.getBounds();
			int itemId = item.getItemId();

			if (itemId == 6512)
			{
				continue;
			}

			String idText = "" + itemId;

			FontMetrics fm = graphics.getFontMetrics();
			Rectangle2D textBounds = fm.getStringBounds(idText, graphics);

			int textX = (int) (slotBounds.getX() + (slotBounds.getWidth() / 2) - (textBounds.getWidth() / 2));
			int textY = (int) (slotBounds.getY() + (slotBounds.getHeight() / 2) + (textBounds.getHeight() / 2));

			graphics.setColor(new Color(255, 255, 255, 65));
			graphics.fill(slotBounds);

			graphics.setColor(Color.BLACK);
			graphics.drawString(idText, textX + 1, textY + 1);
			graphics.setColor(YELLOW);
			graphics.drawString(idText, textX, textY);
		}
	}

	private void renderProjectiles(Graphics2D graphics)
	{
		for (Projectile projectile : client.getTopLevelWorldView().getProjectiles())
		{
			int projectileId = projectile.getId();
			String text = "(ID: " + projectileId + ")";
			int x = (int) projectile.getX();
			int y = (int) projectile.getY();
			LocalPoint projectilePoint = new LocalPoint(x, y, client.getTopLevelWorldView());
			Point textLocation = Perspective.getCanvasTextLocation(client, graphics, projectilePoint, text, 0);
			if (textLocation != null)
			{
				OverlayUtil.renderTextLocation(graphics, textLocation, text, Color.RED);
			}
		}
	}

	private void renderGraphicsObjects(Graphics2D graphics)
	{
		for (GraphicsObject graphicsObject : client.getTopLevelWorldView().getGraphicsObjects())
		{
			LocalPoint lp = graphicsObject.getLocation();
			Polygon poly = Perspective.getCanvasTilePoly(client, lp);

			if (poly != null)
			{
				OverlayUtil.renderPolygon(graphics, poly, Color.MAGENTA);
			}

			String infoString = "(ID: " + graphicsObject.getId() + ")";
			Point textLocation = Perspective.getCanvasTextLocation(
				client, graphics, lp, infoString, 0);
			if (textLocation != null)
			{
				OverlayUtil.renderTextLocation(graphics, textLocation, infoString, Color.WHITE);
			}
		}
	}
}
