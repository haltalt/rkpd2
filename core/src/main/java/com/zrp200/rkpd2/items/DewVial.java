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

package com.zrp200.rkpd2.items;

import com.zrp200.rkpd2.Assets;
import com.zrp200.rkpd2.actors.buffs.Barrier;
import com.zrp200.rkpd2.actors.hero.Hero;
import com.zrp200.rkpd2.actors.hero.Talent;
import com.zrp200.rkpd2.messages.Messages;
import com.zrp200.rkpd2.sprites.ItemSpriteSheet;
import com.zrp200.rkpd2.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;

import java.util.ArrayList;

public class DewVial extends Item {

	private static final int MAX_VOLUME	= 20;

	private static final String AC_DRINK	= "DRINK";

	private static final float TIME_TO_DRINK = 1f;

	private static final String TXT_STATUS	= "%d/%d";

	{
		image = ItemSpriteSheet.VIAL;

		defaultAction = AC_DRINK;

		unique = true;
	}

	private int volume = 0;

	private static final String VOLUME	= "volume";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( VOLUME, volume );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		volume	= bundle.getInt( VOLUME );
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		if (volume > 0) {
			actions.add( AC_DRINK );
		}
		return actions;
	}

	@Override
	public void execute( final Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals( AC_DRINK )) {

			if (volume > 0) {
				
				float missingHealthPercent = 1f - (hero.HP / (float)hero.HT);

				int curShield = 0;
				if (hero.buff(Barrier.class) != null) curShield = hero.buff(Barrier.class).shielding();
				//if (hero.hasTalent(Talent.SHIELDING_DEW)) curShield += Math.floor((missingHealthPercent/0.05f)*0.2f*hero.pointsInTalent(Talent.SHIELDING_DEW)); // this tries to predict the total increase via individual dewdrops.
				int maxShield = Math.round(hero.HT *0.2f*hero.pointsInTalent(Talent.SHIELDING_DEW,Talent.RK_WARDEN));
				if (hero.hasTalent(Talent.SHIELDING_DEW,Talent.RK_WARDEN)){
					float missingShieldPercent = 1f - (curShield / (float)maxShield);
					missingShieldPercent *= 0.2f*hero.pointsInTalent(Talent.SHIELDING_DEW,Talent.RK_WARDEN);
					if (missingShieldPercent > 0){
						missingHealthPercent += missingShieldPercent;
					}
				}

				//trimming off 0.01 drops helps with floating point errors
				int dropsNeeded = (int)Math.ceil((missingHealthPercent / 0.05f) - 0.01f);
				dropsNeeded = (int)GameMath.gate(1, dropsNeeded, volume);

				if (Dewdrop.consumeDew(dropsNeeded, hero)){
					volume -= dropsNeeded;

					hero.spend(TIME_TO_DRINK);
					hero.busy();

					Sample.INSTANCE.play(Assets.Sounds.DRINK);
					hero.sprite.operate(hero.pos);

					updateQuickslot();
				}


			} else {
				GLog.w( Messages.get(this, "empty") );
			}

		}
	}

	public void empty() {volume = 0; updateQuickslot();}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	public boolean isFull() {
		return volume >= MAX_VOLUME;
	}

	public void collectDew( Dewdrop dew ) {

		GLog.i( Messages.get(this, "collected") );
		volume += dew.quantity;
		if (volume >= MAX_VOLUME) {
			volume = MAX_VOLUME;
			GLog.p( Messages.get(this, "full") );
		}

		updateQuickslot();
	}

	public void fill() {
		volume = MAX_VOLUME;
		updateQuickslot();
	}

	@Override
	public String status() {
		return Messages.format( TXT_STATUS, volume, MAX_VOLUME );
	}

}
