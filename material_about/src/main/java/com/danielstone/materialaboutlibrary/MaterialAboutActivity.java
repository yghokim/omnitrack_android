package com.danielstone.materialaboutlibrary;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import com.danielstone.materialaboutlibrary.adapters.MaterialAboutListAdapter;
import com.danielstone.materialaboutlibrary.model.MaterialAboutList;
import com.danielstone.materialaboutlibrary.util.DefaultViewTypeManager;
import com.danielstone.materialaboutlibrary.util.ViewTypeManager;
import com.google.android.material.appbar.AppBarLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public abstract class MaterialAboutActivity extends AppCompatActivity {

    MaterialAboutList list = new MaterialAboutList.Builder().build();
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private MaterialAboutListAdapter adapter;

    protected abstract MaterialAboutList getMaterialAboutList(Context c);

    protected abstract CharSequence getActivityTitle();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mal_material_about_activity);

        CharSequence title = getActivityTitle();
        if (title == null)
            setTitle(R.string.mal_title_about);
        else
            setTitle(title);


        assignViews();
        initViews();

        ListTask task = new ListTask(this);
        task.execute();

    }

    private void assignViews() {
        toolbar = findViewById(R.id.mal_toolbar);
        recyclerView = findViewById(R.id.mal_recyclerview);
        recyclerView.setAlpha(0f);
        recyclerView.setTranslationY(20);
    }

    private void initViews() {
        setSupportActionBar(toolbar);
        if (NavUtils.getParentActivityName(this) != null) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        adapter = new MaterialAboutListAdapter(list, getViewTypeManager());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    protected ViewTypeManager getViewTypeManager() {
        return new DefaultViewTypeManager();
    }

    protected MaterialAboutList getMaterialAboutList() {
        return list;
    }

    protected void setMaterialAboutList(MaterialAboutList materialAboutList) {
        list = materialAboutList;
        adapter.swapData(materialAboutList);
    }

    protected void setScrollToolbar(boolean scrollToolbar) {
        if (toolbar != null) {
            AppBarLayout.LayoutParams params =
                    (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
            if (scrollToolbar) {
                params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                        | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
            } else {
                params.setScrollFlags(0);
            }
        }
    }

    private class ListTask extends AsyncTask<String, String, String> {

        Context context;

        public ListTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... params) {
            list = getMaterialAboutList(context);
            return null;
        }

        @Override
        protected void onPostExecute(String s) {

            adapter.swapData(list);
            recyclerView.animate().alpha(1f).translationY(0f).setDuration(400).setInterpolator(new FastOutSlowInInterpolator()).start();
            super.onPostExecute(s);
            context = null;
        }
    }
}
