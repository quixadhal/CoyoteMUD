package com.planet_ink.coffee_mud.Abilities.Spells;

import com.planet_ink.coffee_mud.interfaces.*;
import com.planet_ink.coffee_mud.common.*;
import com.planet_ink.coffee_mud.utils.*;
import java.util.*;


public class Spell_WeaknessAcid extends Spell
{

	public Spell_WeaknessAcid()
	{
		super();
		myID=this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.')+1);
		name="Weakness to Acid";
		displayText="(Weakness to Acid)";
		miscText="";

		canAffectCode=Ability.CAN_MOBS;
		canTargetCode=Ability.CAN_MOBS;

		canBeUninvoked=true;
		isAutoinvoked=false;
		quality=Ability.MALICIOUS;

		baseEnvStats().setLevel(2);

		baseEnvStats().setAbility(0);
		uses=Integer.MAX_VALUE;
		recoverEnvStats();
	}

	public Environmental newInstance()
	{
		return new Spell_WeaknessAcid();
	}
	public int classificationCode()
	{
		return Ability.SPELL|Ability.DOMAIN_TRANSMUTATION;
	}


	public boolean okAffect(Affect affect)
	{
		if(!super.okAffect(affect))
			return false;

		if((affected==null)||(!(affected instanceof MOB)))
			return true;

		MOB mob=(MOB)affected;
		if((affect.amITarget(mob))&&(Util.bset(affect.targetCode(),Affect.MASK_HURT))
		   &&(affect.sourceMinor()==Affect.TYP_ACID))
		{
			int recovery=(int)Math.round(Util.mul((affect.targetCode()-Affect.MASK_HURT),1.5));
			affect.modify(affect.source(),affect.target(),affect.tool(),affect.sourceCode(),affect.sourceMessage(),affect.targetCode()+recovery,affect.targetMessage(),affect.othersCode(),affect.othersMessage());
		}
		return true;
	}
	
	public void unInvoke()
	{
		// undo the affects of this spell
		if((affected==null)||(!(affected instanceof MOB)))
			return;
		MOB mob=(MOB)affected;
		mob.tell("Your acidic weakness is now gone.");

		super.unInvoke();

	}

	public void affectCharStats(MOB affectedMOB, CharStats affectedStats)
	{
		super.affectCharStats(affectedMOB,affectedStats);
		affectedStats.setStat(CharStats.SAVE_ACID,affectedStats.getStat(CharStats.SAVE_ACID)-100);
	}


	public boolean invoke(MOB mob, Vector commands, Environmental givenTarget, boolean auto)
	{
		MOB target=getTarget(mob,commands,givenTarget);
		if(target==null) return false;

		if(!super.invoke(mob,commands,givenTarget,auto))
			return false;

		boolean success=profficiencyCheck(0,auto);
		if(success)
		{
			FullMsg msg=new FullMsg(mob,target,this,affectType,auto?"A shimmering absorbing field appears around <T-NAMESELF>.":"<S-NAME> invoke(s) a shimmering absorbing field of weakness around <T-NAMESELF>.");
			if(mob.location().okAffect(msg))
			{
				mob.location().send(mob,msg);
				success=maliciousAffect(mob,target,0,-1);
			}
		}
		else
			maliciousFizzle(mob,target,"<S-NAME> attempt(s) to invoke weakness to acid, but fail(s).");

		return success;
	}
}
