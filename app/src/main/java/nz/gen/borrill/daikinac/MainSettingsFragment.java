package nz.gen.borrill.daikinac;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by Chris on 17/07/2015.
 */
public class MainSettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }
}
