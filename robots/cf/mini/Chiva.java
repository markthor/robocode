package cf.mini;

import robocode.*;
import robocode.util.*;
import java.awt.Color;
import java.awt.geom.*;
import java.util.*;

public class Chiva extends AdvancedRobot {
	
/******************* CHIVA ************************\

CHIVA by deathcon

CODESIZE : 1481 

\**************************************************/
	
private static Point2D.Double robotPosition = null;
private static Point2D.Double enemyPosition = null;
private static Rectangle2D.Double BF = null;
private static final int BORDER = 18;

private static final int GF_ZERO = 15;
private static final int GF_ONE = 2*GF_ZERO;
private static final int FACTORS = GF_ONE+1;

private static long lastDccl = 0;

private static int gunStats[][][][][][] = new int[8][9][3][3][6][FACTORS]; // dist,latV,accl,wall,factors

private static int hitStats[][][] = new int[4][5][FACTORS]; // dist,speed,factors

private static Vector eWaves = null;
private static Vector rWaves = null;

private static long time;

private static double enemyEnergy;
private static double enemyAbsoluteBearing;
private static int enemyClockwiseFactor = 1;
private static double enemyLatV;
private static double enemyAbsV;
private static double enemyDistance;

private static double robotHeading;
private static int robotLastVI;
private static int robotClockwiseFactor=1;

private static final double MAX_BULLET_POWER = 3D;
private static final double MOVE_STEP = 170;
	
	public void run(){
		setAdjustRadarForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);
		setColors(Color.white,Color.black,Color.red);
		BF = new Rectangle2D.Double(BORDER,BORDER,getBattleFieldWidth()-BORDER*2,getBattleFieldHeight()-BORDER*2);
		eWaves = new Vector();
		rWaves = new Vector();
		do{
			turnRadarRightRadians(1);
		}while(true);
	}
	
	public void onScannedRobot(ScannedRobotEvent e){
		time = getTime();
		double enemyEnergyDrop,bulletPower,bulletSpeed,accl,moveHeading;
		if( (enemyEnergyDrop=enemyEnergy-(enemyEnergy=e.getEnergy())) > 0 && enemyEnergyDrop <= MAX_BULLET_POWER){
			Wave ew;
			eWaves.add(ew = new Wave());
			ew.firedTime = time-2;
			ew.gunPosition = enemyPosition;
			ew.directHeading = enemyAbsoluteBearing + Math.PI;
			ew.maxBearChange = Math.asin( 8D / ( (ew.bulletSpeed = 20-3*enemyEnergyDrop) ) )*robotClockwiseFactor;
			ew.bin = hitStats[Math.min(3,(int)(enemyDistance/200))][robotLastVI];
		}
		enemyPosition=  calcPoint( (robotPosition = new Point2D.Double(getX(),getY())) , 
		                           (enemyDistance = e.getDistance()) ,
		                           (enemyAbsoluteBearing = (robotHeading = getHeadingRadians()) + e.getBearingRadians()) );
		
		double robotLatV= getVelocity()*Math.sin(robotHeading-enemyAbsoluteBearing+Math.PI);
		if( robotLatV != 0)
			robotClockwiseFactor = robotLatV < 0 ? -1 : 1;
			
		robotLastVI= (int)Math.abs(robotLatV/2) ;
			// rWaves
		for(Iterator i1 = rWaves.iterator(); i1.hasNext() ; ){
			Wave w = (Wave) i1.next();
			if( (time-w.firedTime)*w.bulletSpeed > w.gunPosition.distance(enemyPosition) - BORDER){
				w.bin[w.getHitIndex(enemyPosition)]++;
				i1.remove();
			}
		}
			// movement
		double scores[]=new double[2]; // 0 for antiClockwise 1 for clockwise
		for(Iterator i=eWaves.iterator();i.hasNext();){
			Wave ew=(Wave)i.next();
			double timeToFly = (ew.gunPosition.distance(robotPosition)/ew.bulletSpeed)-(time-ew.firedTime) ;
			if( timeToFly > 0 ){
				int meIndex=ew.getHitIndex(robotPosition);
				int worstIndex=GF_ZERO;
				for(int j=0;j<FACTORS;j++){
					if( ew.bin[j] > ew.bin[worstIndex] ){
						worstIndex = j;
					}
				}
				int thisMoveDir = 1; // clockwise
				if( ((worstIndex - meIndex)*ew.maxBearChange)<0 )
					thisMoveDir = 0; // anticlockwise
				scores[thisMoveDir] += 1/Math.pow(timeToFly,4);
			}
		}
		int circleDirection = 1;
		if(scores[0]>scores[1])
			circleDirection = -1;
			// find point that is in
		Point2D.Double p=null;
		moveHeading=3.18;
		do{}while(!BF.contains(p=calcPoint(robotPosition,MOVE_STEP,enemyAbsoluteBearing+(moveHeading-=.4)*circleDirection)));
			// move
		moveHeading = Utils.normalRelativeAngle(calcAngle(robotPosition, p) - getHeadingRadians());
		setAhead(Math.cos(moveHeading)*100);
		setTurnRightRadians(Math.tan(moveHeading));
			// data
		accl = -Math.abs(enemyAbsV) + Math.abs( enemyAbsV = e.getVelocity() );
		if(accl<0)
			lastDccl=0;
		enemyLatV = enemyAbsV * Math.sin( e.getHeadingRadians() - enemyAbsoluteBearing );
		if(enemyLatV != 0)
			enemyClockwiseFactor = enemyLatV<0 ? -1 : 1;
			// gun
		bulletPower = 2.1;
		if( enemyDistance < 160 )
			bulletPower = MAX_BULLET_POWER;
		Wave rw;
		rWaves.add(rw = new Wave());
		rw.firedTime = time;
		rw.gunPosition = (Point2D.Double)robotPosition.clone();
		rw.directHeading = enemyAbsoluteBearing;
		rw.bulletSpeed = 20-3*bulletPower;
		rw.maxBearChange = Math.asin( 8D / rw.bulletSpeed ) * enemyClockwiseFactor;
		rw.bin = gunStats[(int)Math.min(7,enemyDistance/140)]
						 [(int)Math.abs(enemyLatV)]
						 [accl<0 ? 0 : accl>0 ? 2 : 1]
						 [BF.contains(calcPoint(enemyPosition,enemyDistance,enemyAbsoluteBearing+rw.maxBearChange)) ? 2 : 
						  BF.contains(calcPoint(enemyPosition,enemyDistance,enemyAbsoluteBearing+rw.maxBearChange*.5)) ? 1 : 0]
						 [(int)Math.min(lastDccl++/13,5)];
			// aim				
		int bestIndex=GF_ZERO;
		for(int i=0;i<FACTORS;i++)
			if(rw.bin[i] > rw.bin[bestIndex])
				bestIndex = i;
		setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing-getGunHeadingRadians()+rw.maxBearChange*((bestIndex/(double)GF_ZERO)-1) ));
			
		if( getEnergy() > bulletPower ){
			// shoot
			setFire(bulletPower);
		}	
			// radar
		setTurnRadarRightRadians(Utils.normalRelativeAngle( enemyAbsoluteBearing - getRadarHeadingRadians() )*1.4);
	}
	
// ################### helpers ##########################

	private static final Point2D.Double calcPoint(java.awt.geom.Point2D.Double p, double dist, double ang) {
    	return new Point2D.Double(p.x + dist * Math.sin(ang), p.y + dist * Math.cos(ang));
	}
	
	private static final double calcAngle(Point2D.Double p1, Point2D.Double p2) {
		return Math.atan2(p2.x - p1.x, p2.y - p1.y);
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent e){
		onSeeBullet(e.getHitBullet());	
	}
	
	public void onHitByBullet(HitByBulletEvent e){
		onSeeBullet(e.getBullet());	
	}
	
	private void onSeeBullet(Bullet b){
		Wave bestWave = null;
		Point2D.Double hitPoint=new Point2D.Double(b.getX(),b.getY());
		double bestDistDiff=40;
		for(Iterator i=eWaves.iterator();i.hasNext();){
			Wave tw=(Wave)i.next();
			double thisDistDiff=Math.abs(tw.bulletSpeed*(time-tw.firedTime)-tw.gunPosition.distance(hitPoint));
			if(Math.abs(tw.bulletSpeed-b.getVelocity())<.01 && thisDistDiff<bestDistDiff){
				bestDistDiff = thisDistDiff;
				bestWave = tw;
			}
		}
		if(bestWave != null){
			bestWave.bin[bestWave.getHitIndex(hitPoint)]++;
			eWaves.removeElement(bestWave);
		}
	}
	
// ##################### wave ###########################

	class Wave {
		Point2D.Double gunPosition = null;
		long firedTime;
		double directHeading;
		double bulletSpeed;
		double maxBearChange;
		int bin [];
		
		int getHitIndex(Point2D.Double p){
			return Math.min(GF_ONE,Math.max(0, (int)Math.round(((Utils.normalRelativeAngle( calcAngle(gunPosition,p)-directHeading ) / maxBearChange)+1)*GF_ZERO) ));
		}	
		
	}
				
}