package com.planet_ink.coffee_mud.Abilities.Prayers;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.collections.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.*;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;

import java.util.*;

/*
   Copyright 2019-2019 Bo Zimmerman

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
public class Prayer_WordOfChaos extends Prayer
{
	@Override
	public String ID()
	{
		return "Prayer_WordOfChaos";
	}

	private final static String localizedName = CMLib.lang().L("Word of Chaos");

	@Override
	public String name()
	{
		return localizedName;
	}

	private final static String localizedStaticDisplay = CMLib.lang().L("(Word of Chaos)");

	@Override
	public String displayText()
	{
		return localizedStaticDisplay;
	}

	@Override
	protected int canAffectCode()
	{
		return Ability.CAN_MOBS;
	}

	@Override
	protected int canTargetCode()
	{
		return Ability.CAN_MOBS;
	}

	@Override
	public int abstractQuality()
	{
		return Ability.QUALITY_INDIFFERENT;
	}

	@Override
	public int classificationCode()
	{
		return Ability.ACODE_PRAYER|Ability.DOMAIN_CURSING;
	}

	@Override
	public long flags()
	{
		return Ability.FLAG_CHAOS;
	}

	@Override
	public void affectPhyStats(final Physical affected, final PhyStats affectableStats)
	{
		super.affectPhyStats(affected,affectableStats);
		if(affected==null)
			return;
		if(!(affected instanceof MOB))
			return;
		final MOB mob=(MOB)affected;

		if(mob==invoker)
			return;

		final int xlvl=super.getXLEVELLevel(invoker());
		if(CMLib.flags().isChaotic(mob))
		{
			affectableStats.setArmor(affectableStats.armor()-15-(6*xlvl));
			affectableStats.setAttackAdjustment(affectableStats.attackAdjustment()+20+(4*xlvl));
		}
		else
		if(CMLib.flags().isLawful(mob))
		{
			affectableStats.setArmor(affectableStats.armor()+15+(6*xlvl));
			affectableStats.setAttackAdjustment(affectableStats.attackAdjustment()-20-(4*xlvl));
		}
	}

	@Override
	public void unInvoke()
	{
		// undo the affects of this spell
		if(!(affected instanceof MOB))
			return;
		final MOB mob=(MOB)affected;

		super.unInvoke();

		if(canBeUninvoked())
			mob.tell(L("The word of chaos has been spoken."));
	}

	public static Item getSomething(final MOB mob, final boolean blessedOnly)
	{
		final List<Item> good=new ArrayList<Item>(1);
		final List<Item> great=new ArrayList<Item>(1);
		Item target=null;
		for(int i=0;i<mob.numItems();i++)
		{
			final Item I=mob.getItem(i);
			if((!blessedOnly)||(isLegal(I)))
			{
				if(I.amWearingAt(Wearable.IN_INVENTORY))
					good.add(I);
				else
					great.add(I);
			}
		}
		if(great.size()>0)
			target=great.get(CMLib.dice().roll(1,great.size(),-1));
		else
		if(good.size()>0)
			target=good.get(CMLib.dice().roll(1,good.size(),-1));
		return target;
	}

	public static void endLowerLegalProtections(final Physical target, final int level)
	{
		final List<Ability> V=CMLib.flags().domainAffects(target,Ability.DOMAIN_HOLYPROTECTION);
		for(int v=0;v<V.size();v++)
		{
			final Ability A=V.get(v);
			if((CMath.bset(A.flags(), Ability.FLAG_LAW))
			&&(!CMath.bset(A.flags(), Ability.FLAG_CHAOS)))
			{
				if(CMLib.ableMapper().lowestQualifyingLevel(A.ID())<=level)
					A.unInvoke();
			}
		}
	}

	public static boolean isLegal(final Item item)
	{
		return CMLib.flags().flaggedAffects(item,Ability.FLAG_LAW).size()>0;
	}

	public static void endAllOtherChaosProtections(final MOB from, final Physical target, final int level)
	{

		final List<Ability> V=CMLib.flags().domainAffects(target,Ability.DOMAIN_HOLYPROTECTION);
		for(int v=0;v<V.size();v++)
		{
			final Ability A=V.get(v);
			if((!CMath.bset(A.flags(), Ability.FLAG_LAW))
			&&(CMath.bset(A.flags(), Ability.FLAG_CHAOS)))
			{
				if((CMLib.ableMapper().lowestQualifyingLevel(A.ID())<level)
				||(from==A.invoker())
				||(target==from)
				||(target==A.invoker()))
					A.unInvoke();
			}
		}
	}

	@Override
	public boolean invoke(final MOB mob, final List<String> commands, final Physical givenTarget, final boolean auto, final int asLevel)
	{
		if(!super.invoke(mob,commands,givenTarget,auto,asLevel))
			return false;

		final boolean success=proficiencyCheck(mob,0,auto);
		final String str=auto?L("The word of chaos is spoken."):L("^S<S-NAME> speak(s) the word of chaos@x1 to <T-NAMESELF>.^?",ofDiety(mob));

		final Room room=mob.location();
		if(room!=null)
		for(int i=0;i<room.numInhabitants();i++)
		{
			final MOB target=room.fetchInhabitant(i);
			if(target==null)
				break;
			int affectType=CMMsg.MSG_CAST_VERBAL_SPELL;
			if(auto)
				affectType=affectType|CMMsg.MASK_ALWAYS;
			if(CMLib.flags().isLawful(target))
				affectType=affectType|CMMsg.MASK_MALICIOUS;

			if(success)
			{
				final CMMsg msg=CMClass.getMsg(mob,target,this,affectType,str);
				if(room.okMessage(mob,msg))
				{
					room.send(mob,msg);
					if(msg.value()<=0)
					{
						if(CMLib.flags().canBeHeardSpeakingBy(mob,target))
						{
							final Item I=getSomething(mob,true);
							if(I!=null)
							{
								endLowerLegalProtections(I,CMLib.ableMapper().lowestQualifyingLevel(ID()));
								I.recoverPhyStats();
							}
							endAllOtherChaosProtections(mob,target,CMLib.ableMapper().lowestQualifyingLevel(ID()));
							endLowerLegalProtections(target,CMLib.ableMapper().lowestQualifyingLevel(ID()));
							beneficialAffect(mob,target,asLevel,0);
							target.recoverPhyStats();
						}
						else
						if(CMath.bset(affectType,CMMsg.MASK_MALICIOUS))
							maliciousFizzle(mob,target,L("<T-NAME> did not hear the word of chaos!"));
						else
							beneficialWordsFizzle(mob,target,L("<T-NAME> did not hear the word of chaos!"));
					}
				}
			}
			else
			{
				if(CMath.bset(affectType,CMMsg.MASK_MALICIOUS))
					maliciousFizzle(mob,target,L("<S-NAME> attempt(s) to speak the word of chaos to <T-NAMESELF>, but flub(s) it."));
				else
					beneficialWordsFizzle(mob,target,L("<S-NAME> attempt(s) to speak the word of chaos to <T-NAMESELF>, but flub(s) it."));
				return false;
			}
		}

		// return whether it worked
		return success;
	}
}
