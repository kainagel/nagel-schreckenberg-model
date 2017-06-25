import java.awt.*;
import java.applet.*;

///////////////////////////////////////////////////////////
// Application of the standard CA Nagel Schreckenberg model
// Simulation of traffic flow

// B. Eisenblaetter and L. Neubert 1997
// last modified 04/08/1997

public class RoadApplet extends Applet implements Runnable
{  

  // Init values of the scrollbars
private final int SLOW_INIT = 50;
private final int DENS_INIT = 20;
private final int SPEED_INIT = 100;

  // Dinstance between two updates of the diagrams
private final int UPDATE_DIAGRAM = 10;
  
  // old values for comparision
private double slowdown_old = 0.0;
private double density_old = 0.0;
private double simspeed_old = 1.0;

  // Labels of the two scrollbars
private Label label_slowdown;
private Label label_density;
private Label label_simspeed;

private RoadCanvas canvas;
private Scrollbar slowdown;
private Scrollbar density;
private Scrollbar simspeed;
private Thread runner;

private boolean FirstStep = true;

public void init()
  {  
    // scrollbars of simulation speed, slowdown probability and global density
    simspeed = new Scrollbar(Scrollbar.HORIZONTAL, SPEED_INIT, 1, 1, 100);
    slowdown = new Scrollbar(Scrollbar.HORIZONTAL, SLOW_INIT, 1, 0, 100);
    density = new Scrollbar(Scrollbar.HORIZONTAL, DENS_INIT, 1, 0, 100);
        
    Panel p = new Panel();

    p.setLayout(new GridLayout(3,4));
    p.add(new Label("Simulation speed"));
    p.add(simspeed);
    p.add(label_simspeed =new Label((new Integer(SPEED_INIT).toString())+"%"));
    p.add(new Button("Start"));

    p.add(new Label("Slowdown Prob. P"));
    p.add(slowdown);
    p.add(label_slowdown = new Label((new Integer(SLOW_INIT).toString())+"%"));
    p.add(new Button("Stop"));

    p.add(label_density = new Label("Global Density"));
    p.add(density);
    p.add(label_density = new Label((new Integer(DENS_INIT).toString())+"%"));
    p.add(new Button("Clear Diagrams"));
    
    canvas = new RoadCanvas(getFirstDensity());

    setLayout(new BorderLayout());
    add("North", p);
    add("Center", canvas);

  }

  //////////////////////////////////////
  // get the actual simulation speed

public int getSimSpeed() 
  {  
    double s =  simspeed.getValue();
    if (s != simspeed_old)
      {
	simspeed_old = s;
	label_simspeed.setText((new Double(s).toString())+"%");
      }
    return (int)(100-s);
  }
  
  //////////////////////////////////////
  // get the actual slowdown prob value

public double getSlowdown() 
  {  
    double slow =  slowdown.getValue();
    if (slow != slowdown_old)
      {
	slowdown_old = slow;
	label_slowdown.setText((new Double(slow).toString())+"%");
      }
    return slow*0.01;
  }
  
  /////////////////////////////////////////////////
  // get the slowdown prob value for the first time

public double getFirstDensity()
  {  
    double dens =  density.getValue();
    if (dens != density_old)
      {
	density_old = dens;
      }
    return dens*0.01;
  }  

  ///////////////////////////////////////
  // get the actual global density value

public double getDensity()
  {  
    double dens =  density.getValue();
    if (dens != density_old)
      {
	density_old = dens;

	label_density.setText((new  Double(dens).toString())+"%");
	
      }

    
    return dens*0.01;
  }

public void run()
  {  
    // loop counter
    // shows, whether the diagrams have to be plotted
    int counter = 0;
    while (++counter < 2*UPDATE_DIAGRAM)
      {  
	canvas.update(getSlowdown(), getDensity(), getSimSpeed(),counter);
	if (counter >= UPDATE_DIAGRAM)
	  counter = 0;
        try { Thread.sleep(5); } catch(InterruptedException e) {}
      }
  }
  
public void start()
  {  
    if (runner == null)
      {  
	runner = new Thread(this);
	runner.start();
      }
    else 
      if (runner.isAlive())
	{
	  runner.resume();
	}
  }
  
public boolean action(Event evt, Object arg)
  {  
    if (arg.equals("Start"))
      {  
	stop();
	runner = null;
	start();
      }
    else 
      if (arg.equals("Stop"))
	{
	  stop();
	}
      else 
	if (arg.equals("Clear Diagrams"))
	  {
	    canvas.ClearDiagrams();
	  }
      else 
	{
	  return super.action(evt, arg);
	}
    return true;
  }
  
public void stop()
  {  
    if (runner != null && runner.isAlive())
      {
	runner.suspend();
      }
  }
  
}  // End of "public class RoadApplet extends Applet implements Runnable"


class RoadCanvas extends Canvas
{  

private Road freeway;
private Image buffer;

  // diagram labels  
private final String SpaceLabel = "space";
private final String[] TimeLabel = {"t","i","m","e"};

private final String x1="rho";
private final String y1="q";
private final String x2="rho";
private final String y2="v";
private final String x3="q";
private final String y3="v";
private final String x4="v";
private final String y4="f";
private final String x5="gap";
private final String y5="f";

private final String[] xAxisLabel={x1,x2,x3,x4,x5};
private final String[] yAxisLabel={y1,y2,y3,y4,y5};

  // dot sizes
private final int DOTSIZE = 1;   
private final int XDOTDIST = 1;

  // counter of the rows in the space-time-plot
private int row;

  // some properties of the several graphic outputs
private int xsize;
private int ysize;
private int xsizeSTD;
private int ysizeSTD;
private int xsizeIndy;
private int ysizeIndy;
private int xsizeDiagram;
private int ysizeDiagram;
private int xsizeDiagramPart;
private int ysizeDiagramPart;

private final int xShift = 17;
private final int yShift = 17;
private final int bluebar = 3;


  // some properties of the indianapolis scenario
private int radIn;
private int radOut;
private int radVeh;
private int xIndyMid;
private int yIndyMid;

  // some properties of the diagrams
private int[] xDiagram;
private int[] yDiagram;

private int[] xOrigin;
private int[] yOrigin;

private double[] xDelta;
private double[] yDelta;

  RoadCanvas(double density)
  {  
    freeway = new Road(density);
    row = 0;      
  }

public void update(double slowdown, double density, int simspeed, int counter)
  {  
  
    if (buffer == null) 
      { 
	xsize = size().width;
	ysize = size().height;
	xsizeSTD = xsize/2;
	ysizeSTD = ysize/2;
	xsizeIndy = xsize/2;
	ysizeIndy = ysize/2;
	xsizeDiagram = xsize/2;
	ysizeDiagram = ysize;
	xsizeDiagramPart = xsizeDiagram/8;
	ysizeDiagramPart = ysize/40;

	radIn = (int)(0.36*xsizeIndy);
	radOut = (int)(0.4*xsizeIndy);
	radVeh = (int)(0.38*xsizeIndy);

	xIndyMid = xsizeIndy/2+xShift+bluebar;
	yIndyMid = ysizeSTD+ysizeIndy/2+yShift/2;

	xDiagram = new int[5];
	yDiagram = new int[5];
	xOrigin = new int[5];
	yOrigin = new int[5];
	xDelta = new double[5];
	yDelta = new double[5];
  
	for (int i=0;i<3;i++)
	  {
	    xDiagram[i] = xsizeDiagram-2*xsizeDiagramPart;
	    yDiagram[i] = (ysizeDiagram-4*ysizeDiagramPart)/4;
	    xOrigin[i] = xsizeSTD+xsizeDiagramPart;
	    yOrigin[i] = (i+1)*(yDiagram[i]+ysizeDiagramPart);
	    xDelta[i] = 1.0;
	    yDelta[i] = 1.0;
	  }

	for (int i=3;i<5;i++)
	  {
	    xDiagram[i] = (int)(0.44*xDiagram[0]);
	    yDiagram[i] = (ysizeDiagram-4*ysizeDiagramPart)/4;
	    xOrigin[i] = (i == 3)? xsizeSTD+xsizeDiagramPart: xsizeSTD+xsizeDiagram/2;
	    yOrigin[i] = 4*(yDiagram[i]+ysizeDiagramPart)-8;
	    xDelta[i] = 1.0;
	    yDelta[i] = 1.0;
	  }

	// draw all diagrams
	buffer = createImage(xsize,ysize);
	Graphics bg = buffer.getGraphics();
	for (int i=0;i<5;i++)
	  {
	    freeway.diagramAxis(bg,xOrigin[i],yOrigin[i],xOrigin[i]+xDiagram[i],yOrigin[i]);
	    bg.drawString(xAxisLabel[i],xOrigin[i]+xDiagram[i]+3,yOrigin[i]+5);

	    freeway.diagramAxis(bg,xOrigin[i],yOrigin[i],xOrigin[i],yOrigin[i]-yDiagram[i]);
	    bg.drawString(yAxisLabel[i],xOrigin[i]-10,yOrigin[i]-yDiagram[i]+25);
	  }
	freeway.street(bg,xIndyMid,yIndyMid,radOut,radIn);
	freeway.bluelines(bg,xShift,yShift,bluebar-1,ysizeSTD);
	freeway.SpaceTimeLabels(bg,xsizeSTD/3,3*yShift/4,xsizeSTD/2,yShift/2,50,SpaceLabel,xShift/2,yShift+ysizeSTD/4,xShift/2,yShift+ysizeSTD/2,50,TimeLabel);
      }

    freeway.update(slowdown, density);
    // update all diagrams
    Graphics bg = buffer.getGraphics();
    // update space-time-plot
    freeway.paint(bg,xShift+row,XDOTDIST,DOTSIZE,xShift+bluebar);
    // update indy-scenario
    freeway.indypaint(bg,XDOTDIST*2,DOTSIZE*2,xIndyMid,yIndyMid,radVeh);

    // if enabled, plot diagrams
    if (counter%2 == 0)
      {
	freeway.measure();
	for (int i=0;i<((counter == 6)? 5 : 3);i++)
	  {
	    freeway.diagram(bg,xOrigin[i],yOrigin[i],xDiagram[i],yDiagram[i],xDelta[i],yDelta[i],i);
	  }
      }

    // move space-time-plot upwards
    if (row < ysizeSTD-DOTSIZE) 
      {
	row+=DOTSIZE;
      }
    else
      { 
	bg.copyArea(xShift+bluebar,DOTSIZE+yShift,xsizeSTD,ysizeSTD-DOTSIZE,0,-DOTSIZE);
	bg.clearRect(xShift+bluebar,ysizeSTD-DOTSIZE+yShift,xsizeSTD,DOTSIZE);
      }

    bg.dispose();
    repaint();

    // delay loop
    if (simspeed > 10)
      {
	for (int i=0;i<simspeed*2000;i++)
	  {
	    double x = 100.0*100.0;
	  }
      }
  }

public void ClearDiagrams()
  {
    Graphics bg = buffer.getGraphics();
    freeway.ClearDiagrams(bg,xOrigin,yOrigin,xDiagram,yDiagram);
    bg.dispose();
    repaint();
  }

public void paint(Graphics g)
  {  
       if (buffer != null) 
      {
		g.drawImage(buffer, 0, 0, null);  
      }
  }
  
public void update(Graphics g)
  {  
        paint(g);
  }

}  // End of "class RoadCanvas extends Canvas"


class Road
{

  // length of the road
public final int LENGTH = 300;
  // Maximum speed
public final int MAXSPEED = 5;

  // point of measurement
public final int mp =LENGTH-1;
  // length of the measurement
public final int ml = LENGTH/3;
  // dummy for infinity
public final double DBL_INF = 99999.99;

  // my private colors
private final Color streetcolor = Color.lightGray;
private final Color background = Color.white;
private final Color foreground = Color.black;
private final Color linecolor = Color.blue;

  // colors for different velocities
private final Color v0color = Color.red;
private final Color v1color = new Color(205,69,0);
private final Color v2color = Color.orange;
private final Color v3color = new Color(154,205,50);

private final Color v4color = new Color(84,255,159);
private final Color v5color = new Color(0,255,127);
private final Color vcolor[]={v0color,v1color,v2color,v3color,v4color,v5color};

  // the road as an array of speeds
private int[] speed;
  // number of vehicles on the road
private int cars;
  // 1/number of vehicles
private double invcars;
  // local density of vehicles on the road [veh/site]
private double dens;
  // local mean velocity [sites/timestep]
private double v;
  // local flow of vehicles [vehicles/time]
private double flow;
  // global frequency of gaps
private double[] gapfreq;
  // global frequency of velocities
private double[] vfreq;

public Road(double density)
  {  
    // the road as an array of speeds
    speed    = new int[LENGTH];
    // frequency of the gaps
    gapfreq  = new double[LENGTH];
    // frequency of the velocities
    vfreq    = new double[MAXSPEED+1];
    
    cars = 0;
    int cars_abs = (int)(density*LENGTH);
    invcars = 1.0/((cars_abs > 0)? cars_abs: 1.0/DBL_INF);

    // clear road
    for (int i=0;i<LENGTH;i++)
      {
	speed[i] = -1;
      }

    // put vehicles on the road
    while (cars < cars_abs)
      {
	int i = (int)(Math.random()*LENGTH);		
	if (speed[i] == -1)
	  {
	    speed[i] = 0;
	    cars++;
	  }
      }
  }

  ////////////////////////////////////
  // update of the cellulare automaton

public void update(double prob_slowdown, double prob_create)
  {  
    // number of vehicles
    int car_local = (int)(prob_create*LENGTH);
    // the different between the actual and the requested number of vehicles
    int diff_cars = cars-car_local;

    for(int i=0;i<LENGTH;i++)
      {
	gapfreq[i]=0.0;
      }
    for(int i=0;i<=MAXSPEED;i++)
      {
	vfreq[i]=0.0;
      }
    
    // match the number of vehicles
    if (diff_cars > 0)
      {
	// number of vehivles, that have to be killed
	int kill_cars = 0;
	while (kill_cars < diff_cars)
	  {
	    int i = (int)(Math.random()*LENGTH);		
	    if (speed[i] != -1)
	      {
		speed[i] = -1;
		kill_cars++;
	      }
	  }
	cars = car_local;
	invcars  = 1.0/((cars > 0)? cars: 1.0/DBL_INF);
      }
    else
      if (diff_cars < 0)
	{
	  // number of vehivles, that have to be created
	  int new_cars = 0;
	  while (new_cars < Math.abs(diff_cars))
	    {
	      int i = (int)(Math.random()*LENGTH);		
	      if (speed[i] == -1)
		{
		  speed[i] = MAXSPEED;
		  new_cars++;
		}
	    }
	  cars = car_local;
	  invcars  = 1.0/((cars > 0)? cars: 1.0/DBL_INF);
	}
    
    // where is the 1st vehicle
    int i = -1;
    while((++i < LENGTH) && (speed[i] == -1));
    // go on until reaching the end of the lane
    while (i < LENGTH)
      {
	// searching for the vehicle ahead
	int gap = i;
	while (speed[++gap%LENGTH] == -1);
	car_local++;
	
	// dinstance between two following vehicles
	gap-=(i+1);
	gapfreq[gap]+=((invcars < DBL_INF)? invcars: 0);

	////////////////
	// update rules

	// Acceleration
	if (gap > speed[i])
	  {
	    speed[i] = Math.min(speed[i]+1,MAXSPEED);
	  }
	// Slow down to prevent crashes
	else
	  {
	    speed[i] = gap;
	  }
	// stochastic behavior
	// slowdown with probability prob_slowdown
	if ((speed[i] > 0) && (Math.random() <= prob_slowdown))
	  {
	    speed[i]--;
	  }

	vfreq[speed[i]]+=((invcars < DBL_INF)? invcars: 0);
	// next vehicle is on site j
	i+=(gap+1);
      }

    // move the vehicles

    // where is the 1st vehicle
    car_local = 0;
    i = -1;
    while((++i < LENGTH) && (speed[i] == -1));
    // go on until reaching the end of the lane
    while (i < LENGTH)
      {
	car_local++;
	int inext = i+speed[i];
	int j = inext%LENGTH;
	// exchange the positions
	if (i != j)
	  {
	    speed[j] = speed[i];
	    speed[i] = -1;
	  }
	// searching for the vehicle ahead
	while (speed[++inext%LENGTH] == -1);
	i = inext;
      }
  }

  //////////////////
  // space-time-plot

public void paint(Graphics g,int row,int dotdist,int dotsize,int xshift)
  {  
    for (int i = 0; i < LENGTH; i++)
      {
	if (speed[i] >= 0) 
	  {
	    g.setColor(vcolor[speed[i]]);   
	    g.fillRect((xshift+i)*dotdist,row,dotsize,dotsize);
	  }
      }
    g.setColor(background);   
  }

  /////////////////////////////////
  // plot the indianapolis-scenario
   
public void indypaint(Graphics g, int dotdist, int dotsize,int xm,int ym,int radius)
  {  
    // degrees per vehicle
    double f = 2.0*Math.PI/(double)LENGTH;
    for (int i = 0; i < LENGTH; i++)
      {
	// angle of a vehicle
	double rad = f*i;
	// Erase a vehicle
	if (speed[i] == -1)
	  {
	    g.setColor(streetcolor);
	    g.fillRect((int)(xm+Math.cos(rad)*radius), (int)(ym+Math.sin(rad)*radius), dotsize, dotsize);
	  }
	// plot a vehicle
	else
	  {
	    g.setColor(foreground);
	    g.fillRect((int)(xm+Math.cos(rad)*radius), (int)(ym+Math.sin(rad)*radius), dotsize, dotsize);
	  }
      }
  }

  ////////////////////
  // local measurement

public void measure()
  {
    int vsum=0;
    int rhoc=0;

    for(int i=mp;i>mp-ml;i--)
      {
	if (speed[i] >= 0)
	  {
	    vsum+=speed[i];
	    rhoc++;
	  }
      }
    v = (double)(vsum)/(double)((rhoc > 0) ? rhoc : 1 );
    dens =(double)(rhoc)/(double)(ml);
    flow = v*dens;
  }  

  ////////////////
  // plot diagrams

void diagram(Graphics g, int x1, int y1, int intdx, int intdy, double dbldx, double dbldy, int index)
  {
    double xValue;
    double yValue;
    g.setColor(foreground);
    // fundamental diagrams
    if (index < 3)
      {
	switch(index)
	  {
	  case 0:
	    {
	      // local flow = local flow(local density)
	      xValue = dens;
	      yValue = flow;
	      break;
	    };
	  case 1:
	    {
	      // local mean velocity = local mean velocity(local density)
	      xValue = dens;
	      yValue = v/MAXSPEED;
	      break;
	    };
	  default:
	    {
	      // local mean velocity = local mean velocity(local flow)
	      xValue = flow;
	      yValue = v/MAXSPEED;
	      break;
	    }
	  }
	g.drawRect(x1+(int)(xValue/dbldx*intdx),y1-(int)(yValue/dbldy*intdy),1,1);
      }

    // bar charts
    else
      {
	// bar chart of the frequency of velocities
	if (index == 3)
	  {
	    // clear diagram
	    g.setColor(background);
	    g.fillRect(x1+1,y1-intdy,intdx-1,intdy);
	    drawArrow(g,x1,y1-intdy,10,8,270,true,foreground);
	    drawArrow(g,x1+intdx,y1,10,8,0,true,foreground);
	    // bar widht
	    int bar = intdx/(MAXSPEED+1);
	    g.setColor(foreground);
	    for (int i=0;i<=MAXSPEED;i++)
	      {
		g.setColor(vcolor[i]);
		int y = (int)(vfreq[i]/dbldy*intdy);
		g.fillRect(x1+i*bar+1,y1-y,bar-1,y);
	      }
	  }
	// bar chart of the frequency of gaps
	else
	  {
	    // clear diagram
	    g.setColor(background);
	    g.fillRect(x1+1,y1-intdy,intdx-1,intdy);
	    drawArrow(g,x1,y1-intdy,10,8,270,true,foreground);
	    drawArrow(g,x1+intdx,y1,10,8,0,true,foreground); 
	    // number of bars
	    int xmax = 18;
	    // bar width
	    int bar = intdx/xmax;
	    g.setColor(foreground);
	    for (int i=0;i<xmax;i++)
	      {
		int y = (int)(gapfreq[i]/dbldy*intdy);
		g.fillRect(x1+i*bar+1,y1-y,bar-1,y);
	      }
	  }
      }
  }

  ///////////////
  // diagram axes

public void diagramAxis(Graphics g, int xOr, int yOr, int xDiag, int yDiag)
  {
    g.drawLine(xOr,yOr,xDiag,yDiag);
    drawArrow(g,xDiag,yDiag,10,8,(xOr == xDiag)? 270: 0,true,foreground);
  }

  /////////////////////////////////////////////
  // draw the road of the indianapolis-scenario

public void street(Graphics g, int xm, int ym, int r1, int r2)
  {
    g.setColor(streetcolor);
    g.fillOval(xm-r1,ym-r1,2*r1,2*r1);
    g.setColor(background);
    g.fillOval(xm-r2,ym-r2,2*r2,2*r2);
    g.setColor(linecolor);
    g.drawLine(xm+r2-20,ym,xm+r1+20,ym);
    g.setColor(foreground);
    g.drawArc(xm-r2+30,ym-r2+30,2*r2-60,2*r2-60,0,-270);
    drawArrow(g, xm,ym-r2+30,10,8,355, true, Color.black);
  }

  /////////////////
  // clear diagrams

public void ClearDiagrams(Graphics bg,int xOrigin[],int yOrigin[],int xDiagram[],int yDiagram[])
  {
    bg.setColor(background);
    for (int i=0;i<5;i++)
      {
	bg.fillRect(xOrigin[i]+1,yOrigin[i]-yDiagram[i],xDiagram[i]-1,yDiagram[i]);
	drawArrow(bg,xOrigin[i],yOrigin[i]-yDiagram[i],10,8,270,true,foreground);
	drawArrow(bg,xOrigin[i]+xDiagram[i],yOrigin[i],10,8,0,true,foreground);
      }
    bg.setColor(foreground);
  }

  //////////////////////////////
  // blue line at the first site

public void bluelines(Graphics g,int x1,int y1,int x2,int y2)
  {  
    Color old_color = g.getColor();
    g.setColor(linecolor);
    g.fillRect(x1,y1,x2,y2);
    g.setColor(old_color);
  }

  ///////////////////////////////////////////////////////
  // Labels of the space-time-plot
  // The letters of the ordinate are arranged vertically.

public void SpaceTimeLabels(Graphics g,int x1,int y1,int xA1,int yA1,int length1,String label1,int x2,int y2,int xA2,int yA2,int length2,String[] label2)
  {  
    Color color = foreground;
    Color old_color = g.getColor();
    g.setColor(color);
    g.drawString(label1,x1,y1);
    g.drawLine(xA1,yA1,xA1+length1,yA1);
    drawArrow(g,xA1+length1,yA1,10,8,0,true,color);
    for (int i=0;i<4;i++)
      {
	g.drawString(label2[i],x2-5,y2+i*15);
      }
    g.drawLine(xA2,yA2,xA2,yA2+length2);
    drawArrow(g,xA2,yA2+length2,10,8,90,true,color);
    g.setColor(old_color);
  }

  //////////////////////////////////////////////////////////
  // plot an arrow with any properties like size, angle, ...

public void drawArrow(Graphics g,int xA,int yA,int Length,int Width,int deg,boolean filled,Color color)
  {
    
    double Angle=(double)deg/180.0*Math.PI;
    double sinA = Math.sin(Angle);
    double cosA = Math.cos(Angle);
    int x1 = xA-(int)(cosA*Length + sinA*Width/2);
    int y1 = yA-(int)(sinA*Length - cosA*Width/2);

    Polygon filledPolygon = new Polygon();
    filledPolygon.addPoint(xA, yA);
    filledPolygon.addPoint(x1,y1);
    filledPolygon.addPoint(x1+(int)(sinA*Width),y1-(int)(cosA*Width));
    Color old_color = g.getColor();
    g.setColor(color);
    if(filled)
      {
	g.fillPolygon(filledPolygon); 
      }
    else
      {
	g.drawPolygon(filledPolygon); 
      }
    g.setColor(old_color);
  
  }

}  // End of "class Road"

// End of JavaApplet


