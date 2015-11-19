package kjc;
import robocode.*;
import java.awt.Color;

/**
 * MailMan - a robot by Kyle
 */
public class MailManX extends TeamRobot
{
	/**
	 * run: MailMan's default behavior
	 */
	public void run() {
		// After trying out your robot, try uncommenting the import at the top,
		// and the next line:
		setColors(Color.white,Color.blue,Color.white);
		setTurnGunRight(Double.POSITIVE_INFINITY);
		while(true) {
			// Replace the next 4 lines with any behavior you would like
			setAhead(100);
			turnRight(90);
		}
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		if (!isTeammate(e.getName())) {
			setTurnGunLeft(getGunTurnRemaining());
			setFire(1);
		}
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
	}
	
}
