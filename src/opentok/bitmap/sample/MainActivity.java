package opentok.bitmap.sample;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import opentok.bitmap.sample.R;

import com.opentok.android.BaseVideoCapturer;
import com.opentok.android.BaseVideoRenderer;
import com.opentok.android.Connection;
import com.opentok.android.OpentokError;
import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;


public class MainActivity extends Activity implements
		Session.SessionListener, Publisher.PublisherListener, Session.ConnectionListener,
		Session.SignalListener
{

	Camera mCamera;
	Bitmap bitmap1,bitmap2;
	
	private Runnable rnbl_stream = new Runnable() 
	{
		
		@Override
		public void run() 
		{
			int iTotalBitmapsStreamed = 0;
			while (iTotalBitmapsStreamed < 750)
			{
				if (bStreaming)
				{
					if (iTotalBitmapsStreamed % 2 == 0)
						sendFrameToStreamer(bitmap1);
					else
						sendFrameToStreamer(bitmap2);
					
					// Simulate frame rate 25Hz
					try 
					{
						Thread.sleep(40);
					}
					catch (InterruptedException e) 
					{
						e.printStackTrace();
					}
					iTotalBitmapsStreamed++;
				}
			}
		}
	};

	Thread streamThread;
	
	private static final String LOGTAG = "demo-hello-world";
    private Session mSession;
    private Publisher mPublisher;
    private CustomVideoCapturer customVideoCapturer;
    volatile boolean bStreaming = false;
    @Override
	public void onStart() 
	{
		super.onStart();
	}

	@Override
	public void onStop() 
	{
		super.onStop();
	}

	private void sendFrameToStreamer(Bitmap bmp) 
	{
		
		int[] pixels = new int[bmp.getWidth()*bmp.getHeight()];
		bmp.getPixels(pixels, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());

		customVideoCapturer.provideIntArrayFrame(pixels, BaseVideoCapturer.ARGB, bmp.getWidth(), bmp.getHeight(), 0, false);
	}

	@Override
	public void onPause() 
	{
		try
		{
			if (null != mCamera)
			mCamera.release();
		}
		catch (Exception e) {}
		super.onPause();
	}

	@Override
	public void onDestroy() 
	{
		try
		{
			if (null != mCamera)
			mCamera.release();
		}
		catch (Exception e) {}
		super.onDestroy();
	}

	@Override
	public void onResume() 
	{
		super.onResume();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/**
			Opening the camera before starting OpenTok session caouse an Exception
			to be thrown by org.webrtc.videoengine.VideoCaptureDeviceInfoAndroid.getDeviceInfo()
		*/
		try
		{
			mCamera = Camera.open();
		}
		catch (Exception e) {}
 
		customVideoCapturer = new CustomVideoCapturer(MainActivity.this);
		
		// Load two bitmaps to be sent to the CustomVideoCapturer
		BitmapFactory.Options o = new Options();
		o.inScaled = false;
		bitmap1 = BitmapFactory.decodeResource(this.getResources(), R.drawable.bitmap1, o);
		bitmap2 = BitmapFactory.decodeResource(this.getResources(), R.drawable.bitmap2, o);
				
		// Connect chat session and start publishing
        chatSessionConnect();
        
        // This thread will stream two swapping bitmaps in ~25Hz
        streamThread = new Thread(rnbl_stream);
        
	}
		
	@Override
	public void onBackPressed() 
	{
        if (mSession != null) {
            mSession.disconnect();
        }
        
        this.finish();
		super.onBackPressed();
	}
	
	/**
	 * 
	 * OpenTok Methods
	 * 
	 */
	
	private void chatSessionConnect()
	{
        GetRoomDataTask task = new GetRoomDataTask();
        task.execute("12345", "Nickname");
	}
	
    private void sessionConnect(String api_key, String session_id, String token)
    {
        if (mSession == null) 
        {
            mSession = new Session(MainActivity.this, api_key, session_id);
            mSession.setSessionListener(this);
            mSession.setConnectionListener(this);
            mSession.setSignalListener(this);
            mSession.connect(token);
        }
    }
	
	@Override
	public void onConnected(Session arg0) 
	{
		Log.i(LOGTAG, "Connected to the session.");
        if (mPublisher == null) {
            mPublisher = new Publisher(MainActivity.this, "publisher");
            mPublisher.setPublisherListener(this);
            mPublisher.setCapturer(customVideoCapturer);
            mPublisher.setPublishAudio(false);
            attachPublisherView(mPublisher);
            mSession.publish(mPublisher);
            bStreaming = true;
            streamThread.start();
            Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_LONG).show();
        }
	}
	
    private void attachPublisherView(Publisher publisher) {
        mPublisher.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE,
                BaseVideoRenderer.STYLE_VIDEO_FIT);
    }

    @Override
	public void onDisconnected(Session arg0) {
        mPublisher = null;
        mSession = null;   	
	}

	@Override
	public void onError(Session arg0, OpentokError exception) {}

	@Override
    public void onStreamDropped(Session session, Stream stream) {}

	@Override
    public void onStreamReceived(Session session, Stream stream) {}

	@Override
	public void onError(PublisherKit arg0, OpentokError exception) {}

	@Override
    public void onStreamCreated(PublisherKit publisher, Stream stream) {}

	@Override
	public void onStreamDestroyed(PublisherKit publisher, Stream stream) 
	{
		bStreaming = false;
	}
    
    private class GetRoomDataTask extends AsyncTask<String, Void, String[]> {

        protected HttpClient mHttpClient;

        protected HttpGet mHttpGet;

        public GetRoomDataTask() {
            mHttpClient = new DefaultHttpClient();
        }

        @Override
        protected String[] doInBackground(String... params) {
            String sessionId = null;
            String token = null;
            String apiKey = null;
            initializeGetRequest(params[0]);
            try {
                HttpResponse roomResponse = mHttpClient.execute(mHttpGet);
                HttpEntity roomEntity = roomResponse.getEntity();
                String temp = EntityUtils.toString(roomEntity);
                Log.i(LOGTAG, "retrieved room response: " + temp);
                JSONObject roomJson = new JSONObject(temp);
                sessionId = roomJson.getString("sid");
                token = roomJson.getString("token");
                apiKey = roomJson.getString("apiKey");
            } catch (Exception exception) {
                Log.e(LOGTAG, "could not get room data: " + exception.getMessage());
                return null;
            }
            return new String[] {params[0], sessionId, token, apiKey, params[1]};
        }

        @Override
		protected void onPostExecute(String[] result) {
			super.onPostExecute(result);
			
			if (null == result)
			{
				Log.e(LOGTAG, "No result for server!");
				return;
			}
			
			sessionConnect(result[3], result[1], result[2]);
			
			
		}

		protected void initializeGetRequest(String room) {
            URI roomURI;
            URL url;

            String urlStr = "https://opentokrtc.com/" + room + ".json";
            try {
                url = new URL(urlStr);
                roomURI = new URI(url.getProtocol(), url.getUserInfo(),
                        url.getHost(), url.getPort(), url.getPath(),
                        url.getQuery(), url.getRef());
            } catch (URISyntaxException exception) {
                Log.e(LOGTAG,
                        "the room URI is malformed: " + exception.getMessage());
                return;
            } catch (MalformedURLException exception) {
                Log.e(LOGTAG,
                        "the room URI is malformed: " + exception.getMessage());
                return;
            }
            mHttpGet = new HttpGet(roomURI);
        }
		
    }
    
	@Override
	public void onConnectionCreated(Session seesion, Connection connection) {
		Toast.makeText(getApplicationContext(), "Client connected", Toast.LENGTH_SHORT).show();		
	}

	@Override
	public void onConnectionDestroyed(Session seesion, Connection connection) {
		Toast.makeText(getApplicationContext(), "Client disconnected", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onSignalReceived(Session seesion, String type, String data, Connection connection) {
		
		// Just a test: Enable audio when any message is received
		if(type.compareTo("chat") == 0)
			mPublisher.setPublishAudio(true);
		
		if (null == connection)
			return;
		// data is json formated {name=user,text=msg,date,from,connectionId}
		
		Toast.makeText(getApplicationContext(), "Signal Received: type=" + type + " | data="+data+" | connectionId="+connection.getConnectionId(), Toast.LENGTH_SHORT).show();		
	}
}