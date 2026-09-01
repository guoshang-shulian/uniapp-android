//package io.dcloud.uniplugin.activity;
//
//import android.os.Bundle;
//import android.view.MenuItem;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.ActionBar;
//import androidx.fragment.app.Fragment;
//import androidx.viewpager2.adapter.FragmentStateAdapter;
//import androidx.viewpager2.widget.ViewPager2;
//import uni.dcloud.io.uniplugin_module.R;
//import uni.dcloud.io.uniplugin_module.R;
//
///**
// * 短剧接入演示
// */
//public class TubeActivity extends E2EActivity {
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        ActionBar supportActionBar = getSupportActionBar();
//        if (supportActionBar != null) {
//            supportActionBar.setHomeButtonEnabled(true);
//            supportActionBar.setDisplayHomeAsUpEnabled(true);
//        }
//        setContentView(R.layout.activity_tube);
//
//        //setTitle("短剧接入演示");
//
//        ViewPager2 viewPager2 = findViewById(R.id.tubeViewPager);
//        // BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNav);
//        System.out.println("fighting spirit");
//        viewPager2.setAdapter(new FragmentStateAdapter(this) {
//
//            @NonNull
//            @Override
//            public Fragment createFragment(int position) {
//                return position == 1 ? new TubeFragment() : new Fragment();
//            }
//
//            @Override
//            public int getItemCount() {
//                return 3;
//            }
//
//        });
//        viewPager2.setCurrentItem(1, false);
//        //new BottomNavViewPagerMediator(bottomNavigationView, viewPager2, (bottomNavView, viewPager) -> viewPager.setUserInputEnabled(false)).attach();
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
//        if (item.getItemId() == android.R.id.home) {
//            getOnBackPressedDispatcher().onBackPressed();
//        }
//        return super.onOptionsItemSelected(item);
//    }
//
//}
