/**
 * Copyright (c) 2011 Chase
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *    1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 
 *    2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 
 *    3. This notice may not be removed or altered from any source
 *    distribution.
 */

package cs;

import java.awt.Color;
import cs.utils.Tools;
import robocode.BattleEndedEvent;
import robocode.Event;
import robocode.WinEvent;

/**
 * <pre>
 *    Nene Robot
 * Diakama Weapon
 *  Kakeru Movement
 * Chikaku Radar
 *   Azuma RunlessRobot
 * </pre>
 * 
 * @author Chase
 */
public final class Nene extends GunBase {
	@Override
	public void onBattleStarted(Event e) {
		super.onBattleStarted(e);
		//TODO Load data
	}

	@Override
	public void onBattleEnded(BattleEndedEvent e) {
		super.onBattleEnded(e);
		//TODO Save data
	}

	@Override
	public void onRoundStarted(Event e) {
		super.onRoundStarted(e);

		peer.setAdjustRadarForGunTurn(true);
		peer.setAdjustGunForBodyTurn(true);

		float hue = 0.1f;
		peer.setBodyColor(bodyColor = Color.getHSBColor(hue, 0.5f, 1.0f));
		peer.setGunColor(Color.getHSBColor(hue, 0.7f, 0.95f));
		peer.setRadarColor(Color.getHSBColor(hue, 0.9f, 0.9f));
		peer.setScanColor(Color.getHSBColor(hue, 0.2f, 1.0f));
		peer.setBulletColor(Color.WHITE);

		emote("pawa-appu! go! pyuuuu~");
	}

	@Override
	public void onTurnEnded(Event e) {
		super.onTurnEnded(e);
		execute();
	}

	@Override
	public void onWin(WinEvent e) {
		super.onWin(e);
		emote("YOSH! Rettsu rokku!");
	}


	private int victory = 0;
	/**
	 * Called from the movement, I just keep it here so it is out of the way!
	 */
	protected void doVictoryDance() {
		/**
		 * Victory Dance
		 */
		final String message = "VICTORY";
		Color c = Tools.getColorForLetter(message.charAt(victory++));

		peer.setBodyColor(c);
		peer.setGunColor(c);
		peer.setRadarColor(c);
		peer.setScanColor(c);

		setMove(0);
		setMaxVelocity(0);

		setTurnRadar(100);
		setTurnGun(100);
		setTurnBody(100);
		if(victory >= message.length())
			victory = 0;

		g.setColor(c);
		for(int i=0;i<10;++i) {
			float x = (float)(Math.random()*fieldWidth);
			float y = (float)(Math.random()*fieldHeight);
			g.drawString("VICTORY", x, y);
		}
	}


	public void save() {
		//Movement
		//Save states based on most used and last used indexes
	}

	/**
	 * Binary Floating Point Packing; Lossy<br>
	 * (Will Be) Used for data saving.
	 */
	public static final long packDoubleToFewerBits(int maxBits, double maxValue, double value, boolean hasNegative) {
		int total_values = (1 << maxBits) - 1;

		//down side, no precise value for zero when using negatives
		//might fix this by using special value (and reducing other values by 1)

		if(hasNegative) {
			value += maxValue;
			maxValue *= 2.0;
		}

		double divisor = maxValue / total_values;

		long i = (long)Math.rint(value / divisor);

		return i;
	}

	/**
	 * Binary Floating Point Unpacking; Lossy<br>
	 * (Will Be) Used for data loading.
	 */
	public static final double unpackDoubleFromFewerBits(int maxBits, double maxValue, long value, boolean hasNegative) {
		int total_values = (1 << maxBits) - 1;

		double work = value/(double)total_values;

		if(hasNegative) {
			work *= maxValue * 2.0;
			work -= maxValue;
		} else {
			work *= maxValue;
		}

		return work;
	}

	public static void main(String[] args) {
		double error = 0;
		for(int i=0;i<1000;++i) {
			double test = Math.random();
			double repack = unpackDoubleFromFewerBits(8,1.0,packDoubleToFewerBits(8,1.0,test,false),false);

			System.out.println("Double: " + test);
			System.out.println("Packed: " + repack);

			double diff = Math.abs(test - repack);
			System.out.println("Difference: " + diff);

			error += diff;
		}

		error /= 1000;
		System.out.println("Average Difference: " + error);
	}
}