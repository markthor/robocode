package voidious.dgun;

import robocode.AdvancedRobot;
import robocode.RobocodeFileOutputStream;
import voidious.utils.WaveIndexSet;
import voidious.utils.DUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class MainGunHighBuffer extends MainBufferBase {
	private double[][][][][][][][] _binsLatBulAccelWallRWallVchangeDlet;
	
	public MainGunHighBuffer(int bins) {
		super(bins);
		
		LATERAL_VELOCITY_SLICES = new double[] {.5, 1.25, 2.25, 6.75};
		BULLET_TIME_SLICES = new double[] {14, 28, 42, 56};
		WALL_DISTANCE_SLICES = new double[] {.2, .4, .6, .8, 1.1};
		WALL_REVERSE_SLICES = new double[] {.5};
		VCHANGE_TIME_SLICES = new double[] {.05, .15, .35, .45};
		DLET_SLICES = new double[] {26, 58};
		
		_binsLatBulAccelWallRWallVchangeDlet = new double
			[LATERAL_VELOCITY_SLICES.length + 1]
			[BULLET_TIME_SLICES.length + 1]
			[ACCEL_SLICES]
			[WALL_DISTANCE_SLICES.length + 1]
			[WALL_REVERSE_SLICES.length + 1]
			[VCHANGE_TIME_SLICES.length + 1]
			[DLET_SLICES.length + 1]
			[_bins + 1];
	}

	public double[] getStatArray(WaveIndexSet s) {
		return _binsLatBulAccelWallRWallVchangeDlet
		    [s.latVelIndex][s.bulletTimeIndex][s.accelIndex]
		    [s.wallDistanceIndex][s.wallReverseIndex][s.vChangeIndex]
		    [s.dletIndex];
	}
	
    // CREDIT: Vic Stewart, creator of WikiTargeting concept
    // http://robowiki.net?WikiTargeting
    public void save(AdvancedRobot robot, String enemyName) {
        long quotaAvailable = robot.getDataQuotaAvailable();
        while (quotaAvailable < 2500) {
            File[] robotFiles = robot.getDataDirectory().listFiles();

            int randFileIndex =
                Math.min((int)(Math.random() * robotFiles.length),
                robotFiles.length - 1);
            quotaAvailable += robotFiles[randFileIndex].length();
            robotFiles[randFileIndex].delete();
        }

        try {
            RobocodeFileOutputStream rfos =
                new RobocodeFileOutputStream(robot.getDataFile(enemyName + ".mg_high"));

            byte[] ba = new byte[3];
            double[] thisSegmentBins;

            int x = 0;

            double totalVisits = 0;
            int totalFilled = 0;
            double totalVisitsTwo = 0;
            int totalFilledTwo = 0;

            for(int a=0;a<=LATERAL_VELOCITY_SLICES.length;a++) {
            for(int b=0;b<=BULLET_TIME_SLICES.length;b++) {
            for(int c=0;c<ACCEL_SLICES;c++) {
            for(int d=0;d<=WALL_DISTANCE_SLICES.length;d++) {
            for(int e=0;e<=WALL_REVERSE_SLICES.length;e++) {
            for(int f=0;f<=VCHANGE_TIME_SLICES.length;f++) {
            for(int g=0;g<=DLET_SLICES.length;g++, x++) {
                thisSegmentBins = 
                	_binsLatBulAccelWallRWallVchangeDlet
                		[a][b][c][d][e][f][g];
                double visits = thisSegmentBins[0];
                if (visits > 0) {
                    totalFilled++;
                    totalVisits += visits;
                }
                if (visits >= 2) {
                    totalFilledTwo++;
                    totalVisitsTwo += visits;
                }
                if (visits >= 2) {
                    int highIndex = (_bins-1)/2;
                    double highScore = 0;
                    for (int i = 0; i < _bins; i++) {
                        if (thisSegmentBins[i+1] > highScore) {
                            highIndex = i;
                            highScore = thisSegmentBins[i+1];
                        }
                    }
                    convertBinDataToByteArray(ba, x, highIndex);
                    rfos.write(ba, 0, 3);
                }
            }
            }
            }
            }
            }
            }
            }
            rfos.close();
            System.out.println("\nMain Gun:\n  Saving data for " + 
            		totalFilledTwo + " of " + totalFilled + 
            		" visited segments, of 18,000 total, " +
                    "\n  accounting for " + 
                    (((double)Math.round((((double)totalVisitsTwo) 
                    	/ totalVisits) * 10000)) / 100.0) + 
                    "% of total (weighted) visits.");
        } catch (IOException e) {
            System.out.println("WARNING: IOException trying to write: " + e);
        }
    }

    // CREDIT: Vic Stewart, creator of WikiTargeting concept
    // http://robowiki.net?WikiTargeting
    public void restore(AdvancedRobot robot, String enemyName) {
        int xVal, highBin;
        try {
            FileInputStream fis =
                new FileInputStream(robot.getDataFile(enemyName + ".mg_high"));

            byte[] ba = new byte[3];
            int[] binData = new int[2];
            double[] thisSegmentBins;
            int x = 0;

            if (fis.available() < 3) { throw new Exception(); }

            fis.read(ba);
            convertByteArrayToBinData(binData, ba);

            xVal = binData[0];
            highBin = binData[1];

            int loaded = 0;
            for(int a=0;a<=LATERAL_VELOCITY_SLICES.length;a++) {
            for(int b=0;b<=BULLET_TIME_SLICES.length;b++) {
            for(int c=0;c<ACCEL_SLICES;c++) {
            for(int d=0;d<=WALL_DISTANCE_SLICES.length;d++) {
            for(int e=0;e<=WALL_REVERSE_SLICES.length;e++) {
            for(int f=0;f<=VCHANGE_TIME_SLICES.length;f++) {
            for(int g=0;g<=DLET_SLICES.length;g++, x++) {
                if (xVal == x) {
                    loaded++;
                    thisSegmentBins = 
                    	_binsLatBulAccelWallRWallVchangeDlet
                    		[a][b][c][d][e][f][g];
                    for (int i = 0; i < _bins; i++) {
                        thisSegmentBins[i+1] = (float)
                            (1.0 / (DUtils.square(Math.abs(i - highBin)) + 1));
                    }
                    thisSegmentBins[0] = 5f;

                    if (fis.available() >= 3) {
                        fis.read(ba, 0, 3);
                        convertByteArrayToBinData(binData, ba);

                        xVal = binData[0];
                        highBin = binData[1];
                    }
                }
            }
            }
            }
            }
            }
            }
            }

            fis.close();

            System.out.println("Loaded " + loaded + " GuessFactor gun segments.");
        } catch (Exception e) {
            System.out.println(
                "NOTICE: Failed to restore GuessFactor gun data.");
            return;
        }
    }

}
