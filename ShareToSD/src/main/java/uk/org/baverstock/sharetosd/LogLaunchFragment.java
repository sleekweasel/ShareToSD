package uk.org.baverstock.sharetosd;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

public class LogLaunchFragment extends Fragment{
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
            return;
        }

        TextView textView = (TextView) getView().findViewById(R.id.log);
        textView.setText(DownloadService.log.toString());
        textView.append("\n* dataString " + data);

        final File file = new File(data.getPath());

        Button openDirectory = (Button) getView().findViewById(R.id.open_directory);
        openDirectory.setText("Open " + file.getParent());
        openDirectory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view)
            {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file.getParentFile()), "resource/folder");
                startActivity(intent);
            }
        });

        Button openFile = (Button) getView().findViewById(R.id.open_file);
        final String type = intent.getType();
        openFile.setText("Open " + type + " " + file.getName());
        openFile.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), type);
                startActivity(intent);
            }
        });
    }
}
