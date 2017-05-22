package inaka.com.mangosta.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.c77.androidstreamingclient.lib.rtp.RtpMediaDecoder;

import org.ice4j.TransportAddress;

import java.io.IOException;
import java.util.Properties;

import inaka.com.mangosta.activities.MainMenuActivity;

/**
 * Created by rafalslota on 26/04/2017.
 */
public class VideoStreamFragment extends Fragment implements View.OnClickListener {

    private RtpMediaDecoder rtpMediaDecoder;
    private SurfaceView surface;
    private Properties configuration;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        surface = new SurfaceView(getActivity());
        configuration = new Properties();
        try {
            configuration.load(getActivity().getApplicationContext().getAssets().open("conf.ini"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        rtpMediaDecoder = new RtpMediaDecoder(surface, configuration, getRTPServerPorts());
        surface.setOnClickListener(this);

        return surface;
    }

    @Override
    public void onResume() {
        super.onResume();
        rtpMediaDecoder.restart();
    }

    @Override
    public void onPause() {
        super.onPause();
        rtpMediaDecoder.release();
    }

    @Override
    public void onClick(View v) {
        reloadVideoPlayer();
    }

    public void reloadVideoPlayer() {
        rtpMediaDecoder.setServerPort(getRTPServerPorts());
        rtpMediaDecoder.restart();
    }

    public Pair<Integer, Integer> getRTPServerPorts() {
        MainMenuActivity activity = (MainMenuActivity) getActivity();
        return activity.getProxyRTP().getServerSockPorts();
    }
}
