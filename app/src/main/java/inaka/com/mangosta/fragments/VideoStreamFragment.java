package inaka.com.mangosta.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.c77.androidstreamingclient.lib.rtp.RtpMediaDecoder;

import java.io.IOException;
import java.net.SocketException;
import java.util.Properties;

import inaka.com.mangosta.videostream.ProxyRTPServer;
import inaka.com.mangosta.videostream.VideoStreamBinding;

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
        
        return surface;
    }

    @Override
    public void onResume() {
        super.onResume();
        rtpMediaDecoder = new RtpMediaDecoder(surface, configuration);
        rtpMediaDecoder.start();
        surface.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        rtpMediaDecoder.restart();
    }
}
