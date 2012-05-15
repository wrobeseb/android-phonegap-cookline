package com.facebook.samples.myapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.os.Handler;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.android.*;
import com.facebook.android.Facebook.*;

import org.json.JSONException;
import org.json.JSONObject;

public class App extends Activity {
	public static final String APP_ID = "321416411224717";
	
	Facebook facebook = new Facebook(APP_ID);

	private AsyncFacebookRunner mAsyncRunner;
	private Handler mHandler;
	private SharedPreferences mPrefs;
	private TextView mWelcomeLabel;
	private Button mLoginButton;
	private Button mPostFeedButton;
	private Button mSendRequestButton;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // UI properties
        mWelcomeLabel = (TextView) findViewById(R.id.welcomeText);
        mLoginButton = (Button) findViewById(R.id.loginButton);
        mSendRequestButton = (Button) findViewById(R.id.sendRequestButton);
        mPostFeedButton = (Button) findViewById(R.id.postFeedButton);
        
        // Facebook properties
        mAsyncRunner = new AsyncFacebookRunner(facebook);
        mHandler = new Handler();
        
        /*
         * Get existing saved session information
         */
        mPrefs = getPreferences(MODE_PRIVATE);
        String access_token = mPrefs.getString("access_token", null);
        long expires = mPrefs.getLong("access_expires", 0);
        if(access_token != null) {
            facebook.setAccessToken(access_token);
        }
        if(expires != 0) {
            facebook.setAccessExpires(expires);
        }
          
        // Parse any incoming notifications and save
        Uri intentUri = getIntent().getData();
        if (intentUri != null) {
        	String alertMessage = null;
        	// Check if incoming request. If not, an incoming deep link
        	if (intentUri.getQueryParameter("request_ids") != null && intentUri.getQueryParameter("ref").equals("notif")) {
        		alertMessage = "Incoming Request: " + intentUri.getQueryParameter("request_ids");
        	} else {
        		alertMessage = "Incoming Deep Link: " + intentUri.toString();
        	}
        	if (alertMessage != null) {
        		AlertDialog.Builder builder = new AlertDialog.Builder(this);
            	builder.setMessage(alertMessage)
            	       .setCancelable(false)
            	       .setPositiveButton("OK", new DialogInterface.OnClickListener() {
            	           public void onClick(DialogInterface dialog, int id) {
            	        	   dialog.cancel();
            	           }
            	       });
            	AlertDialog alert = builder.create();
            	alert.show();
        	}
        }
        
        if (facebook.isSessionValid()) {
        	// Set the logged in UI
        	mWelcomeLabel.setText(R.string.label_welcome);
        	mLoginButton.setText(R.string.button_logout);

		// Get the user's data
		requestUserData();
        } else {
        	// Set the logged out UI
        	mWelcomeLabel.setText(R.string.label_login);
        	mLoginButton.setText(R.string.button_login);
        }
        
        mLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	// Logging in
                if(!facebook.isSessionValid()) {
                	facebook.authorize(App.this, new DialogListener() {
                        @Override
                        public void onComplete(Bundle values) {
                        	// Set the logged in UI
                        	mWelcomeLabel.setText(R.string.label_welcome);
                        	mLoginButton.setText(R.string.button_logout);
                        	
                        	// Save session data
                        	SharedPreferences.Editor editor = mPrefs.edit();
				editor.putString("access_token", facebook.getAccessToken());
				editor.putLong("access_expires", facebook.getAccessExpires());
				editor.commit();

				// Get the user's data
				requestUserData();
                        }

                        @Override
                        public void onFacebookError(FacebookError error) {}

                        @Override
                        public void onError(DialogError e) {}

                        @Override
                        public void onCancel() {}
                    });
                } else {
                	// Logging out
                	mAsyncRunner.logout(App.this, new BaseRequestListener() {
                		@Override
                		public void onComplete(String response, Object state) {
                			/*
                             * callback should be run in the original thread, not the background
                             * thread
                             */
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                	// Set the logged out UI
                                	mWelcomeLabel.setText(R.string.label_login);
                                	mLoginButton.setText(R.string.button_login);
                                	
                        			// Clear the token information
                        			SharedPreferences.Editor editor = mPrefs.edit();
                        			editor.putString("access_token", null);
                        			editor.putLong("access_expires", 0);
                        			editor.commit();
                                }
                            });
                		}
                	});
                }
            }
        });
        
        mPostFeedButton.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        	     if (facebook.isSessionValid()) {
        		Bundle params = new Bundle();
        		params.putString("caption", "Android Rules");
			params.putString("description", "Check out my awesome Android app designed to be social using Facebook Platform.");
			params.putString("picture", "http://www.facebookmobileweb.com/hackbook/img/facebook_icon_large.png");
			params.putString("name", "I'm using my Android app");
			params.putString("link", "http://www.facebookmobileweb.com/hackbook/");

			facebook.dialog(App.this, "feed", params, new DialogListener() {
            		  @Override
            		  public void onComplete(Bundle values) {
            		    	final String returnId = values.getString("post_id");
            		    	if (returnId != null) {
            		    		// Show the post Id if successful
            		    		Toast.makeText(getApplicationContext(), 
            		                			"Posted: " +  returnId, 
            		                			Toast.LENGTH_SHORT).show();
            		    	}
            		  }

            		  @Override
            		  public void onFacebookError(FacebookError error) {}
            		     
            		  @Override
            		  public void onError(DialogError e) {}

            		  @Override
            		    public void onCancel() {
            		    	// Show a message if the user canceled the post
            		    	Toast.makeText(getApplicationContext(), 
            		    			"Post cancelled", 
            		    			Toast.LENGTH_SHORT).show();
            		  }
            		 });
        		} else {
        			// If the session is not valid show a message
        			Toast.makeText(getApplicationContext(), "You need to login to use post to feed.", 
                    		Toast.LENGTH_LONG).show();
        		}
        	}
        });
        
        mSendRequestButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	 if(facebook.isSessionValid()) {
            		 // A request showing how to send extra data that could signify a gift
            		 Bundle params = new Bundle();
            		 params.putString("message", "Learn how to make your Android apps social");
            		 params.putString("data", 
            		                  "{\"badge_of_awesomeness\":\"1\"," +
            		                  "\"social_karma\":\"5\"}");
            		 facebook.dialog(App.this, "apprequests", params, new DialogListener() {
            		    @Override
            		    public void onComplete(Bundle values) {
            		    	final String returnId = values.getString("request");
            		    	if (returnId != null) {
            		    		// Show the request Id if request sent successfully
            		    		Toast.makeText(getApplicationContext(), 
            		                			"Request sent: " +  returnId, 
            		                			Toast.LENGTH_SHORT).show();
            		    	}
            		    }

            		    @Override
            		    public void onFacebookError(FacebookError error) {}
            		     
            		    @Override
            		    public void onError(DialogError e) {}

            		    @Override
            		    public void onCancel() {
            		    	// Show a message if the user canceled the request
            		    	Toast.makeText(getApplicationContext(), 
            		    			"Request cancelled", 
            		    			Toast.LENGTH_SHORT).show();
            		    }
            		 });
            	 } else {
            		 // If the session is not valid show a message
            		 Toast.makeText(getApplicationContext(), "You need to login to use send requests.", 
                     		Toast.LENGTH_LONG).show();
            	 }
            }
        });
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Callback to handle the post-authorization flow.
        facebook.authorizeCallback(requestCode, resultCode, data);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        // Extend the session information if it is needed
        if ((facebook != null) && facebook.isSessionValid()) {
            facebook.extendAccessTokenIfNeeded(this, null);
        }
    }

    public void requestUserData() {
        Bundle params = new Bundle();
        params.putString("fields", "name");
        mAsyncRunner.request("me", params, new BaseRequestListener() {
        @Override
        public void onComplete(final String response, final Object state) {
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(response);

                final String welcomeText = getString(R.string.label_welcome) + ", " + jsonObject.getString("name");

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mWelcomeLabel.setText(welcomeText);
                    }
                });

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        });
    }

}
