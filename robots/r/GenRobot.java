package r;
import robocode.*;
import java.awt.Color;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;

public class GenRobot extends AdvancedRobot {

	public double[] p;
	public int[] run;
	public int[] scan;
	public int[] hbb;
	public int[] hw;
	public int[] hr;
	
	public void run() {
		out.println("1.Robot zagrugen");
		setColors(Color.green,Color.green,Color.green);
		try {
			BufferedReader reader = new BufferedReader(new FileReader(getDataFile("robot.gen")));
			String s = reader.readLine();
			reader.close();
			
			out.println(s);
			out.println("2.FileReader zagrugen");
			//1
			String[] a = s.split("/");		
			String[] m = a[0].split(" ");
			p = new double[m.length];
			for(int i = 0; i < m.length; i++)
				p[i] = Double.parseDouble(m[i]);
			//2
			m = a[1].split(";");
			s = "";
			for(int i = 0; i < m.length; i++)
			{				
				String[] m2 = m[i].split(" ");
				for(int u = 0; u < m2.length; u++)
					s += m2[u] + " ";
			}
			s = s.substring(0, s.length() - 1);
			out.println(s);
			m = s.split(" ");
			run  = new int[m.length];
			for(int i = 0; i < m.length; i++)
				run[i] = Integer.parseInt(m[i]);
			//3
			m = a[2].split(";");
			s = "";
			for(int i = 0; i < m.length; i++)
			{
				String[] m2 = m[i].split(" ");
				for(int u = 0; u < m2.length; u++)
					s += m2[u] + " ";
			}
			s = s.substring(0, s.length() - 1);
			out.println(s);
			m = s.split(" ");
			scan  = new int[m.length];
			for(int i = 0; i < m.length; i++)
				scan[i] = Integer.parseInt(m[i]);
			//4
			m = a[3].split(";");
			s = "";
			for(int i = 0; i < m.length; i++)
			{
				String[] m2 = m[i].split(" ");
				for(int u = 0; u < m2.length; u++)
					s += m2[u] + " ";
			}
			s = s.substring(0, s.length() - 1);
			out.println(s);
			m = s.split(" ");
			hbb  = new int[m.length];
			for(int i = 0; i < m.length; i++)
				hbb[i] = Integer.parseInt(m[i]);
			//5
			m = a[4].split(";");
			s = "";
			for(int i = 0; i < m.length; i++)
			{
				String[] m2 = m[i].split(" ");
				for(int u = 0; u < m2.length; u++)
					s += m2[u] + " ";
			}
			s = s.substring(0, s.length() - 1);
			out.println(s);
			m = s.split(" ");
			hw  = new int[m.length];
			for(int i = 0; i < m.length; i++)
				hw[i] = Integer.parseInt(m[i]);
			//6	
			m = a[5].split(";");
			s = "";
			for(int i = 0; i < m.length; i++)
			{
				String[] m2 = m[i].split(" ");
				for(int u = 0; u < m2.length; u++)
					s += m2[u] + " ";
			}
			s = s.substring(0, s.length() - 1);
			out.println(s);
			m = s.split(" ");
			hr  = new int[m.length];
			for(int i = 0; i < m.length; i++)
				hr[i] = Integer.parseInt(m[i]);
					
			out.println("3.Vse massivy zagrugeny");
		} catch (IOException e){
			out.println("FileReader ne zagrugen");
			out.println(e);
		}
		while(true) { 
			setTurnRadarLeft(360);
			p[0] = getX();
			p[1] = getY();
			p[2] = getEnergy();
													  Function(run);  }}
	public void onScannedRobot(ScannedRobotEvent e) {
		p[3] = Math.sin(getHeadingRadians() + e.getBearingRadians()) * e.getDistance();
		p[4] = Math.cos(getHeadingRadians() + e.getBearingRadians()) * e.getDistance(); 
		p[5] = e.getDistance();	
		p[6] = getEnergy();
		p[7] = getVelocity();
		p[8] = getHeading();
		p[9] = e.getBearing();
													  Function(scan); }
	public void onHitByBullet(HitByBulletEvent e)   { Function(hbb);  }
	public void onHitWall(HitWallEvent e)           { Function(hw);   }
	public void onHitRobot(HitRobotEvent e)         { Function(hr);   }
	
	public void Function(int[] a) {
		for(int i = 0; i < a.length; i++){
			
			switch (a[i])
			{
				case 1:
				case 2:
				case 3:
				case 4:
				case 5:
				case 6:
				case 7:
				case 8:
				case 9:
				case 10:
				case 11:
				case 12:
				{
					i = f2(i, a);
					break;
				}
				case 13:
				{
					if(p[a[i+1]] > p[a[i+2]])
					{
						i+=4;
						i = f2(i, a);
					}
					else
					{
						i+=3;
						i += a[i];
					}
					break;
				}
				case 14:
				{
					if(p[a[i+1]] == p[a[i+2]])
					{
						i+=4;
						i = f2(i, a);
					}
					else
					{
						i+=3;
						i += a[i];
					}
					break;
				}
				case 15:
				{
					if(p[a[i+1]] > p[a[i+2]])
					{
						i+=4;
						i = f2(i, a);
						i++;
						i += a[i];
					}
					else
					{
						i+=3;
						i += a[i] + 2;
						i = f2(i, a);
					}
					break;
				}
				case 16:
				{
					if(p[a[i+1]] == p[a[i+2]])
					{
						i+=4;
						i = f2(i, a);
						i++;
						i += a[i];
					}
					else
					{
						i+=3;
						i += a[i] + 2;
						i = f2(i, a);
					}
					break;
				}
			}
		}
		execute();
	}

	public int f2(int i, int[] a)
	{
		switch (a[i])
		{
			case 1:
			{
				setAhead(p[a[i+1]]);
				i++;
				break;
			}
			case 2:
			{
				setBack(p[a[i+1]]);
				i++;
				break;
			}
			case 3:
			{
				setTurnLeft(p[a[i+1]]);
				i++;
				break;
			}
			case 4:
			{
				setTurnRight(p[a[i+1]]);
				i++;
				break;
			}
			case 5:
			{
				setTurnGunLeft(p[a[i+1]]);
				i++;
				break;
			}
			case 6:
			{
				setTurnGunRight(p[a[i+1]]);
				i++;
				break;
			}
			case 7:
			{
				fire(p[a[i+1]]);
				i++;
				break;
			}
			case 8:
			{				
				p[a[i+1]] = (p[a[i+2]]);
				i+=2;
				break;
			}
			case 9:
			{
				p[a[i+1]] = p[a[i+2]] + p[a[i+3]];
				i+=3;
				break;
			}
			case 10:
			{
				p[a[i+1]] = p[a[i+2]] - p[a[i+3]];
				i+=3;
				break;
			}
			case 11:
			{
				p[a[i+1]] = p[a[i+2]] * p[a[i+3]];
				i+=3;
				break;
			}
			case 12:
			{
				if(p[a[i+3]] != 0)
					p[a[i+1]] = p[a[i+2]] / p[a[i+3]];
				i+=3;
				break;
			}
		}
		return i;
	}
}