/*
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 
 * Copyright 2011, 2012 Peter Güttinger
 * 
 */

package ch.njol.skript.effects;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import ch.njol.skript.Skript;
import ch.njol.skript.api.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.Slot;

/**
 * 
 * @author Peter Güttinger
 * 
 */
public class EffHealth extends Effect {
	
	static {
		Skript.registerEffect(EffHealth.class,
				"damage %slots% by %integer%",
				"damage %livingentities% by %integer%",
				"heal %livingentities% [by %-integer%]",
				"repair %slots% [by %-integer%]");
	}
	
	private Expression<Object> damageables;
	private Expression<Integer> damage;
	private boolean heal = false;
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean init(final Expression<?>[] vars, final int matchedPattern, final ParseResult parser) {
		damageables = (Expression<Object>) vars[0];
		damage = (Expression<Integer>) vars[1];
		heal = (matchedPattern >= 2);
		return true;
	}
	
	@Override
	public void execute(final Event e) {
		int damage = 0;
		if (this.damage != null)
			damage = this.damage.getSingle(e);
		for (final Object damageable : damageables.getArray(e)) {
			if (damageable instanceof Slot) {
				ItemStack is = ((Slot) damageable).getItem();
				if (this.damage == null) {
					is.setDurability((short) 0);
				} else {
					is.setDurability((short) (is.getDurability() + (heal ? -damage : damage)));
					if (is.getDurability() >= is.getType().getMaxDurability())
						is = null;
				}
				((Slot) damageable).setItem(is);
			} else if (damageable instanceof LivingEntity) {
				if (this.damage == null) {
					((LivingEntity) damageable).setHealth(((LivingEntity) damageable).getMaxHealth());
				} else {
					((LivingEntity) damageable).setHealth(((LivingEntity) damageable).getHealth() + (heal ? damage : -damage));
				}
			}
		}
	}
	
	@Override
	public String getDebugMessage(final Event e) {
		return (heal ? "heal " : "damage ") + damageables.getDebugMessage(e) + (damage == null ? "" : " by " + damage.getDebugMessage(e));
	}
	
}
