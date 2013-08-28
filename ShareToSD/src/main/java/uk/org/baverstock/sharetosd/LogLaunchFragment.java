package uk.org.baverstock.sharetosd;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class LogLaunchFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.log_launch_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Intent intent = getActivity().getIntent();
        final Uri data = intent.getData();

        if (data == null) {
            getView().setVisibility(View.GONE);
            return;
        }
        getView().setVisibility(View.VISIBLE);

        TextView textView = (TextView) getView().findViewById(R.id.log);
        textView.setText(DownloadService.log.toString());
        textView.append("\n* dataString " + data);
    }
}
