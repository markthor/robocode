/*
Retreat v1.0 by Sheldor.  11/24/2012
A NanoBot with reverse ramming movement + reverse HOT.
Codesize: 102 Bytes without any colors.

Retreating is a fencing technique, for more information, see:
https://en.wikipedia.org/wiki/Fencing_terminology#R

Credits:
Thanks go to all open source bot authors and contributors to the RoboWiki.

Retreat is open source and released under the terms of the RoboWiki Public Code License (RWPCL) - Version 1.1
see license here: http://robowiki.net/wiki/RWPCL
*/

package sheldor.nano;

import robocode.*;
import robocode.util.Utils;

public class Retreat extends AdvancedRobot
{
	//En garde!
	public void run()
	{
		//Keep the radar pointed at its target while the bot turns.
		setAdjustRadarForGunTurn(true);
		
		//Start spinning radar.
		while(true)
		{
			turnRadarLeftRadians(1);
		}
	}
	
	public void onScannedRobot(ScannedRobotEvent e)
	{
		//Local variables.
		double absoluteBearing;
		double bearing;
				
		//Turn the gun as far away from the enemy as possible.
		setTurnGunRightRadians(Utils.normalRelativeAngle(
		 (absoluteBearing = (bearing = e.getBearingRadians()) + getHeadingRadians()) - Math.PI - getGunHeadingRadians()));
		 
		//Fire maximum power.
		setFire(3);
		
		//Reverse ramming movement.
		setTurnRightRadians(Math.tan(bearing -= Math.PI));
		setAhead(Math.cos(bearing) * 100);
		
		//Wide lock radar.
		setTurnRadarRightRadians(2.0 * Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians()));
	}
}											