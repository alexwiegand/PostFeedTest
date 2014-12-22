package com.example.postfeedtest;


import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class MainActivity extends FragmentActivity {
	
	private FBPostFeedFragment mainFragment;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        if (savedInstanceState == null) {
        	// Add the fragment on initial activity setup
        	mainFragment = new FBPostFeedFragment();
            getSupportFragmentManager()
            .beginTransaction()
            .add(android.R.id.content, mainFragment)
            .commit();
        } else {
        	// Or set the fragment from restored state info
        	mainFragment = (FBPostFeedFragment) getSupportFragmentManager()
        	.findFragmentById(android.R.id.content);
        }
    }	
    
}
