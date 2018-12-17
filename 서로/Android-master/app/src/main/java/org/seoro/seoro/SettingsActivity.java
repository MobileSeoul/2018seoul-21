package org.seoro.seoro;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.seoro.seoro.recyclerview.SettingsAdapter;

public class SettingsActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mRecyclerView = findViewById(R.id.SettingsActivity_RecyclerView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        SettingsAdapter settingsAdapter = new SettingsAdapter(this);
        settingsAdapter.setOnItemClickListener(new SettingsAdapter.OnItemClickListener() {
            @Override
            public void onClick(View v, int position) {
                switch (position) {
                    case 1:
                        break;
                    case 2:
                        break;
                    case 4: {
                        Intent intent = new Intent(
                                SettingsActivity.this,
                                ChangePasswordActivity.class
                        );
                        startActivity(intent);

                        break;
                    }
                    case 5:
                        break;
                    case 6:
                        startActivity(new Intent(
                                SettingsActivity.this,
                                OpenSourceLicenseActivity.class
                        ));

                        break;
                }
            }
        });

        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setAdapter(settingsAdapter);
    }
}
