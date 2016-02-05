package com.planet_ink.coffee_mud.Items.CompTech;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.CMSecurity.DbgFlag;
import com.planet_ink.coffee_mud.core.collections.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.BasicTech.StdElecItem;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.ShipComponent.ShipEngine;
import com.planet_ink.coffee_mud.Items.interfaces.Technical.TechCommand;
import com.planet_ink.coffee_mud.Libraries.interfaces.*;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;

import java.lang.ref.WeakReference;
import java.util.*;

/*
   Copyright 2016-2016 Bo Zimmerman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
public class StdCompShieldGenerator extends StdElecCompItem implements ShipComponent
{
	@Override
	public String ID()
	{
		return "StdElecCompSensor";
	}

	@Override
	public TechType getTechType()
	{
		return Technical.TechType.SHIP_SHIELD;
	}
	
	private volatile long lastPowerConsumption=0;
	private volatile long powerSetting=Integer.MAX_VALUE;
	private volatile WeakReference<SpaceShip> myShip = null;
	
	@Override
	public void setOwner(ItemPossessor container)
	{
		super.setOwner(container);
		myShip = null;
	}

	/**
	 * The maximum range of objects that this sensor can detect
	 * @return maximum range of objects that this sensor can detect
	 */
	protected long getSensorMaxRange()
	{
		return SpaceObject.Distance.Parsec.dm;
	}

	@Override
	public int powerNeeds()
	{
		return (int) Math.min((int) Math.min(powerCapacity,powerSetting) - power, maxRechargePer);
	}
	
	protected synchronized SpaceShip getMyShip()
	{
		if(myShip == null)
		{
			final Area area = CMLib.map().areaLocation(this);
			if(area instanceof SpaceShip)
				myShip = new WeakReference<SpaceShip>((SpaceShip)area);
			else
				myShip = new WeakReference<SpaceShip>(null);
		}
		return myShip.get();
	}
	
	@Override
	public boolean okMessage(Environmental host, CMMsg msg)
	{
		if(!super.okMessage(host, msg))
			return false;
		final SpaceShip ship = getMyShip(); 
		if(msg.target() == ship)
		{
			switch(msg.targetMinor())
			{
			case CMMsg.TYP_DAMAGE: // laser, energy, some other kind of directed damage
			{
				if((msg.value() > 1)&&(this.lastPowerConsumption>0))
				{
					//final int dmg = msg.value();
					// some shields actually mitigate this!
				}
				break;
			}
			case CMMsg.TYP_WEAPONATTACK: // kinetic damage taken to the outside of the ship, collissions end up here
			{
				if((msg.value() > 1)&&(this.lastPowerConsumption>0))
				{
					//final int dmg = msg.value();
					// some shields actually mitigate this!
				}
				break;
			}
			}
		}
		return true;
	}

	@Override
	public void executeMsg(Environmental myHost, CMMsg msg)
	{
		super.executeMsg(myHost, msg);
		if(msg.amITarget(this))
		{
			switch(msg.targetMinor())
			{
			case CMMsg.TYP_ACTIVATE:
			{
				final LanguageLibrary lang=CMLib.lang();
				final String[] parts=msg.targetMessage().split(" ");
				final TechCommand command=TechCommand.findCommand(parts);
				final Software controlI=(msg.tool() instanceof Software)?((Software)msg.tool()):null;
				final MOB mob=msg.source();
				if(command==null)
					reportError(this, controlI, mob, lang.L("@x1 does not respond.",me.name(mob)), lang.L("Failure: @x1: control failure.",me.name(mob)));
				else
				{
					final Object[] parms=command.confirmAndTranslate(parts);
					if(parms==null)
						reportError(this, controlI, mob, lang.L("@x1 did not respond.",me.name(mob)), lang.L("Failure: @x1: control syntax failure.",me.name(mob)));
					else
					if(command == TechCommand.POWERSET)
					{
						powerSetting=((Long)parms[0]).intValue();
						if(powerSetting<0)
							powerSetting=0;
						else
						if(powerSetting > powerCapacity())
							powerSetting = powerCapacity();
					}
					else
						reportError(this, controlI, mob, lang.L("@x1 refused to respond.",me.name(mob)), lang.L("Failure: @x1: control command failure.",me.name(mob)));
				}
				break;
			}
			case CMMsg.TYP_POWERCURRENT:
				// shields should constantly consume what they have
				if(activated())
				{
					this.lastPowerConsumption = this.power;
					this.power = 0;
				}
				else
				{
					this.lastPowerConsumption = 0;
					this.power = 0;
				}
				break;
			case CMMsg.TYP_DEACTIVATE:
				this.activate(false);
				this.lastPowerConsumption = 0;
				this.power = 0;
				//TODO:what does the ship need to know?
				break;
			}
		}
	}

	@Override
	public boolean sameAs(Environmental E)
	{
		if(!(E instanceof StdCompShieldGenerator))
			return false;
		return super.sameAs(E);
	}
}