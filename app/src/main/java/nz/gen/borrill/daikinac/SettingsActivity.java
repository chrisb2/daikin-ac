package nz.gen.borrill.daikinac;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by Chris on 17/07/2015.
 */
public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainSettingsFragment()).commit();
    }
}
