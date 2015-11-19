package jp;
import robocode.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.awt.Graphics2D;
import java.awt.Color;

/**
 * Perpy - a robot by James Picone - a1148734
 * Perpy is a one-on-one bot that moves perpendicularly around the target, and uses circular targeting
 */
public class Perpy extends AdvancedRobot {
	//Constants
	private final double MOVE_FORWARDS=100; //Amount to move forwards
	private final double TARGETING_ITERATIONS=10; //How far ahead to look to determine time-to-hit for iterative targeting
	private final int PATTERN_HISTORY_LENGTH=500; //How many ticks of scans to keep for pattern-matching data.
	private final int PATTERN_RECENTHISTORY_LENGTH=15; //How many ticks of scans count as 'recent' history for the PM gun
	private final int PATTERN_THRESHOLD=40; //What 'closeness' score is the absolute threshold for a pattern to be found. PM gun, again
	private final double GUN_HEAT_THRESHOLD=0.3;
	private final int WAVESURFING_BINS=29; //Number of wavesurfing bins to keep
	private final int WAVESURFING_BIN_FACTOR=WAVESURFING_BINS/2; //A number that turns up in converting GFs to WS bins.
	private final int WAVESURFING_ACCELERATING=2;
	private final int WAVESURFING_DECELERATING=0;
	private final int WAVESURFING_CONSTANT=1;
	private final int WAVESURFING_LATV_SEGS=3;
	private final double BLIND_MAN_STICK=150; //How much space we try to keep between us and the wall.
	private final double WALLSMOOTH_INCREMENT=Math.PI/16; //How large an angle we had to our turn to try and get away from the wall when wall-smoothing
	private final double WALLSMOOTH_LIMIT=Math.PI; //Math.PI/3; //The amount by which our smoothing angle can change before we consider it a dead-end and just reverse.
	private double DIST_PREFERRED=400; //Our preferred distance to keep from the enemy
	private final double DIST_INCREMENT=20;
	private final double DIST_THRESHOLD=5;
	private final double DIST_MULT=200; //A factor used in finding our preferred distance.
	private final double PI_ON_2=Math.PI*0.5;
	private final double TWO_PI=2*Math.PI;
	private final double SCAN_THRESHOLD=20;
		
	//State
	private int headdir=1; //The direction we're travelling around the target. Flipping the sign of this is the main means of dodging bullets for Perpy.
	private int radarspindir=1; //Which direction we're spinning the radar in. Every time we lose track of the enemy, this value flips sign - it helps to retrieve a lock faster.
	private double bulletpower=3; //The power we're using when firing. Changing this at runtime may cause some contamination of GF stats.
	private GuessFactorGun ggun=new GuessFactorGun(); //A reference to the bot's GuessFactorGun object.
	private CircularGun cgun=new CircularGun(); //A reference to the bot's CircularGun
	private Gun hgun=new HeadOnGun(); //A reference to the bot's head-on-targeting gun
	private PatternMatchingGun pgun=new PatternMatchingGun(); //A reference to the bot's pattern-matching gun.
	private Enemy enemy=null; //A reference to the variable used to store enemy statistics
	private ArrayList enemies=new ArrayList();
	private ArrayList bullets=new ArrayList(); //An arraylist used to store waves, which are used for collecting GuessFactorGun data
	private ArrayList enemywaves=new ArrayList(); //An arraylist of waves fired off by the enemy - finding the safe bits of these waves is how Perpy's movement works
	private ArrayList vguns=new ArrayList(); //An arraylist used to store the different guns I can use to fire.
	private double bh; //The battlefield height
	private double bw; //The battlefield width
	private double goal_angle=0; //The angle we're trying to drive at this tick. This may be changed at runtime by wall-smoothing - trying to avoid the wall.
	private boolean fire_gun=false; //Do we fire the gun next tick?
	private double dist_selfhits=0;
	private double dist_enemyhits=0;
	private boolean melee=false;
	private double lastscan=0;
	private boolean victorydance=false;
	private boolean stopping=false;
	
	//Battle stats.
	private int bulletsfired=0; //Number of bullets fired
	private int bulletshit=0; //Number of times we hit
	private int timeshit=0; //Number of times we were hit
	private double[][][][] ws_bins=new double[WAVESURFING_LATV_SEGS+1][3][10][WAVESURFING_BINS]; //This array stores data about where we've been hit - stocking it provides fodder for our wavesurfing algorithm.
	private ArrayList ws_dirs=new ArrayList(); //Stores my direction of travel over the last three ticks
	private ArrayList ws_bear=new ArrayList(); //Stores the absolute bearing from my enemy to me over the last three ticks
	private ArrayList ws_vel=new ArrayList();
	private ArrayList ws_latv=new ArrayList();
	
	//saving
	private static RoundMemory savedata;
	
	public void onPaint(Graphics2D g) { //Used for debugging - drawing the pretty waves I make, as well as enemywaves, virtual bullets, my pattern-matching gun's predictions, my 'blind man's stick', used for avoiding the walls, where I think the enemy is, and where the enemy could possibly escape to if I fired now.
		g.setPaint(Color.WHITE); //Draw the stick in white.
		double stick=BLIND_MAN_STICK*headdir; //Calculate where to stick the stick, as it were.
		g.draw(new Line2D.Double(getX(),getY(),getX()+Math.sin(getHeadingRadians())*stick,getY()+Math.cos(getHeadingRadians())*stick)); //It's a line of length BLIND_MAN_STICK, in front of me.
		g.setPaint(Color.RED); //Paint the waves in red.
		double r=0; //The radius of the wave we're painting
		for(int a=0;a<bullets.size();a++) {//Loop through the contents of the bullets array
			Wave w=(Wave) bullets.get(a); //Recover the Wave object
			r=bulletV(w.power)*(getTime()-w.timefired); //Work out how far it's travelled in the time since it was created
			g.draw(new Ellipse2D.Double(w.ix-r,w.iy-r,r*2,r*2)); //Draw it.
		}
		for(int a=0;a<vguns.size();a++) {//Loop through the contents of the virtual guns array
			Gun gun=(Gun) vguns.get(a); //Recover the Gun object
			gun.drawVBullets(g); //Draw its virtual bullets
		}
		g.setPaint(Color.BLUE); //Draw the enemy's waves in blue.
		for(int a=0;a<enemywaves.size();a++) {//Loop through the contents of the bullets array
			EnemyWave w=(EnemyWave) enemywaves.get(a); //Recover the Wave object
			r=bulletV(w.power)*(getTime()-w.timefired); //Work out how far it's travelled in the time since it was created
			g.draw(new Ellipse2D.Double(w.ix-r,w.iy-r,r*2,r*2)); //Draw it.
		}
		g.setPaint(Color.GREEN); //Draw enemy-related data in green.
		if(enemy!=null) {//If we don't have an enemy, we should skip this stuff
			g.draw(new Rectangle2D.Double(enemy.ex-18,enemy.ey-18,36,36)); //Draw where we think the enemy is as a square of side length 36 - if a bullet gets in here, the tank is hit.
			double b = Math.atan2(enemy.ey-getY(),enemy.ex-getX()); //The absolute bearing from us to the enemy.
			double endx1=getX()+Math.sqrt(bw*bw+bh*bh)*Math.cos(b+maxEscapeAngle(bulletpower));
			double endy1=getY()+Math.sqrt(bw*bw+bh*bh)*Math.sin(b+maxEscapeAngle(bulletpower));
			double endx2=getX()+Math.sqrt(bw*bw+bh*bh)*Math.cos(b-maxEscapeAngle(bulletpower));
			double endy2=getY()+Math.sqrt(bw*bw+bh*bh)*Math.sin(b-maxEscapeAngle(bulletpower)); //These work out the coordinates of the endpoint of a line starting from me and going outside the battlefield, representing the maximum angle that the enemy could possibly travel in.
			g.draw(new Line2D.Double(getX(),getY(),endx1,endy1));
			g.draw(new Line2D.Double(getX(),getY(),endx2,endy2)); //Draw those lines.
			pgun.drawTargeting(enemy,g); //Draw our pattern-matching gun's predictions of our enemy's movement.
		}
		if(enemywaves.size()>0) {
			double leastTimeToHit=999;
			int bestfact=0;
			for(int i=0; i<enemywaves.size(); i++) {
				EnemyWave currwave=(EnemyWave) enemywaves.get(i);
				double d=dist(currwave.ix, currwave.iy, getX(), getY());
				double timeTaken=d/bulletV(currwave.power);
				double timeGone=getTime()-currwave.timefired;
				double timeLeft=timeTaken-timeGone;
				if(timeLeft>0&&timeLeft<leastTimeToHit) {bestfact=i; leastTimeToHit=timeLeft;}
			}
			
			EnemyWave currwave=(EnemyWave) enemywaves.get(bestfact);
			Point2D.Double currpos=predictLoc(currwave,8, headdir);
			g.draw(new Ellipse2D.Double(currpos.getX(),currpos.getY(),10,10));
			currpos=predictLoc(currwave,8,-headdir);
			g.setPaint(Color.BLUE);
			g.draw(new Ellipse2D.Double(currpos.getX(),currpos.getY(),10,10));
			currpos=predictLoc(currwave,0,headdir);
			g.setPaint(Color.RED);
			g.draw(new Ellipse2D.Double(currpos.getX(),currpos.getY(),10,10));			
		}
			
	}
	
	public void run() { //Does all actual action, calls all the wavesurfing stuff, does targeting, etc.
		//Initialisation
		setColors(Color.RED,Color.RED,Color.RED); //Make us red.
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true); //Make gun/radar/body seperate entities
		bh=getBattleFieldHeight(); //Set the battlefield height and width state variables
		bw=getBattleFieldWidth();
		
		vguns.add(cgun);
		vguns.add(ggun);
		vguns.add(hgun);
		vguns.add(pgun); //Add our guns to the virtual guns array.
		
		if(savedata!=null) {
			loadData();
		}
		else {
			for(int j=0; j<WAVESURFING_LATV_SEGS+1; j++) {
				for(int i=0; i<3; i++) {
					for(int k=0; k<10; k++) {
						addToWSBins(j, i, k*100,(int) WAVESURFING_BIN_FACTOR,3);
					} //Pre-stock our wavesurfing data with a fake power 3 'hit' at GF 1 and GF 0 - That's intended to have us avoiding HOT, CT, and LT right from the start.
				}
			}
		}
			
		while(true) {//To infinity... AND BEYOND. Infinite because we want the bot to keep going until it gets killed, which kills this process.
			enemy=getBestEnemy();
			ws_vel.add(0,new Double(getVelocity()));
			if(ws_vel.size()>2) {ws_vel.remove(2);}
			if(getOthers()>1) {melee=true;}
			else {melee=false;}
			
			if(enemy!=null){
				double latv=getVelocity()*Math.sin(getHeadingRadians()-Math.atan2(enemy.ey-getY(), enemy.ex-getX()));
				ws_latv.add(0, new Double(latv));
				if(ws_latv.size()>2) {ws_latv.remove(2);}
			}
				
			doRadar(); //Runs the radar routine, which keeps the radar right on our enemy.	
			if(!melee) {
				iterateWaves(); //Loop over all the waves, check for waves hitting the enemy, their waves hitting me, and update all appropriate statistics.
				determineDist();
			}
			
			goal_angle=0;
			if(enemy!=null) { //If we have an enemy, run some targeting procedures.				
				if(!melee) {goal_angle=enemy.turnToFace();} //Turns us so we're perpendicular to the enemy - also takes account how far from the enemy we want to be
				iterateVGuns(); //Update all our virtual bullets.
				if(getGunHeat()<GUN_HEAT_THRESHOLD) {
					if(fire_gun) {setFire(bulletpower); fire_gun=false;}
					determineBPower();
					Gun g=(Gun) vguns.get(pickBestGun()); //Get the gun that's got the most hits on the enemy so far.
					g.targeting(enemy,false); //Use it.
				}
			}
			if(melee) {determineAngleMelee();}
			goal_angle=wallSmooth(goal_angle, new Point2D.Double(getX(), getY()), getHeadingRadians(), headdir, false); //Change goal_angle slightly if we're going to run into the wall soon
			stopping=false;
			setMaxVelocity(8);
			if(!melee) {doSurfing();} //Decides whether to switch directions or not, depending on statistical data about where we've been hit.
			if(victorydance) {victoryDance();}
			if(stopping) {setMaxVelocity(0);}
			setTurnRightRadians(normalRelativeAngle(goal_angle)); //Start turning.
			setAhead(MOVE_FORWARDS*headdir); //Move. Turning is done elsewhere.
			execute(); //Run all the queued-up events.
		}
	}
	
	public void victoryDance() {
		goal_angle=Math.sin(getTime()*Math.PI*0.1)*Math.PI*0.5;
		headdir=sign(Math.cos(getTime()*Math.PI*0.2));
		setTurnRadarRightRadians(Math.PI*2);
		setTurnGunLeftRadians(Math.PI*2);
	}
	
	public void determineAngleMelee() {
		double cx=dist(getX(),getY(),0,getY())-dist(bw,getY(),getX(),getY());
		double cy=dist(getX(),getY(),getX(),0)-dist(getX(),bh,getX(),getY());
		Enemy tempenemy;
		for(int a=0; a<enemies.size(); a++) {
			tempenemy=(Enemy) enemies.get(a);
			double length=1/dist(tempenemy.ex, tempenemy.ey, getX(), getY());
			double angle=Math.atan2(getY()-enemy.ey, getX()-enemy.ex);
			cx+=length*Math.sin(angle)*headdir;
			cy+=length*Math.cos(angle)*headdir;
		}
		goal_angle=Math.atan2(cy,cx)-getHeadingRadians();		
	}
	
	public Enemy getBestEnemy() {
		double bestdist=999;
		int bestindex=0;
		Enemy temp=null;
		if(enemies.size()<1) {return null;}
		for(int a=0; a<enemies.size(); a++) {
			temp=(Enemy) enemies.get(a);
			if(dist(getX(),getY(),temp.ex,temp.ey)<bestdist) {bestindex=a; bestdist=dist(getX(), getY(), temp.ex, temp.ey);}
		}
		return (Enemy) enemies.get(bestindex);
	}
	
	public void determineBPower() {
		if(melee) {bulletpower=3; return;}
		double hitrate=100*(((double) bulletshit)/((double) bulletsfired));
		if(hitrate>16&&bulletsfired>0) {bulletpower=3; return;}
		bulletpower=Math.max(0.1, Math.min(3, getEnergy()*3/100));
	}
	
	public void determineDist() {
		//if(dist_selfhits==0||dist_selfhits+dist_enemyhits<DIST_THRESHOLD) {return;}
		//double dfactor=dist_enemyhits/dist_selfhits;
		//if(dfactor<1) {DIST_PREFERRED+=DIST_INCREMENT;}
		//else {DIST_PREFERRED-=DIST_INCREMENT;}
		//dist_selfhits=0;
		//dist_enemyhits=0;
	}
	
	public void addToWSBins(double latv, int acc, double d, int bin, double p) { //Add to a wavesurfing bin, taking into account distance segmentation and bin smoothing.
		if(melee) {return;}
		d*=0.01; //Divide distance by 100, but faster - fits it into a segment.
		d=(int) d;
		d=Math.max(0,Math.min(d,9));
		latv=(int) determineLatVSeg(latv);
		for(int b=0; b<WAVESURFING_BINS; b++) { //Loop through all our bins.
			double k=(1+Math.abs(b-bin)); // one over this value squared is the value of p that is added to the bin.
			ws_bins[(int) latv][acc][(int) d][b]+=p*bin/(k*k); //Add to the appropriate bin an amount of the value we're adding to the actual bin we were hit in. The ratio depends on the distance from the actual hit, and it falls off as 1/x**2
		}
	}
	
	public int determineLatVSeg(double latv) {
		latv=Math.abs(latv);
		latv=latv*WAVESURFING_LATV_SEGS*0.125;
		return (int) latv;
	}
	
	public void doRadar() { //Try to keep a radar lock on our enemy, or just spin it if we don't have one.
		if(getTime()-lastscan>SCAN_THRESHOLD||melee||enemy==null) {
			setTurnRadarLeftRadians(Math.PI*2*radarspindir); //We have no enemy, just spin the radar a full circle.
			return;
		}
		//The next bit assumes the enemy is moving in a regular arc, which is probably true enough on the timescales we're talking about. Basically, it's circular targeting with the radar beam.
		
		double diff=(getTime()-enemy.lastscan); //Work out how long since the enemy was last scanned.
		double rad = enemy.velocity/enemy.avgturn; //Work out how large their turn radius is
		if(enemy.avgturn==0) {rad=0;} //If they haven't turned at all, set radius to 0. This avoids getting a NaN when they're moving in a straight line.
		double change = diff * enemy.avgturn; //Work out how much their heading will change over 'diff' ticks.
		double ey = enemy.ey + (Math.sin(enemy.lastheading + change) * rad) - (Math.sin(enemy.lastheading) * rad);
		double ex = enemy.ex + (Math.cos(enemy.lastheading) * rad) - (Math.cos(enemy.lastheading + change) * rad); //Work out their x and y coordinates in diff ticks. This is basic trig.
		double px=getX()+getVelocity()*Math.sin(getHeadingRadians());
		double py=getY()+getVelocity()*Math.cos(getHeadingRadians()); //Work out where I'll be in a tick.
		double ang=normalRelativeAngle(getRadarHeadingRadians()-(Math.PI/2-Math.atan2(ey-py,ex-px))); //Works out the angle between where I'll be and where they'll be, and then finds how much I need to turn the radar from that.
		setTurnRadarLeftRadians(ang); //Sets up the radar turn.
	}
	
	public void doSurfing() { //Decide whether or not to turn around based on a statistical guesstimate of our danger if we keep going and if we flip.
		if(enemywaves.size()<1) {return;} //If our enemy hasn't fired any relevant bullets (That we've noticed) no point asking
		double keepdanger=evaluateDanger(headdir,8);
		double flipdanger=evaluateDanger(-headdir,8); //This works out the danger involved in flipping and the danger involved in going the same direction.
		double stopdanger=evaluateDanger(headdir, 0);
		if(stopdanger<keepdanger&&stopdanger<flipdanger) {stopping=true;}
		if(keepdanger>flipdanger) {wallAvoidHit();} //Quite simply, if going straight is more dangerous then flipping, flip.
	}
	
	public void enemyFiredBullet(Enemy e, double ediff) { //Add the appropriate wave to our set of waves.
		if(ws_bear.size()<3||melee) {return;}
		enemywaves.add(new EnemyWave(e.ex,e.ey,((Double) ws_bear.get(2)).doubleValue(),ediff,getTime()-1,((Integer) ws_dirs.get(2)).intValue(),getAcceleration(), ((Double) ws_latv.get(1)).doubleValue())); //Note that we see the bullet a tick after it's been fired - hence the subtraction of one from getTime()
	}
	
	public double evaluateDanger(int dir,double maxV) { //Work out how dangerous it is for me to go in direction 'dir', statistically.
		double currdanger=0; //We keep adding to this value to get the total danger value.
		double leastTimeToHit=999;
		int bestfact=0;
		for(int i=0; i<enemywaves.size(); i++) {
			EnemyWave currwave=(EnemyWave) enemywaves.get(i);
			double d=dist(currwave.ix, currwave.iy, getX(), getY());
			double timeTaken=d/bulletV(currwave.power);
			double timeGone=getTime()-currwave.timefired;
			double timeLeft=timeTaken-timeGone;
			if(timeLeft>0&&timeLeft<leastTimeToHit) {bestfact=i; leastTimeToHit=timeLeft;}
		}
		
		EnemyWave currwave=(EnemyWave) enemywaves.get(bestfact);
		double d=dist(currwave.ix, currwave.iy, getX(), getY());
		Point2D.Double currpos=predictLoc(currwave,maxV, dir);
		double currangle=Math.atan2(currpos.getY()-currwave.iy, currpos.getX()-currwave.ix)-currwave.angle;
		currangle=normalRelativeAngle(currangle);
		double distfact=Math.sqrt(getTime()-currwave.timefired);
		currangle/=maxEscapeAngle(currwave.power)*-currwave.dir;
		//if(dir!=headdir) {
		//	log("Reversing puts me at GF "+currangle);
		//}
		//else {
		//	log("Going forwards puts me at GF "+currangle);
		//}
		d*=0.01;
		d=Math.max(0,Math.min(9,d));
		currangle++;
		currangle*=WAVESURFING_BIN_FACTOR;
		currangle=Math.max(Math.min(currangle,WAVESURFING_BINS-1),0);
		currdanger+=ws_bins[determineLatVSeg(currwave.latv)][currwave.accfactor][(int) d][(int) currangle]*distfact;
		return currdanger;
	}
	
	public void fireVBullets() { //Fires a virtual bullet from every gun.
		if(vguns.size()<1) {return;} //No guns, don't bother.
		for(int a=0;a<vguns.size();a++) { //Loop through everything in the vguns ArrayList.
			Gun g=(Gun) vguns.get(a); //Recover the Gun object.
			if(!melee||g.use_melee) {g.fireVBullet(enemy);} //Fire off virtual bullets
		}
	}
	
	private int getAcceleration() {
		if(ws_vel.size()<2) {return WAVESURFING_CONSTANT;}
		Double d1=(Double) ws_vel.get(0);
		Double d2=(Double) ws_vel.get(1);
		double acc=d1.doubleValue()-d2.doubleValue();
		return sign(acc)+1;
	}
	
	public void iterateVGuns() { //Loops over all our virtual bullets, and checks if they've hit.
		if(vguns.size()<1) {return;} //If we have virtual guns, through some freak accident, skip it.
		for(int a=0;a<vguns.size();a++) { //Loop through everything in the vguns ArrayList.
			Gun g=(Gun) vguns.get(a); //Recover the Gun object.
			g.checkVBullets(enemy); //Check the gun's virtual bullets.
		}
	}
	
	public void iterateWaves() { //Loops over all our waves, as well as our enemy waves, and checks if they've hit anything.
		for(int a=0;a<bullets.size();a++) { //Loop through everything in the bullets ArrayList.
			Wave w=(Wave) bullets.get(a); //Recover the Wave object.
			if(w.checkHit(enemy)||w.outOfRange()) {bullets.remove(w); a--;} //Check if it's hit the enemy. If it has, remove it from the ArrayList. a is decremented because the ArrayList will shrink by one otherwise, and this could cause problems if not accounted for.
		}
		for(int a=0;a<enemywaves.size();a++) { //Loop through everything in the enemywaves ArrayList.
			EnemyWave w=(EnemyWave) enemywaves.get(a); //Recover the EnemyWave object.
			if(w.checkHit()) {enemywaves.remove(w); a--;} //Check if it's hit me. If it has, remove it from the ArrayList. a is decremented because the ArrayList will shrink by one otherwise, and this could cause problems if not accounted for.
		}
	}
	
	private void loadData() {
		DIST_PREFERRED=savedata.saved_dist;
		ws_bins=savedata.saved_ws.clone();
		ggun.bins=savedata.saved_gf.clone();
		for(int a=0; a<4; a++) {
			Gun g=(Gun) vguns.get(a);
			g.timesfired=savedata.saved_vg[0][a];
			g.numhits=savedata.saved_vg[1][a];
		}
	}
	
	private double normalRelativeAngle(double angle) {//Puts an angle into the range -PI to PI. Useful when finding out the minimum amount of turning we can get away with on gun/radar/body
		if (angle > -Math.PI && angle <= Math.PI) {return angle;} //It's already in the range, just return it.
		while (angle <= -Math.PI) {angle += TWO_PI;} //If it's less then -PI, add 2PI 'till it's bigger
		while (angle > Math.PI) {angle -= TWO_PI;} //If it's bigger then PI, subtract 2PI 'till it isn't.
		return angle;
	}
	
	public int pickBestGun() { //Return the index of our best virtual gun.
		int bestindex=0; 
		double bestvalue=0; //By default, use the gun in index 0 - that is, Circular targeting. But if anything else has any hits, it'll override it.
		Gun g=null;
		for(int a=0;a<vguns.size();a++) { //Loop over the vguns array.
			g=(Gun) vguns.get(a); //Recover the Gun object.
			if(g.getHitRate()>bestvalue) {bestindex=a; bestvalue=g.getHitRate();} //If it's got a better hitrate then our current best game, then flip over
		}
		return bestindex;
	}
	
	private Point2D.Double predictLoc(EnemyWave currwave, double maxV, int d) {
		if(enemy==null) {return new Point2D.Double(getX(), getY());}
		double accel=0;
		double virtV=getVelocity();
		double virtH=getHeadingRadians();
		double rot=0;
		double ang=0;
		Point2D.Double virtPos=new Point2D.Double(getX(), getY());
		double timetot=dist(virtPos.getX(), virtPos.getY(), currwave.ix, currwave.iy)/bulletV(currwave.power);
		for(double left=timetot-(getTime()-currwave.timefired); left>0; left--) {
			ang=enemy.turnToFace(virtPos,d);
			//d=wallSmoothDirChange(ang, virtPos, virtH, d);
			ang=wallSmooth(ang, virtPos, virtH, d, true);
			rot=(Math.PI*0.0625-Math.PI*0.00416666*Math.abs(virtV))*sign(ang);
			if(Math.abs(rot)>Math.abs(ang)) {rot=ang;}
			virtH+=rot*sign(virtV);
			if(sign(virtV)!=d) {accel=2;}
			else {accel=1;}
			if(virtV<-maxV) {accel=2*d;}
			if(virtV>maxV) {accel=-2*d;}
			virtV=Math.max(Math.min(virtV+accel*d,8),-8);
			virtPos=new Point2D.Double(virtPos.getX()+Math.sin(virtH)*virtV,virtPos.getY()+Math.cos(virtH)*virtV);
		}
		return virtPos;
	}
	
	private void printStats() { //Print out statistics on our often we were hit, and how often we hit the opponent, as well as the hitrate of various guns
		double hitrate=100*(((double) bulletshit)/((double) bulletsfired)); //Work out what percentage of the bullets we fired hit our intended target
		log("Number of bullets fired: "+bulletsfired); 
		log("Number of successful hits: "+bulletshit);
		log("Hitrate: "+hitrate+"%");
		log("Number of times hit: "+timeshit); //Print some stats out.
		for(int a=0; a<vguns.size(); a++) { //Loop through all our guns and get their hitrates
			Gun g = (Gun) vguns.get(a);
			log(g.name+" hitrate: "+g.getHitRate()*100+"%");
		}
		saveData();
	}
	
	private void saveData() {
		savedata=new RoundMemory();
		savedata.saved_dist=DIST_PREFERRED;
		savedata.saved_ws=ws_bins.clone();
		savedata.saved_gf=ggun.bins.clone();
		for(int a=0; a<4; a++) {
			Gun g=(Gun) vguns.get(a);
			savedata.saved_vg[0][a]=g.timesfired;
			savedata.saved_vg[1][a]=g.numhits;
		}
	}
	
	private void wallAvoidHit() { //Called when I want to reverse direction. Name is for hysterical raisins.
		headdir*=-1; //Reverse our direction of travel.
	}
	
	private double wallSmooth(double a,Point2D.Double currpos, double h, int d, boolean is_predicting) { //Tries to keep us away from the wall by increasing our turning angle slightly if we're going to be inside a wall soonish. It's a simple iterative algorithm - there's probably some way to do it non-iteratively, but the added complexity isn't worth the extra speed as of now.
		double last=a;	
		while(willHitWall(a,currpos,h,d)) {a+=WALLSMOOTH_INCREMENT*d; if(Math.abs(a-last)>=WALLSMOOTH_LIMIT&&!melee) {a=last; if(!is_predicting) {wallAvoidHit();}; return a;}} //If our BLIND_MAN_STICK is inside the wall, increase our goal angle by a small increment. Keep doing this until it's out.
		return a; //Make sure we're turning as little as possible.
	}
		
	private int wallSmoothDirChange(double a,Point2D.Double currpos, double h, int d) { //Tries to keep us away from the wall by increasing our turning angle slightly if we're going to be inside a wall soonish. It's a simple iterative algorithm - there's probably some way to do it non-iteratively, but the added complexity isn't worth the extra speed as of now.
		double last=a;		
		while(willHitWall(a,currpos,h,d)) {a+=WALLSMOOTH_INCREMENT*d; if(Math.abs(a-last)>=WALLSMOOTH_LIMIT) {return -d;}} //If our BLIND_MAN_STICK is inside the wall, increase our goal angle by a small increment. Keep doing this until it's out.
		return d; //Make sure we're turning as little as possible.
	}
	
	public double waveToGF(EnemyWave currwave) {
		double currangle=Math.atan2(getY()-currwave.iy,getX()-currwave.ix);
		currangle-=currwave.angle;
		currangle=normalRelativeAngle(currangle);
		currangle/=maxEscapeAngle(currwave.power);
		currangle++;
		currangle*=WAVESURFING_BIN_FACTOR*currwave.dir*headdir;
		return currangle;
	}
	
	private boolean willHitWall(double a,Point2D.Double currpos, double h, int d) { //Works out if our BLIND_MAN_STICK is in the wall - basically, if there's a wall that many pixels ahead of us.
		double distance=BLIND_MAN_STICK*d; //How far the BLIND_MAN_STICK stretches
		double xend=currpos.getX()+distance*Math.sin(h+a);
		double yend=currpos.getY()+distance*Math.cos(h+a); //The x and y coords of the BLIND_MAN_STICK's endpoint
		if((xend>=bw-18) || (xend<=18) || (yend>=bh-18) || (yend<=18)) {return true;} //If that point is outside the battlefield, the stick is outside. (A bot is 18 units wide)
		return false;  //clearly, if it gets here, it's inside the battlefield
	}
	
	public void onBulletHit(BulletHitEvent e) { //If we hit an enemy with a bullet, update our stats, and subtract from our estimate of our enemy's energy, just to keep it up to date.
		bulletshit++; //Update stats.
		if(enemy!=null) { //Make sure we actually have an enemy.
			if(enemy.name.equals(e.getName())) {enemy.lastenergy=e.getEnergy();}
			dist_enemyhits+=e.getBullet().getPower();
		}
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent b) { //Once again, use this to collect wavesurfing stats.
		Bullet e=b.getHitBullet(); 
		if(e.getName().equals(getName())) {e=b.getBullet();} //Find the bullet our enemy fired
		if(enemywaves.size()<1) {return;} //If we have no waves, there isn't one that matches.
		EnemyWave closest=null;
		double cdist=bh*bh+bw*bw;
		for(int a=0; a<enemywaves.size(); a++) {
			EnemyWave w=(EnemyWave) enemywaves.get(a);
			if(Math.abs(e.getPower()-w.power)<0.05) {
				double xf=w.ix-e.getX();
				double yf=w.iy-e.getY();
				double d=Math.abs(xf*xf+yf*yf-((getTime()-w.timefired)*bulletV(w.power)));
				if(d<cdist) {cdist=d; closest=w;}
			}
		} //Yeah, you've seen all this before
		if(closest==null) {log("Couldn't find enemywave that matches bullet collision"); return;}
		double bheading=PI_ON_2-e.getHeadingRadians();
		bheading-=closest.angle;
		bheading=normalRelativeAngle(bheading);
		bheading/=maxEscapeAngle(e.getPower());
		bheading*=-closest.dir;
		log("Two bullets collided at GF "+bheading);
		bheading+=1;
		if(bheading<0||bheading>2) {return;}
		bheading*=WAVESURFING_BIN_FACTOR;
		double xf=closest.ix-e.getX();
		double yf=closest.iy-e.getY();
		double d=Math.sqrt(xf*xf+yf*yf);
		addToWSBins(closest.latv, closest.accfactor, d, (int) bheading, e.getPower());
	}
	
	public void onHitByBullet(HitByBulletEvent e) { //Update our statistics, including wavesurfing stats.
		timeshit++; //We've got another hit on record.
		dist_selfhits+=e.getPower();
		if(enemywaves.size()<1) {return;} //If we don't have any enemy wave to match it to, ignore it.
		EnemyWave closest=null; //Our closest EnemyWave to a match
		double cdist=bh*bh+bw*bw; //How far away the closest EnemyWave is.
		for(int a=0; a<enemywaves.size(); a++) { //Loop through all our EnemyWaves.
			EnemyWave w=(EnemyWave) enemywaves.get(a); //Get the EnemyWave.
			if(Math.abs(e.getPower()-w.power)<0.05) { //If the power of that wave isn't really close to the power of the bullet, don't bother.
				double xf=w.ix-getX();
				double yf=w.iy-getY();
				double d=Math.abs(xf*xf+yf*yf-((getTime()-w.timefired)*bulletV(w.power))); //Else, find out how close it is (Technically, the square, because that'll do well enough for comparisions and is faster)
				if(d<cdist) {cdist=d; closest=w;} //If it's closer to us then our current bullet, that's the wave we want.
			}
		}
		if(closest==null) {log("Couldn't find enemywave that matches bullet hit"); return;} //If we couldn't find a wave to match it to, alert me to it.
		double bheading=PI_ON_2-e.getHeadingRadians(); //Work out which angle the bullet was travelling at - the Math.PI/2 is because robocode trig is north-at-0, increases clockwise, which is backwards. This rectifies that.
		bheading-=closest.angle;
		bheading=normalRelativeAngle(bheading);
		bheading/=maxEscapeAngle(e.getPower()); //Get it as a ratio of the maximum angle I could have been hit at, given the power with which it was fired.
		bheading*=-closest.dir; //Get the appropriate sign, given my current heading and the way I was heading when the wave was fired.
		log("Hit at GF "+bheading); //Log the GF I was hit at to the terminal.
		bheading+=1; //Get the GF from 0 to 2
		if(bheading<0||bheading>2) {return;} //If the GF is out of range, we've clearly got the wrong wave, so we'll just drop it.
		bheading*=WAVESURFING_BIN_FACTOR;  //Convert the heading to a bin value
		double xf=closest.ix-getX();
		double yf=closest.iy-getY();
		double d=Math.sqrt(xf*xf+yf*yf); //Get the distance it travelled, for segmentation purposes.
		addToWSBins(closest.latv, closest.accfactor, d, (int) bheading, e.getPower()); //Finally, drop it in the array.
	}
	
	public void onRobotDeath(RobotDeathEvent r) {
		for(int a=0; a<enemies.size(); a++) {
			if(r.getName().equals(((Enemy) enemies.get(a)).name)) {enemies.remove(a); break;}
		}
	} //If our enemy becomes an ex-enemy, make sure we don't keep trying to shoot them. That could be embarrassing.

	public void onScannedRobot(ScannedRobotEvent e) { //Updates our scan of the enemy, and makes a new enemy object if we don't have one.
		ws_dirs.add(0,new Integer(headdir));
		lastscan=getTime();
		Enemy tempenemy=getEnemy(e.getName());
		if(tempenemy==null) {enemies.add(new Enemy(e.getName(),e.getEnergy(),e.getHeadingRadians(),getTime(),e.getBearingRadians(),e.getDistance(),e.getVelocity()));}
		else {tempenemy.newScan(e);}
	}
	
	public Enemy getEnemy(String name) {
		if(enemies.size()<1) {return null;}
		for(int a=0;a<enemies.size();a++) {
			if(name.equals(((Enemy) enemies.get(a)).name)) {return (Enemy) enemies.get(a);}
		}
		return null;
	}
	
	public void onSkippedTurn(SkippedTurnEvent e) {log("Skipped a turn");} //I'd like to know if I'm doing too much calculating
					
	public void onWin(WinEvent e) {//Taunt our foe, and print out some stats
		log("Pwnt"); //Taunt'd
		printStats();
		victorydance=true;
	}
	
	private double bulletV(double p) {return (20-3*p);} //Returns the velocity of a bullet with power p - Robocode physics, again
	private double dist(double x1,double y1,double x2,double y2) {return Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));} //Works out the distance between two points given in x and y coordinates. Just a helper method.
	private void log(String s) {System.out.println(getTime()+": "+s);} //We log stuff by printing to the terminal. Logging to a file might be a good thing to do at some point.
	private double maxEscapeAngle(double p) {return Math.asin(8/bulletV(p));} //Calculates the maximum change-of-gun-angle we could require to hit an enemy. Note that this assumes physics is continous - the discrete-ness of Robocode physics may throw an enemy just outside this angle
	private double min(double a, double b, double c, double d) {return Math.min(a,min(b,c,d));} //Did you know that Math.min only accepts two arguments? That sucks. Hence, a few helper methods to get the minimum of multiple numbers. Defined recursively, to make them slightly prettier
	private double min(double a, double b, double c) {return Math.min(a,Math.min(b,c));}
	public void onDeath(DeathEvent e) {printStats();} //Ha, as if THIS would happen...
	private int sign(double a) {return a<0?-1:1;}
	
	private class CircularGun extends Gun { //This gun fires assuming the enemy will be moving in a regular arc. Only needs 2 scans to set up! More accurate then GF against things like Spinbot or Walls, but less accurate against things like MyFirstrobot
		public CircularGun() {name="CircularGun";} //Set the name of the gun when it's created.
		public Point2D.Double calcEnemyPos(double when, Enemy e) {//Calculate where the enemy will be on the 'when'th tick, using data collected over two scans. Can hit regular circular motion, or regular linear motion. May hit accelerating bodies. Linear corrects for walls, but circular doesn't.
			double diff = when - e.lastscan; //Calculate how many ticks ahead we're looking
			double newY; //The Y coordinate the opposition will be at
			double newX; //The X coordinate the opposition will be at.	
				
			if (Math.abs(e.avgturn) > 0.00001) {//If the rate of turning is significant, use circular targeting
				double rad = e.velocity/e.avgturn; //Work out how large the turn radius is
				double change = diff * e.avgturn; //Work out how much their heading will change over 'diff' ticks.
				newY = e.ey + (Math.sin(e.lastheading + change) * rad) - (Math.sin(e.lastheading) * rad);
				newX = e.ex + (Math.cos(e.lastheading) * rad) - (Math.cos(e.lastheading + change) * rad); //Maths'd
			}
			else {//Didn't turn much, just use linear
				newY = e.ey; 
				newX = e.ex; //Initial estimate for location in 'diff' ticks
				double xmult=Math.sin(e.lastheading)*e.velocity;
				double ymult=Math.cos(e.lastheading)*e.velocity; //How far in the x and y direction the enemy moves every tick
				boolean setbreak=false; //A boolean used for midloop exiting
				for(double a=1;a<=diff;a++) {//Start stepping through those ticks, one by one...
					newX+=xmult;
					newY+=ymult; //Add to x and y coordinates
					if(newX>bw-18||newX<18) {newX-=xmult; setbreak=true;}
					if(newY>bh-18||newY<18) {newY-=ymult; setbreak=true;} //If the enemy hit a wall this tick, move it back to the last location and break the loop
					if(setbreak) {break;} //Log'd and break'd
				}
			}
			
			return new Point2D.Double(newX, newY); //Return the location of the enemy
		}
	}
		
	private class Enemy { //This class contains statistics we've collected on our opponent. Used for aiming purposes, etc.
		private String name=""; //The name of our adversary.
		private double lastenergy=100; //The energy they had last time we scanned them
		private double lastheading=0; //The heading they had last time we scanned them, in radians
		private double lastscan=0; //The time we last scanned them at
		private double avgturn=0; //Their average change of heading since our last scan
		private double ex=0; //The x and y coords they were last at.
		private double ey=0;
		private double velocity=0; //The velocity they had
		private double bearing=0; //The bearing they were at last time we scanned them (Relative), in radians
		private int direction=1; //The direction they're travelling around us. Assumes they're moving perpendicularly to us.
		
		public Enemy(String n, double e, double he, double s, double b, double d, double vee) { //Just set up the variables appropriately
			name=n;
			lastenergy=e;
			lastheading=he;
			avgturn=0;
			lastscan=s;
			ex=getX()+Math.sin(b+getHeadingRadians())*d; //Calculate the enemy's xloc
			ey=getY()+Math.cos(b+getHeadingRadians())*d; //Calculate the enemy's yloc
			velocity=vee;
			bearing=b;
		}
		
		private void newScan(ScannedRobotEvent e) { //Update our knowledge of the enemy. Called when we scan them
			ws_bear.add(0,new Double(Math.atan2(getY()-ey,getX()-ex)));
			if(ws_bear.size()>3) {ws_bear.remove(3);}
			if(ws_dirs.size()>3) {ws_dirs.remove(3);}
			double ediff=lastenergy-e.getEnergy(); //Work out the difference in their energy since our last scan
			if(ediff>=0.1&&ediff<=3) {enemyFiredBullet(this,ediff);} //If it's in bullet-firin' range, assume they've fired. Dodging phantom bullets caused by hitting the wall isn't massively harmful, nor will most bots that matter be hitting walls much
			lastenergy=e.getEnergy(); //Set their energy.
			double lh=lastheading;
			double ls=lastscan;
			lastscan=getTime();
			lastheading=e.getHeadingRadians();
			velocity=e.getVelocity();
			bearing=e.getBearingRadians();
			
			if(ls!=lastscan) {avgturn=(lastheading-lh)/(lastscan-ls);} //If some time has passed since our last scan, calculate the average rate of turn.
			else {avgturn=0;} //Otherwise, it's 0.
			
			if(Math.sin(lastheading-(bearing+getHeadingRadians()))*getVelocity()<0) {direction=1;}
			else {direction=-1;} //Work out which way they're travelling 'round us.
			
			ex=getX()+Math.sin(bearing+getHeadingRadians())*e.getDistance(); //Calculate the enemy's xloc
			ey=getY()+Math.cos(bearing+getHeadingRadians())*e.getDistance(); //Calculate the enemy's yloc
			
			pgun.addData(this); //Add this scan to our pattern-matcher's data
		}
		
		private double turnToFace() {
			double d=headdir*((dist(getX(), getY(), ex, ey)-DIST_PREFERRED)/DIST_MULT);
			if(lastenergy<=3) {return bearing-PI_ON_2;}
			return bearing-(PI_ON_2-d);
		}
		
		private double turnToFace(Point2D.Double currpos, int dir) { //Turn so that we're perpendicular to the enemy.
			double d = dir*((dist(currpos.getX(), currpos.getY(), ex, ey) - DIST_PREFERRED)/DIST_MULT);
			return bearing-(PI_ON_2-d);
		}
	}
	
	private class GuessFactorGun extends Gun { //Works by collecting statistics on which gun bearing works best, and then firing at that heading.
		private final int numbins=31; //The number of 'base' bins I'm going to put gun-aim data in. Splits up the maxEscapeAngle that our enemy can use into numbins slices.
		private final int binfactor=(int) (numbins*0.5); //Just a number that gets used a lot when converting between guessfactors and bin numbers.
		private int[][][][][] bins=new int[WAVESURFING_LATV_SEGS+1][3][5][10][numbins]; //Data is segmented according to the distance the enemy is from the wall, the distance the enemy is from the corner, and the distance the enemy is from me, not just by firing angle. Helps against bots with adaptive movement.
		
		public GuessFactorGun() {name="GuessFactorGun"; use_melee=false;}	
		private void gotHit(int bin,double latv, double dist,double distfromwall,double distfromcorner) { //If I've got a hit in some bin, throw it into the array.
			bin--;
			bin=Math.max(0,Math.min(numbins,bin));
			addToBin(bin,latv, dist,distfromwall,distfromcorner); //Add to that bin.
		}
		
		private void addToBin(int bin, double latv, double d,double w, double c) {
			w=Math.max(0,Math.min(w*0.01,2));
			c=Math.max(0,Math.min(c*0.01,4));
			d=Math.max(0,Math.min(d*0.01,9));
			bins[determineLatVSeg(latv)][(int) w][(int) c][(int) d][bin]++;
		} //Drops a hit into the appropriate bin.
		
		private double getMaxBin(double latv, double d,double dw, double dc) { //Find the best bin with the appropriate parameters.
			double maxind=binfactor; //By default, fire at GF 0.
			double maxval=0; //The value of the maximum indice we've found so far.
			int l=determineLatVSeg(latv);
			d=(d*0.01);
			dw=(dw*0.01);
			dc=(dc*0.01);
			dw=Math.max(0,Math.min(dw,2));
			dc=Math.max(0,Math.min(4,dc));
			d=Math.max(0,Math.min(9,d));			
			for(int a=numbins-1;a>=0;a--) {if(bins[l][(int) dw][(int) dc][(int) d][a]>maxval) {maxind=a; maxval=bins[l][(int) dw][(int) dc][(int) d][a];}} //Simple linear search
			return (maxind-binfactor)/binfactor; //Convert it into a GF.
		}
		
		public void targeting(Enemy e, boolean virt) { //Take aim...
			double dw=min(e.ex,e.ey,bw-e.ex,bh-e.ey); //Work out the minimum distance from our opponent to the wall.
			double dc=min(dist(0,0,e.ex,e.ey),dist(0,bh,e.ex,e.ey),dist(bw,0,e.ex,e.ey),dist(bw,bh,e.ex,e.ey)); //Work out the minimum distance between our opponent and a corner.
			double d=dist(getX(),getY(),e.ex,e.ey);
			double latv=e.velocity*Math.sin(e.lastheading-Math.atan2(getY()-enemy.ey, getX()-enemy.ex));
			double gf=getMaxBin(latv, d,dw,dc); //Get the appropriate GF.
			if(e.lastenergy==0) {gf=0;} //If our opponent is disabled, correct the GF immediately rather then waiting for waves to do it.
			double angle=-gf*maxEscapeAngle(bulletpower); //Work out which angle it is we should shoot.
			double gunoffset = (e.bearing+getHeadingRadians())+angle; //Work out how much the gun needs to be turned to hit our estimate for the opponent's location
			fireAt(gunoffset,virt);
		}
	}
	
	private class Gun { //Ancestor of all guns. Does Head-On-Targeting, but shouldn't be used, normally.
		public int timesfired=0;
		public int numhits=0;
		public ArrayList vbullets=new ArrayList();
		public final boolean IS_VIRTUAL=true; //Statistical data used for selecting the best gun to use.
		public String name="Gun";
		public boolean use_melee=true;
		
		public void targeting(Enemy e, boolean virt) { //this procedure works out which angle to fire at for all guns, taking that gun's calcEnemyPos. If it's called with virt true, it fires a virtual bullet rather then the real deal
			double time; //Temporary variable for 'time' loop
			double nexttime; //See above
		
			Point2D.Double p = new Point2D.Double(e.ex, e.ey); //The current location of the enemy

			double px=getX()+Math.sin(getHeadingRadians())*getVelocity();
			double py=getY()+Math.cos(getHeadingRadians())*getVelocity(); //Work out where we'll be in a tick, for better aiming, particularly against stationary targets. It takes a round to fire the gun, you see.
		
			for (int i = 0; i < TARGETING_ITERATIONS; i++) {//This is a little complex - basically, it works out roughly how long it'll take for the bullet to reach the enemy using an iterative process. The initial estimate is how long it takes for the bullet to reach the enemy's current location. Then we work out how much further the bullet has to travel to hit the enemy's new location, after they've moved on. Basically, the bullet plays the part of Achilles to the enemy's turtle. About ten iterations almost always gets it, but some increase may help
				nexttime = Math.round((dist(px,py,p.x,p.y)/bulletV(bulletpower))); //Works out how long it takes to hit the current estimate
				time = getTime() + nexttime; //Add the current time to nexttime for the purposes of location-calculating
    		   	p = calcEnemyPos(time+1,e); //Calculate their location in that time. The +1 is because it takes a turn to fire the gun
			}
		
			double gunoffset = (PI_ON_2 - Math.atan2(p.y - py,p.x -  px)); //Work out how much the gun needs to be turned to hit our estimate for the opponent's location
		
			fireAt(gunoffset,virt);
		}
		
		public void fireAt(double ang,boolean virt) { //Does the actual firing at that angle.
			if(bulletpower>getEnergy()) {return;}
			double off=normalRelativeAngle(getGunHeadingRadians()-ang);
			off=Math.max(-maxEscapeAngle(bulletpower),Math.min(maxEscapeAngle(bulletpower),off));
			ang=getGunHeadingRadians()-off;
			if(!virt) {setTurnGunLeftRadians(normalRelativeAngle(getGunHeadingRadians()-ang)); if(getGunHeat()<=0&&Math.abs(off)<Math.PI/9) {fireVBullets(); fire_gun=true; bulletsfired++; log("Fired "+name);}}
			else {
				if(getGunHeat()<=0) {
					vbullets.add(new VBullet(ang,getX(),getY(),getTime()+1,bulletpower));
					bullets.add(new Wave(getX(),getY(),Math.atan2(enemy.ey-getY(),enemy.ex-getX()),bulletpower,getTime()+1,enemy.direction)); //Add a new wave to our bullets ArrayList, for this scan.
				}
			}
		}
		
		public Point2D.Double calcEnemyPos(double when, Enemy e) {return new Point2D.Double(e.ex,e.ey);} //Calculates where the enemy will be at the time when, for all guns. Most of them override this.
		
		public void fireVBullet(Enemy e) {targeting(e, IS_VIRTUAL); timesfired++;} //Called to fire a virtual bullet from this gun
		public void VBulletHit() {numhits++;} //Called when a virtual bullet from this gun hits the enemy
		public void checkVBullets(Enemy e) { //Called to check if any of the virtual bullets have hit
			if(vbullets.size()<1) {return;} //No vbullets, don't bother.
			for(int i=0;i<vbullets.size();i++) { //Loop through all the bullets.
				VBullet v=(VBullet) vbullets.get(i);
				if(v.checkHit(e)) {VBulletHit(); vbullets.remove(i); i--;} //If it's hit, or out of the battlefield, remove it. Also, if it hit, make sure the gun knows.
				else if(v.outOfBounds()) {vbullets.remove(i); i--;}
			}
		}
		
		public double getHitRate() { //Returns the rate at which the gun's virtual bullets have hit, as a percentage/100
			if(timesfired==0) {return 0;} //Don't go giving us NaNs.
			else {return ((double) numhits)/((double) timesfired);}
		}
		
		private void drawVBullets(Graphics2D g) { //Draws the gun's virtual bullets. Useful for debugging.
			for(int a=0;a<vbullets.size();a++) {
				VBullet vb=(VBullet) vbullets.get(a);
				g.draw(new Ellipse2D.Double(vb.getX()-5,vb.getY()-5,10,10));
			}
		}
	}
	
	private class HeadOnGun extends Gun { //This gun fires assuming our opponent is SittingDuck. Doesn't work too well against things that enjoy the advantages of moving. Occasionally, it can do decently when other guns fail
		public HeadOnGun () {name="HeadOnGun";}
	}
	
	private class PatternMatchingGun extends Gun { //This gun works by finding patterns in our enemy's past movements.
		private ArrayList history=new ArrayList(); //This array stores a long log of or enemy's movements.
		private Enemy lastdata; //The last scan we got - hopefully, only one tick old.
		private int bestpattern=0; //The index of the start of the best pattern we've found so far.
		
		public PatternMatchingGun() {name="PatternMatchingGun"; use_melee=false;}
		private int findBestPattern() { //This method finds the closest pattern to our opponents last few moves in the log.
			int bestindex=history.size(); //By default, return the last position as the best pattern.
			double bestscore=PATTERN_THRESHOLD;
			double curscore=0;
			PatternVector current;
			for(int hlength=2; hlength<PATTERN_RECENTHISTORY_LENGTH; hlength++) {
				for(int a=0;a<history.size()-hlength;a++) {
					curscore=0;
					for(int b=history.size()-hlength;b<history.size();b++) {
						current=(PatternVector) history.get(a);
						curscore+=current.compare((PatternVector) history.get(b));
					}
					if(curscore<bestscore) {bestindex=a+hlength+1; bestscore=curscore;}
				}
			} //This really isn't that complex. Basically, it loops through all the positions in our array up to the most recent history of the enemy, and compares that with what's happened recently. The best match is remembered.
			return (int) bestindex;
		}
		
		private void addData(Enemy e) { //Add a new scan to our log.
			if(lastdata==null) {lastdata=e;} //Set our last scan appropriately.
			history.add(new PatternVector(normalRelativeAngle(lastdata.lastheading-e.lastheading),e.velocity));
			if(history.size()>PATTERN_HISTORY_LENGTH) {history.remove(0);}
			lastdata=new Enemy(e.name,e.lastenergy,e.lastheading,e.lastscan,e.bearing,0,e.velocity); //simple enough.
			bestpattern=findBestPattern(); //Set the best pattern now that there's some new history present.
		}
		
		public Point2D.Double calcEnemyPos(double when, Enemy e) { //Calculate our enemy's position assuming they'll keep going in the same pattern. Works really well against anything that uses repetitive movement - even complex repetitive movement. That includes spinbot, walls, MyFirstRobot, and, most likely, your bot.
			double diff=when-getTime();
			double ix=e.ex;
			double iy=e.ey;
			double head=e.lastheading;
			PatternVector p;
			for(int a=bestpattern;a<=(diff+bestpattern);a++) { //Sequentially applies the pattern vectors in our best pattern so far to where our enemy is currently
				if(history.size()>a) {
					p=(PatternVector) history.get(a);
					head=p.calcH(head);
					ix=p.calcXPos(ix,head);
					iy=p.calcYPos(iy,head);
				}
			}
			return new Point2D.Double(ix,iy);
		}
		
		public void drawTargeting(Enemy e, Graphics2D graph) { //A modified version of the targeting proc that draws where we think they'll end up.
			double time; //Temporary variable for 'time' loop
			double nexttime; //See above
		
			Point2D.Double p = new Point2D.Double(e.ex, e.ey); //The current location of the enemy

			double px=getX()+Math.sin(getHeadingRadians())*getVelocity();
			double py=getY()+Math.cos(getHeadingRadians())*getVelocity(); //Work out where we'll be in a tick, for better aiming, particularly against stationary targets. It takes a round to fire the gun, you see.
		
			for (int i = 0; i < TARGETING_ITERATIONS; i++) {//This is a little complex - basically, it works out roughly how long it'll take for the bullet to reach the enemy using an iterative process. The initial estimate is how long it takes for the bullet to reach the enemy's current location. Then we work out how much further the bullet has to travel to hit the enemy's new location, after they've moved on. Basically, the bullet plays the part of Achilles to the enemy's turtle. About ten iterations almost always gets it, but some increase may help
				nexttime = Math.round((dist(px,py,p.x,p.y)/bulletV(bulletpower))); //Works out how long it takes to hit the current estimate
				time = getTime() + nexttime; //Add the current time to nexttime for the purposes of location-calculating
    		   	p = drawCalcEnemyPos(time+1,e,graph); //Calculate their location in that time. The +1 is because it takes a turn to fire the gun
			}
		}			
		
		public Point2D.Double drawCalcEnemyPos(double when, Enemy e,Graphics2D graph) { //A modified calcEnemyPos that draws where we think they'll be
			double diff=when-getTime();
			double ix=e.ex;
			double iy=e.ey;
			double ox=ix;
			double oy=iy;
			double head=e.lastheading;
			PatternVector p;
			for(int a=bestpattern;a<=(diff+bestpattern);a++) {
				if(history.size()>a) {
					p=(PatternVector) history.get(a);
					ox=ix;
					oy=iy;
					head=p.calcH(head);
					ix=p.calcXPos(ix,head);
					iy=p.calcYPos(iy,head);
					graph.draw(new Line2D.Double(ox,oy,ix,iy));
				}
			}
			return new Point2D.Double(ix,iy);
		}
	}
				
	private class Wave { //Used for collecting GF stats.
		private double ix=0; //The location of the centre of the wave, in xy coords.
		private double iy=0;
		private double power=3; //The 'bullet power' of our wave.
		private double timefired=0; //When our wave was fired off.
		private double angle=0; //The angle head-on-targeting would have given us.
		private final double hitdist=18; //How close to the centre of the bot we need to be to hit.
		private int dir=1; //Which direction the enemy was travelling in when the bullet was fired.
		
		public Wave(double sx, double sy, double a, double p, double time, int d) { //Just sets values appropriately.
			ix=sx;
			iy=sy;
			angle=a;
			power=p;
			timefired=time;
			dir=d;
		}
		
		private boolean checkHit(Enemy e) { //Checks if we've hit the enemy, updates stats correctly if we have.
			if(e==null) {return false;} //If we've been given a null enemy, then we haven't hit them.
			double d = dist(ix,iy,e.ex,e.ey); //Work out the distance between the centre of the wave and the enemy.
			if(Math.abs((getTime()-timefired)*bulletV(power)-d)<hitdist) { //If the wave has moved far enough that the 'edge' of it is within hitdist of our opponent, we have a hit.
				double hitangleabs = Math.atan2(e.ey-iy, e.ex-ix); //Work out the absolute angle between the centre of the wave and our opponent
				double hitanglerel = normalRelativeAngle(hitangleabs-angle); //Work out the angle that is relative to our head-on-targeting angle
				double gf = Math.max(-1, Math.min(1, hitanglerel/maxEscapeAngle(power)))*dir; //Convert that into a GF
				double dw=min(e.ex,e.ey,bw-e.ex,bh-e.ey); //Work out how far from the wall the enemy was.
				double dc=min(dist(0,0,e.ex,e.ey),dist(0,bh,e.ex,e.ey),dist(bw,0,e.ex,e.ey),dist(bw,bh,e.ex,e.ey)); //Work out how far from the corner the enemy is.
				double latv=e.velocity*Math.sin(e.lastheading-Math.atan2(getY()-enemy.ey, getX()-enemy.ex));
				ggun.gotHit((int) ((gf*ggun.binfactor)+ggun.binfactor+1),latv, d,dw,dc); //Update our stats
				return true; //And tell the world!
			}
			return false; //Otherwise, nothing interesting.
		}
		
		private boolean outOfRange() { //Checks if the wave has gone outside the battlefield
			if((getTime()-timefired)*bulletV(power)>Math.sqrt(bh*bh+bw*bw)) {return true;}
			return false;
		}
	}
	
	private class EnemyWave { //Used for collecting Wavesurfing stats.
		private double ix=0; //The location of the centre of the wave, in xy coords.
		private double iy=0;
		private double power=3; //The 'bullet power' of their wave.
		private double timefired=0; //When their wave was fired off.
		private double angle=0; //The angle head-on-targeting would have given us.
		private final double hitdist=-18; //How close to the centre of the bot we need to be to hit. Note that this is set up so that the wave can go past us for a bit, so that we don't lose waves before the bullet reaches us
		private int dir=1; //Which direction we were travelling in when the bullet was fired.
		private int accfactor=0;
		private double latv=0;
		
		public EnemyWave(double sx, double sy, double a, double p, double time, int d, int acc, double lv) { //Just sets values appropriately.
			ix=sx;
			iy=sy;
			angle=a;
			power=p;
			timefired=time;
			dir=d;
			accfactor=acc;
			latv=lv;
		}
		
		private boolean checkHit() { //Checks if the wave has gone far enough past that we're safe
			double d = dist(ix,iy,getX(),getY()); //Work out the distance between the centre of the wave and us
			if(d-(getTime()-timefired)*bulletV(power)<hitdist) { //If the wave has moved far enough that it's past us, drop it
				double bheading=waveToGF(this); //We add a little bit to the bin the wave hit us at to try and smooth out results. We don't want to stay in one place too long, after all.
				if(bheading<WAVESURFING_BINS) {addToWSBins(latv, accfactor, (getTime()-timefired)*bulletV(power),(int) bheading,0.1);}
				return true;
			}
			return false; //Otherwise, nothing interesting.
		}
	}
	
	private class VBullet { //Used for working out which gun is the most accurate one to use.
		private double ix=0;
		private double iy=0; //The coordinates of the point the vbullet was fired at.
		private double ang=0; //What angle the bullet was 'fired' at.
		private double p=0; //The power of the bullet
		private double timefired=0; //When it was fired.
		private final double hitdist=18; //How close it needs to be to hit it. Note that bots are squares, not circles.
		
		public VBullet(double a, double sx, double sy, double s, double pow) { //Set up values appropriately.
			ang=a;
			ix=sx;
			iy=sy;
			p=pow;
			timefired=s;
		}
		
		private boolean checkHit(Enemy e) { //Check if the bullet has hit our foe this tick.
			if(Math.abs(getX()-e.ex)<hitdist&&Math.abs(getY()-e.ey)<hitdist) {return true;} //Remember, bots are squares, not circles.
			else {return false;}
		}
		
		private boolean outOfBounds() { //Checks if the bot has exited the playing field. Amusingly, that's harder then checking if it's hit our opponent
			double fx=getX();
			double fy=getY(); //This should be fairly clear
			if(fx<0||fy<0||fx>bw||fy>bh) {return true;}
			else {return false;}
		}
		
		private double getX() {return ix+Math.sin(ang)*bulletV(p)*(getTime()-timefired);} //Return our bullet's current xloc
		private double getY() {return iy+Math.cos(ang)*bulletV(p)*(getTime()-timefired);} //Return our bullet's current yloc
	}
	
	private class PatternVector { //Stored by our pattern-matching gun to represent an opponent's past moves.
		private double hchange=0; //Our opponents change-of-heading this tick.
		private double length=0; //Our opponents velocity this tick.
		
		public PatternVector(double h, double l) { //Give things the right values
			hchange=h;
			length=l;
		}
		
		public double compare(PatternVector p) {//Get a numerical value representing the difference between this vector and another one.
			double hd=hchange-p.hchange;
			double ld=length-p.length;
			return Math.abs(hd)+Math.abs(ld);
		}
		
		public double calcXPos(double x, double h) {return x+Math.sin(h)*length;} //Calculate the x position this vector would give a bot with some heading and xloc.
		public double calcYPos(double y, double h) {return y+Math.cos(h)*length;} //Calculate the y position this vector would give a bot with some heading and yloc.
		public double calcH(double h) {return h-hchange;} //Calculate the heading this vector would give to a bot with some heading
	}
	
	private class RoundMemory {
		private double[][][][] saved_ws=new double[WAVESURFING_LATV_SEGS+1][3][10][WAVESURFING_BINS];
		private int[][][][][] saved_gf=new int[WAVESURFING_LATV_SEGS+1][3][5][10][ggun.numbins];
		private int[][] saved_vg=new int[2][4];
		private double saved_dist=0;
	}
}