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

package com.zrp200.rkpd2.actors.mobs.npcs;

import com.zrp200.rkpd2.Assets;
import com.zrp200.rkpd2.actors.Actor;
import com.zrp200.rkpd2.actors.Char;
import com.zrp200.rkpd2.actors.blobs.CorrosiveGas;
import com.zrp200.rkpd2.actors.blobs.ToxicGas;
import com.zrp200.rkpd2.actors.buffs.Buff;
import com.zrp200.rkpd2.actors.buffs.Burning;
import com.zrp200.rkpd2.actors.buffs.Corruption;
import com.zrp200.rkpd2.actors.buffs.PrismaticGuard;
import com.zrp200.rkpd2.actors.hero.Hero;
import com.zrp200.rkpd2.actors.mobs.Mob;
import com.zrp200.rkpd2.effects.CellEmitter;
import com.zrp200.rkpd2.effects.Speck;
import com.zrp200.rkpd2.items.armor.glyphs.AntiMagic;
import com.zrp200.rkpd2.items.armor.glyphs.Brimstone;
import com.zrp200.rkpd2.levels.features.Chasm;
import com.zrp200.rkpd2.sprites.CharSprite;
import com.zrp200.rkpd2.sprites.PrismaticSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class PrismaticImage extends AbstractMirrorImage {
	
	{
		spriteClass = PrismaticSprite.class;
		
		HP = HT = 8;

		intelligentAlly = true;
		
		WANDERING = new Wandering();
	}
	
	private int deathTimer = -1;
	
	@Override
	protected boolean act() {
		
		if (!isAlive()){
			deathTimer--;
			
			if (deathTimer > 0) {
				sprite.alpha((deathTimer + 3) / 8f);
				spend(TICK);
			} else {
				destroy();
				sprite.die();
			}
			return true;
		}
		
		if (deathTimer != -1){
			if (paralysed == 0) sprite.remove(CharSprite.State.PARALYSED);
			deathTimer = -1;
			sprite.resetColor();
		}
		return super.act();
	}
	
	@Override
	public void die(Object cause) {
		if (deathTimer == -1) {
			if (cause == Chasm.class){
				super.die( cause );
			} else {
				deathTimer = 5;
				sprite.add(CharSprite.State.PARALYSED);
			}
		}
	}
	
	private static final String HEROID	= "hero_id";
	private static final String TIMER	= "timer";
	
	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( TIMER, deathTimer );
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		deathTimer = bundle.getInt( TIMER );
	}
	
	public void duplicate( Hero hero, int HP ) {
		duplicate(hero);
		this.HP = HP;
		HT = PrismaticGuard.maxHP( hero );
	}
	
	@Override
	public int damageRoll() {
		if (hero != null) {
			return Random.NormalIntRange( 1 + hero.lvl/8, 4 + hero.lvl/2 );
		} else {
			return Random.NormalIntRange( 1, 4 );
		}
	}

	@Override
	public int drRoll() {
		if (hero != null){
			return hero.drRoll();
		} else {
			return 0;
		}
	}
	
	@Override
	public int defenseProc(Char enemy, int damage) {
		damage = super.defenseProc(enemy, damage);
		if (hero != null && hero.belongings.armor() != null){
			return hero.belongings.armor().proc( enemy, this, damage );
		} else {
			return damage;
		}
	}
	
	@Override
	public void damage(int dmg, Object src) {
		
		//TODO improve this when I have proper damage source logic
		if (hero != null && hero.belongings.armor() != null && hero.belongings.armor().hasGlyph(AntiMagic.class, this)
				&& AntiMagic.RESISTS.contains(src.getClass())){
			dmg -= AntiMagic.drRoll(hero.belongings.armor().buffedLvl());
		}
		
		super.damage(dmg, src);
	}
	
	@Override
	public float speed() {
		if (hero != null && hero.belongings.armor() != null){
			return hero.belongings.armor().speedFactor(this, super.speed());
		}
		return super.speed();
	}

	@Override
	public boolean isImmune(Class effect) {
		if (effect == Burning.class
				&& hero != null
				&& hero.belongings.armor() != null
				&& hero.belongings.armor().hasGlyph(Brimstone.class, this)){
			return true;
		}
		return super.isImmune(effect);
	}

	private class Wandering extends Mob.Wandering{
		
		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			if (!enemyInFOV){
				Buff.affect(hero, PrismaticGuard.class).set( HP );
				destroy();
				CellEmitter.get(pos).start( Speck.factory(Speck.LIGHT), 0.2f, 3 );
				sprite.die();
				Sample.INSTANCE.play( Assets.Sounds.TELEPORT );
				return true;
			} else {
				return super.act(enemyInFOV, justAlerted);
			}
		}
		
	}
	
}
