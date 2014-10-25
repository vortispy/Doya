package vortispy.doya;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

/**
 * Created by vortispy on 2014/07/03.
 */
public class MainTabListener<T extends Fragment>
        implements ActionBar.TabListener {

    private Fragment fragment;
    private final Activity activity;
    private final String tag;
    private final Class<T> cls;
    private ViewPager viewPager;

    public MainTabListener(
            Activity activity, String tag, Class<T> cls, ViewPager viewPager) {
        this.activity = activity;
        this.tag = tag;
        this.cls = cls;
        this.viewPager = viewPager;
    }
    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        if(fragment == null){
            fragment = Fragment.instantiate(activity, cls.getName());
            ft.add(android.R.id.content, fragment, tag);
        }
        else{
            ft.attach(fragment);
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        if(fragment != null){
            ft.detach(fragment);
        }
    }

}