package com.mainframevampire.ryan.wheretobuy.ui;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mainframevampire.ryan.wheretobuy.R;
import com.mainframevampire.ryan.wheretobuy.adapters.GridAdapter;
import com.mainframevampire.ryan.wheretobuy.database.ProductsDataSource;
import com.mainframevampire.ryan.wheretobuy.model.BioIsland;
import com.mainframevampire.ryan.wheretobuy.model.Blackmores;
import com.mainframevampire.ryan.wheretobuy.model.ListName;
import com.mainframevampire.ryan.wheretobuy.model.Ostelin;
import com.mainframevampire.ryan.wheretobuy.model.ProductPrice;
import com.mainframevampire.ryan.wheretobuy.model.Swisse;
import com.mainframevampire.ryan.wheretobuy.util.ConfigHelper;
import com.mainframevampire.ryan.wheretobuy.util.GetInfoFromWebsite;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName() ;
    public static final String LIST_NAME = "LIST_NAME";
    public static final String IS_FIRST_RUN = "IS_FIRST_RUN";
    private ProgressDialog mProgressDialogFirstTime;
    private float mFloat = 0;
    private String mLastUpdateDate;
    private String mCurrentDate;

    private ProgressBar mProgressBar;
    private ImageView mRefreshImageView;
    private TextView mRate;
    private TextView mLastUpdateDateTextView;

    private RecyclerView mGridRecyclerView;
    private TextView mHeader;
    private Handler mHandler;
    private ArrayList<ProductPrice> mProductPrices;
    private GridAdapter mGridAdapter;
    private GridLayoutManager mGridLayoutManager;

    private int mNumColumns = 0;
    private int mNumRows = 0;
    private int mNumberOfOnePage = 0;
    private int mTotalCounts = 0;

    //define a custom intent action
    public static final String BROADCAST_ACTION = "com.mainframevampire.ryan.wheretobuy.BROADCAST";
    public static final String KEY_MESSAGE = "com.mainframevampire.ryan.wheretobuy.MESSAGE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mRefreshImageView = (ImageView) findViewById(R.id.refreshImageView);
        mLastUpdateDateTextView = (TextView) findViewById(R.id.lastUpdateDate);
        mGridRecyclerView = (RecyclerView) findViewById(R.id.bestChoiceRecyclerView);
        mHeader = (TextView) findViewById(R.id.recommended_header);
        mRate = (TextView) findViewById(R.id.rate);
        mHandler = new Handler();

        mProgressBar.setVisibility(View.INVISIBLE);

        ProductsDataSource dataSource = new ProductsDataSource(MainActivity.this);
        mLastUpdateDate = dataSource.readProductsTableWithId("OST004").getLastUpdateDateString();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        mCurrentDate = dateFormat.format(new Date());

        String lastUpdateSummary = getString(R.string.last_update_date_is) + " " + mLastUpdateDate;
        mLastUpdateDateTextView.setText(lastUpdateSummary);

        int countOfTable = dataSource.readProductsTableGetCount();
        if (isNetworkAvailable()) {
            new getRateInBackGround().execute();
            if (countOfTable == 0) {
                //if count == 0, then app first run
                toggleRefresh();
                new DownloadPriceFirstTime().execute();
            } else {
                //if last update date is not today, download the data in the background
                if(!lastUpdateIsToday()){
                    toggleRefresh();
                    //new DownloadPriceInBackground().execute();
                    String message = String.format("Last update is %s, the latest price is downloading", mLastUpdateDate);
                    mLastUpdateDateTextView.setText(message);
                    for (String brand : ListName.Brands) {
                        Intent intent = new Intent(this, DownloadService.class);
                        intent.putExtra(IS_FIRST_RUN, false);
                        intent.putExtra(LIST_NAME, brand);
                        startService(intent);
                    }
                }
                //load recommations to the list
                loadDataToGridList();
            }
        } else {
            if (countOfTable == 0) {
                mProgressDialogFirstTime = new ProgressDialog(MainActivity.this);
                mProgressDialogFirstTime.setCancelable(false);
                mProgressDialogFirstTime.setIndeterminate(false);
                mProgressDialogFirstTime.setTitle("please open your network");
                mProgressDialogFirstTime.setMessage("This is the app's first run, products' info needs to be downloaded");
                mProgressDialogFirstTime.show();
            }
            else {
                loadDataToGridList();
            }
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(KEY_MESSAGE);
            //update recommations to the list
            updateDataInGridList();
            if (message.equals("Ostelin")) {
                toggleRefresh();
                String lastUpdateSummary = getString(R.string.last_update_date_is) + " " + mCurrentDate;
                mLastUpdateDateTextView.setText(lastUpdateSummary);
                mLastUpdateDateTextView.setTextColor(Color.BLACK);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(BROADCAST_ACTION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    //hide share menu item
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem shareItem = menu.findItem(R.id.share);
        shareItem.setVisible(false);
        MenuItem postFacebookItem = menu.findItem(R.id.post_facebook);
        postFacebookItem.setVisible(false);
        return super.onPrepareOptionsMenu(menu);
    }

    //menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this, SearchResultsActivity.class)));
        searchView.setQueryHint(getResources().getString(R.string.search_hint));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        ProductsDataSource dataSource = new ProductsDataSource(MainActivity.this);
        int countCustomisedProducts = dataSource.readTableGetCustomisedCount();
        int countBlackmoresProducts = dataSource.readTableGetBrandCount("Blackmores");
        int countBioislandProducts = dataSource.readTableGetBrandCount("BioIsland");
        int countOsterlinProducts = dataSource.readTableGetBrandCount("Ostelin");
        switch (item.getItemId()) {
            case R.id.swisse:
                intent = new Intent(this, ProductListActivity.class);
                intent.putExtra(LIST_NAME, "Swisse");
                startActivity(intent);
                return true;
            case R.id.blackmores:
                if (countBlackmoresProducts != Blackmores.id.length) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("still downloading")
                            .setMessage("Please wait for Blackmores products' price to be downloaded");
                    builder.create().show();
                } else {
                    intent = new Intent(this, ProductListActivity.class);
                    intent.putExtra(LIST_NAME, "Blackmores");
                    startActivity(intent);
                }
                return true;
            case R.id.bioIsland:
                if (countBioislandProducts != BioIsland.id.length) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("still downloading")
                            .setMessage("Please wait for BioIsland products' price to be downloaded");
                    builder.create().show();
                } else {
                    intent = new Intent(this, ProductListActivity.class);
                    intent.putExtra(LIST_NAME, "BioIsland");
                    startActivity(intent);
                }
                return true;
            case R.id.ostelin:
                if (countOsterlinProducts != Ostelin.id.length) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("still downloading")
                            .setMessage("Please wait for Ostelin products' price to be downloaded");
                    builder.create().show();
                } else {
                    intent = new Intent(this, ProductListActivity.class);
                    intent.putExtra(LIST_NAME, "Ostelin");
                    startActivity(intent);
                }
                return true;
            case R.id.customise:
                if (countCustomisedProducts == 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("No Products in MYLIST")
                            .setMessage("Please add your favourite products in each branch list");
                    builder.create().show();
                } else {
                    intent = new Intent(this, ProductListActivity.class);
                    intent.putExtra(LIST_NAME, "MyList");
                    startActivity(intent);
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }



    private class DownloadPriceFirstTime extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialogFirstTime = new ProgressDialog(MainActivity.this);
            mProgressDialogFirstTime.setTitle("Downloading info from Websites");
            mProgressDialogFirstTime.setMessage("Products'information needs to be downloaded for app's first run");
            //mProgressDialogFirstTime.show();
            //mLastUpdateDateTextView.setText(R.string.text_for_first_run);
            //mLastUpdateDateTextView.setTextColor(Color.RED);
            mProgressDialogFirstTime.setCancelable(false);
            mProgressDialogFirstTime.setIndeterminate(false);
            mProgressDialogFirstTime.show();
            mLastUpdateDateTextView.setText(R.string.text_for_first_run);
            mLastUpdateDateTextView.setTextColor(Color.RED);
        }

        @Override
        protected Void doInBackground(Void... params) {
            GetInfoFromWebsite.getSwissePrice();
            createSwisseValueInTable();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mProgressDialogFirstTime.dismiss();
            //load recommations to the list
            loadDataToGridList();
            for (String brand : ListName.Brands) {
                if (!brand.equals("Swisse")) {
                    Intent intent = new Intent(MainActivity.this, DownloadService.class);
                    intent.putExtra(IS_FIRST_RUN, true);
                    intent.putExtra(LIST_NAME, brand);
                    startService(intent);
                }
            }
            mLastUpdateDateTextView.setText("Other products' price is still downloading");
        }
    }

    private class getRateInBackGround extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            mFloat = GetInfoFromWebsite.getExchangeRate();
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mRate.setText("AUD/CNY:" + mFloat);
        }

    }

    private void createSwisseValueInTable() {
        ProductsDataSource dataSource = new ProductsDataSource(MainActivity.this);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String currentDateString = dateFormat.format(new Date());
        for (int i = 0; i < Swisse.id.length; i++) {
            String recommendationFlag = getRecomendationFlag(Swisse.lowestPrice[i], Swisse.highestPrice[i]);
            ProductPrice productPrice = new ProductPrice(
                    Swisse.id[i],
                    Swisse.shortName[i],
                    Swisse.longName[i],
                    "Swisse",
                    Swisse.lowestPrice[i],
                    Swisse.highestPrice[i],
                    Swisse.whichIsLowest[i],
                    Swisse.information[i],
                    Swisse.cmwPrice[i],
                    Swisse.plPrice[i],
                    Swisse.flPrice[i],
                    Swisse.twPrice[i],
                    Swisse.hwPrice[i],
                    Swisse.cmwURL[i],
                    Swisse.plURL[i],
                    Swisse.flURL[i],
                    Swisse.twURL[i],
                    Swisse.hwURL[i],
                    "N",
                    recommendationFlag,
                    currentDateString);
            dataSource.createContents(productPrice);
        }
    }



    private String getRecomendationFlag(Float lowestPrice, Float highestPrice) {
        Float savePrice = highestPrice - lowestPrice;
        if (savePrice/highestPrice >= 0.4) {
            return "Y";
        } else {
            return "N";
        }
    }

    private boolean lastUpdateIsToday() {
        return mCurrentDate.equals(mLastUpdateDate);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()){
            isAvailable = true;
        }
        return isAvailable;
    }

    private void toggleRefresh(){
        if (mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    private void loadDataToGridList() {
        ProductsDataSource dataSource = new ProductsDataSource(MainActivity.this);
        mNumColumns = ConfigHelper.getNumberColumn(this);
        //get best choices
        mGridRecyclerView = (RecyclerView) findViewById(R.id.bestChoiceRecyclerView);
        mGridRecyclerView.setHasFixedSize(true);

        mTotalCounts = dataSource.readTableGetRecommendedCount();

        mNumRows = ConfigHelper.getNumberRowsForGrid(this);
        mNumberOfOnePage = (mNumRows + 1) * mNumColumns;
        Log.d(TAG, "mTotalCounts:" + mTotalCounts);
        Log.d(TAG, "mNumberOfOnePage:" + mNumberOfOnePage);
        Log.d(TAG, "mNumRows:" + mNumRows);
        Log.d(TAG, "mNumColumns:" + mNumColumns);

        //clear data if it's not null
        if (mProductPrices == null) {
            //load first page data
            mProductPrices = dataSource.readTableByRecommendationFlag("Y", mNumberOfOnePage, " ");
            mGridLayoutManager = new GridLayoutManager(this, mNumColumns, GridLayoutManager.VERTICAL, false);
            mGridRecyclerView.setLayoutManager(mGridLayoutManager);
            mGridRecyclerView.setHasFixedSize(true);

            mGridAdapter = new GridAdapter(this, mProductPrices, mGridRecyclerView, mNumRows);
            Log.d(TAG, "mProductPrices first load:" + mProductPrices.size());
            Log.d(TAG, "lastIdInPreviousPage first load: " + mProductPrices.get(mProductPrices.size() - 1).getID());
            mGridRecyclerView.setAdapter(mGridAdapter);
        }

        mGridAdapter.setOnLoadListener(new GridAdapter.OnLoadListener() {
            @Override
            public void onLoadHeader() {
                if (mTotalCounts >= mNumberOfOnePage) {
                    Log.d(TAG, "you've reached the top");
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mHeader.setVisibility(View.VISIBLE);
                        }
                    });

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mHeader.setVisibility(View.GONE);
                        }
                    }, 1000);
                }
            }

            @Override
            public void onLoadData() {
                if (mTotalCounts >= mNumberOfOnePage) {
                    //add progress item
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "mProductPrices.size() before add: " + mProductPrices.size());
                            mProductPrices.add(null);
                            mGridAdapter.notifyItemInserted(mProductPrices.size() - 1);
                            Log.d(TAG, " mAdapter progress bar ");
                            Log.d(TAG, "mProductPrices.size() after add: " + mProductPrices.size());
                        }
                    });

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //remove progress item
                            Log.d(TAG, "mProductPrices.size() before remove: " + mProductPrices.size());
                            mProductPrices.remove(mProductPrices.size() - 1);
                            mGridAdapter.notifyItemRemoved(mProductPrices.size());
                            Log.d(TAG, "mProductPrices.size() after remove: " + mProductPrices.size());

                            if (mProductPrices.size() == mTotalCounts) {
                                Log.d(TAG, " reached the end");
                                mGridAdapter.setLoaded();
                            } else {
                                loadNextRecommendedProductsFromDatabase();
                            }
                        }
                    }, 2000);
                }
            }
        });

        mGridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (mGridAdapter.getItemViewType(position)) {
                    case 1: //item view
                        return 1;
                    case 0: //progress bar
                        return mNumColumns; //number of columns of the grid
                    default:
                        return -1;
                }
            }
        });

    }

    private void updateDataInGridList() {
        if (mProductPrices != null) {
            ProductsDataSource dataSource = new ProductsDataSource(MainActivity.this);
            mTotalCounts = dataSource.readTableGetRecommendedCount();
            ArrayList<ProductPrice> productPrices = dataSource.readTableByRecommendationFlag("Y", mNumberOfOnePage, " ");
            mGridAdapter.updateData(productPrices);
            mProductPrices = productPrices;
            mGridAdapter.notifyDataSetChanged();
            mGridAdapter.setLoaded();
        }
    }

    //append the next page of data into the adapter
    private void loadNextRecommendedProductsFromDatabase() {
        final ProductsDataSource dataSource = new ProductsDataSource(this);
        final ArrayList<ProductPrice> productPrices;

        final String lastIdInPreviousPage = mProductPrices.get(mProductPrices.size() - 1).getID();

        productPrices = dataSource.readTableByRecommendationFlag("Y", mNumberOfOnePage, lastIdInPreviousPage);


        for (ProductPrice productPrice: productPrices) {
            mProductPrices.add(productPrice);
        }
        mGridAdapter.notifyItemInserted(mProductPrices.size() - 1);
        mGridAdapter.setLoaded();
    }



}
