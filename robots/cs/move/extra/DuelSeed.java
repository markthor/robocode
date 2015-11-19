package cs.move.extra;

import ags.utils.KdTree;

public class DuelSeed {
	private static final double[] seed = new double[] {
		0.0000,-0.00,0.00,0.00,0.5,0.5,550,0,
	};

	public static final void seed(KdTree<Data> tree) {
		//Seed
		for(int i=0;i<seed.length;i+=8)
			addData(tree,createData(0.1,seed,i));

	}

	private static final void addData(KdTree<Data> tree, Data d) {
		tree.addPoint(d.getWeightedDataArray(), d);
	}

	private static final Data createData(double weight, double[] d, int n) {
		return createData(weight, d[n],d[n+1],d[n+2],d[n+3],d[n+4],d[n+5],d[n+6],(long)d[n+7]);
	}

	private static final Data createData(double weight, double gf, double lat, double adv,
			double distLast10, double wallFwd, double wallRev, double dist,
			long timeSinceDirChange) {
		Data data = new Data();
		data.weight = weight;
		data.guessfactor = gf;
		data.lateralVelocity = lat;
		data.advancingVelcity = adv;
		data.distLast10 = distLast10;
		data.wallForward = wallFwd;
		data.wallReverse = wallRev;
		data.distance = dist;
		data.timeSinceDirectionChange = timeSinceDirChange;
		return data;
	}
}
