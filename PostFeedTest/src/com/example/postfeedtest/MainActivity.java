package com.example.postfeedtest;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.LoginButton;

public class MainActivity extends FragmentActivity {
	
	private LoginButton loginButton;
	private Button shareButton;
	private GraphUser user;
	
	private static final String TAG = "MainFragment";
	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
	private static final String PENDING_PUBLISH_KEY = "pendingPublishReauthorization";
	private static final String FEED_TITLE = "Post Feed Test App";
	private static final String FEED_DESCRIPTION = "Great sample app to post feed to FB";
	private static final String FEED_PICTURE = "http://www.truthforlife.org/static/uploads/new-android-store.png";
		
	private boolean pendingPublishReauthorization = false;
	private boolean bLoggedIn;
	
	private UiLifecycleHelper uiHelper;

	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);
        
        bLoggedIn = false;
        
        loginButton = (LoginButton) findViewById(R.id.authButton);
        loginButton.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
			
			@Override
			public void onUserInfoFetched(GraphUser user) {
				// TODO Auto-generated method stub
				MainActivity.this.user = user;
				bLoggedIn = true;
				
				updateUI();
			}
		});
        
        shareButton = (Button)findViewById(R.id.btnShare);
        shareButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				postToWall(FEED_TITLE, FEED_DESCRIPTION, FEED_PICTURE);
			}
		});

        if (savedInstanceState != null) {
			pendingPublishReauthorization = 
				savedInstanceState.getBoolean(PENDING_PUBLISH_KEY, false);
		}
        
        updateUI();
    }
    
    @Override
	public void onResume() {
	    super.onResume();
	    Session session = Session.getActiveSession();
	    if (session != null &&
	           (session.isOpened() || session.isClosed()) ) {
	        onSessionStateChange(session, session.getState(), null);
	    }

	    uiHelper.onResume();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    uiHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback() {
	        @Override
	        public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
	            Log.e("Activity", String.format("Error: %s", error.toString()));
	        }

	        @Override
	        public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
	            Log.i("Activity", "Success!");
	        }
	    });
	}

	@Override
	public void onPause() {
	    super.onPause();
	    uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
	    super.onDestroy();
	    uiHelper.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    outState.putBoolean(PENDING_PUBLISH_KEY, pendingPublishReauthorization);
	    uiHelper.onSaveInstanceState(outState);
	}
	
	private void onSessionStateChange(Session session, SessionState state, Exception exception) {
		if (state.isOpened()) {
            
            if (pendingPublishReauthorization && 
            		state.equals(SessionState.OPENED_TOKEN_UPDATED)) {
            	pendingPublishReauthorization = false;
            	postToWall(FEED_TITLE, FEED_DESCRIPTION, FEED_PICTURE);
            }
        }
		
		updateUI();
	}
	
	private Session.StatusCallback callback = new Session.StatusCallback() {
	    @Override
	    public void call(Session session, SessionState state, Exception exception) {
	        onSessionStateChange(session, state, exception);
	    }
	};
	
	/*
	 * Update UI Method
	 */
    
	private void updateUI() {
        Session session = Session.getActiveSession();
        boolean enableButtons = (session != null && session.isOpened());

        shareButton.setVisibility((enableButtons) ? View.VISIBLE : View.INVISIBLE);
    }
	
	/*
	 * Post Method - Title, Text, Picture
	 */
	
	private void postToWall(String title, String description, String picture) {
	    Session session = Session.getActiveSession();
	    if (session != null) {

		    // Check for publish permissions    
		    List<String> permissions = session.getPermissions();
		        if (!isSubsetOf(PERMISSIONS, permissions)) {
		        	pendingPublishReauthorization = true;
		            Session.NewPermissionsRequest newPermissionsRequest = new Session
		                    .NewPermissionsRequest(this, PERMISSIONS);
		            session.requestNewPublishPermissions(newPermissionsRequest);
		            return;
		       }

		    Bundle postParams = new Bundle();
		    postParams.putString("name", "Post Feed Test");
		    postParams.putString("caption", title);
		    postParams.putString("description", description);
		    postParams.putString("link", "https://developers.facebook.com/android");
		    postParams.putString("picture", picture);

		    Request.Callback callback= new Request.Callback() {
				
				@Override
				public void onCompleted(com.facebook.Response response) {
					// TODO Auto-generated method stub
					JSONObject graphResponse = response
                            .getGraphObject()
                            .getInnerJSONObject();
					 String postId = null;
					 try {
					     postId = graphResponse.getString("id");
					 } catch (JSONException e) {
					     Log.i(TAG,
					         "JSON error "+ e.getMessage());
					 }
					 FacebookRequestError error = response.getError();
					 if (error != null) {
					     Toast.makeText(getApplicationContext(),
					          error.getErrorMessage(),
					          Toast.LENGTH_SHORT).show();
				     } else {
				         Toast.makeText(getApplicationContext(), 
				        		 "New Feed posted : " + postId,
				        		 Toast.LENGTH_LONG).show();
				     }					 
				}
			};

		    Request request = new Request(session, "me/feed", postParams, 
		                          HttpMethod.POST, callback);

		    RequestAsyncTask task = new RequestAsyncTask(request);
		    task.execute();
		}
	}
	
	/*
	 * Helper method to check a collection for a string.
	 */
	
	private boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
	    for (String string : subset) {
	        if (!superset.contains(string)) {
	            return false;
	        }
	    }
	    return true;
	}
	
	/*
	 * Get current active session
	 */
	
	public Session getActiveSession() {
		return Session.getActiveSession();		
	}
	
	/*
	 * Check user logged in
	 */
	
	public boolean isLoggedIn() {
		return bLoggedIn;
	}
	
	/*
	 * Get Access Token String
	 */
	
	public String getAccessToken() {
		return Session.getActiveSession().getAccessToken();
	}
}
