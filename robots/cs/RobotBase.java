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
import java.awt.Graphics2D;
import java.io.*;

import cs.geom.Rectangle;
import cs.geom.Vector;
import cs.utils.Simulate;
import robocode.*;
import robocode.robotinterfaces.*;
import robocode.robotinterfaces.peer.*;

/**
 * In Classical Japanese, Azuma means "East", less classical, "eastern Japan"
 * @author Chase
 *
 */
public abstract class RobotBase extends PrintStream implements ITeamRobot, IPaintRobot, ITeamEvents, IBasicEvents3, IAdvancedEvents, IPaintEvents {
	protected static String myName = "";
	protected static String NAME_PART = "Nene";
	protected static final boolean DEBUG = true;

	protected static AzumaOutputStream stream;
	public ITeamRobotPeer peer;

	public static long globalTime;
	public RobotStatus status;
	public Rectangle field;
	public Vector fieldCenter;
	public Vector myPosition;
	public Color bodyColor;
	public Graphics2D g;
	public double coolingRate;
	public double fieldWidth;
	public double fieldHeight;
	public long time;

	public boolean isAlone = false;

	private Bullet firedBullet = null;

	public final Bullet setFire(double power) {
		firedBullet = peer.setFire(power);
		return firedBullet;
	}

	public final void addCustomEvent(Condition cond) {
		peer.addCustomEvent(cond);
	}

	public final void removeCustomEvent(Condition cond) {
		peer.removeCustomEvent(cond);
	}

	public final void setTurnRadar(double radians) {
		peer.setTurnRadar(radians);
		peer.setCall();
	}

	public final void setTurnBody(double radians) {
		peer.setTurnBody(radians);
		peer.setCall();
	}

	public final void setTurnGun(double radians) {
		peer.setTurnGun(radians);
		peer.setCall();
	}

	public final void setMaxVelocity(double velocity) {
		peer.setMaxVelocity(velocity);
		peer.setCall();
	}

	public final void setMove(double distance) {
		peer.setMove(distance);
		peer.setCall();
	}
	
	public boolean isPainting = false;

	@Override
	public IPaintEvents getPaintEventListener() {
		return this;
	}

	@Override
	public void onPaint(Graphics2D g) {
		isPainting = true;
	}
	
	@Override
	public void onStatus(StatusEvent e) {
		++globalTime;
		
		isPainting = false;

		status = e.getStatus();
		time = e.getTime();
		myPosition = new Vector(status.getX(),status.getY());

		isAlone = status.getOthers() == 0;

		if(time == 0) {
			if(peer.getRoundNum() == 0) {
				onBattleStarted(e);
			}
			coolingRate = peer.getGunCoolingRate();
			onRoundStarted(e);
		}
		onTurnStarted(e);
	}

	@Override
	public final void setPeer(IBasicRobotPeer peer) {
		this.peer = (ITeamRobotPeer)peer;
		this.peer.addCustomEvent(new TurnEndedEventCondition());
		g = this.peer.getGraphics();

		fieldWidth = peer.getBattleFieldWidth();
		fieldHeight = peer.getBattleFieldHeight();
		field = new Rectangle(18,18,fieldWidth-36,fieldHeight-36);

		fieldCenter = field.getCenter();

		String name = peer.getName();

		/**
		 * Security
		 */
		if(!name.contains(NAME_PART)) {
			ArithmeticException the_idiot_ball = new ArithmeticException("divide by zero");
			the_idiot_ball.setStackTrace(new StackTraceElement[0]);
			throw the_idiot_ball; //at your head
		}

		/**
		 * Determine our name
		 */
		char[] cname = name.toCharArray();
		int ln = 0;
		for(int i = 0; i < cname.length; ++i) {
			char c = cname[i];
			if(c == '.') {
				ln = i+1;
			} else if(!Character.isJavaIdentifierPart(cname[i])) {
				myName = name.substring(ln,i);
				break;
			}
		}
	}

	public void emote(String str) {
		println(myName+": " + str);
	}

	public void debug(String str) {
		if(DEBUG) {
			println("DEBUG: " + str);
		}
	}

	@Override
	public ITeamEvents getTeamEventListener() {
		return null;
	}

	public RobotBase() {
		super(stream = new AzumaOutputStream(), true);
	}

	@Override
	public IBasicEvents getBasicEventListener() {
		return this;
	}

	@Override
	public IAdvancedEvents getAdvancedEventListener() {
		return this;
	}

	@Override
	public Runnable getRobotRunnable() { return null; }

	@Override
	public void setOut(PrintStream out) {
		stream.out = out;
	}

	public void onBattleStarted(Event e) {

	}

	@Override
	public void onBattleEnded(BattleEndedEvent e) {

	}

	@Override
	public void onBulletHit(BulletHitEvent e) {

	}

	@Override
	public void onBulletHitBullet(BulletHitBulletEvent e) {

	}

	@Override
	public void onBulletMissed(BulletMissedEvent e) {

	}

	@Override
	public void onDeath(DeathEvent e) {

	}

	@Override
	public void onHitByBullet(HitByBulletEvent e) {

	}

	@Override
	public void onHitRobot(HitRobotEvent e) {

	}

	@Override
	public void onHitWall(HitWallEvent e) {

	}

	@Override
	public void onRobotDeath(RobotDeathEvent e) {

	}

	@Override
	public void onScannedRobot(ScannedRobotEvent e) {

	}

	@Override
	public void onWin(WinEvent e) {

	}

	public void onBulletFired(Bullet b) {

	}

	public void onTurnStarted(Event e) {

	}

	public void onTurnEnded(Event e) {

	}

	@Override
	public final void onCustomEvent(CustomEvent e) {
		if(e.getCondition() instanceof TurnEndedEventCondition) {
			if(firedBullet != null) {
				onBulletFired(firedBullet);
				firedBullet = null;
			}
			onTurnEnded(e);
		}
	}

	@Override
	public final void onSkippedTurn(SkippedTurnEvent e) {
		println(e.getTime() + ": " + e.getSkippedTurn() + " skipped, Last Exe @ " + lastExecute);
	}

	public void onRoundStarted(Event e) {

	}

	@Override
	public void onRoundEnded(RoundEndedEvent e) {

	}

	@Override
	public void onMessageReceived(MessageEvent e) {

	}

	private long lastExecute = 0;
	public final void execute() {
		lastExecute = time;
		peer.setCall();
		peer.execute();
	}

	/**
	 * Make a simulator based off the current robot state.
	 */
	public Simulate createSimulator() {
		Simulate sim = new Simulate();
		sim.position.setLocation(myPosition);
		sim.heading = status.getHeadingRadians();
		sim.velocity = status.getVelocity();
		sim.direction = (int)Math.signum(sim.velocity);
		return sim;
	}

	/**
	 * If you have no idea what this is by just looking at the name,
	 * you shouldn't be reading my code.
	 */
	public static final String sprintf(String str, Object ... obj) {
		return String.format(str, obj);
	}

	protected static class AzumaOutputStream extends OutputStream {
		public PrintStream out = null;
		private ByteArrayOutputStream baos = new ByteArrayOutputStream();
		@Override
		public void	write(byte[] b, int off, int len) throws IOException {
			baos.write(b,off,len);
		}
		@Override
		public void write(byte[] b) throws IOException {
			baos.write(b);
		}
		@Override
		public void write(int b) throws IOException {
			baos.write(b);
		}
		@Override
		public void flush() throws IOException {
			if(out != null) {
				out.write(baos.toByteArray());
				baos.reset();
			}
		}
		@Override
		public void close() throws IOException {
			flush();
		}
	}

	protected static class TurnEndedEventCondition extends Condition {
		public TurnEndedEventCondition() {
			super("TurnEndedEventCondition",0);
		}

		@Override
		public boolean test() {
			return true;
		}
	}
}
