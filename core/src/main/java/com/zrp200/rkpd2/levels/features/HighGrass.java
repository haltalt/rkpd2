/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2021 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.zrp200.rkpd2.levels.features;

import com.zrp200.rkpd2.Assets;
import com.zrp200.rkpd2.Dungeon;
import com.zrp200.rkpd2.ShatteredPixelDungeon;
import com.zrp200.rkpd2.actors.Actor;
import com.zrp200.rkpd2.actors.Char;
import com.zrp200.rkpd2.actors.buffs.Buff;
import com.zrp200.rkpd2.actors.buffs.Invisibility;
import com.zrp200.rkpd2.actors.hero.Hero;
import com.zrp200.rkpd2.actors.hero.HeroClass;
import com.zrp200.rkpd2.actors.hero.Talent;
import com.zrp200.rkpd2.effects.CellEmitter;
import com.zrp200.rkpd2.effects.particles.LeafParticle;
import com.zrp200.rkpd2.items.Dewdrop;
import com.zrp200.rkpd2.items.Generator;
import com.zrp200.rkpd2.items.armor.glyphs.Camouflage;
import com.zrp200.rkpd2.items.artifacts.SandalsOfNature;
import com.zrp200.rkpd2.items.food.Berry;
import com.zrp200.rkpd2.levels.Level;
import com.zrp200.rkpd2.levels.Terrain;
import com.zrp200.rkpd2.scenes.GameScene;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

public class HighGrass {
	
	//prevents items dropped from grass, from trampling that same grass.
	//yes this is a bit ugly, oh well.
	private static boolean freezeTrample = false;

	public static boolean plant(int cell) {
		int t = Dungeon.level.map[cell];
		if ((t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.EMBERS
				|| t == Terrain.GRASS || t == Terrain.FURROWED_GRASS)
				&& Dungeon.level.plants.get(cell) == null){
			Level.set(cell, Terrain.HIGH_GRASS);
			GameScene.updateMap(cell);
			return true;
		}
		return false;
	}

	public static void playVFX(int pos) {
		CellEmitter.get(pos).burst(LeafParticle.LEVEL_SPECIFIC, 4);
	}

	public static void trample( Level level, int pos ) {
		
		if (freezeTrample) return;
		
		Char ch = Actor.findChar(pos);
		boolean furrow = ch instanceof Hero
				&& (((Hero)ch).heroClass == HeroClass.HUNTRESS
				|| ((Hero)ch).heroClass == HeroClass.RAT_KING);
		if (level.map[pos] == Terrain.FURROWED_GRASS){
			if (ch instanceof Hero && furrow){
				//Do nothing
				freezeTrample = true;
			} else {
				Level.set(pos, Terrain.GRASS);
			}
			
		} else {
			if (ch instanceof Hero && furrow){
				Level.set(pos, Terrain.FURROWED_GRASS);
				freezeTrample = true;
			} else {
				Level.set(pos, Terrain.GRASS);
			}
			
			int naturalismLevel = 0;
			
			if (ch != null) {
				SandalsOfNature.Naturalism naturalism = ch.buff( SandalsOfNature.Naturalism.class );
				if (naturalism != null) {
					if (!naturalism.isCursed()) {
						naturalismLevel = naturalism.itemLevel() + 1;
						naturalism.charge(1);
					} else {
						naturalismLevel = -1;
					}
				}

				//berries try to drop on floors 2/3/4/6/7/8, to a max of 4/6
				Talent.NatureBerriesAvailable berries = ch.buff(Talent.NatureBerriesAvailable.class);
				if (berries != null) {
					int targetFloor = 2 + 2*((Hero)ch).pointsInTalent(Talent.NATURES_BOUNTY);
					targetFloor -= berries.count();
					targetFloor += (targetFloor >= 5) ? 3 : 2;

					//If we're behind: 1/10, if we're on page: 1/30, if we're ahead: 1/90
					boolean droppingBerry = false;
					if (Dungeon.depth > targetFloor)        droppingBerry = Random.Int(10) == 0;
					else if (Dungeon.depth == targetFloor)  droppingBerry = Random.Int(30) == 0;
					else if (Dungeon.depth < targetFloor)   droppingBerry = Random.Int(90) == 0;

					if (droppingBerry){
						berries.countDown(1);
						level.drop(new Berry(), pos).sprite.drop();
						if (berries.count() <= 0){
							berries.detach();
						}
					}

				}
			}
			
			if (naturalismLevel >= 0) {
				// TODO NERF
				// sigh.
				int points = Dungeon.hero.pointsInTalent(Talent.NATURES_BETTER_AID);
				// Seed, scales from 1/25 to 1/5
				// NBA increases by 17%/33%/50%
				if (Random.Float() < (1+points/6f)/(25 - (naturalismLevel * 5))) {
					level.drop(Generator.random(Generator.Category.SEED), pos).sprite.drop();
				}
				
				// Dew, scales from 1/6 to 1/3
				// NBA increases by 1/12 / 1/6 / 1/4
				if (Random.Float(24 - naturalismLevel*3) <= 3*(1 + .25*(points/3f))) {
					level.drop(new Dewdrop(), pos).sprite.drop();
				}
			}
			
			if (ch instanceof Hero) {
				Hero hero = (Hero) ch;
				
				//Camouflage
				//FIXME doesn't work with sad ghost
				if (hero.belongings.armor() != null && hero.belongings.armor().hasGlyph(Camouflage.class, hero)) {
					Buff.prolong(hero, Invisibility.class, 3 + hero.belongings.armor.buffedLvl()/2);
					Sample.INSTANCE.play( Assets.Sounds.MELD );
				}
			}
			
		}
		
		freezeTrample = false;
		
		if (ShatteredPixelDungeon.scene() instanceof GameScene) {
			GameScene.updateMap(pos);
			
			playVFX(pos);
			if (Dungeon.level.heroFOV[pos]) Dungeon.observe();
		}
	}
}
