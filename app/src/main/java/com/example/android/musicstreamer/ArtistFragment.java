package com.example.android.musicstreamer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Created by kenm on 7/8/2015.
 */
public class ArtistFragment extends Fragment {
    private final String LOG_TAG = "ArtistFragment";
    private View rootView;
    private OnItemSelectedListener listener;

    ListView mAristListView;
    ArtistAdapter mArtistAdapter;
    EditText mEditText;

    private static final String POSITION = "POSITION";
    private static final String ID       = "ID";
    private static final String ARTIST   = "ARTIST";

    public ArtistFragment() {
    }

    public interface OnItemSelectedListener {
        public void onArtistItemSelected(Bundle bundle);
        public void onArtistEntered();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.artist_fragment, container, false);
        Log.v(LOG_TAG, "onCreateView");

        SetupViewList();
        SetupEditText();
        SetupAdapter();

        if ((savedInstanceState != null) &&
            (savedInstanceState.containsKey(getActivity().getString(R.string.saved_artist_state)))) {
            mArtistAdapter.restore(savedInstanceState);
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.v(LOG_TAG, "onSaveInstanceState");
        mArtistAdapter.save(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.v(LOG_TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        Log.v(LOG_TAG, "onAttach");

        if (activity instanceof OnItemSelectedListener) {
            listener = (OnItemSelectedListener) activity;
        } else {
            throw new ClassCastException(activity.toString()
                    + " must implemenet ArtistFragment.OnItemSelectedListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.v(LOG_TAG, "onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.v(LOG_TAG, "onStop");
    }

    @Override
    public void onPause() {
        Log.v(LOG_TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        Log.v(LOG_TAG, "onDetach");

        listener = null;
    }

    @Override
    public void onDestroy() {
        Log.v(LOG_TAG, "onDestroy");
        super.onDestroy();
    }

    private void SetupViewList(){
        Log.d(LOG_TAG, "SetupViewList");
        mAristListView = (ListView) rootView.findViewById(R.id.listview_artists);

        mAristListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Artist artist = (Artist) parent.getAdapter().getItem(position);

                Bundle bundle = new Bundle();
                bundle.putInt(POSITION, position);
                bundle.putString(ID, artist.getSpotifyID());
                bundle.putString(ARTIST, artist.getName());

                listener.onArtistItemSelected(bundle);
            }
        });
    }

    private void SetupAdapter(){
        Log.d(LOG_TAG, "SetupAdapter");
        mArtistAdapter = new ArtistAdapter(getActivity().getApplicationContext(),R.layout.artist_list_item);
        mAristListView.setAdapter(mArtistAdapter);
    }

    private void emptyViewList() {
        Log.d(LOG_TAG, "emptyViewList");
        mArtistAdapter.clear();
    }

    private void noArtistFound(){
        Log.d(LOG_TAG, "noArtistFound");
        String temp = getActivity().getApplicationContext().getResources().getString(R.string.artist_not_found);
        Toast.makeText(getActivity().getApplicationContext(), temp, Toast.LENGTH_SHORT).show();

        emptyViewList();
    }

    // setup the edit text field and look for an ENTER
    // onEnter..., clear list or show list based on the info we have
    private void SetupEditText() {
        Log.d(LOG_TAG, "SetupEditText");
        mEditText = (EditText) rootView.findViewById(R.id.userInputArtistText);
        mEditText.clearFocus();
        mEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d(LOG_TAG, "onFocusChange hasFocus=" +hasFocus);
                if (hasFocus) {
                    InputMethodManager in = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    in.showSoftInput(mEditText, 0);
                }
            }
        });

        mEditText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(LOG_TAG, "onKey keyCode=" +keyCode);

                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    if (mEditText.getText().length() > 0) {
                        mArtistAdapter.fetchNewData(mEditText.getText().toString());
                    } else {
                        noArtistFound();
                    }

                    // sometimes the keyboard hangs around, lets make sure we remove it when enter is pressed...
                    HideKeyboard();
                    listener.onArtistEntered();

                    return true;
                }
                return false;
            }
        });
    }

    private void HideKeyboard() {
        Log.d(LOG_TAG, "HideKeyboard");
        InputMethodManager in = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        in.hideSoftInputFromWindow(mEditText.getApplicationWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

}
