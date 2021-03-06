package bd.edu.daffodilvarsity.classorganizer.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.ybq.android.spinkit.SpinKitView;

import org.polaric.colorful.ColorfulActivity;

import bd.edu.daffodilvarsity.classorganizer.R;
import bd.edu.daffodilvarsity.classorganizer.adapter.WelcomeSlideAdapter;
import bd.edu.daffodilvarsity.classorganizer.data.Download;
import bd.edu.daffodilvarsity.classorganizer.service.UpdateService;
import bd.edu.daffodilvarsity.classorganizer.utils.CourseUtils;
import bd.edu.daffodilvarsity.classorganizer.utils.DataChecker;
import bd.edu.daffodilvarsity.classorganizer.utils.PrefManager;
import bd.edu.daffodilvarsity.classorganizer.utils.UpdateGetter;
import io.reactivex.disposables.Disposable;

public class WelcomeActivity extends ColorfulActivity implements AdapterView.OnItemSelectedListener {

    private static final String TAG = "WelcomeActivity";

    private ViewPager viewPager;
    private WelcomeSlideAdapter myViewPagerAdapter;
    private LinearLayout dotsLayout;
    private TextView[] dots;
    private int[] layouts;
    private Button btnPrevious, btnNext;
    private TextView checkText;
    private ProgressBar progressBar;
    private Disposable mDisposable;
    private ImageView cloud;
    public boolean isActivityRunning = false;
    //  viewpager change listener
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);
            onPageSelectedCustom(position);
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };
    private PrefManager prefManager;

    @Override
    protected void onResume() {
        super.onResume();
        isActivityRunning = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityRunning = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Checking for first time launch - before calling setContentView()
        prefManager = new PrefManager(this);
        registerReceiver();
        if (!prefManager.isFirstTimeLaunch()) {
            launchHomeScreen();
            finish();
        }

        // Making notification bar transparent
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        setContentView(R.layout.activity_welcome);


        viewPager = (ViewPager) findViewById(R.id.view_pager);
        dotsLayout = (LinearLayout) findViewById(R.id.layoutDots);
        btnPrevious = (Button) findViewById(R.id.btn_previous);
        btnNext = (Button) findViewById(R.id.btn_next);


        /* layouts of all welcome sliders */
        layouts = new int[]{
                R.layout.welcome_slide1,
                R.layout.welcome_slide2,
                R.layout.welcome_slide3,
                R.layout.welcome_slide4,
                R.layout.welcome_slide5,
                R.layout.welcome_slide6};

        // adding bottom dots
        addBottomDots(0);

        /* making notification bar transparent*/
        changeStatusBarColor();

        myViewPagerAdapter = new WelcomeSlideAdapter(this, layouts);
        viewPager.setAdapter(myViewPagerAdapter);
        viewPager.addOnPageChangeListener(viewPagerPageChangeListener);
        //Disabling previous on the opening page
        onPageSelectedCustom(0);

        //disabling swipe
        viewPager.setOnTouchListener((v, event) -> true);

        btnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int current = getItem(0);
                if (findViewById(R.id.spinner_load_spin) != null) {
                    findViewById(R.id.spinner_load_spin).setVisibility(View.GONE);
                }
                viewPager.setCurrentItem(current - 1);
                if ((current - 1) == 0) {
                    onPageSelectedCustom(0);
                }
            }
        });

        btnNext.setOnClickListener(v -> {

            // checking for last page
            // if last page home screen will be launched
            int current = getItem(+1);
            if (btnNext.getText().toString().equalsIgnoreCase(getResources().getString(R.string.skip))) {
            }
            if (current == layouts.length - 1) {
                //class slide
                if (myViewPagerAdapter.getClassDataCode() > 0) {
                    DataChecker.errorMessage(WelcomeActivity.this, myViewPagerAdapter.getClassDataCode(), null);
                    showSnackBar(myViewPagerAdapter.getCampus(), myViewPagerAdapter.getDept(),
                            myViewPagerAdapter.getProgram(), myViewPagerAdapter.getSection(),
                            Integer.toString(myViewPagerAdapter.getLevel() + 1),
                            Integer.toString(myViewPagerAdapter.getTerm() + 1));
                    viewPager.setCurrentItem(current - 1);
                } else {
                    viewPager.setCurrentItem(current);
                }
            } else if (current == layouts.length - 2) {
                //campus slide
                String[] params = new String[]{myViewPagerAdapter.getCampus(), myViewPagerAdapter.getDept(), myViewPagerAdapter.getProgram()};
                if (myViewPagerAdapter.getCampusDataCode() > 0 ) {
                    DataChecker.errorMessage(WelcomeActivity.this, myViewPagerAdapter.getCampusDataCode(), null);
                    viewPager.setCurrentItem(current - 1);
                } else {
                    new ClassSlidePreloadTask().execute(params);
                }


            } else if (current == layouts.length - 3) {
                //user type slide
                viewPager.setCurrentItem(current);
            } else if (current < layouts.length) {
                // move to next screen
                viewPager.setCurrentItem(current);
            } else {
                prefManager.setSemesterCount(CourseUtils.getInstance(getApplicationContext()).getSemesterCount(prefManager.getCampus(), prefManager.getDept(), prefManager.getProgram()));
                prefManager.saveSemester(CourseUtils.getInstance(getApplicationContext()).getCurrentSemester(prefManager.getCampus(), prefManager.getDept(), prefManager.getProgram()));
                myViewPagerAdapter.loadSemester();
                launchHomeScreen();
            }
        });
    }

    private void addBottomDots(int currentPage) {
        dots = new TextView[layouts.length];

        int[] colorsActive = getResources().getIntArray(R.array.array_dot_active);
        int[] colorsInactive = getResources().getIntArray(R.array.array_dot_inactive);

        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(colorsInactive[currentPage]);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0)
            dots[currentPage].setTextColor(colorsActive[currentPage]);
    }

    private int getItem(int i) {
        return viewPager.getCurrentItem() + i;
    }

    private void launchHomeScreen() {
        prefManager.setFirstTimeLaunch(false);
        Intent i = new Intent(WelcomeActivity.this, MainActivity.class);
        startActivity(i);
        finish();
    }

    public void onPageSelectedCustom(int position) {
        if (position < 3) {
            btnPrevious.setVisibility(View.GONE);
        } else {
            btnPrevious.setVisibility(View.VISIBLE);
        }
        // changing the next button text 'NEXT' / 'GOT IT'
        if (position == layouts.length - 1) {
            // last page. make button text to GOT IT
            btnNext.setPadding(0, 0, 32, 0);
            btnNext.setText(getString(R.string.start));
        } else if (position == 1) {
            btnNext.setText(R.string.skip);
            if (cloud == null) {
                cloud = (ImageView) findViewById(R.id.cloud_icon);
            }
            if (checkText == null) {
                checkText = (TextView) findViewById(R.id.check_Text);
            }
            if (progressBar == null) {
                progressBar = findViewById(R.id.welcome_routine_update_progress);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    progressBar.setProgressTintList(ColorStateList.valueOf(Color.WHITE));
                }
            }
            checkForUpdate();

        } else {
            // still pages are left
            btnNext.setText(getString(R.string.next));
        }
    }

    /**
     * Making notification bar transparent
     */
    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    /*Method to display snackbar properly*/
    public void showSnackBar(final String campus, final String dept, final String program, final String section, final String level, final String term) {
        String message = getString(R.string.contact_mailll);
        View rootView = findViewById(R.id.welcome_view_pager_parent);
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG);
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.bg_screen4));
        snackbar.setAction(R.string.send_mail, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                composeEmail(campus, dept, program, section, level, term);
            }
        });
        snackbar.show();
    }

    public void composeEmail(String campus, String department, String program, String section, String level, String term) {
        String appVersion = null;
        PackageInfo packageInfo = null;
        try {
            packageInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (packageInfo != null) {
            appVersion = packageInfo.versionName;
        }
        String message = "Campus: " + campus.substring(0, 1).toUpperCase() + campus.substring(1, campus.length()).toLowerCase();
        message += "\nDepartment: " + department.toUpperCase();
        message += "\nProgram: " + program.substring(0, 1).toUpperCase() + program.substring(1, program.length()).toLowerCase();
        message += "\nSection: " + section + "\nLevel: " + level + "\nTerm: " + term;
        message += "\nApp version: " + appVersion;
        message += "\nDB version: " + prefManager.getDatabaseVersion();
        message += "\n";
        message += "\n*** Important: Insert your class routine for quicker response ***";
        String subject = getString(R.string.suggestion_email_subject);
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{getResources().getString(R.string.auth_email)});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private class ClassSlidePreloadTask extends AsyncTask<String, Void, Void> {

        private static final String TAG = "ClassSlidePreloadTask";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.e(TAG, "Started");
            SpinKitView spinKitView = (SpinKitView) findViewById(R.id.spinner_load_spin);
            spinKitView.setVisibility(View.VISIBLE);
            btnNext.setVisibility(View.GONE);
        }

        @Override
        protected Void doInBackground(final String... params) {
            Long start = System.currentTimeMillis();
            if (myViewPagerAdapter.isStudent()) {
                if (myViewPagerAdapter.getCampusDataCode() > 0) {
                    DataChecker.errorMessage(WelcomeActivity.this, myViewPagerAdapter.getCampusDataCode(), null);
                    showSnackBar(myViewPagerAdapter.getCampus(), myViewPagerAdapter.getDept(), myViewPagerAdapter.getProgram(), myViewPagerAdapter.getSection(), Integer.toString(myViewPagerAdapter.getLevel() + 1), Integer.toString(myViewPagerAdapter.getTerm() + 1));
                } else {
                    myViewPagerAdapter.getClassHelper().createClassAdapters(prefManager.getCampus(), prefManager.getDept(), prefManager.getProgram());
                }
            } else {
                String campus = params[0];
                String department = params[1];
                String program = params[2];
                myViewPagerAdapter.getClassHelper().createTeacherInitAdapter(campus, department, program);
                Log.e(TAG, "Doing");
            }
            Long end = System.currentTimeMillis();
            Log.e(TAG, "Done in "+(end-start)+" ms");
            return null;
        }

        @Override
        protected void onPostExecute(Void current) {
            super.onPostExecute(current);
            int currentItem;
            Long start = System.currentTimeMillis();
            if (myViewPagerAdapter.isStudent()) {
                if (myViewPagerAdapter.getCampusDataCode() > 0) {
                    currentItem = layouts.length - 3;
                } else {
                    myViewPagerAdapter.getClassHelper().attachClassSpinners();
                    currentItem = layouts.length - 2;
                }
            } else {
                myViewPagerAdapter.getClassHelper().attachTeacherInitAdapter();
                currentItem = layouts.length - 2;
            }
            btnNext.setVisibility(View.VISIBLE);
            viewPager.setCurrentItem(currentItem);
            Long end = System.currentTimeMillis();
            Log.e(TAG, "Finished in "+(end-start)+" ms");
        }


    }
    public void setStatusText(String statusText) {
        if (checkText != null && isActivityRunning) {
            checkText.setText(statusText);
        }
    }

    public void startDownload() {
        setStatusText(getResources().getString(R.string.downloading_update_text));
        setProgressBar(0);
    }

    public void setProgressBar(int progress) {
        if (progressBar != null && isActivityRunning) {
            if (progress > 0 && progress <= 100) {
                if (progressBar.getVisibility() == View.GONE) {
                    progressBar.setVisibility(View.VISIBLE);
                }
                progressBar.setIndeterminate(false);
                progressBar.setProgress(progress);
            } else if (progress == UpdateService.UPDATE_NORMAL || progress == UpdateService.UPDATE_SEMESTER) {
                progressBar.setVisibility(View.GONE);
                btnNext.setText(getString(R.string.next));
            } else if (progress == UpdateService.UPDATE_VERIFYING) {
                if (progressBar.getVisibility() == View.GONE) {
                    progressBar.setVisibility(View.VISIBLE);
                }
                progressBar.setIndeterminate(true);
            } else if (progress < 0){
                progressBar.setVisibility(View.GONE);
                btnNext.setText(getString(R.string.next));
            } else {
                progressBar.setIndeterminate(true);
            }
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction() != null && intent.getAction().equals(UpdateService.PROGRESS_UPDATE)){

                Download download = intent.getParcelableExtra(UpdateService.TAG_DOWNLOAD);

                if(download.getProgress() == UpdateService.UPDATE_NORMAL || download.getProgress() == UpdateService.UPDATE_SEMESTER){
                    //routine updated
                    setProgressBar(UpdateService.UPDATE_NORMAL);
                    setStatusText(getString(R.string.update_successful_text));
                    if (cloud != null && isActivityRunning) cloud.setImageResource(R.drawable.ic_cloud_done_white_48dp);
                } else if (download.getProgress() < 0) {
                    setStatusText(getString(R.string.download_failed_welcome));
                } else {
                    setProgressBar(download.getProgress());
                }
            }
        }
    };

    private void registerReceiver(){
        LocalBroadcastManager bManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UpdateService.PROGRESS_UPDATE);
        bManager.registerReceiver(broadcastReceiver, intentFilter);
    }

    public void noUpdateAvailable() {
        setStatusText(getString(R.string.already_updated_text));
        setProgressBar(UpdateService.UPDATE_FAILED);
    }

    private void checkForUpdate() {
        if (checkText != null) {
            checkText.setText(R.string.check_latest_routine_text);
        }
        if (cloud != null) {
            cloud.setImageResource(R.drawable.ic_cloud_download_white_48dp);
        }
        progressBar.setIndeterminate(true);
        mDisposable = UpdateGetter.getInstance(this).getUpdate();
    }
}