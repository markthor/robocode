/*******************************************************************************
 * GuessFactor targeting.  Just a test platform.                               *
 ******************************************************************************/
package NDH;
import robocode.*;
import robocode.util.Utils;
import java.util.Vector;

public class GuessFactor extends AdvancedRobot {
   static final int segs = 25; // number of segments for GF
   static int[][][][] stats = new int[37][3][3][segs];
   Vector<WaveBullet> waves = new Vector<WaveBullet>();
   int dir = 1; // corresponds to clockwise movement of other bot
   double lastBearing = 0;
   double lastlastBearing = 0;
   double lastEnergy = 100;
   
   public void run() {
      setAdjustGunForRobotTurn(true);
      setAdjustRadarForGunTurn(true);
      turnRadarLeft(Double.POSITIVE_INFINITY);
      while(true)
         scan();
   }
   
   public void onScannedRobot(ScannedRobotEvent e) {
      narrowLock(e);
      double power = guessFactor(e);
      fire(power);
      dodge(e);
   }
   
   private double guessFactor(ScannedRobotEvent e) {
      double eBearing = getHeadingRadians() + e.getBearingRadians();
      double dist = e.getDistance();
      double eX = getX() + dist * Math.sin(eBearing);
      double eY = getY() + dist * Math.cos(eBearing);
      double eV = e.getVelocity();
      double power = Math.min(getEnergy(), 3);
      
      for (int i = 0; i < waves.size(); i++){
         WaveBullet wb = waves.get(i);
         if (wb.checkHit(eX, eY, getTime())) {
            waves.remove(wb);
            i--;
         }
      }
      
      if (eV != 0) {
         if (Math.sin(e.getHeadingRadians() - eBearing) * eV < 0)
            dir  = -1;
         else
            dir = 1;
      }
      
      int acc = accel(eBearing - lastBearing, lastBearing - lastlastBearing);
      int [] currentStats = getStats(e);
      WaveBullet newWB = new WaveBullet(getX(), getY(), eBearing, power, dir,
                                        getTime(), currentStats);
      waves.addElement(newWB);
      
      int bestGF = 15;
      for (int i = 0; i < segs; i++) {
         if ((currentStats[bestGF] < currentStats[i]))
            bestGF = i;
      }
      
      double correction = newWB.indextoCorrection(bestGF);
      setTurnGunRightRadians(Utils.normalRelativeAngle(eBearing - 
                                                       getGunHeadingRadians() +
                                                       correction));
      return power;
   }
   
   private void dodge(ScannedRobotEvent e) {
      double dEnergy = lastEnergy - e.getEnergy();
      if ((dEnergy <= 3) && (dEnergy >= .1)) {
         double eBearing = e.getBearing();
         double turn1 = 90 - eBearing;
         double turn2 = 270 - eBearing;
         double dist = (Math.random() * 100) + 300;
         
         setTurnRight(Math.min(turn1, turn2));
         if (Math.random() < .5)
            setAhead(dist);
         else
            setBack(dist);
      }
      lastEnergy = e.getEnergy();
   }   
   
   private int[] getStats(ScannedRobotEvent e) {
      double eBearing = getHeadingRadians() + e.getBearingRadians();
      int acc = accel(eBearing - lastBearing, lastBearing - lastlastBearing);
      lastlastBearing = lastBearing;
      lastBearing = eBearing;
      return stats[(int) e.getHeading() / 10]
         [acc]
         [nearWall(e)];
   }
   
   private int nearWall(ScannedRobotEvent e) {
      // is the robot near a wall (1) a corner (0) or in the middle (2)?
      double NEAR = 36;
      double dist = e.getDistance();
      double eBearing = e.getBearing();
      double eX = getX() + dist * Math.sin(eBearing);
      double eY = getY() + dist * Math.cos(eBearing);
      double width = getBattleFieldWidth();
      double height = getBattleFieldHeight();
      
      if ((width - eX <= NEAR) || (eX <= NEAR)) {
         if((height - eY <= NEAR) || (eY <= NEAR))
            return 0;
         else
            return 1;
      }
      return 2;
   }
   
   private int accel(double deltaBearing, double oldDeltaBearing) {
      // is the robot accelerating (2)?, deccel (0)? or neither (1)?
      double delta = Math.abs(deltaBearing) - Math.abs(oldDeltaBearing);
      if (delta < 1)
         return 0;
      else if (delta > 1)
         return 2;
      else
         return 1;
   }
   
   private double narrowLock(ScannedRobotEvent e) {
      double radarTurn = getHeading() + e.getBearing() - getRadarHeading();
      setTurnRadarRight(Utils.normalRelativeAngleDegrees(radarTurn));
      return radarTurn;
   }
   
//========================================
   private class WaveBullet {
      private double startX;
      private double startY;
      private double startBearing;
      private double power;
      private long fireTime;
      private int direction;
      private int[] returnSegment;
      
      public WaveBullet(double x, double y, double bearing, double power,
                        int direction, long time, int[] segment) {
         this.startX = x;
         this.startY = y;
         this.startBearing = bearing;
         this.power = power;
         this.direction = direction;
         this.fireTime = time;
         this.returnSegment = segment;
      }
      
      private double maxEscapeAngle() {
         return Math.asin(8 / bulletSpeed());
      }
      
      private double bulletSpeed() {
         return robocode.Rules.getBulletSpeed(this.power);
      }
      
      public double indextoCorrection(double index) {
         double GF = 2 * index / (segs - 1) - 1;
         return direction * this.maxEscapeAngle() * GF;
      }
      
      public boolean checkHit(double eX, double eY, long time) {
         double dist = Math.sqrt((this.startX - eX) * (this.startX - eX) +
                                 (this.startY - eY) * (this.startY - eY));
         double travelDist = (time - this.fireTime) * bulletSpeed();
         if (dist <= travelDist) {
            double currentBearing = Math.atan2(eX - startX, eY - startY);
            double error = Utils.normalRelativeAngle(currentBearing - 
                                                     startBearing);
            double GF;
            if (error / maxEscapeAngle() > 1)
               GF = direction;
            else if (error / maxEscapeAngle() < -1)
               GF = -direction;
            else
               GF = error / maxEscapeAngle() * direction;
            int index = (int) Math.round((segs - 1) /2 * (GF + 1));
            returnSegment[index]++;
            
            return true;
         }
         return false;
      }
   }
}

