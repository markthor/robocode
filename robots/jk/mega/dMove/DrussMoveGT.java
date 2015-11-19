package jk.mega.dMove;

import jk.mega.dGun.*;
import jk.mega.*;
import jk.precise.util.*;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;     // for Point2D's
import java.lang.*;         // for Double and Integer objects
import java.util.*; // for collection of waves
import java.awt.Color;
//import ags.utils.*;
import java.io.*;

public class DrussMoveGT{// extends AdvancedRobot {

   static final boolean TC = false;  
   static final boolean VCS = true;
   static  boolean MC2K7 = false;
	
   static ArrayList statBuffers = new ArrayList();
   static ArrayList flattenerBuffers = new ArrayList();
   static ArrayList ABSBuffers = new ArrayList();
   static ArrayList flattenerTickBuffers = new ArrayList();
   static KDTree<Scan> surfBufferTree = 
   new KDTree.Manhattan<Scan>(new Scan().location().length);
   static KDTree<Scan> flatBufferTree = 
   new KDTree.Manhattan<Scan>(new Scan().location().length);
   static KDTree<Scan> ABSBufferTree = 
   new KDTree.Manhattan<Scan>(new Scan().location().length);
   static KDTree<Scan> tickBufferTree = 
   new KDTree.Manhattan<Scan>(new Scan().location().length);
   
   // static ArrayList<Scan> visitScans = new ArrayList<Scan>();
   // static ArrayList<Scan> hitScans = new ArrayList<Scan>();
   
   public Point2D.Double _myLocation = new Point2D.Double();     // our bot's location
   public Point2D.Double _enemyLocation =  new Point2D.Double(400,300);  // enemy bot's location
   public Point2D.Double nextEnemyLocation;
   public int time_since_dirchange;
   public double direction = 1;

   public static ArrayList _distances;
   public static ArrayList<Double> _lateralVelocitys;
   public static ArrayList _advancingVelocitys;
   public ArrayList<EnemyWave> _enemyWaves;
   public ArrayList<BulletTracker> myBullets;
   public static ArrayList _flattenerTickWaves;
   public static ArrayList _surfDirections;
   public static ArrayList _surfAbsBearings;
	
   private static double BULLET_POWER = 1.9;

   private static double lateralDirection;

 // We must keep track of the enemy's energy level to detect EnergyDrop,
 // indicating a bullet is fired
   public double _oppEnergy = 100.0;

 // This is a rectangle that represents an 800x600 battle field,
 // used for a simple, iterative WallSmoothing method (by Kawigi).
 // If you're not familiar with WallSmoothing, the wall stick indicates
 // the amount of space we try to always have on either end of the tank
 // (extending straight out the front or back) before touching a wall.
   public static Rectangle2D.Double _fieldRect
     = new java.awt.geom.Rectangle2D.Double(18, 18, 764, 564);
   public  ArrayList goToTargets;
   public  Point2D.Double lastGoToPoint;
   public static double WALL_STICK = 160;
   public long lastScanTime = 0;
   public static double totalEnemyDamage;
   public static double weightedEnemyFirerate, weightedEnemyHitrate;
   public static double totalMyDamage;
   public boolean surfStatsChanged;
   public double enemyGunHeat;
   public double imaginaryGunHeat;
   public static double bestDistance = 400;
   public static boolean flattenerEnabled = false, flattenerStarted = false;
   public ScannedRobotEvent lastScan;
   
   static KDTree<Float> bulletPowerTree = new KDTree.Euclidean<Float>(3);
	
   long moveTime;
   long gunTime;

   EnemyWave mainWave;
   EnemyWave secondWave;
   EnemyWave thirdWave;
	
   boolean painting = false;
   ArrayList firstPointsPainting;
   ArrayList nextPointsPainting;
   static int scanCounter;

   EnemyMoves eMove;
   AdvancedRobot bot;

   boolean seen;

   public DrussMoveGT(AdvancedRobot bot){
      this.bot = bot;
   
      if(bot.getRoundNum() != 0){
         System.out.println("Enemy damage: " + totalEnemyDamage);
         System.out.println("My damage:    " + totalMyDamage);
         System.out.println("Accumulated, weighted enemy hitrate % : " + Math.round(1000.0*weightedEnemyHitrate/weightedEnemyFirerate)/10.0 + "%");
         System.out.println("Flattener enabled: " + flattenerEnabled);
      }
      
      if(!TC & bot.getRoundNum() == 0){
         if(VCS){
            //loadBufferManager.StatBuffers();
            statBuffers = BufferManager.getStatBuffers();
            flattenerBuffers = BufferManager.getFlattenerBuffers();
            ABSBuffers = BufferManager.getABSBuffers();
            flattenerTickBuffers = BufferManager.getFlattenerTickBuffers();
            
            
            //preloaded HOT hit
            BufferManager.SingleBuffer sb = new BufferManager.SingleBuffer();
            sb.bins = new int[7];
            BufferManager.StatBuffer stb = ((BufferManager.StatBuffer)statBuffers.get(0));
            stb.stats[0][0][0][0][0][0][0][0][0] = sb;
            sb.bins[0] = EnemyWave.MIDDLE_BIN;
            sb.binsUsed = 1;
            sb.weight = stb._weight;
            sb.rollingDepth = stb.rollingDepth;
         }
         else{
            Scan s = new Scan();
            s.index = EnemyWave.MIDDLE_BIN;
            s.bft = -1;
            surfBufferTree.addPoint(s.location(),s);
         }
         
         _fieldRect = new java.awt.geom.Rectangle2D.Double(18, 18, bot.getBattleFieldWidth() - 36, bot.getBattleFieldHeight() - 36); 
      }
      eMove = new EnemyMoves(bot);
   	
      lateralDirection = 1;
      
      _lateralVelocitys = new ArrayList();
      _advancingVelocitys = new ArrayList();
      _enemyWaves = new ArrayList();
      myBullets = new ArrayList<BulletTracker>();
      _flattenerTickWaves = new ArrayList();
      _surfDirections = new ArrayList();
      _surfAbsBearings = new ArrayList();
      _distances = new ArrayList();
      
   }
   public void onTick(boolean move){
   
      if((move && seen && lastScanTime + 1 < bot.getTime() ) || bot.getOthers() == 0){
         _time = bot.getTime();
         _myLocation =// project(
            new Point2D.Double(bot.getX(), bot.getY())
            // ,bot.getHeadingRadians(),bot.getVelocity())
            ;
      
         enemyGunHeat = Math.max(0.0, enemyGunHeat - bot.getGunCoolingRate());
         imaginaryGunHeat = Math.max(enemyGunHeat,imaginaryGunHeat - bot.getGunCoolingRate());
                  
         updateWaves();
      
         doSurfing(); 
                  
      }
   
   }
     
   
   
   long _time;
   public long getTime(){
      return _time;
   }

   public void onScannedRobot(ScannedRobotEvent e) {
   
      seen = true;
      _time = bot.getTime();    
      long stime = -System.nanoTime();
      
      eMove.onScannedRobot(e);
      _myLocation = new Point2D.Double(bot.getX(), bot.getY());
      lastScan = e;
      lastScanTime = getTime();
      if(!TC){
         if(_surfDirections.size() == 0)
            enemyGunHeat = imaginaryGunHeat = bot.getGunHeat();
         
         double lateralVelocity = bot.getVelocity()*FastTrig.sin(e.getBearingRadians());
         double advancingVelocity = -bot.getVelocity()*FastTrig.cos(e.getBearingRadians());
         double absBearing = e.getBearingRadians() + bot.getHeadingRadians();
         if(lateralVelocity > 0)
            lateralDirection = 1;
         else if(lateralVelocity < 0)
            lateralDirection = -1;
         
         
         _surfDirections.add(0,new Integer((int)lateralDirection));
         _surfAbsBearings.add(0, new Double(absBearing + Math.PI));
         _lateralVelocitys.add(0, new Double(Math.abs(lateralVelocity)));
         _advancingVelocitys.add(0, new Integer((int)Math.round(advancingVelocity)));
         _distances.add(0, new Double(e.getDistance()));
         
         enemyGunHeat = Math.max(0.0, enemyGunHeat - bot.getGunCoolingRate());
         imaginaryGunHeat = Math.max(enemyGunHeat,imaginaryGunHeat - bot.getGunCoolingRate());
         
         nextEnemyLocation = project(project(_myLocation,absBearing,e.getDistance()),e.getHeadingRadians(),e.getVelocity());  
         
         
         double bulletPower = _oppEnergy - e.getEnergy();
         addWave(bulletPower);
         addImaginaryWave();
         addFlattenerTickWave();
            //System.out.println(_flattenerTickWaves.size() + " flattener tick waves out there...");
            
         _oppEnergy = e.getEnergy();
         
         // update after EnemyWave detection, because that needs the previous
         // enemy location as the source of the wave
         _enemyLocation = project(_myLocation, absBearing, e.getDistance());
         if(!eMove.initialised())
            eMove.predict();
         Point2D.Double predictedEloc = eMove.get((int)getTime());
         if(predictedEloc.distance(_enemyLocation) > 0.1*e.getDistance() & bot.getRoundNum() > 1)
            surfStatsChanged = true;
         
         updateWaves();
         antiBulletShadows();
         double wHitPerc = weightedEnemyHitrate/weightedEnemyFirerate*100;
         if(( wHitPerc> 9 & bot.getRoundNum() > 1)
            |( wHitPerc > 8 & bot.getRoundNum() > 4) ){
            if(!flattenerEnabled)
               System.out.println("Flattener Enabled");
            flattenerEnabled = true;
            flattenerStarted = true;
         }
         else 
            if(bot.getRoundNum() < 15)
            {
               if(flattenerEnabled)
                  System.out.println("Flattener Disabled");
               flattenerEnabled = false;
            }
         
      
      }
      moveTime = stime + System.nanoTime();
      
      scanCounter++;
   }
   
   public DataToGun getGunInfo(){
   
   
      Point2D.Double finalMirrorPoint;
      double finalHeading;
      double finalVelocity;
      long finalTime;
      if(thirdWave != null && thirdWave.safestPoint != null && thirdWave.safestPoint.predictionStatus != null){
         finalMirrorPoint = thirdWave.safestPoint.predictionStatus.endPoint;
         finalHeading = thirdWave.safestPoint.predictionStatus.finalHeading;
         finalVelocity = thirdWave.safestPoint.predictionStatus.finalVelocity;
         finalTime = thirdWave.safestPoint.predictionStatus.time - getTime();
      }
      else if(secondWave != null && secondWave.safestPoint != null && secondWave.safestPoint.predictionStatus != null){
         finalMirrorPoint = secondWave.safestPoint.predictionStatus.endPoint;
         finalHeading = secondWave.safestPoint.predictionStatus.finalHeading;
         finalVelocity = secondWave.safestPoint.predictionStatus.finalVelocity;
         finalTime = secondWave.safestPoint.predictionStatus.time - getTime();
      }
      else if(mainWave != null && mainWave.safestPoint != null && mainWave.safestPoint.predictionStatus != null){
         finalMirrorPoint = mainWave.safestPoint.predictionStatus.endPoint;
         finalHeading = mainWave.safestPoint.predictionStatus.finalHeading;         
         finalVelocity = mainWave.safestPoint.predictionStatus.finalVelocity;
         finalTime = mainWave.safestPoint.predictionStatus.time - getTime();
      }
      else{
         finalMirrorPoint = _myLocation;
         finalHeading = bot.getHeadingRadians();
         finalVelocity = bot.getVelocity();  
         finalTime = 0;
      }
      DataToGun dtg = new DataToGun();
      dtg.futurePoint = finalMirrorPoint;
      dtg.futureHeading = finalHeading;
      dtg.futureVelocity = finalVelocity;
      dtg.enemyBP = predictBulletPower(bot.getEnergy(), _oppEnergy, ((Double)_distances.get(0)).doubleValue());
      dtg.timeInFuture = (int)finalTime;
      // gunTime = stime + System.nanoTime();
         
      
      return dtg;
   }
   public void logMyBullet(Bullet bullet){
      if(bullet != null){
         BulletTracker bt = new BulletTracker();
         bt.startLocation = _myLocation;
         bt.fireTime = getTime();
         bt.b = bullet;
         myBullets.add(bt);
         updateShadows(bt);
      }
   }
   
   
   public double predictBulletPower(double myEnergy, double enemyEnergy, double distance){
   
      List<KDTree.SearchResult<Float>> cl = bulletPowerTree.nearestNeighbours(
            new double[]{myEnergy/200, enemyEnergy/200, distance/1200},
            Math.min((int)Math.ceil(Math.sqrt(bulletPowerTree.size())),20));
            
      double[][] bullHist = new double[2][cl.size()];
      		
      Iterator<KDTree.SearchResult<Float>> it = cl.iterator();
      int i = 0;
      while(it.hasNext()){
         KDTree.SearchResult<Float> p = it.next();
         double weight = 1/(p.distance + 0.1);
         bullHist[0][i] = weight;
         bullHist[1][i] = p.payload;
         i++;
      }
      double maxScore = 0; 
      double maxPower = 2;
      for(int j = 0; j < i; j++){
         double score = 0;
         for(int k = 0; k < j; k++)
            score += bullHist[0][k]/(sqr(bullHist[1][j] - bullHist[1][k]) + 0.1);
         for(int k = j+1; k < i; k++)
            score += bullHist[0][k]/(sqr(bullHist[1][j] - bullHist[1][k]) + 0.1);
         score *= bullHist[0][j];
         
         if(score > maxScore){
            maxScore = score;
            maxPower = bullHist[1][j];
         }
      }  
   	
      return maxPower;
   
   }
   public void addImaginaryWave(){
      if(enemyGunHeat <= bot.getGunCoolingRate() & imaginaryGunHeat <= bot.getGunCoolingRate()){
      
      
         double bulletPower = predictBulletPower(bot.getEnergy(),_oppEnergy,((Double)_distances.get(0)).doubleValue());
         
         imaginaryGunHeat = 1 + bulletPower/5 + bot.getGunCoolingRate();//they only fire next tick, so add 1 gunCoolingRate
                  
         EnemyWave ew = new EnemyWave();
         ew.fireTime = getTime();
         ew.bulletVelocity = bulletVelocity(bulletPower);
         ew.distanceTraveled = -ew.bulletVelocity;
         ew.direction = ((Integer)_surfDirections.get(0)).intValue();
         ew.directAngle = ((Double)_surfAbsBearings.get(0)).doubleValue();
         ew.fireLocation = nextEnemyLocation; // next tick
         ew.imaginary = true;
         
         float lastLatVel = (float)_lateralVelocitys.get(0).doubleValue();
         float prevLatVel = (float)lastLatVel;
         try{prevLatVel = (float)_lateralVelocitys.get(1).doubleValue();}
         catch(Exception ex){}
         
         float accel = lastLatVel - prevLatVel;
         
         float distance = (float)((Double)_distances.get(0)).doubleValue();
             
         float advVel = (float)((Integer)_advancingVelocitys.get(0)).intValue();
         
         float BFT = (float)(distance/ew.bulletVelocity);
         	 
         float tsdirchange = 0;
         for(int i = 1; i < _surfDirections.size() - 2; i++)
            if(((Integer)_surfDirections.get(i-1)).intValue() == ((Integer)_surfDirections.get(i)).intValue())
               tsdirchange++;
            else 
               break;
         
         	 
         float tsvchange = 0;
         for(int i = 1; i < _lateralVelocitys.size() - 2; i++)
            if(_lateralVelocitys.get(i-1).doubleValue() <= _lateralVelocitys.get(i).doubleValue() + 0.4)
               tsvchange++;
            else 
               break;
               
      			      
         float dl10 = 0;
         for(int i = 0; i < Math.min(10, _lateralVelocitys.size() - 2); i++)
            dl10 += (float)(_lateralVelocitys.get(i).doubleValue()*((Integer)_surfDirections.get(i)).intValue());
         dl10 = Math.abs(dl10)*(10/8.0f);
            
         double MEA = ew.maxEscapeAngle();
         		
         float forwardWall = (float)(wallDistance(_enemyLocation,distance,ew.directAngle, ew.direction)/MEA);
         float reverseWall = (float)(wallDistance(_enemyLocation,distance,ew.directAngle, -ew.direction)/MEA);
         
         tsdirchange/=BFT;
         tsvchange/=BFT;
         
         if(VCS){
         
            ew.indexes =  BufferManager.getIndexes(
                  lastLatVel,
               	advVel,
                  BFT, 
                  tsdirchange,
                  accel,
                  tsvchange,
                  dl10,
                  forwardWall,
                  reverseWall 
                  );
         
            ew.allStats = BufferManager.getStats(
                  statBuffers,
                  ew.indexes
                  );
            if(flattenerStarted){
               ew.flattenerStats = BufferManager.getStats(
                  flattenerBuffers,
                  ew.indexes
                  );
               ew.ABSStats = BufferManager.getStats(
                  ABSBuffers,
                  ew.indexes);
               ew.flattenerTickStats = BufferManager.getStats(
                  flattenerTickBuffers,
                  ew.indexes
                  );     	
            		
            }
         }          
         else{
            Scan s = new Scan();
         
            s.latVel = limit(0,lastLatVel/8,1);
            s.advVel = limit(0,advVel/8,1);
            s.bft = limit(0,BFT/(1200/11.0f),1);
            s.forwardWall = limit(0,forwardWall,1);
            s.reverseWall = limit(0,reverseWall,1);
            s.lastVel = limit(0,prevLatVel/8,1);
            s.accel = (accel + 2)/3;
            s.timeSinceDecel = limit(0,tsvchange/BFT,1);
            s.timeSinceDirChange = limit(0,tsdirchange/BFT,1);
            s.distLast10 = limit(0,dl10/(100), 1);
            s.pointInTime = scanCounter;
            ew.scan = s;
         }
         
         // if(secondWave == null)
         surfStatsChanged = true;
         _enemyWaves.add(ew);
         updateShadows(ew);
         
      }
   }
   
   public void addWave(double bulletPower){
      if (enemyGunHeat == 0.0 & bulletPower < 3.01 & bulletPower > 0.099
         & _surfDirections.size() > 2
         ) {
         enemyGunHeat = 1 + bulletPower/5 - bot.getGunCoolingRate();//they fired last tick, so subtract 1 gunCoolingRate
         imaginaryGunHeat = enemyGunHeat;
      	
      	
         bulletPowerTree.addPoint(
            new double[]{bot.getEnergy()/200,
               (_oppEnergy + bulletPower)/200, 
               ((Double)_distances.get(2)).doubleValue()/1200},
            (float)bulletPower); 
      	
      	
         EnemyWave imaginaryWave = null;  
         for (int x = 0; x < _enemyWaves.size(); x++) {
            EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
            if(ew.imaginary & ew.fireTime == getTime() - 2){
               imaginaryWave = ew;
               _enemyWaves.remove(x);
               x--;
            }
         }
      	
         EnemyWave ew = new EnemyWave();
         ew.fireTime = getTime() - 2;
         ew.bulletVelocity = bulletVelocity(bulletPower);
         ew.distanceTraveled = ew.bulletVelocity;
         ew.direction = ((Integer)_surfDirections.get(2)).intValue();
         ew.directAngle = ((Double)_surfAbsBearings.get(2)).doubleValue();
         ew.fireLocation = (Point2D.Double)_enemyLocation.clone(); // last tick
      
         float lastLatVel = (float)_lateralVelocitys.get(2).doubleValue();
         float prevLatVel = (float)lastLatVel;
         try{prevLatVel = (float)_lateralVelocitys.get(3).doubleValue();}
         catch(Exception ex){}
         
         float accel = lastLatVel - prevLatVel;
         
         float distance = (float)((Double)_distances.get(2)).doubleValue();
             
         float advVel = (float)((Integer)_advancingVelocitys.get(2)).intValue();
         
         float BFT = (float)(distance/ew.bulletVelocity);
         	 
         float tsdirchange = 0;
         for(int i = 3; i < _surfDirections.size(); i++)
            if(((Integer)_surfDirections.get(i-1)).intValue() == ((Integer)_surfDirections.get(i)).intValue())
               tsdirchange++;
            else 
               break;
         
         	 
         float tsvchange = 0;
         for(int i = 3; i < _lateralVelocitys.size(); i++)
            if(_lateralVelocitys.get(i-1).doubleValue() <= _lateralVelocitys.get(i).doubleValue() + 0.4)
               tsvchange++;
            else 
               break;
               
         		      
         float dl10 = 0;
         for(int i = 2; i < Math.min(10, _lateralVelocitys.size()); i++)
            dl10 += (float)(_lateralVelocitys.get(i).doubleValue()*((Integer)_surfDirections.get(i)).intValue());
         dl10 = Math.abs(dl10)*(10/8.0f);
            
         double MEA = ew.maxEscapeAngle();
         		
         float forwardWall = (float)(wallDistance(_enemyLocation,distance,ew.directAngle, ew.direction)/MEA);
         float reverseWall = (float)(wallDistance(_enemyLocation,distance,ew.directAngle, -ew.direction)/MEA);
         
         tsdirchange/=BFT;
         tsvchange/=BFT;
         if(VCS){
            ew.indexes =  BufferManager.getIndexes(
                  lastLatVel,
               	advVel,
                  BFT, 
                  tsdirchange,
                  accel,
                  tsvchange,
                  dl10,
                  forwardWall,
                  reverseWall 
                  );
            		
         // if(imaginaryWave != null
         // &&
         // Math.abs(ew.bulletVelocity - imaginaryWave.bulletVelocity) < 0.05){
            // boolean same = true;
            // for(int i = 0; i < ew.indexes.length && same; i++){
               // int[] p = ew.indexes[i];
               // int[] q = imaginaryWave.indexes[i];
               // for(int j = 0; j < p.length; j++)
                  // if(p[j] != q[j]){
                     // same = false;
                     // break;
                  //    // System.out.println("Data difference in dimension " + j);
                  // }
            // 
            // }
         // 
         // 
            // if(same){
            //    // System.out.println("Copying data from imaginary wave");
               // ew.indexes = imaginaryWave.indexes;
               // ew.allStats = imaginaryWave.allStats;
               // ew.flattenerStats = imaginaryWave.flattenerStats;
               // ew.flattenerTickStats = imaginaryWave.flattenerTickStats;
            // // ew.bestBins = imaginaryWave.bestBins;
               // ew.scan = imaginaryWave.scan;
            // }
         // 
         //       
         // }
         // if(ew.allStats == null){
         
            ew.allStats = BufferManager.getStats(
                  statBuffers,
                  ew.indexes
                  );
            if(flattenerStarted){
               ew.flattenerStats = BufferManager.getStats(
                  flattenerBuffers,
                  ew.indexes
                  );
               ew.ABSStats = BufferManager.getStats(
                  ABSBuffers,
                  ew.indexes);
               ew.flattenerTickStats = BufferManager.getStats(
                  flattenerTickBuffers,
                  ew.indexes
                  );
            }
         }
         else{
           
            Scan s = new Scan();
         
            s.latVel = limit(0,lastLatVel/8,1);
            s.advVel = limit(0,advVel/8,1);
            s.bft = limit(0,BFT/(1200/11.0f),1);
            s.forwardWall = limit(0,forwardWall,1);
            s.reverseWall = limit(0,reverseWall,1);
            s.lastVel = limit(0,prevLatVel/8.0f,1);
            s.accel = (accel + 2)/3.0f;
            s.timeSinceDecel = limit(0,tsvchange/BFT,1);
            s.timeSinceDirChange = limit(0,tsdirchange/BFT,1);
            s.distLast10 = limit(0,dl10/(100f), 1);
            s.pointInTime = scanCounter;
            ew.scan = s;
         }
         	
         // }
         // if(secondWave == null)
         surfStatsChanged = true;
      		
         _enemyWaves.add(ew);
         updateShadows(ew);
      }
   
   }
   public void addFlattenerTickWave(){
      if(!flattenerStarted || _surfDirections.size() < 4)   
         return;
      	
      double bulletPower = predictBulletPower(bot.getEnergy(),_oppEnergy,((Double)_distances.get(2)).doubleValue()); 	
      	
      EnemyWave imaginaryWave = null;  
      for (int x = 0; x < _enemyWaves.size(); x++) {
         EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
         if(ew.imaginary & ew.fireTime == getTime() - 2){
            imaginaryWave = ew;
            _enemyWaves.remove(x);
            x--;
         }
      }
      	
      EnemyWave ew = new EnemyWave();
      ew.fireTime = getTime() - 2;
      ew.bulletVelocity = bulletVelocity(bulletPower);
      ew.distanceTraveled = ew.bulletVelocity;
      ew.direction = ((Integer)_surfDirections.get(2)).intValue();
      ew.directAngle = ((Double)_surfAbsBearings.get(2)).doubleValue();
      ew.fireLocation = (Point2D.Double)_enemyLocation.clone(); // last tick
      
      float lastLatVel = (float)_lateralVelocitys.get(2).doubleValue();
      float prevLatVel = (float)lastLatVel;
      try{prevLatVel = (float)_lateralVelocitys.get(3).doubleValue();}
      catch(Exception ex){}
         
      float accel = lastLatVel - prevLatVel;
         
      float distance = (float)((Double)_distances.get(2)).doubleValue();
             
      float advVel = (float)((Integer)_advancingVelocitys.get(2)).intValue();
         
      float BFT = (float)(distance/ew.bulletVelocity);
         	 
      float tsdirchange = 0;
      for(int i = 3; i < _surfDirections.size(); i++)
         if(((Integer)_surfDirections.get(i-1)).intValue() == ((Integer)_surfDirections.get(i)).intValue())
            tsdirchange++;
         else 
            break;
         
         	 
      float tsvchange = 0;
      for(int i = 3; i < _lateralVelocitys.size(); i++)
         if(_lateralVelocitys.get(i-1).doubleValue() <= _lateralVelocitys.get(i).doubleValue() + 0.4)
            tsvchange++;
         else 
            break;
               
         		      
      float dl10 = 0;
      for(int i = 2; i < Math.min(10, _lateralVelocitys.size()); i++)
         dl10 += (float)(_lateralVelocitys.get(i).doubleValue()*((Integer)_surfDirections.get(i)).intValue());
      dl10 = Math.abs(dl10)*(10/8.0f);
            
      double MEA = ew.maxEscapeAngle();
         		
      float forwardWall = (float)(wallDistance(_enemyLocation,distance,ew.directAngle, ew.direction)/MEA);
      float reverseWall = (float)(wallDistance(_enemyLocation,distance,ew.directAngle, -ew.direction)/MEA);
         
      tsdirchange/=BFT;
      tsvchange/=BFT;
      if(VCS){
         ew.indexes =  BufferManager.getIndexes(
                  lastLatVel,
               	advVel,
                  BFT, 
                  tsdirchange,
                  accel,
                  tsvchange,
                  dl10,
                  forwardWall,
                  reverseWall 
                  );
            		
      
         ew.flattenerTickStats = BufferManager.getStats(
                  flattenerTickBuffers,
                  ew.indexes
                  );
      }
      else{
         Scan s = new Scan();
         
         s.latVel = limit(0,lastLatVel/8,1);
         s.advVel = limit(0,advVel/8,1);
         s.bft = limit(0,BFT/(1200/11.0f),1);
         s.forwardWall = limit(0,forwardWall,1);
         s.reverseWall = limit(0,reverseWall,1);
         s.lastVel = limit(0,prevLatVel/8.0f,1);
         s.accel = (accel + 2)/3.0f;
         s.timeSinceDecel = limit(0,tsvchange/BFT,1);
         s.timeSinceDirChange = limit(0,tsdirchange/BFT,1);
         s.distLast10 = limit(0,dl10/(100f), 1);
         s.pointInTime = scanCounter;
         ew.scan = s;
      }
         // }
         // if(secondWave == null)
         // surfStatsChanged = true;
      		
      _flattenerTickWaves.add(ew);
      updateShadows(ew);
      // }
   
   }
   public void endOfRound(){
      if(bot.getRoundNum() + 1 == bot.getNumRounds())
      {
         System.out.println("Enemy damage: " + totalEnemyDamage);
         System.out.println("My damage:    " + totalMyDamage);
         System.out.println("Accumulated, weighted enemy hitrate % : " + (100*weightedEnemyHitrate/weightedEnemyFirerate));
         if(VCS){
            statBuffers.clear();
            flattenerBuffers.clear();
         // visitScans.clear();
         }
         if(!MC2K7){
         
         }
         System.gc();
      }
      System.out.println(Insulter.getInsult());
   }  
	
   public void onHitRobot(HitRobotEvent e){
      _time = bot.getTime();    
      _oppEnergy -= 0.6;
   }
	
	
   public void onBulletHit(BulletHitEvent e){
      _time = bot.getTime();    
      double power = e.getBullet().getPower();
      double damage = 4*power;
      if(power > 1)
         damage += 2*(power - 1);
   
      if( enemyGunHeat < bot.getGunCoolingRate() & bot.getOthers() == 0 ){
         
         double bulletPower = 2;
         if(_enemyWaves != null && _enemyWaves.size() > 0)
            bulletPower = (20 - ((EnemyWave)_enemyWaves.get(0)).bulletVelocity)/3;
         bulletPower = Math.min(bulletPower,_oppEnergy);
         addWave(bulletPower);	
         totalMyDamage += _oppEnergy - e.getEnergy();
      }
      else
         totalMyDamage += Math.min(_oppEnergy,damage);
         
      _oppEnergy -= Math.min(_oppEnergy,damage);
      // if(!MC2K7)
         // dgun.onBulletHit(e);
   }
   public void onDeath(DeathEvent e) {
      _time = bot.getTime();    
      Vector v = bot.getAllEvents();
      Iterator i = v.iterator();
      while(i.hasNext()){
         Object obj = i.next();
         if(obj instanceof HitByBulletEvent) {
            onHitByBullet((HitByBulletEvent) obj);
         }
      }
      endOfRound();
   }
   public void onRobotDeath(RobotDeathEvent e){
      _time = bot.getTime();    
   
   }
   public void onWin(WinEvent e){
      _time = bot.getTime();    
      endOfRound();	
   }
   
   public void onSkippedTurn(SkippedTurnEvent e){
      _time = bot.getTime();    
      System.out.println("SKIPPED TURN AT " + e.getTime());
      System.out.println("move time:" + moveTime);
      System.out.println("gun time:" + gunTime);
   }
   public void antiBulletShadows(){
      if(flattenerStarted){
         for(int x = 0; x < myBullets.size(); x++){
            BulletTracker bt = myBullets.get(x);
            if(bt.flattenerLogged)
               continue;
            Point2D.Double bulLoc = new Point2D.Double(bt.b.getX(), bt.b.getY());
            double dist = bt.startLocation.distance(bulLoc);
            if(dist + bt.b.getVelocity() > _enemyLocation.distance(bt.startLocation)){
               double bearing = absoluteBearing(bt.startLocation,_enemyLocation);
               double bVel = bt.b.getVelocity();
               double bHead = bt.b.getHeadingRadians();
               for(int i = 0; i < bt.crossedWaves.size(); i++){
                  EnemyWave ew = bt.crossedWaves.get(i);
                  double totalDist = bt.startLocation.distance(ew.fireLocation);
               	
                  double bDist = dist;
                  double ewDist = ew.distanceTraveled;
                  double ewVel = ew.bulletVelocity;
                  double k = (ewDist + bDist - totalDist)/(bVel + ewVel);
               
                  if(bDist < k*bVel || ewDist < k*ewVel || k < 0)
                     continue;//intersection before one was fired or no intersection yet
                  Point2D.Double shadowLoc = project(_enemyLocation,bHead,-bVel*k);
                  logABS(ew,shadowLoc);
                  if(ew.bestBins != null)
                     surfStatsChanged = true;
                  
               }
               bt.flattenerLogged = true;
            }
         }
      }
   }
   public void updateWaves() {
      if(bot.getOthers() == 0)
         for (int x = 0; x < _enemyWaves.size(); x++) {
            EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
            ew.imaginary = false;
         }
         
      for(int x = 0; x < myBullets.size(); x++){
         Bullet b = myBullets.get(x).b;
         if(!b.isActive()){
            myBullets.remove(x);
            x--;
         }
      }
         
      for (int x = 0; x < _enemyWaves.size(); x++) {
         EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
      
         ew.distanceTraveled = (getTime() - ew.fireTime) * ew.bulletVelocity;
         double myDistFromCenter =  _myLocation.distance(ew.fireLocation);
       
         if((ew.imaginary && ew.distanceTraveled > ew.bulletVelocity+1)
         || ew.distanceTraveled > myDistFromCenter + 50){
            _enemyWaves.remove(x);
            x--;
            continue;
         }
       
       
         if(ew.distanceTraveled > myDistFromCenter - ew.bulletVelocity
         && !ew.flattenerLogged){
         
            if(flattenerStarted && bot.getOthers() > 0)
               logFlattener(ew,_myLocation);
               
            ew.flattenerLogged = true;
            double botWidth = 2*FastTrig.atan(25/(ew.distanceTraveled - 18));
            double hitChance = botWidth/ew.maxEscapeAngle();
            weightedEnemyFirerate += 1/hitChance;
            ew.bestBins = null;
         }
         
         
      }
      if(flattenerStarted)
         for (int x = 0; x < _flattenerTickWaves.size(); x++) {
            EnemyWave ew = (EnemyWave)_flattenerTickWaves.get(x);
         
            ew.distanceTraveled = (getTime() - ew.fireTime) * ew.bulletVelocity;
            double myDistFromCenter =  _myLocation.distance(ew.fireLocation);
         
            if(ew.distanceTraveled > myDistFromCenter - ew.bulletVelocity
            && flattenerStarted && bot.getOthers() > 0){
               logTickFlattener(ew,_myLocation);
               _flattenerTickWaves.remove(x);
               x--;
            
            }
         }
   }
   public void updateShadows(EnemyWave ew){
   
      for(int i = 0, j = myBullets.size(); i < j; i++){
         BulletTracker b = myBullets.get(i);
         ew.logShadow(getTime(),b);
      }
   
   }
   public void updateShadows(BulletTracker b){
      
      for(int i = 0, j = _enemyWaves.size(); i < j; i++){
         EnemyWave ew = _enemyWaves.get(i);
         if(!ew.bulletGone)
            ew.logShadow(getTime(),b);
      }
   
   }

   public EnemyWave getClosestSurfableWave() {
      double closestDistance = Double.POSITIVE_INFINITY; 
      EnemyWave surfWave = null;
   
      for (int x = 0; x < _enemyWaves.size(); x++) {
         EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
         double distance = _myLocation.distance(ew.fireLocation)
             - ew.distanceTraveled;
      
         if (!ew.bulletGone && distance > ew.bulletVelocity 
         && distance < closestDistance) {
            surfWave = ew;
            closestDistance = distance;
         }
      }
   
      return surfWave;
   }

 // Given the EnemyWave that the bullet was on, and the point where we
 // were hit, calculate the index into our stat array for that factor.


 // Given the EnemyWave that the bullet was on, and the point where we
 // were hit, update our stat array to reflect the danger in that area.
   public void logHit(EnemyWave ew, Point2D.Double targetLocation, boolean bulletHitBullet) {
      int index = (int)Math.round(ew.getFactorIndex(targetLocation));
   
      // int min = Math.max(1, index - 20);
      // int max = Math.min(BINS, index + 20);
      // float[] morphProfile = new float[BINS];
      // for(int i = 1; i < BINS; i++)
         // morphProfile[i] =  1f / (sqr((index - i)/3f) + 1f);
      // int newBins = 0;
      if(VCS)
         for(int i = 0, k = ew.allStats.size(); i < k; i++){
            BufferManager.SingleBuffer sb = (BufferManager.SingleBuffer)ew.allStats.get(i);
         
            if(sb.bins == null){
               sb.bins = new int[(int)Math.ceil(sb.rollingDepth*2) + 1];
               sb.hits = -1;  
            }
            // newBins++;
         
            if(sb.binsUsed < sb.bins.length)
               sb.binsUsed+=1;
            
         
         // for(int j = sb.binsUsed-1; j > 0; j--)
            // sb.bins[j] = sb.bins[j-1];
            sb.hits = (sb.hits+1)%sb.bins.length;      
            sb.bins[sb.hits] = index;
         
         }
      
      else{
      
         ew.scan.index = index;
         surfBufferTree.addPoint(ew.scan.location(),ew.scan);
      }
   	
      surfStatsChanged = true;
      for(int i = 0, k = _enemyWaves.size(); i < k; i++)
         ((EnemyWave)_enemyWaves.get(i)).bestBins = null;
   
   }
   public void logFlattener(EnemyWave ew, Point2D.Double targetLocation) {
      if((VCS && ew.flattenerStats == null) || bot.getOthers() == 0)
         return;
         
      int index = (int)Math.round(ew.getFactorIndex(targetLocation));
      
      // int min = Math.max(1, index - 20);
      // int max = Math.min(BINS, index + 20);
      // float[] morphProfile = new float[BINS];
      // for(int i = 1; i< BINS; i++)
         // morphProfile[i] =  1f / (sqr((index - i)/3f) + 1f);
   //       
      // int x = index;
      // int newBins = 0;
      if(VCS)
         for(int i = 0, k = ew.flattenerStats.size(); i < k; i++){
            BufferManager.SingleBuffer sb = (BufferManager.SingleBuffer)ew.flattenerStats.get(i);
         
            if(sb.bins == null){
               sb.bins = new int[(int)Math.ceil(sb.rollingDepth*2) + 1];
               sb.hits = -1;  
            }
            // newBins++;
         
            if(sb.binsUsed < sb.bins.length)
               sb.binsUsed+=1;
         
         // for(int j = sb.binsUsed-1; j > 0; j--)
            // sb.bins[j] = sb.bins[j-1];
         	
            sb.hits = (sb.hits+1)%sb.bins.length;      
            sb.bins[sb.hits] = index;
         
         
         }
      else{
         Scan s = (Scan)(ew.scan.clone());
         s.index = index;
         flatBufferTree.addPoint(s.location(),s);
      }
   }
   public void logABS(EnemyWave ew, Point2D.Double targetLocation) {
      if((VCS && ew.ABSStats == null) || bot.getOthers() == 0)
         return;
         
      int index = (int)Math.round(ew.getFactorIndex(targetLocation));
      
   	
      //System.out.println("Logging ABS");
      if(VCS)
         for(int i = 0, k = ew.ABSStats.size(); i < k; i++){
            BufferManager.SingleBuffer sb = (BufferManager.SingleBuffer)ew.ABSStats.get(i);
         
            if(sb.bins == null){
               sb.bins = new int[(int)Math.ceil(sb.rollingDepth*2) + 1];
               sb.hits = -1;  
            }
            // newBins++;
         
            if(sb.binsUsed < sb.bins.length)
               sb.binsUsed+=1;
         
         // for(int j = sb.binsUsed-1; j > 0; j--)
            // sb.bins[j] = sb.bins[j-1];
         	
            sb.hits = (sb.hits+1)%sb.bins.length;      
            sb.bins[sb.hits] = index;
         
         
         }
      else{
         Scan s = (Scan)(ew.scan.clone());
         s.index = index;
         ABSBufferTree.addPoint(s.location(),s);
      }
   }
   public void logTickFlattener(EnemyWave ew, Point2D.Double targetLocation) {
      if((VCS && ew.flattenerTickStats == null) || bot.getOthers() == 0)
         return;
      int index = (int)Math.round(ew.getFactorIndex(targetLocation));
      
      if(VCS)
         for(int i = 0, k = ew.flattenerTickStats.size(); i < k; i++){
            BufferManager.SingleBuffer sb = (BufferManager.SingleBuffer)ew.flattenerTickStats.get(i);
         
            if(sb.bins == null){
               sb.rollingDepth *= 10;
               sb.bins = new int[(int)Math.ceil(sb.rollingDepth*2) + 1];
               sb.hits = -1;  
            }
         
            if(sb.binsUsed < sb.bins.length)
               sb.binsUsed+=1;
         
            sb.hits = (sb.hits+1)%sb.bins.length;      
            sb.bins[sb.hits] = index;
         }
      else{
         ew.scan.index = index;
         tickBufferTree.addPoint(ew.scan.location(),ew.scan);
      }
   }
    // static double sqr(double d){
      // return d*d;
   // }
   static float sqr(float f){
      return f*f;
   }
    // static double rollingAvg(double value, double newEntry, double depth, double weighting ) {
      // return (value * depth + newEntry * weighting)/(depth + weighting);
   // } 
    // static float rollingAvg(float value, float newEntry, float depth, float weighting ) {
      // return (value * depth + newEntry * weighting)/(depth + weighting);
   // }
   //optimized version that doesn't have 'weighting' but takes a default as 1
   static float rollingAvg(float value, float newEntry, float depth) {
      return (value * depth + newEntry)/(depth + 1);
   }
 /**
 * Bell curve smoother... also know as gaussian smooth or normal distrabution
 * Credits go to Chase-san
 * @param x Current Position
 * @param c Center (current index your adding)
 * @param w Width (number of binIndexes)
 * @return value of a bellcurve
 */ 
   // public static final double smoothingModifier = 5;
    // public static double bellcurve(int x, int c, int w) {
      // int diff = Math.abs(c - x);
      // double binsmooth = smoothingModifier/w;
   // 
   // //I suppose technically you could also use Math.exp(-(binsmooth*binsmooth*diff*diff)/2.0);
      // return  Math.exp(-(binsmooth*binsmooth*diff*diff)/2.0);
   //    // return Math.pow(Math.E, -(binsmooth*binsmooth*diff*diff)/2.0);
   // }
   public void onBulletHitBullet(BulletHitBulletEvent e){
      _time = bot.getTime();    
   
      if (!_enemyWaves.isEmpty()) {
         Point2D.Double hitBulletLocation = new Point2D.Double(
             e.getHitBullet().getX(), e.getHitBullet().getY());
         EnemyWave hitWave = null;
      
         // look through the EnemyWaves, and find one that could've hit the bullet
         hitWave = getCollisionWave(hitBulletLocation,e.getHitBullet().getPower());
      
         if (hitWave != null) {
            if(hitWave.distanceTraveled <= hitWave.bulletVelocity*2)
               hitBulletLocation = project(hitBulletLocation,e.getHitBullet().getHeadingRadians(),e.getHitBullet().getVelocity());
            logHit(hitWave, hitBulletLocation, true);
             // We can remove this wave now, of course.
            hitWave.bulletGone = true;
         }
         else
            System.out.println("ERROR: DETECTED BULLET ON NONEXISTANT WAVE!");
        
         Bullet b = e.getBullet();
         myBullets.remove(b);
         for(int i = 0, j = _enemyWaves.size(); i < j; i++){
            EnemyWave ew = _enemyWaves.get(i);
            // Arrays.fill(ew.binCleared,1);
            
            BulletTracker bt = null;
            for(int k = 0; k < myBullets.size() && bt == null; k++)
               if(myBullets.get(k).b.equals(b))
                  bt = myBullets.get(k);
            if(bt != null)
               ew.clearShadow(getTime(),bt);//run the bullet backwards to see if it cleared anything in the past
            // for(int k = 0, m = myBullets.size(); k < m; k++){
               // Bullet bul = myBullets.get(k);
               // logShadow(ew,bul,false);
               // logShadow(ew,bul,true);
            // }
         }
      } 
      else
         System.out.println("ERROR: DETECTED BULLET WITHOUT WAVES!");
      // if(!MC2K7)
         // dgun.onBulletHitBullet(e);
   }
   public void onHitByBullet(HitByBulletEvent e) {
      _time = bot.getTime();    
     // If the _enemyWaves collection is empty, we must have missed the
     // detection of this wave somehow.
      if (!_enemyWaves.isEmpty()) {
         Bullet bullet = e.getBullet();
         Point2D.Double hitBulletLocation = new Point2D.Double(
             e.getBullet().getX(), e.getBullet().getY());
         EnemyWave hitWave = null;
      
         // look through the EnemyWaves, and find one that could've hit us.
         hitWave = getCollisionWave(_myLocation,e.getBullet().getPower());
         if (hitWave != null) {
            // hitBulletLocation = 
               // project(hitWave.fireLocation, bullet.getHeading(), hitWave.distanceTraveled);
         
            logHit(hitWave, hitBulletLocation, false);
            int index = (int)Math.round(hitWave.getFactorIndex(hitBulletLocation));
            if(hitWave.binCleared != null && hitWave.binCleared[index] == 0)
               System.out.println("Hit by bullet in shadow");
         
             // We can remove this wave now, of course.
            hitWave.bulletGone = true;
            if(_enemyLocation.distance(hitBulletLocation) > 200){
               double botWidth = 2*FastTrig.atan(25/(hitWave.distanceTraveled - 18));
               double hitChance = botWidth/hitWave.maxEscapeAngle();
               weightedEnemyHitrate += 1/hitChance;
            }
         }
         else
            System.out.println("ERROR: DETECTED BULLET ON NONEXISTANT WAVE!");
      }
      else
         System.out.println("ERROR: DETECTED BULLET WITHOUT WAVES!");
     
      double power = e.getBullet().getPower();
      double damage = 4*power;
      if(power > 1)
         damage += 2*(power - 1);
         
      totalEnemyDamage += damage;
      
      _oppEnergy += power*3;
   }
   EnemyWave getCollisionWave(Point2D.Double point, double bulletPower){
      for (int x = 0; x < _enemyWaves.size(); x++) {
         EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
         double dist = ew.distanceTraveled - point.distance(ew.fireLocation);
         if (Math.abs(dist - 10) < 50
         && Math.abs(bulletVelocity(bulletPower)-ew.bulletVelocity) < 0.01) {
           //  System.out.println("wave distance from bullet: " + dist);
            return ew;
           
         }
      }
      
      return null;
   }

 // CREDIT: mini sized predictor from Apollon, by rozu
 // http://robowiki.net?Apollon
   public ArrayList predictPositions(PredictionStatus startPos, EnemyWave surfWave, double direction, double goodDist) {
      Point2D.Double predictedPosition = startPos.endPoint;
      long time = startPos.time;
      ArrayList positions = new ArrayList();
   
      double predictedVelocity = startPos.finalVelocity;
      double predictedHeading = startPos.finalHeading;
      double maxTurning, moveAngle, prefOffset, moveDir;
      Point2D.Double eLoc = _enemyLocation;
   
      int counter = 0; // number of ticks in the future
      boolean intercepted = false;
   
      do {
      //keeps a distance of 500
         //prefOffset = Math.PI/2;//(0.00154*surfWave.fireLocation.distance(predictedPosition) + 0.8);
         // double enemyDistance = _enemyLocation.distance(predictedPosition);
      
         // prefOffset = Math.PI/2;// - 1 + limit(350,enemyDistance, 800)/600;
      
         double absBearing = absoluteBearing(
                 eMove.get((int)time)//eLoc = project(eLoc,lastScan.getHeadingRadians(), lastScan.getVelocity())
               // surfWave.fireLocation
               ,
                predictedPosition
               // _myLocation    
               );
         prefOffset = Math.PI/2 - 1 + limit(200,eLoc.distance(predictedPosition),goodDist)/goodDist;
         // }
      
         moveAngle =
             wallSmoothing(predictedPosition, absBearing+ (direction * prefOffset), direction)
             - predictedHeading;
         moveDir = 1;
      
         if(FastTrig.cos(moveAngle) < 0) {
            moveAngle += Math.PI;
            moveDir = -1;
         }
      
         moveAngle = FastTrig.normalRelativeAngle(moveAngle);
      
      // maxTurning is built in like this, you can't turn more then this in one tick
         maxTurning = (Math.PI/18) - (Math.PI/240)*Math.abs(predictedVelocity);
         
         predictedHeading = FastTrig.normalRelativeAngle(predictedHeading
             + limit(-maxTurning, moveAngle, maxTurning));
      
      // this one is nice ;). if predictedVelocity and moveDir have
         // different signs you want to brake down
      // otherwise you want to accelerate (look at the factor "2")
         // predictedVelocity += (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
      	
         double velAddition = (predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
         
         
         predictedVelocity = limit(-8, predictedVelocity + velAddition, 8);
         
      // calculate the new predicted position
         predictedPosition = project(predictedPosition, predictedHeading, predictedVelocity);
         
         PlaceTime pt = new PlaceTime();
         pt.place = predictedPosition;
         pt.time = (long)((surfWave.fireLocation.distance(pt.place) - surfWave.distanceTraveled - surfWave.bulletVelocity)/surfWave.bulletVelocity) + getTime();  
      
         positions.add(pt);
      
         counter++;
      
         if (startPos.endPoint.distance(predictedPosition) > (pt.time - time)*8 + 28
             
         	 ) {
            intercepted = true;
         }
         
        
            
            
      } while((!intercepted || counter < 10) && counter < 90);
      
      return positions;
   }
   
   public static void filterPlaceTimes(ArrayList positions, double threshold){
      double tSq = threshold*threshold;
      ArrayList newPositions = new ArrayList(positions.size());
      Point2D.Double[] posPoints = new Point2D.Double[positions.size()];
      for(int i = 0; i < positions.size(); i++){
         PlaceTime p1 = (PlaceTime)positions.get(i);
         Point2D.Double p1p = p1.place;
         boolean withinThreshold = false;
         int minPos = Math.max(newPositions.size()-5,0);
         for(int j = newPositions.size()-1; j >= minPos; j--){
            double minDistSq = p1p.distanceSq(posPoints[j]);
            if(minDistSq < tSq){
               withinThreshold = true;
               break;
            }
         }
         if(!withinThreshold){
            posPoints[newPositions.size()]=p1p;
            newPositions.add(p1);
         }
      }
      positions.clear();
      positions.addAll(newPositions);
   }
   static float power(float k, int n){
      float end = 1;
      
      for(int i = 0; i < n; i++)
         end *= k;
      
      return end;
   
   }
   public static void getBins(EnemyWave wave, float[] profile){
      wave.bestBins = new float[EnemyWave.BINS];
      if(wave.binCleared == null){
         wave.binCleared = new float[EnemyWave.BINS];
         for(int i = 0; i < EnemyWave.BINS; i++)
            wave.binCleared[i] = 1;  
      }
      if(VCS)
         for(int i = 0,k = wave.allStats.size(); i < k; i++){
            BufferManager.SingleBuffer sb = (BufferManager.SingleBuffer)wave.allStats.get(i);
            if(sb.bins == null || sb.binsUsed == 0)
               continue;
            float roll = 1 - 1 / (sb.rollingDepth + 1);
            float multFactor = sb.binsUsed*
               (flattenerEnabled?sb.weight:((1-roll)/(roll-power(roll,sb.binsUsed+1))));
            for(int j = sb.hits; j >= 0; j--){
               wave.bestBins[sb.bins[j]] += multFactor;
               multFactor *= roll;
            }
            if(sb.binsUsed == sb.bins.length)
               for(int j = sb.binsUsed - 1; j > sb.hits; j--){
                  wave.bestBins[sb.bins[j]] += multFactor;
                  multFactor *= roll;
               }
         }
      else{
         Scan s = wave.scan;
         List<KDTree.SearchResult<Scan>> cl = 
            surfBufferTree.nearestNeighbours(
            s.location(),
            Math.min((int)Math.ceil(Math.sqrt(surfBufferTree.size())),10));
         Iterator<KDTree.SearchResult<Scan>> it = cl.iterator();
         int i = 0;
         double sumDist = 0.00000000000001;
         while(it.hasNext() & i < Math.sqrt(cl.size())){
            KDTree.SearchResult<Scan> p = it.next();
            sumDist += p.distance;
            i++;
         }
         double invAvgDist = i/sumDist;
         it = cl.iterator();
         
         while(it.hasNext()){
            KDTree.SearchResult<Scan> p = it.next();
            double weight = Math.exp(-0.5*p.distance*invAvgDist);
            wave.bestBins[p.payload.index] += weight;
         }
      }
      heightNormalize(wave.bestBins);
                  
   
      if(flattenerEnabled & (!VCS || wave.flattenerStats != null)){
                  
         float[] flattenerBins = new float[EnemyWave.BINS];
         if(VCS)
            for(int i = 0, k = wave.flattenerStats.size(); i < k; i++){
               BufferManager.SingleBuffer sb = (BufferManager.SingleBuffer)wave.flattenerStats.get(i);
               if(sb.bins == null || sb.binsUsed == 0)
                  continue;
               float roll = 1 - 1 / (sb.rollingDepth + 1);
               float multFactor = sb.binsUsed*sb.weight;
               for(int j = sb.hits; j >= 0; j--){
                  flattenerBins[sb.bins[j]] += multFactor;
                  multFactor *= roll;
               }
               if(sb.binsUsed == sb.bins.length)
                  for(int j = sb.binsUsed - 1; j > sb.hits; j--){
                     flattenerBins[sb.bins[j]] += multFactor;
                     multFactor *= roll;
                  }
            }
         else{
            Scan s = wave.scan;
            List<KDTree.SearchResult<Scan>> cl = 
               flatBufferTree.nearestNeighbours(
               s.location(),
               Math.min((int)Math.ceil(Math.sqrt(flatBufferTree.size())),10));
            Iterator<KDTree.SearchResult<Scan>> it = cl.iterator();
            int i = 0;
            double sumDist = 0.00000000000001;
            while(it.hasNext() & i < Math.sqrt(cl.size())){
               KDTree.SearchResult<Scan> p = it.next();
               sumDist += p.distance;
               i++;
            }
            double invAvgDist = i/sumDist;
            it = cl.iterator();
         
            while(it.hasNext()){
               KDTree.SearchResult<Scan> p = it.next();
               double weight = Math.exp(-0.5*p.distance*invAvgDist);
               flattenerBins[p.payload.index] += weight;
            }
         
         
         }
         heightNormalize(flattenerBins);
                     
         float[] flattenerTickBins = new float[EnemyWave.BINS];
         if(VCS)
            for(int i = 0, k = wave.flattenerTickStats.size(); i < k; i++){
               BufferManager.SingleBuffer sb = (BufferManager.SingleBuffer)wave.flattenerTickStats.get(i);
               if(sb.bins == null || sb.binsUsed == 0)
                  continue;
               float roll = 1 - 1 / (sb.rollingDepth + 1);
               float multFactor = sb.binsUsed*sb.weight;
               for(int j = sb.hits; j >= 0; j--){
                  flattenerTickBins[sb.bins[j]] += multFactor;
                  multFactor *= roll;
               }
               if(sb.binsUsed == sb.bins.length)
                  for(int j = sb.binsUsed - 1; j > sb.hits; j--){
                     flattenerTickBins[sb.bins[j]] += multFactor;
                     multFactor *= roll;
                  }
            }
         else {
            Scan s = wave.scan;
            List<KDTree.SearchResult<Scan>> cl = 
               tickBufferTree.nearestNeighbours(
               s.location(),
               Math.min((int)Math.ceil(Math.sqrt(tickBufferTree.size())),10));
            Iterator<KDTree.SearchResult<Scan>> it = cl.iterator();
            int i = 0;
            double sumDist = 0.00000000000001;
            while(it.hasNext() & i < Math.sqrt(cl.size())){
               KDTree.SearchResult<Scan> p = it.next();
               sumDist += p.distance;
               i++;
            }
            double invAvgDist = i/sumDist;
            it = cl.iterator();
         
            while(it.hasNext()){
               KDTree.SearchResult<Scan> p = it.next();
               double weight = Math.exp(-0.5*p.distance*invAvgDist);
               flattenerTickBins[p.payload.index] += weight;
            }
         
         
         }
         heightNormalize(flattenerTickBins);
         float[] ABSBins = new float[EnemyWave.BINS];
         if(VCS)
            for(int i = 0, k = wave.ABSStats.size(); i < k; i++){
               BufferManager.SingleBuffer sb = (BufferManager.SingleBuffer)wave.ABSStats.get(i);
               if(sb.bins == null || sb.binsUsed == 0)
                  continue;
               float roll = 1 - 1 / (sb.rollingDepth + 1);
               float multFactor = sb.binsUsed*sb.weight;
               for(int j = sb.hits; j >= 0; j--){
                  ABSBins[sb.bins[j]] += multFactor;
                  multFactor *= roll;
               }
               if(sb.binsUsed == sb.bins.length)
                  for(int j = sb.binsUsed - 1; j > sb.hits; j--){
                     ABSBins[sb.bins[j]] += multFactor;
                     multFactor *= roll;
                  }
            }
         else {
            Scan s = wave.scan;
            List<KDTree.SearchResult<Scan>> cl = 
               tickBufferTree.nearestNeighbours(
               s.location(),
               Math.min((int)Math.ceil(Math.sqrt(tickBufferTree.size())),10));
            Iterator<KDTree.SearchResult<Scan>> it = cl.iterator();
            int i = 0;
            double sumDist = 0.00000000000001;
            while(it.hasNext() & i < Math.sqrt(cl.size())){
               KDTree.SearchResult<Scan> p = it.next();
               sumDist += p.distance;
               i++;
            }
            double invAvgDist = i/sumDist;
            it = cl.iterator();
         
            while(it.hasNext()){
               KDTree.SearchResult<Scan> p = it.next();
               double weight = Math.exp(-0.5*p.distance*invAvgDist);
               ABSBins[p.payload.index] += weight;
            }
         
         
         }
         heightNormalize(ABSBins);
      
         for(int i = 1; i < EnemyWave.BINS; i++)
            wave.bestBins[i] = wave.bestBins[i]*0.5f + flattenerBins[i]*0.35f + flattenerTickBins[i]*0.075f;// + ABSBins[i]*0.075f;
           
      }
       //Smooth!
   
      
      final int width = EnemyWave.BINS/2;
      float[] smoothBins = new float[EnemyWave.BINS];
      for(int i = EnemyWave.BINS - 1; i > -1; i--)
         if(wave.bestBins[i] != 0.0)
            for(int j = EnemyWave.BINS-1; j > -1; j--)
               smoothBins[j] += wave.bestBins[i]*profile[j - i + EnemyWave.BINS];
      wave.bestBins = smoothBins;
   
      // areaNormalize(wave.bestBins);
      heightNormalize(wave.bestBins);   
                   
   }
   public static final float[] narrowprofile = new float[EnemyWave.BINS*2];
   public static final float[] wideprofile = new float[EnemyWave.BINS*2];
   static{
      for(int i = EnemyWave.BINS*2 - 1; i > -1; i--){
         narrowprofile[i] =  1f / (sqr((EnemyWave.BINS - i)*0.2f) + 1f);
         wideprofile[i] = 1f / (sqr((EnemyWave.BINS - i)*0.1f) + 1f);
      }
   }
   public PlaceTime getBestPoint(EnemyWave surfWave, EnemyWave nextWave, EnemyWave nnWave){
   
      if(surfWave.bestBins == null){
         getBins(surfWave,wideprofile);
         surfStatsChanged = true;   
      }
      if(nextWave != null && nextWave.bestBins == null){
         getBins(nextWave,wideprofile);
         surfStatsChanged = true;   
      }
      if(nnWave != null && nnWave.bestBins == null){
         getBins(nnWave,wideprofile);
      }
      
      if(nextWave != null && (nextWave.possPoints == null || surfStatsChanged)){
         // nextWave.possPoints = predictPositions(nextWave,  1, 650);
      //    
         // ArrayList reverse = predictPositions(nextWave, -1, 650);
         // nextWave.possPoints.ensureCapacity(reverse.size() + nextWave.possPoints.size());
         // for(int i = 0; i < reverse.size() ; i++)
            // nextWave.possPoints.add(0,reverse.get(i));
      //       
      // 		
         nextWave.weight = getWaveWeight(nextWave);
      //    
         // for(int i = 0; i < nextWave.possPoints.size(); i++){
            // PlaceTime pt = (PlaceTime)(nextWave.possPoints.get(i));
            // pt.danger = getDanger(nextWave,pt);
         // }
      //    
         surfStatsChanged = true;
      }
      
      // if(nnWave != null && (nnWave.possPoints == null || surfStatsChanged)){
         // nnWave.possPoints = predictPositions(nnWave,  1, 650);
      //    
         // ArrayList reverse = predictPositions(nnWave, -1, 650);
         // nnWave.possPoints.ensureCapacity(reverse.size() + nnWave.possPoints.size());
         // for(int i = 0; i < reverse.size() ; i++)
            // nnWave.possPoints.add(0,reverse.get(i));
      //       
      // }
      
   	
      
      if(surfWave.safePoints == null || surfWave.safestPoint == null || surfStatsChanged)
      {
         surfWave.weight = getWaveWeight(surfWave);
         
         double vel = bot.getVelocity();
      
         eMove.predict();
      	
         PredictionStatus now = new PredictionStatus();
         now.finalHeading = bot.getHeadingRadians();
         now.finalVelocity = bot.getVelocity();
         now.distanceRemaining = bot.getDistanceRemaining();
         now.time = getTime();
         now.endPoint = _myLocation;
         if(now.distanceRemaining < 0){
            now.finalVelocity = -now.finalVelocity;
            now.distanceRemaining = -now.distanceRemaining;
            now.finalHeading = FastTrig.normalAbsoluteAngle(now.finalHeading + Math.PI);	
         }
         
         if(surfWave.safePoints == null || surfStatsChanged){
            surfWave.safePoints = predictPositions(now, surfWave,  lateralDirection, 650);
            ArrayList reversePoints = predictPositions(now, surfWave, -lateralDirection, 650);
            surfWave.safePoints.ensureCapacity(reversePoints.size() + surfWave.safePoints.size());
            for(int i = 0; i < reversePoints.size() ; i++)
               surfWave.safePoints.add(0,reversePoints.get(i));
            filterPlaceTimes(surfWave.safePoints,7);
         }
      	
      	
         ArrayList points;
         // if(nextWave == null){
            // int fireAhead =Math.max(1,(int)Math.floor(imaginaryGunHeat/getGunCoolingRate()));
            // int fireTime = (int)getTime() + fireAhead;
            // Point2D.Double fireLocation = _enemyLocation;//project(_enemyLocation,enemyHeading,fireAhead*enemyVelocity);
            // double bulletPower = predictBulletPower(getEnergy(),_oppEnergy,((Double)_distances.get(0)).doubleValue());
         // 
            // EnemyWave fwave = new EnemyWave();
            // fwave.fireLocation = fireLocation;
            // fwave.fireTime = fireTime;
            // fwave.bulletVelocity = 20 - 3*bulletPower;
            // fwave.futureWave = true;
         // 
            // fwave.possPoints = predictPositions(fwave,  1, 650);
         // 
            // ArrayList reverse = predictPositions(fwave, -1, 650);
            // fwave.possPoints.ensureCapacity(reverse.size() + fwave.possPoints.size());
            // for(int i = 0; i < reverse.size() ; i++)
               // fwave.possPoints.add(0,reverse.get(i));
         //    
            // fwave.weight = getWaveWeight(fwave);
         //    
            // points = getPreciseIntersectionPredictionsFireWave(surfWave,surfWave.safePoints,now, fwave);
         // }
         // else
         points = getPreciseIntersectionPredictions(surfWave,surfWave.safePoints,now);
            
         ArrayList bestNextPoints = null;
         
         float minDanger = Float.POSITIVE_INFINITY;
         for(int i = 0, k = points.size(); i < k; i++){
            PlaceTime pt = (PlaceTime)(points.get(i));
            pt.danger = getDanger(surfWave, pt);
            
            if(pt.danger < minDanger ){
               surfWave.safestPoint = pt;
               minDanger = pt.danger;
            }
         }
         Collections.sort(points);//sorts according to danger
         if(nextWave == null){
            // minDanger = Float.POSITIVE_INFINITY;
         //                
            // for(int i = 0, k = points.size(); i < k; i++){
               // PlaceTime pt = (PlaceTime)(points.get(i));
               // if(pt.danger >= minDanger)
                  // continue;//sorted so all after here also to big to be possible
            //    // if(pt.predictionStatus == null)
            //       // pt.predictionStatus = futureStatusPreciseIntersection(
            //          // _myLocation,
            //          // pt.place, 
            //          // now.finalVelocity, 
            //          // now.finalHeading,
            //          // now.time,
            //          // surfWave);
            //       //  (long)((surfWave.fireLocation.distance(pt.place) - surfWave.distanceTraveled - surfWave.bulletVelocity)/surfWave.bulletVelocity) + getTime();
               // float minSecondDanger = Float.POSITIVE_INFINITY;
               // EnemyWave fWave = pt.predictionStatus.fwave;
               // ArrayList nextPoints = getPredictions(fWave,fWave.possPoints,pt.predictionStatus);
               // PlaceTime safePt = null;
               // for(int j = 0, l = nextPoints.size(); j < l; j++){
                  // PlaceTime nextPt = (PlaceTime)nextPoints.get(j);
                  // if(nextPt.predictionStatus == null){
                     // if(nextPt.danger < minSecondDanger){
                        // minSecondDanger = nextPt.danger;
                        // safePt = nextPt;}
                  // }
                  // else{
                     // float d = getDanger(fWave,nextPt);
                     // if(d < minSecondDanger){
                        // minSecondDanger = d;                         
                        // safePt = nextPt;
                     // }
                  // }
               // }
               // if(nextPoints.size() > 0)
               //    // if(minSecondDanger != Float.POSITIVE_INFINITY)
                  // pt.danger += minSecondDanger;
            // 
            // 
               // if(pt.danger < minDanger ){
                  // surfWave.safestPoint = pt;
                  // fWave.safestPoint = safePt;
                  // minDanger = pt.danger;
                  // bestNextPoints = nextPoints;
               // }
            // }
         
         	
         	
         }
         // if(nextWave != null){
         else{
            minDanger = Float.POSITIVE_INFINITY;
         
            
            MovePredictor mp = new MovePredictor();
            for(int i = 0, k = points.size(); i < k; i++){
               PlaceTime pt = (PlaceTime)(points.get(i));
               if(pt.danger >= minDanger)
                  continue;//sorted so all after here also to big to be possible
               if(pt.predictionStatus == null){
                  mp.recycle(
                     _myLocation,
                     pt.place, 
                     now.finalVelocity, 
                     now.finalHeading,
                     now.time,
                     surfWave);
                  pt.predictionStatus = mp.predictToPreciseIntersection();
               }
                  //  (long)((surfWave.fireLocation.distance(pt.place) - surfWave.distanceTraveled - surfWave.bulletVelocity)/surfWave.bulletVelocity) + getTime();
               ArrayList possPoints = predictPositions(pt.predictionStatus,nextWave,1,650);
               ArrayList revPoints = predictPositions(pt.predictionStatus,nextWave,-1,650);
               possPoints.ensureCapacity(possPoints.size() + revPoints.size());
               for(int q = 0; q < revPoints.size(); q++)
                  possPoints.add(0,revPoints.get(q));
               filterPlaceTimes(possPoints,7);
               
               float minSecondDanger = Float.POSITIVE_INFINITY;
               ArrayList nextPoints = getPredictions(nextWave,possPoints,pt.predictionStatus);
               PlaceTime safePt = null;
               for(int j = 0, l = nextPoints.size(); j < l; j++){
                  PlaceTime nextPt = (PlaceTime)nextPoints.get(j);
                  // if(nextPt.predictionStatus == null){
                     // if(nextPt.danger < minSecondDanger){
                        // minSecondDanger = nextPt.danger;
                        // safePt = nextPt;}
                  // }
                  // else{
                  float d = getDanger(nextWave,nextPt);
                  if(d < minSecondDanger){
                     minSecondDanger = d;                         
                     safePt = nextPt;
                  }
                  // }
               }
               if(nextPoints.size() > 0)
                  // if(minSecondDanger != Float.POSITIVE_INFINITY)
                  pt.danger += minSecondDanger;
            
            
               if(pt.danger < minDanger ){
                  surfWave.safestPoint = pt;
                  nextWave.safestPoint = safePt;
                  minDanger = pt.danger;
                  bestNextPoints = nextPoints;
                  nextWave.possPoints = possPoints;
               }
            }
            if(nextWave != null && nextWave.safestPoint != null && nextWave.safestPoint.predictionStatus == null){
               mp.recycle(surfWave.safestPoint.place,
                        nextWave.safestPoint.place, 
                        surfWave.safestPoint.predictionStatus.finalVelocity, 
                        surfWave.safestPoint.predictionStatus.finalHeading,
                        surfWave.safestPoint.time,
                        nextWave);
            
               nextWave.safestPoint.predictionStatus = mp.predictToIntersection();
                        
                        
            }
            
            
            if(nnWave != null 
            && nnWave.safestPoint != null 
            && nnWave.safestPoint.predictionStatus != null
            && nextWave != null 
            && nextWave.safestPoint != null 
            && nextWave.safestPoint.predictionStatus != null
            && nnWave.possPoints != null
            && nnWave.possPoints.size() > 0){   
               
               ArrayList possPoints = predictPositions(nextWave.safestPoint.predictionStatus,nnWave,1,650);
               ArrayList revPoints = predictPositions(nextWave.safestPoint.predictionStatus,nnWave,-1,650);
               possPoints.ensureCapacity(possPoints.size() + revPoints.size());
               for(int q = 0; q < revPoints.size(); q++)
                  possPoints.add(0,revPoints.get(q));
               filterPlaceTimes(possPoints,7);
            	
               ArrayList nextPoints = getPredictions(
                     nnWave,
                     possPoints,
                     nextWave.safestPoint.predictionStatus);
                  
               PlaceTime safePt = null;
               float minnnDanger = Float.POSITIVE_INFINITY;
               for(int j = 0, l = nextPoints.size(); j < l; j++){
                  PlaceTime nextPt = (PlaceTime)nextPoints.get(j);
                  
                  float d = getDanger(nnWave,nextPt);
                  if(d < minnnDanger){
                     minnnDanger =d;                         
                     safePt = nextPt;
                  }
               }
               nnWave.safestPoint = safePt;
               if( safePt != null && safePt.predictionStatus == null){
                  mp.recycle(
                        nextWave.safestPoint.place,
                        safePt.place, 
                        nextWave.safestPoint.predictionStatus.finalVelocity, 
                        nextWave.safestPoint.predictionStatus.finalHeading,
                        nextWave.safestPoint.time,
                        nnWave);
                  safePt.predictionStatus = mp.predictToIntersection();
               }
            }
           
         }
         //if(!(surfWave != null && nextWave == null))
         surfStatsChanged = false;
         
         if(painting){
            firstPointsPainting = points;
            nextPointsPainting = bestNextPoints;
         }
         
      }
   
      return surfWave.safestPoint;
   
   }
   public float getWaveWeight(EnemyWave wave){
      // double tta = (wave.fireLocation.distance(_myLocation) - wave.distanceTraveled)/wave.bulletVelocity;
      
      // double relevance = Math.pow(0.88,tta/2);
      // double relevance = tta*tta - 200*tta + 10000;
      // double relevance = Math.pow(0.96,tta);
      double bp = (20 - wave.bulletVelocity)/3;
      
   		
      return (float)((bp*4 + Math.max(0,bp - 1)*2));
   
   }
   public float getDanger(EnemyWave wave, PlaceTime pt){
   
      if(!_fieldRect.contains(pt.place))
         return Float.POSITIVE_INFINITY;
      
         
            
      Point2D.Double startPlace;
   
      boolean precise = false;
      if(pt.predictionStatus != null){
         startPlace = pt.predictionStatus.endPoint;
         if(pt.predictionStatus.intersectionIndices != null)
            precise = true;
      }
      else
         startPlace = pt.place;
     
      double waveCenterDistHere = wave.fireLocation.distance(startPlace);
     
      float thisDanger;
      if(precise){
         int[] intIndices = {(int)Math.round(pt.predictionStatus.intersectionIndices[0]),
               (int)Math.round(pt.predictionStatus.intersectionIndices[1])};
               
         thisDanger = getAverageDanger(wave.bestBins, wave.binCleared,intIndices);
         double width = pt.predictionStatus.intersectionIndices[1] - pt.predictionStatus.intersectionIndices[0];
         width *= wave.maxEscapeAngle()*(1.0/EnemyWave.MIDDLE_BIN);
         thisDanger *= width;
         // System.out.println("Using precise!");
      }
      else{
      
         int index = (int)Math.round(wave.getFactorIndex(startPlace));
         double botWidthAtEnd = 40/(waveCenterDistHere - 34);
         double inv_binWidth = EnemyWave.MIDDLE_BIN/wave.maxEscapeAngle();
         int botBinWidthAtEnd = (int)Math.round(botWidthAtEnd*inv_binWidth);
         
         thisDanger = getAverageDanger(wave.bestBins, wave.binCleared,index,botBinWidthAtEnd);
         thisDanger *= botWidthAtEnd;//*botWidthAtEnd;
      }
      
      if(wave.futureWave)
         thisDanger *= 0.2;
      Point2D.Double futureEloc = eMove.get((int)pt.time);
      thisDanger /= Math.max(0.1,Math.sqrt(Math.min(
         Math.min(futureEloc.distanceSq(startPlace), waveCenterDistHere*waveCenterDistHere),
         _enemyLocation.distanceSq(startPlace)))-34);
       
      float tta = (float)((waveCenterDistHere - wave.distanceTraveled)/wave.bulletVelocity);
      
      // double relevance = Math.pow(0.88,tta/2);
      float relevance = tta*tta - 200*tta + 10000;
      // double relevance = Math.pow(0.96,tta);
      //float relevance = (float)Math.pow(0.9,0.3*tta);
   	
      return thisDanger*relevance*wave.weight;
      
   }
   public ArrayList getPreciseIntersectionPredictions(EnemyWave wave, ArrayList points, PredictionStatus start){
      MovePredictor mp = new MovePredictor();
      ArrayList likelyPoints = new ArrayList(points.size());
      int maxStop = points.size() - 1;
      for(; maxStop >= 0; maxStop--){
         PlaceTime guessPT = (PlaceTime)points.get(maxStop);
         mp.recycle(
            start.endPoint,
            guessPT.place,
            start.finalVelocity,
            start.finalHeading,
            start.time,wave);
         PredictionStatus futureStatus = mp.predictToPreciseIntersection();  
            // PredictionStatus futureStatus = futureStatusPreciseIntersection(
            // start.endPoint,
            // guessPT.place,
            // start.finalVelocity,
            // start.finalHeading,
            // start.time,wave);
      
            
         // guessPT = clone(guessPT);
         guessPT.time = futureStatus.time;
         guessPT.predictionStatus = futureStatus;
         
         likelyPoints.add(guessPT);
      }
               
      return likelyPoints;
      
   } 
   public ArrayList getPredictions(EnemyWave wave, ArrayList points, PredictionStatus start){
      ArrayList likelyPoints = new ArrayList(points.size());
      MovePredictor mp = new MovePredictor();
      int maxStop = points.size() - 1;
      for(; maxStop >= 0; maxStop--){
         PlaceTime guessPT = (PlaceTime)points.get(maxStop);
         // PredictionStatus futureStatus = futureStatus(
            // start.endPoint,
            // guessPT.place,
            // start.finalVelocity,
            // start.finalHeading,
            // start.time,wave);
         mp.recycle(
            start.endPoint,
            guessPT.place,
            start.finalVelocity,
            start.finalHeading,
            start.time,wave);
         PredictionStatus futureStatus = mp.predictToIntersection(); 
            
         // guessPT = clone(guessPT);
         guessPT.time = futureStatus.time;
         guessPT.predictionStatus = futureStatus;
         likelyPoints.add(guessPT);
      
         if(guessPT.predictionStatus.finalVelocity == 0.0
            & guessPT.predictionStatus.distanceRemaining == 0.0)
            break;
      }
         
      int minStop = 0;
      for(; minStop <= maxStop; minStop++){
         PlaceTime guessPT = (PlaceTime)points.get(minStop);
         // PredictionStatus futureStatus = futureStatus(
            // start.endPoint,
            // guessPT.place,
            // start.finalVelocity,
            // start.finalHeading,
            // start.time,wave);
         mp.recycle(
            start.endPoint,
            guessPT.place,
            start.finalVelocity,
            start.finalHeading,
            start.time,wave);
         PredictionStatus futureStatus = mp.predictToIntersection(); 
         
         // guessPT = clone(guessPT);
         guessPT.time = futureStatus.time;
         guessPT.predictionStatus = futureStatus;
         likelyPoints.add(guessPT);
         
         if(guessPT.predictionStatus.finalVelocity == 0.0
            & guessPT.predictionStatus.distanceRemaining == 0.0)
            break;
      }
         
      for(int i = minStop; i <= maxStop; i++){
         PlaceTime guessPT = ((PlaceTime)points.get(i));
         guessPT.predictionStatus = null;
         likelyPoints.add(
            // clone(
            guessPT
            //  )
            );
      }
      
      return likelyPoints;
      
   }  
   /*public ArrayList getPreciseIntersectionPredictionsFireWaveAt(EnemyWave wave, ArrayList points, PredictionStatus start, EnemyWave fwave){
      ArrayList likelyPoints = new ArrayList(points.size());
       
      double maxDist = (fwave.fireTime - start.time)*8 + 30;
      	
      int maxStop = points.size() - 1;
      for(; maxStop >= 0; maxStop--){
         PlaceTime guessPT = (PlaceTime)points.get(maxStop);
         if(guessPT.place.distance(start.endPoint) > maxDist)
            continue;
         	
         EnemyWave clonewave = new EnemyWave();
         clonewave.fireLocation = fwave.fireLocation;
         clonewave.fireTime = fwave.fireTime;
         clonewave.bulletVelocity = fwave.bulletVelocity;
         clonewave.possPoints = fwave.possPoints;
         clonewave.weight = fwave.weight;
         clonewave.futureWave = true;
         	
         PredictionStatus futureStatus = futureStatusPreciseIntersectionFireWaveAt(
               start.endPoint,
               guessPT.place,
               start.finalVelocity,
               start.finalHeading,
               start.time,wave, clonewave);
               
         guessPT = clone(guessPT);
         guessPT.time = futureStatus.time;
         guessPT.predictionStatus = futureStatus;
         if(futureStatus.distanceRemaining < 30)
            likelyPoints.add(guessPT);
      }
                  
      return likelyPoints;
         
   }*/

   static PlaceTime clone(PlaceTime pt){
      PlaceTime p = new PlaceTime();
      p.predictionStatus = pt.predictionStatus;
      p.place = pt.place;
      p.time = pt.time;
      p.danger = pt.danger;
      return p;
   
   }

      	//3 optimized methods from the new robocode engine
   private static double getNewVelocity(double velocity, double distance) {
      if(distance < 0)
         return -getNewVelocity(-velocity,-distance);
         
      final double goalVel = Math.min(getMaxVelocity(distance), 8);
   
      if(velocity >= 0)
         return limit(velocity - 2,
            goalVel, velocity + 1);
     
      return limit(velocity - 1,
         goalVel, velocity + maxDecel(-velocity));
   }

   static final double getMaxVelocity(double distance) {
      final double decelTime =  Math.round(
         //sum of 0... decelTime, solving for decelTime 
         //using quadratic formula, then simplified a lot
         Math.sqrt(distance + 1));
   
      final double decelDist = Math.max(0, (decelTime) * (decelTime-1) );
         // sum of 0..(decelTime-1)
         // * Rules.DECELERATION*0.5;
   
      return Math.max(0,((decelTime - 1) * 2) + ((distance - decelDist) / decelTime));
   }
 
   private static final double maxDecel(double speed) {
      return limit(1,speed*0.5 + 1, 2);
   }
/*
   public PredictionStatus futureStatusPreciseIntersectionFireWaveAt(Point2D.Double fromLocation, Point2D.Double toLocation, double initialVelocity, double initialHeading, long currentTime, EnemyWave wave, EnemyWave newWave){
      double bearing = absoluteBearing(fromLocation,toLocation);
      double velocity = initialVelocity;
      double distanceRemaining = fromLocation.distance(toLocation);;
      long time = currentTime - wave.fireTime;
      double heading = initialHeading;
      
      Point2D.Double endPoint = (Point2D.Double)fromLocation.clone();  
      int counter = 140;//5 + (int)Math.ceil(endPoint.distance(wave.fireLocation)/(wave.bulletVelocity-8)) - time;;
      double sinVal = 0, cosVal=0;
      boolean inline = false;
      
      PreciseWave pw = new PreciseWave();
      pw.fireLocation = wave.fireLocation;
      pw.bulletVelocity = wave.bulletVelocity;
      
      long fireTime = newWave.fireTime - wave.fireTime;
      int fireLatOffset;
      if(fireTime == 0)
         fireLatOffset = -(int)time;
      else
         fireLatOffset = 0;
      double latdir = lateralDirection;
      double[] latVels = new double[(int)fireTime + 1 + fireLatOffset];
      double[] advVels = new double[(int)fireTime + 1 + fireLatOffset];
      double[] latdirs = new double[(int)fireTime + 1 + fireLatOffset];
   	
      do{
         time++;
         if(!inline && (distanceRemaining > 1 || Math.abs(velocity) > 0.1))
         {
            double maxTurn = Math.PI/18 - Math.PI/240*Math.abs(velocity);
            bearing = absoluteBearing(endPoint,toLocation);
            double offset = Utils.normalRelativeAngle(bearing - heading);
            if(-Math.PI/2 > offset || offset > Math.PI/2){
               offset = Utils.normalRelativeAngle(offset + Math.PI);
               velocity = -velocity;
               heading += Math.PI;
            }
            offset = limit(-maxTurn,offset,maxTurn);
            heading += offset;
            sinVal = FastTrig.sin(heading);
            cosVal = FastTrig.cos(heading);
            if(-0.0001 < offset && offset < 0.0001)
               inline = true;
         }
         
         velocity = getNewVelocity(velocity, distanceRemaining);
         endPoint.x += sinVal*velocity;
         endPoint.y += cosVal*velocity;
         
         if(endPoint.x < W
         || endPoint.x > E
         || endPoint.y < S
         || endPoint.y > N){
         
            velocity *= 0.5;
         
            endPoint.x -= sinVal*velocity;
            endPoint.y -= cosVal*velocity;
            velocity = 0;
         }
         Point2D.Double eLoc = eMove.get((int)(time + wave.fireTime));
         Rectangle2D.Double eRect = new Rectangle2D.Double(eLoc.x - 18, eLoc.y - 18, 36,36);
         if(eRect.intersects(endPoint.x-18,endPoint.y-18,36,36)){
            velocity = 0;
         }
          
         if(velocity > distanceRemaining)
            inline = false;
         if(inline)
            distanceRemaining = Math.abs(distanceRemaining - velocity);
         else
            distanceRemaining = endPoint.distance(toLocation);
       
         if(time < fireTime){
         //update bot state/history
            double ebearing = absoluteBearing(endPoint,newWave.fireLocation) - heading;
            double latVel = velocity*FastTrig.sin(ebearing);
            double advVel = -velocity*FastTrig.cos(ebearing);
            
            if(latVel > 0)
               latdir = 1;
            else if(latVel < 0)
               latdir = -1;
         
            latdirs[(int)time + fireLatOffset] = latdir;
            latVels[(int)time + fireLatOffset] = Math.abs(latVel);
            advVels[(int)time + fireLatOffset] = advVel;
         }
         else if(time == fireTime){
         //grab data from buffers, create bins etc
            double absEBearing = absoluteBearing(endPoint,newWave.fireLocation);
            double ebearing = absEBearing - heading;
            double latVel = velocity*FastTrig.sin(ebearing);
            double advVel = -velocity*FastTrig.cos(ebearing);
            
            if(latVel > 0)
               latdir = 1;
            else if(latVel < 0)
               latdir = -1;
               
            latdirs[(int)time + fireLatOffset] = latdir;
            latVels[(int)time + fireLatOffset] = Math.abs(latVel);
            advVels[(int)time + fireLatOffset] = advVel;
            
            newWave.direction = (int)latdir;
            newWave.directAngle = Utils.normalAbsoluteAngle(absEBearing + Math.PI);
            newWave.imaginary = true;
         	
            double lastLatVel = Math.abs(latVel);
            if(time + fireLatOffset > 0)
               lastLatVel = latVels[(int)time-1 + fireLatOffset];
            else if(_lateralVelocitys.size() > 1)
               lastLatVel = _lateralVelocitys.get(1).doubleValue();
         	
            float accel = (float)(latVel - lastLatVel);
            float eDist = (float)endPoint.distance(newWave.fireLocation);
            float BFT = (float)(eDist/newWave.bulletVelocity);
            
            float tsdirchange = 0;
            boolean broken = false;
            for(int i = (int)time + fireLatOffset; i > 0; i--)
               if(latdirs[i] == latdirs[i-1])
                  tsdirchange++;
               else{
                  broken = true;
                  break;
               }
            if(!broken){
               for(int i = 1; i < _surfDirections.size() - 2; i++)
                  if(((Integer)_surfDirections.get(i-1)).intValue() == ((Integer)_surfDirections.get(i)).intValue())
                     tsdirchange++;
                  else 
                     break;
            }
            
            broken = false;
            float tsvchange = 0;
            for(int i = (int)time + fireLatOffset; i > 0; i--)
               if(latVels[i] <= latVels[i-1]+0.4)
                  tsvchange++;
               else{
                  broken = true;
                  break;
               }
            if(!broken){
               for(int i = 1; i < _lateralVelocitys.size() - 2; i++)
                  if(_lateralVelocitys.get(i-1).doubleValue() <= _lateralVelocitys.get(i).doubleValue() + 0.4)
                     tsvchange++;
                  else 
                     break;
            }
            float dl10 = 0;
            int stop = Math.max((int)time + fireLatOffset-10,-1);
            for(int i = (int)time + fireLatOffset; i > stop; i--)
               dl10 +=  (float)(latVels[i]*latdirs[i]);
            stop = 10 - (int)time + fireLatOffset;
            if(stop > 0){
               stop = Math.min(stop, _lateralVelocitys.size() - 2);
               for(int i = 0; i < stop; i++)
                  dl10 += (float)(_lateralVelocitys.get(i).doubleValue()*((Integer)_surfDirections.get(i)).intValue());
            }
            dl10 = Math.abs(dl10)*(10/8.0f);
            
            double MEA = newWave.maxEscapeAngle();
         	
            float forwardWall = (float)(wallDistance(newWave.fireLocation,eDist,newWave.directAngle, newWave.direction)/MEA);
            float reverseWall = (float)(wallDistance(newWave.fireLocation,eDist,newWave.directAngle, -newWave.direction)/MEA);
         
            tsdirchange/=BFT;
            tsvchange/=BFT;
         
            newWave.indexes = BufferManager.getIndexes((float)Math.abs(latVel),
               													(float)advVel,
               													BFT,
               													tsdirchange,
               													accel,
               													tsvchange,
               													dl10,
               													forwardWall,
               													reverseWall);
         }
      	
      	
                
      } while(time < fireTime //pointRad > wave.bulletVelocity*(time+1)
       //  && (Math.abs(distanceRemaining) > 0.1 || Math.abs(velocity) > 0.1)
      && --counter != 0 );
      
      if(counter == 0)//  {
         System.out.println("PREVENTED PREDICTION FREEZE futureStatusPreciseIntersectionFireWaveAt !!");
      // time = (long)(endPoint.distance(wave.fireLocation)/wave.bulletVelocity) + wave.fireTime;
   
      PredictionStatus status = new PredictionStatus();
      status.distanceRemaining = distanceRemaining;
      status.endPoint = (Point2D.Double)endPoint.clone();
      status.finalVelocity = velocity;
      status.finalHeading = Utils.normalAbsoluteAngle(heading);
      status.time = time + wave.fireTime;//(long)(endPoint.distance(wave.fireLocation)/wave.bulletVelocity) + wave.fireTime;
   
      	
      newWave.allStats = BufferManager.getStats(
                  statBuffers,
                  newWave.indexes);
      if(flattenerStarted){
         newWave.flattenerStats = BufferManager.getStats(
                  flattenerBuffers,
                  newWave.indexes);
         newWave.ABSStats = BufferManager.getStats(
                  ABSBuffers,
                  newWave.indexes);
         newWave.flattenerTickStats = BufferManager.getStats(
                  flattenerTickBuffers,
                  newWave.indexes);
      }
         	
      
      getBins(newWave,narrowprofile);
      //updateShadows(newWave);
      
      status.fwave = newWave;
   
   
      return status;
   }
*/
   static double sqr(double d){
      return d*d;
   }
   static int sqr(int i){
      return i*i;
   }
   
   public static void heightNormalize(float[] bins){
      float max = 0;
      for(int i = 1; i < bins.length; i++)
         if(bins[i] > max)
            max = bins[i];
      if(max != 0){
         max = 1/max;
      
         for(int i = 1; i < bins.length; i++)
            bins[i] *= max;
      }
   }
   public static void areaNormalize(float[] bins){
      float total = 0;
      for(int i = 1; i < bins.length; i++)
         total += bins[i];
      total = 1/total;
      if(total != 0){
         for(int i = 1; i < bins.length; i++)
            bins[i] *= total;
      }
   }
   public float getAverageDanger(float[] bins, float[] binsLevel, int index, int botBinWidth){
      botBinWidth = (int)limit(2, botBinWidth, EnemyWave.BINS - 1);
      float totalDanger = 0;
      
      int minIndex = Math.max(1,index - botBinWidth/2);
      int maxIndex = Math.min(EnemyWave.BINS - 1, index + botBinWidth/2) + 1;
      for(int i = minIndex; i < maxIndex; i++)
         totalDanger += bins[i]*binsLevel[i];
   	
      return totalDanger/(maxIndex - minIndex);
   
   }
   public float getAverageDanger(float[] bins, float[] binsLevel, int[] minMaxIndexes){
      float totalDanger = 0;
      
      int minIndex = minMaxIndexes[0];
      int maxIndex = minMaxIndexes[1]+1;
      for(int i = minIndex; i < maxIndex; i++)
         totalDanger += bins[i]*binsLevel[i];
   	
      return totalDanger/(maxIndex - minIndex);
   
   }

   public void doSurfing() {
      mainWave = getClosestSurfableWave();
      boolean surf = false;
      if(mainWave != null){
         _enemyWaves.remove(mainWave);
         secondWave = getClosestSurfableWave();
         
         if(secondWave != null){
            _enemyWaves.remove(secondWave);
            thirdWave = getClosestSurfableWave();
            _enemyWaves.add(secondWave);
         }
         
         _enemyWaves.add(mainWave);
         PlaceTime bestPoint = getBestPoint(mainWave, secondWave, thirdWave);
         if(bestPoint != null && bestPoint.place != null){
            surf = true;  
            goTo(bestPoint, mainWave);
            direction = -lateralDirection;
         }
         else{
            mainWave = secondWave = null;
         }
      }
      else
         secondWave = null;
         
   		
      if (!surf) {
      /*
         if(_distances.size() > 0){
            PredictionStatus now = new PredictionStatus();
            now.finalHeading = bot.getHeadingRadians();
            now.finalVelocity = bot.getVelocity();
            now.distanceRemaining = bot.getDistanceRemaining();
            now.time = getTime();
            now.endPoint = _myLocation;
            if(now.distanceRemaining < 0){
               now.finalVelocity = -now.finalVelocity;
               now.distanceRemaining = -now.distanceRemaining;
               now.finalHeading = FastTrig.normalAbsoluteAngle(now.finalHeading + Math.PI);	
            }
            ArrayList points;
         
            int fireAhead =Math.max(1,(int)Math.floor(imaginaryGunHeat/bot.getGunCoolingRate()));
            int fireTime = (int)getTime() + fireAhead;
            Point2D.Double fireLocation = _enemyLocation;//project(_enemyLocation,enemyHeading,fireAhead*enemyVelocity);
            double bulletPower = predictBulletPower(bot.getEnergy(),_oppEnergy,((Double)_distances.get(0)).doubleValue());
         
            EnemyWave fwave = new EnemyWave();
            fwave.fireLocation = fireLocation;
            fwave.fireTime = fireTime;
            fwave.bulletVelocity = 20 - 3*bulletPower;
         
            fwave.possPoints = predictPositions(now,fwave,  1, 650);
         
            ArrayList reverse = predictPositions(now,fwave, -1, 650);
            fwave.possPoints.ensureCapacity(reverse.size() + fwave.possPoints.size());
            for(int i = 0; i < reverse.size() ; i++)
               fwave.possPoints.add(0,reverse.get(i));
            
            fwave.weight = getWaveWeight(fwave);
            
            points = getPreciseIntersectionPredictionsFireWaveAt(fwave,fwave.possPoints,now, fwave);
         
            float minDanger = Float.POSITIVE_INFINITY;
         
                     
            for(int i = 0, k = points.size(); i < k; i++){
               PlaceTime pt = (PlaceTime)(points.get(i));
               ArrayList secondPoints = getPredictions(pt.predictionStatus.fwave,fwave.possPoints,pt.predictionStatus);
               
               float minSecond = Float.POSITIVE_INFINITY;
               for(int j = 0, m = secondPoints.size(); j < m; j++)
                  minSecond = Math.min(minSecond,getDanger(pt.predictionStatus.fwave,(PlaceTime)secondPoints.get(j)));
               //pt.danger = getDanger(pt.predictionStatus.fwave,pt);
               
               if(minSecond < minDanger ){
                  fwave.safestPoint = pt;
                  minDanger = minSecond;
                  mainWave = fwave;
               }
            }
         }
         if(mainWave != null){
            goTo(mainWave.safestPoint,mainWave);
         }
         else*/
         {
            if(_enemyLocation != null){
            
               double distance = _enemyLocation.distanceSq(_myLocation);
               double absBearing = absoluteBearing(_myLocation, _enemyLocation);
               double headingRadians = bot.getHeadingRadians();
               double stick =limit(121,distance,160);
               double  goAngle, revGoAngle, revOffset;
               double offset = revOffset = Math.max(Math.PI/3 + 0.021,Math.PI/2 + 1 -limit(0.4,distance/(400*400),1.2));
               int count = 50;
               Point2D.Double endPoint, revEndPoint;
            
               while(!_fieldRect.
               contains(endPoint = project(_myLocation,goAngle = absBearing + direction*(offset -= 0.02), stick))
               & count-- > 0);
            
               count = 50;
            
               while(!_fieldRect.
               contains(revEndPoint = project(_myLocation,revGoAngle = absBearing - direction*(revOffset -= 0.02), stick))
               & count-- > 0);
            
               if(offset < revOffset){
                  direction = -direction;
                  goAngle = revGoAngle;
               }
            
            
               bot.setAhead(50*FastTrig.cos(goAngle -= headingRadians));
               bot.setTurnRightRadians(FastTrig.tan(goAngle));
            }
         }
      }
      
   }
   
   private void goTo(PlaceTime pt, EnemyWave surfWave) {
      Point2D.Double place = pt.place;
      lastGoToPoint = place;
      double distance = _myLocation.distance(place);
      double dir = 1;
      double angle = FastTrig.normalRelativeAngle(absoluteBearing(_myLocation, place) - bot.getHeadingRadians());
      if(-1 < distance & distance < 1 )
         angle = 0;
         
      if (Math.abs(angle) > Math.PI/2) {
         dir = -1;
         if (angle > 0) {
            angle -= Math.PI;
         }
         else {
            angle += Math.PI;
         }
      }
      
      if(pt.predictionStatus != null 
      && Math.abs(pt.predictionStatus.distanceRemaining) <= 0.1 
      && Math.abs(pt.predictionStatus.finalVelocity) <= 0.1){
         double myVel = bot.getVelocity();
         double heading = bot.getHeadingRadians();
         if(myVel < 0){
            myVel = -myVel;
            heading += Math.PI;
         }
         double maxTurn = Math.PI/18 - (Math.PI/240)*myVel;
         heading += limit(-maxTurn,angle, maxTurn);
         
         double nextVel = limit(1,myVel + 1, 8);
      
         Point2D.Double nextLocation = project(_myLocation,heading,nextVel);
         MovePredictor mp = new MovePredictor(nextLocation,pt.place,nextVel,heading,getTime() + 1,surfWave);
         PredictionStatus stillOption = mp.predictToIntersection();
      
         if(Math.abs(stillOption.distanceRemaining) <= 0.1){
            distance = 100;
            //angle = 0;
         }
      }
   	
      bot.setTurnRightRadians(angle);
      bot.setAhead(distance*dir);
       
   }
   static double decelDistance(double vel){
   
      int intVel = (int)Math.ceil(vel);
      switch(intVel){  
         case 8:
            return 6 + 4 + 2;
         case 7:
            return 5 + 3 + 1;
         case 6:
            return 4 + 2;
         case 5:
            return 3 + 1;
         case 4:
            return 2;
         case 3:
            return 1;
         case 2:
            // return 2;
         case 1:
            // return 1;
         case 0:
            return 0;
            
         default:
            return 6 + 4 + 2;
      
      
      }
      
      // double dist = 0;
      // while(vel > 0){
         // vel = limit(0, vel - 2, 8);
         // dist += vel;
      // }
      // return dist;
   }  
	
	
   private double absoluteBearing(Point2D source, Point2D target) {
      return FastTrig.atan2(target.getX() - source.getX(), target.getY() - source.getY());
   }
 // This can be defined as an inner class if you want.




//non-iterative wallsmoothing by Simonton - to save your CPUs
   public static final double HALF_PI = Math.PI / 2;
   public static final double WALKING_STICK = 160;
   public static final double WALL_MARGIN = 19;
   public static final double S = WALL_MARGIN;
   public static final double W = WALL_MARGIN;
   public static final double N = 600 - WALL_MARGIN;
   public static final double E = 800 - WALL_MARGIN;

 // angle = the angle you'd like to go if there weren't any walls
 // oDir  =  1 if you are currently orbiting the enemy clockwise
 //         -1 if you are currently orbiting the enemy counter-clockwise
 // returns the angle you should travel to avoid walls
   double wallSmoothing(Point2D.Double botLocation, double angle, double oDir) {
      // if(!_fieldRect.contains(project(botLocation,angle + Math.PI*(oDir + 1),WALKING_STICK))){
      angle = smoothWest(N - botLocation.y, angle - HALF_PI, oDir) + HALF_PI;
      angle = smoothWest(E - botLocation.x, angle + Math.PI, oDir) - Math.PI;
      angle = smoothWest(botLocation.y - S, angle + HALF_PI, oDir) - HALF_PI;
      angle = smoothWest(botLocation.x - W, angle, oDir);
      
      // for bots that could calculate an angle that is pointing pretty far
      // into a corner, these three lines may be necessary when travelling
      // counter-clockwise (since the smoothing above may have moved the 
      // walking stick into another wall)
      angle = smoothWest(botLocation.y - S, angle + HALF_PI, oDir) - HALF_PI;
      angle = smoothWest(E - botLocation.x, angle + Math.PI, oDir) - Math.PI;
      angle = smoothWest(N - botLocation.y, angle - HALF_PI, oDir) + HALF_PI;
      // }
      return angle;
   }

 // smooths agains the west wall
   static double smoothWest(double dist, double angle, double oDir) {
      if (dist < -WALKING_STICK * FastTrig.sin(angle)) {
         return FastTrig.acos(oDir * dist / WALKING_STICK) - oDir * HALF_PI;
      }
      return angle;
   }
   
	//CREDIT: MORE STUFF BY SIMONTON =)

 // eDist  = the distance from you to the enemy
 // eAngle = the absolute angle from you to the enemy
 // oDir   =  1 for the clockwise orbit distance
 //          -1 for the counter-clockwise orbit distance
 // returns: the positive orbital distance (in radians) the enemy can travel
 //          before hitting a wall (possibly infinity).
   static double wallDistance(Point2D.Double sourceLocation, double eDist, double eAngle, int oDir) {
      return Math.min(Math.min(Math.min(
         distanceWest(N - sourceLocation.getY(), eDist, eAngle - HALF_PI, oDir),
         distanceWest(E - sourceLocation.getX(), eDist, eAngle + Math.PI, oDir)),
         distanceWest(sourceLocation.getY() - S, eDist, eAngle + HALF_PI, oDir)),
         distanceWest(sourceLocation.getX() - W, eDist, eAngle, oDir));
   }
 
   static double distanceWest(double toWall, double eDist, double eAngle, int oDir) {
      if (eDist <= toWall) {
         return Double.POSITIVE_INFINITY;
      }
      double wallAngle = FastTrig.acos(-oDir * toWall / eDist) + oDir * HALF_PI;
      return FastTrig.normalAbsoluteAngle(oDir * (wallAngle - eAngle));
   }


 // CREDIT: from CassiusClay, by PEZ
 //   - returns point length away from sourceLocation, at angle
 // robowiki.net?CassiusClay
   public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) {
      return new Point2D.Double(sourceLocation.x + FastTrig.sin(angle) * length,
         sourceLocation.y + FastTrig.cos(angle) * length);
   }

 // got this from RaikoMicro, by Jamougha, but I think it's used by many authors
 //  - returns the absolute angle (in radians) from source to target points
   public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
      return FastTrig.atan2(target.x - source.x, target.y - source.y);
   }
   public static int limit(int min, int value, int max) {
      if(value > max)
         return max;
      if(value < min)
         return min;
    
      return value;
   }
   public static double limit(double min, double value, double max) {
      if(value > max)
         return max;
      if(value < min)
         return min;
    
      return value;
   }
   public static float limit(float min, float value, float max) {
      if(value > max)
         return max;
      if(value < min)
         return min;
    
      return value;
   }

   public static double bulletVelocity(double power) {
      return (20D - (3D*power));
   }



   public void onPaint(java.awt.Graphics2D g) {
      painting = true;
      // if(!MC2K7)
         // dgun.onPaint(g); 
      g.setColor(Color.red);
      _time = bot.getTime();
      
      for(int i = 0; i < _enemyWaves.size(); i++){
         g.setColor(Color.red);
         EnemyWave w = (EnemyWave)(_enemyWaves.get(i));
         int radius = (int)(w.bulletVelocity*(getTime() - w.fireTime));
         Point2D.Double center = w.fireLocation;
         if(radius - 40 < center.distance(_myLocation)){
            // g.drawOval((int)(center.x - radius ), (int)(center.y - radius), radius*2, radius*2);
            if(w.bestBins != null){
               double MEA = w.maxEscapeAngle();
               double max = 0;
               for(int j = 0; j < EnemyWave.BINS; j++)
                  max = Math.max(max,w.bestBins[j]*w.binCleared[j]);
               max = 1/max;
               for(int j = 0; j < EnemyWave.BINS; j++){   
               
                  double thisDanger = w.bestBins[j]*w.binCleared[j]*max;
                  if(w.binCleared[j] == 0)
                     g.setColor(Color.black);
                  else
                     g.setColor(Color.blue);
                  if(thisDanger > 0.1)
                     g.setColor(Color.green);
                  if(thisDanger > 0.3)
                     g.setColor(Color.yellow);
                  if(thisDanger > 0.6)
                     g.setColor(Color.orange);
                  if(thisDanger > 0.9)
                     g.setColor(Color.red);
                  Point2D.Double p1 = project(center, w.directAngle + w.direction*( j - EnemyWave.MIDDLE_BIN)/(double)EnemyWave.MIDDLE_BIN*MEA/0.9, radius-w.bulletVelocity);
                  Point2D.Double p2 = project(center, w.directAngle + w.direction*(j - EnemyWave.MIDDLE_BIN)/(double)EnemyWave.MIDDLE_BIN*MEA/0.9, radius);
                  g.drawLine((int)(p1.x),(int)(p1.y),(int)(p2.x),(int)(p2.y));
               
               
               }
            }
            if(w.imaginary){
               g.setColor(Color.white);  
               g.drawString("imaginary wave in air",100,35);
               g.drawString("velocity: " + w.bulletVelocity,100,25);
               g.drawString("traveled distance: " + w.distanceTraveled,100,15);
            }    
         }
      }
      if(mainWave != null
      && mainWave.safestPoint != null
      && mainWave.safestPoint.predictionStatus != null
      && mainWave.safestPoint.predictionStatus.intersectionIndices != null){
         PlaceTime pt = mainWave.safestPoint;
         if(pt.predictionStatus.intersectionIndices != null){
            g.setColor(Color.white);
            double radius = pt.predictionStatus.endPoint.distance(mainWave.fireLocation);
            Point2D.Double center = mainWave.fireLocation;
            double MEA = mainWave.maxEscapeAngle();
            Point2D.Double p1 = project(center, mainWave.directAngle + mainWave.direction*( pt.predictionStatus.intersectionIndices[0] - EnemyWave.MIDDLE_BIN)/(double)EnemyWave.MIDDLE_BIN*MEA/0.9, radius);
            Point2D.Double p2 = project(center, mainWave.directAngle + mainWave.direction*( pt.predictionStatus.intersectionIndices[1] - EnemyWave.MIDDLE_BIN)/(double)EnemyWave.MIDDLE_BIN*MEA/0.9, radius);
            g.drawLine((int)p1.x,(int)p1.y,(int)p2.x,(int)p2.y);
            g.drawLine((int)p1.x,(int)p1.y,(int)mainWave.fireLocation.x,(int)mainWave.fireLocation.y);
            g.drawLine((int)p2.x,(int)p2.y,(int)mainWave.fireLocation.x,(int)mainWave.fireLocation.y);
            
            g.drawRect((int)pt.predictionStatus.endPoint.x - 18, (int)pt.predictionStatus.endPoint.y - 18, 36,36);
         }
      
      }
      // {  g.setColor(Color.red);
         // for(int i = 0; i < myBullets.size(); i++){
            // Bullet b = myBullets.get(i).b;
            // Point2D.Double p1 = new Point2D.Double(b.getX(), b.getY());
            // Point2D.Double p2 = project(p1,b.getHeadingRadians(),b.getVelocity());
            // g.drawLine((int)(p1.x),(int)(p1.y),(int)(p2.x),(int)(p2.y));
         // }
      // }
      {
         g.setColor(Color.white);  
         g.drawString("enemy gunheat: " + enemyGunHeat,300,15);
         g.drawString("imaginary enemy gunheat" + imaginaryGunHeat,300,5);
         g.drawRect((int)_myLocation.x - 18, (int)_myLocation.y - 18, 36,36);
      }
      
      if(firstPointsPainting != null){
         
         for(int i = 0; i < firstPointsPainting.size(); i++){
            g.setColor(Color.green);
            PlaceTime pt = ((PlaceTime)firstPointsPainting.get(i));
            Point2D.Double goToTarget = pt.place;
            if(pt.predictionStatus != null){
               goToTarget = pt.predictionStatus.endPoint;
               
               
               
               // if(pt.predictionStatus.debug)
                  // g.setColor(Color.red);  
            }
                       
            g.drawOval((int)goToTarget.x - 2, (int)goToTarget.y - 2, 4,4);
         }
      }
      if(lastGoToPoint != null){
         g.setColor(Color.orange);
         g.drawOval((int)lastGoToPoint.x - 3, (int)lastGoToPoint.y - 3, 6,6);
         g.drawOval((int)lastGoToPoint.x - 4, (int)lastGoToPoint.y - 4, 8,8);
      }
      if(secondWave != null && secondWave.possSafePT != null){
         g.setColor(Color.white);
         g.drawOval((int)secondWave.possSafePT.place.x - 3, (int)secondWave.possSafePT.place.y - 3, 6,6);
      }
      if(nextPointsPainting != null){
         g.setColor(Color.pink);
         for(int i = 0; i < nextPointsPainting.size(); i++){
            PlaceTime pt =  ((PlaceTime)nextPointsPainting.get(i));
            Point2D.Double goToTarget;
            if(pt.predictionStatus == null)
               goToTarget = pt.place;
            else
               goToTarget = pt.predictionStatus.endPoint;
            g.drawOval((int)goToTarget.x - 2, (int)goToTarget.y - 2, 4,4);
         }
      }
      
      if(lastScan != null){
         g.setColor(Color.yellow);
         int bft = (int)(_enemyLocation.distance(_myLocation)/14);  
         if(!eMove.initialised())
            eMove.predict();
         
         for(int i = 0; i < bft; i++){
            Point2D.Double p = eMove.get((int)getTime() + i);
            if(p == null)
               break;
            g.drawOval((int)p.x - 2, (int)p.y - 2, 4,4);
         
         }
      }
   	
   //          if(mainWave != null && mainWave.possPoints != null){
   //             g.setColor(Color.red);
   //             for(int i = 0; i < mainWave.possPoints.size(); i++){
   //                Point2D.Double goToTarget = ((PlaceTime)mainWave.possPoints.get(i)).place;
   //                g.drawOval((int)goToTarget.x - 2, (int)goToTarget.y - 2, 4,4);
   //             }
   //          }
   //          if(mainWave != null && mainWave.possSafePT != null){
   //             g.setColor(Color.magenta);
   //             g.drawOval((int)mainWave.possSafePT.place.x - 3, (int)mainWave.possSafePT.place.y - 3, 6,6);
   //          
   //          }
      // g.setColor(Color.white);
   
   }
}   

class Scan implements Cloneable{
   static float 
      latVelWeight = 30,
      advVelWeight = 15,
   	bftWeight = 12,
      forwardWallWeight = 18,
      reverseWallWeight = 7,
      lastVelWeight = 4,
   	accelWeight = 10,
      timeSinceDecelWeight = 8,
      timeSinceDirChangeWeight = 8,
      distLast10Weight = 8,
   	timeWeight = 5/1000.0f;
    
    
    
   
   float latVel,
      advVel,
      bft,
   	accel,
      forwardWall,
      reverseWall,
      lastVel,
      timeSinceDecel,
      timeSinceDirChange,
      distLast10;
   int pointInTime;
   int index;
   
   public Object clone(){
      Scan s = new Scan();
      s.latVel = latVel;
      s.advVel = advVel;
      s.bft = bft;
      s.accel = accel;
      s.forwardWall = forwardWall;
      s.reverseWall = reverseWall;
      s.lastVel = lastVel;
      s.timeSinceDecel = timeSinceDecel;
      s.timeSinceDirChange = timeSinceDirChange;
      s.distLast10 = distLast10;
      s.pointInTime = pointInTime;
      s.index = index;
      return s;
   }
      

   double[] location(){
      return new double[]{
            latVel*latVelWeight,
            advVel*advVelWeight,
            bft*bftWeight,
            timeSinceDirChange*timeSinceDirChangeWeight,
            timeSinceDecel*timeSinceDecelWeight,
            accel*accelWeight,
            distLast10*distLast10Weight,
            forwardWall*forwardWallWeight,
            reverseWall*reverseWallWeight,
            // pointInTime*timeWeight
            };
   
   
   
   }
}

