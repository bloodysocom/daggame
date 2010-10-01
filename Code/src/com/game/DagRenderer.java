package com.game;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.game.MessageHandler.MsgReceiver;

import android.R.string;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.text.GetChars;
import android.util.Log;

/**
 * Renderer for the GLSurface
 * 
 * TODO: Set the correct device coordinates, right now it's messed up.
 * @author Ying
 *
 */
/**
 * @author Ying
 *
 */

public class DagRenderer implements GLSurfaceView.Renderer 
{	
	// To receive messages from the logic thread.
	private Handler handler;
	GL10 gl = null;
	
	//Debug info
	FloatBuffer floatBuff;
	float[] debSquare = new float[] 
	                              { 30f, 30f, 1.0f,
									0f, 30f, 1.0f,
									30f, 0f, 1.0f,
									0f, 0f, 1.0f };
	
	// A square to be drawn as the cursor, at each position we have a cursor
	FloatBuffer cursorBuff;
	
	// Reference to the cursors
	private Vector<Cursor> cursorsRef;
	
	// Position for each player cursor
	private Vector<float[]> cursorPos;
	
	// Color for each player cursor
	private Vector<float[]> cursorColor;
	
	private int height;
	private int width;
	
	//The tilemap rendered in the debug mode
	private Vector<Tile> tileMap;	
	
	private int bufferLength;
	
	public DagRenderer()
	{
		super();
		
		tileMap = null;
		
		// Link square to a float buffer
		floatBuff = makeFloatBuffer(debSquare);
		cursorBuff = makeFloatBuffer(debSquare);
		
		cursorPos = new Vector<float[]>();
		cursorColor = new Vector<float[]>();
		cursorsRef = new Vector<Cursor>();
		
		// Create one cursor per player
		CreateCursors(Preferences.Get().GetNumberOfPlayers());
		
	    height = 0;
	    width = 0;
		
		// Initialize handler
		this.handler = new Handler() 
		{
	        public void handleMessage(Message msg) 
	        {
	        	if(msg.what == MsgType.NEW_TILEMAP.ordinal())
	        	{	        	
	        		tileMap = (Vector<Tile>) msg.obj;
	        		int rowTiles = msg.arg1;
	        		int columnTiles = msg.arg2;
	        		width=rowTiles*Constants.TileWidth;
	        		height=columnTiles*Constants.TileWidth;
	        		
	        		float[] floatArray= new float[rowTiles*columnTiles*6*3];
	        		
	        		Iterator<Tile> it = tileMap.listIterator();
	        		Tile tile = null;
	        		bufferLength = 0;
	        		for(int j = 0; j < columnTiles; j++){
	        			for(int i = 0; i < rowTiles; i++){			
	        				tile = it.next();
	        				if(tile.maxCapacity > 0){
	        					floatArray[bufferLength] = i*Constants.TileWidth; 
	        					floatArray[bufferLength+1] = j*Constants.TileWidth;
	        					floatArray[bufferLength+2] = 0.0f; 
	        					
	        					floatArray[bufferLength+3] = i*Constants.TileWidth+Constants.TileWidth; 
	        					floatArray[bufferLength+4] = j*Constants.TileWidth;
	        					floatArray[bufferLength+5] = 0.0f; 
	        					
	        					floatArray[bufferLength+6] = i*Constants.TileWidth; 
	        					floatArray[bufferLength+7] = j*Constants.TileWidth+Constants.TileWidth;
	        					floatArray[bufferLength+8] = 0.0f; 
	        					
	        					floatArray[bufferLength+9] = i*Constants.TileWidth+Constants.TileWidth; 
	        					floatArray[bufferLength+10] = j*Constants.TileWidth;
	        					floatArray[bufferLength+11] = 0.0f; 
	        					
	        					floatArray[bufferLength+12] = i*Constants.TileWidth+Constants.TileWidth; 
	        					floatArray[bufferLength+13] = j*Constants.TileWidth+Constants.TileWidth;
	        					floatArray[bufferLength+14] = 0.0f; 
	        					
	        					floatArray[bufferLength+15] = i*Constants.TileWidth; 
	        					floatArray[bufferLength+16] = j*Constants.TileWidth+Constants.TileWidth;
	        					floatArray[bufferLength+17] = 0.0f; 
	        					
	        					bufferLength += 18;
	        				}
	        			}			
	        		}
	        		
	        		floatBuff = makeFloatBuffer(floatArray); 
	        	}
	        	else if(msg.what == MsgType.NEW_BITMAP.ordinal()){
	        		
	        	}
	        	else if(msg.what == MsgType.UPDATE_CURSOR_POS.ordinal())
	        	{
	        		ChangeCursorPosition(msg.arg1, (float[]) msg.obj);
	        	}
	        	else if(msg.what == MsgType.GET_CURSOR_VECTOR.ordinal())
	        	{
	        		Vector<Cursor> curs = (Vector<Cursor>) msg.obj;
	        		for(int i = 0; i < curs.size(); i++)
	        		{
	        			cursorsRef.add(curs.elementAt(i));
	        		}
	        	}
	        }
	    };
	    
	    MessageHandler.Get().SetRendererHandler(this.handler);
	    MessageHandler.Get().Send(MsgReceiver.LOGIC, MsgType.RENDERER_CONSTRUCTOR_DONE);
	    
	    Camera.Get().SetRenderRef(this);
	}
	
	public void onSurfaceCreated(GL10 gl, EGLConfig config) 
    {        
    	
    }
	
	public void onSurfaceChanged(GL10 gl, int w, int h) 
	{        
		Log.i("DagRenderer","Surface changed: " + w + " / " + h );
		this.gl = gl;
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glViewport(0,0,w,h);
		GLU.gluPerspective(gl, 45.0f, ((float)w)/h, 900f, 3000f); // TODO: Make them dependant on map size
		
		//GLU.gluUnProject(winX, winY, winZ, model, modelOffset, project, projectOffset, view, viewOffset, obj, objOffset)
		gl.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		
		height = h;
		width = w;
	}
	 
	public void onDrawFrame(GL10 gl) 
    {
		this.gl = gl;
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();
		
		//gl.glTranslatef(-Camera.Get().GetScreenWidth()/2.0f,-Camera.Get().GetScreenHeight()/2.0f,-(2.0f*Camera.Get().GetScreenWidth()));
		//gl.glTranslatef(-width/2.0f,-height/2.0f,-(2.0f*width));
		gl.glTranslatef(-Camera.Get().X(),-Camera.Get().Y(),-Camera.Get().Z());
		
		// Draw debug square
		gl.glColor4f(1.0f, 1.0f, 1.0f, 0.5f);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, floatBuff);		
		if(tileMap != null){
			gl.glDrawArrays(GL10.GL_TRIANGLES, 0, bufferLength/3);
			DrawCursors(gl);
		}
		
				
    }
	
	protected static FloatBuffer makeFloatBuffer(float[] arr)
	{
	ByteBuffer bb = ByteBuffer.allocateDirect(arr.length*4);
	bb.order(ByteOrder.nativeOrder());
	FloatBuffer fb = bb.asFloatBuffer();
	fb.put(arr);
	fb.position(0);
	return fb;
	}
	
	private void DrawCursors(GL10 gl)
	{	
		/*
		for(int i = 0; i < cursorPos.size(); i++)
		{		
			float[] cp = cursorPos.elementAt(i);
			float[] cc = cursorColor.elementAt(i);
			
			gl.glPushMatrix();
			
			gl.glTranslatef(cp[0],cp[1],cp[2]);		
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, cursorBuff);
			gl.glColor4f(cc[0], cc[1], cc[2], cc[3]);		
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
			
			gl.glPopMatrix();
		}
		*/
		
		for(int i = 0; i < cursorsRef.size(); i++ )
		{
			cursorsRef.elementAt(i).DrawCursors(gl);
		}
	}
	
	private void CreateCursors(int numbCursors)
	{
		for(int i=0; i < numbCursors; i++)
		{
			float[] pos = new float[3];
			pos[0] = 0;
			pos[1] = 0;
			pos[2] = 1;
			cursorPos.add(pos);
			
			float[] color = new float[4];
			color[0] = 90;
			color[1] = 0;
			color[2] = 0;
			color[3] = 1;
			cursorColor.add(color);
		}
	}
	
	private void ChangeCursorPosition(int cursor, float[] position)
	{
		cursorPos.elementAt(cursor)[0] = position[0];
		cursorPos.elementAt(cursor)[1] = position[1];
		cursorPos.elementAt(cursor)[2] = position[2];
		
		/*Log.i("Renderer", "New cursor pos: " + cursorPos.elementAt(cursor)[0] + 
				", " + cursorPos.elementAt(cursor)[1] +
				", " + cursorPos.elementAt(cursor)[2]);*/
	}
	
	private void ChangeCursorColor(int cursor, float[] color)
	{
		cursorColor.elementAt(cursor)[0] = color[0];
		cursorColor.elementAt(cursor)[1] = color[1];
		cursorColor.elementAt(cursor)[2] = color[2];
		cursorColor.elementAt(cursor)[3] = color[3];
	}
	
	
	/**
    * Record the current modelView matrix state. Has the side effect of
    * setting the current matrix state to GL_MODELVIEW
    * @param gl
    */
   public float[] getCurrentModelView(GL10 gl) 
   {
   	float[] mModelView = new float[16];
       getMatrix(gl, GL10.GL_MODELVIEW, mModelView);
       return mModelView;
   }

   /**
    * Record the current projection matrix state. Has the side effect of
    * setting the current matrix state to GL_PROJECTION
    * @param gl
    */
   public float[] getCurrentProjection(GL10 gl) 
   {
   	float[] mProjection = new float[16];
       getMatrix(gl, GL10.GL_PROJECTION, mProjection);
       return mProjection;
   }

   
   private void getMatrix(GL10 gl, int mode, float[] mat) 
   {
       MatrixTrackingGL gl2 = (MatrixTrackingGL) gl;
       gl2.glMatrixMode(mode);
       gl2.getMatrix(mat, 0);
   }
   
   private void Print(String name, float[] vec)
   {
	   String text = "";
	   for(int i = 0; i < vec.length; i+=1)
	   {
		   text = text + vec[i] + ", ";
	   }
	   Log.i(name, text);
   }
   private void Print(String name, int[] vec)
   {
	   String text = "";
	   for(int i = 0; i < vec.length; i+=1)
	   {
		   text = text + vec[i] + ", " ;
	   }
	   Log.i(name, text);
   }
   
   public Vec2 GetWorldCoords( Vec2 touch, float screenW, float screenH, Camera cam)
   {
	   Log.i("World Coords", "-------------- ");
	   Vec2 worldPos = new Vec2();
	   
	   /* matrice de transformation */
	   float[] m, A, in, out;
	   m = new float[16];
	   A = new float[16];
	   in = new float[4];
	   out = new float[4];

	   int oglCoordSysY = (int) (screenH - cam.Y());
	   
	   int oglTouchY = (int) (screenH - touch.Y());
	   
	   /* transformation coordonnees normalisees entre -1 et 1 */
	   //in[0] = (float) ((touch.X() - cam.X()) * 2.0f / screenW - 1.0);
	   //in[1] = (float) ((touch.Y() - oglCoordSysY) * 2.0f / screenH - 1.0);
	   
	   in[0] = (float) ((touch.X()) * 2.0f / screenW - 1.0);
	   in[1] = (float) ((oglTouchY) * 2.0f / screenH - 1.0);
	   in[2] = - 1.0f;
	   in[3] = 1.0f;
	   
	   Print("In:", in);

	   /* calcul transformation inverse */
	   Matrix.multiplyMM(A, 0, getCurrentProjection(gl), 0, getCurrentModelView(gl), 0);
	   Matrix.invertM(m, 0, A, 0);	   

	   /* d'ou les coordonnees objets */
	   Matrix.multiplyMV(out, 0, m, 0, in, 0);
	   Print("Out: ", out);
	   
	   if (out[3] == 0.0)
	   {
		   Log.e("World coords", "Could not calculate world coordinates");
		   return worldPos;
	   }
	   
	   worldPos.Set(out[0] / out[3], out[1] / out[3]);
	   float worldZ = out[2] / out[3];
	   
	   Log.i("World Coords", "World x: " + worldPos.X() + ", " + worldPos.Y() + ", " + worldZ);
		
	   
	   
	   return worldPos;
	   
   }
   

	
	
}

/*
 * private static final float[] _tempGluUnProjectData = new float[40]; 
	private static final int     _temp_m   = 0; 
	private static final int     _temp_A   = 16; 
	private static final int     _temp_in  = 32; 
	private static final int     _temp_out = 36; 
	
	public float[] GetWorldCoords(float winx, float winy, float winz, int viewport[]) 
	{ 
		if(this.gl == null)
		{
			Log.e("DagRenderer", "Gl not initialized yet");
			return null;
		}
		float[] model = getCurrentModelView(gl);
		float[] proj = getCurrentProjection(gl);
		float[] xyz = new float[3];
		
	   /* Transformation matrices */
		//   float[] m = new float[16], A = new float[16]; 
		//   float[] in = new float[4], out = new float[4]; 
	   /* Normalize between -1 and 1 * 
	   _tempGluUnProjectData[_temp_in]   = (winx - viewport[0]) * 2f / viewport[2] - 1.0f; 
	   _tempGluUnProjectData[_temp_in+1] = (winy - viewport[1]) * 2f / viewport[3] - 1.0f; 
	   _tempGluUnProjectData[_temp_in+2] = 2f * winz - 1.0f; 
	   _tempGluUnProjectData[_temp_in+3] = 1.0f; 
	   
	   /* Get the inverse * 
	   android.opengl.Matrix.multiplyMM(_tempGluUnProjectData, _temp_A, proj, 0, model, 0); 
	   android.opengl.Matrix.invertM(_tempGluUnProjectData, _temp_m, _tempGluUnProjectData, _temp_A); 
	   android.opengl.Matrix.multiplyMV(_tempGluUnProjectData, _temp_out, 
	        _tempGluUnProjectData, _temp_m, 
	        _tempGluUnProjectData, _temp_in); 
	   if (_tempGluUnProjectData[_temp_out+3] == 0.0) 
	   {
		   Log.e("DagRenderer", "Generic ninja error message when trying to use gluUnProject");
		   return null; 
	   }
	   
	   xyz[0]  =  _tempGluUnProjectData[_temp_out  ] / _tempGluUnProjectData[_temp_out+3]; 
	   xyz[1] = _tempGluUnProjectData[_temp_out+1] / _tempGluUnProjectData[_temp_out+3]; 
	   xyz[2] = _tempGluUnProjectData[_temp_out+2] / _tempGluUnProjectData[_temp_out+3]; 
	   
	   return xyz;
	} 
	
	**
     * Record the current modelView matrix state. Has the side effect of
     * setting the current matrix state to GL_MODELVIEW
     * @param gl
     *
    public float[] getCurrentModelView(GL10 gl) 
    {
    	float[] mModelView = new float[16];
        getMatrix(gl, GL10.GL_MODELVIEW, mModelView);
        return mModelView;
    }

    **
     * Record the current projection matrix state. Has the side effect of
     * setting the current matrix state to GL_PROJECTION
     * @param gl
     *
    public float[] getCurrentProjection(GL10 gl) 
    {
    	float[] mProjection = new float[16];
        getMatrix(gl, GL10.GL_PROJECTION, mProjection);
        return mProjection;
    }

    
    private void getMatrix(GL10 gl, int mode, float[] mat) 
    {
        MatrixTrackingGL gl2 = (MatrixTrackingGL) gl;
        gl2.glMatrixMode(mode);
        gl2.getMatrix(mat, 0);
    }
    */

/*
 * int[] view= new int[4];
	   view[0] = cam.X(); view[1] = cam.Y();
	   view[2] = (int) screenW; view[3] = (int) screenH;
	   
	   float[] obj = new float[3];
	   
	   Print("View", view);
	   Print("Obj", obj);
	   Print("Model",getCurrentModelView(gl) );
	   Print("Proj", getCurrentProjection(gl));
	   
	   GLU.gluUnProject(
			   (float)touch.X(), (float)touch.Y(), 0.1f, 
			   getCurrentModelView(gl), 0, 
			   getCurrentProjection(gl), 0, 
			   view, 0, 
			   obj, 0);
	   
	   Log.i("World Coords", "x: " + obj[0] + ", " + obj[1] + ", " + obj[2]);
	   */
