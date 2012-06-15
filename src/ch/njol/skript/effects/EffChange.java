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

import org.bukkit.event.Event;

import ch.njol.skript.Skript;
import ch.njol.skript.api.Changer;
import ch.njol.skript.api.Changer.ChangeMode;
import ch.njol.skript.api.Changer.ChangerUtils;
import ch.njol.skript.api.Effect;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.UnparsedLiteral;
import ch.njol.skript.util.Patterns;

/**
 * @author Peter Güttinger
 * 
 */
public class EffChange extends Effect {
	
	private static Patterns<ChangeMode> patterns = new Patterns<ChangeMode>(new Object[][] {
			
			{"(add|give) %objects% to %objects%", ChangeMode.ADD},
			{"give %objects% %objects%", ChangeMode.ADD},
			
			{"set %objects% to %objects%", ChangeMode.SET},
			
			{"remove %objects% from %objects%", ChangeMode.REMOVE},
			
			{"(clear|delete) %objects%", ChangeMode.CLEAR},
	
	});
	
	static {
		Skript.registerEffect(EffChange.class, patterns.getPatterns());
	}
	
	private Expression<?> changed;
	private Expression<?> changer = null;
	
	private ChangeMode mode;
	
	private boolean single = true;
	private Changer<?, ?> c = null;
	
	@Override
	public boolean init(final Expression<?>[] vars, final int matchedPattern, final ParseResult parser) {
		
		mode = patterns.getInfo(matchedPattern);
		
		switch (mode) {
			case ADD:
				if (matchedPattern == 0) {
					changer = vars[0];
					changed = vars[1];
				} else {
					changer = vars[1];
					changed = vars[0];
				}
			break;
			case SET:
				changer = vars[1];
				changed = vars[0];
			break;
			case REMOVE:
				changer = vars[0];
				changed = vars[1];
			break;
			case CLEAR:
				changed = vars[0];
			break;
		}
		
		if (changed instanceof UnparsedLiteral)
			return false;
		
		Class<?> r = changed.acceptChange(mode);
		if (r == null) {
			final ClassInfo<?> ci = Skript.getSuperClassInfo(changed.getReturnType());
			if (ci == null || ci.getChanger() == null || (r = ci.getChanger().acceptChange(mode)) == null) {
				if (mode == ChangeMode.SET)
					Skript.error(changed + " can't be set");
				else if (mode == ChangeMode.CLEAR)
					Skript.error(changed + " can't be cleared/deleted");
				else
					Skript.error(changed + " can't have something " + (mode == ChangeMode.ADD ? "added to" : "removed from") + " it");
				return false;
			}
			c = ci.getChanger();
		}
		
		if (r.isArray()) {
			single = false;
			r = r.getComponentType();
		}
		
		if (changer != null) {
			final Expression<?> v = changer.getConvertedExpression(r);
			if (v == null) {
				if (mode == ChangeMode.SET)
					Skript.error(changed + " can't be set to " + changer);
				else
					Skript.error(changer + " can't be " + (mode == ChangeMode.ADD ? "added to" : "removed from") + " " + changed);
				return false;
			}
			changer = v;
			
			if (!changer.isSingle() && single) {
				if (mode == ChangeMode.SET)
					Skript.error(changed + " can only be set to one " + Skript.getExactClassName(r) + ", but multiple are given");
				else
					Skript.error("only one " + Skript.getExactClassName(r) + " can be " + (mode == ChangeMode.ADD ? "added to" : "removed from") + " " + changed + ", but multiple are given");
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	protected void execute(final Event e) {
		if (c != null)
			ChangerUtils.change(c, changed.getArray(e), changer == null ? null : single ? changer.getSingle(e) : changer.getArray(e), mode);
		changed.change(e, changer == null ? null : single ? changer.getSingle(e) : changer.getArray(e), mode);
	}
	
	@Override
	public String getDebugMessage(final Event e) {
		return "add " + changer.getDebugMessage(e) + " to " + changed.getDebugMessage(null);
	}
	
}
