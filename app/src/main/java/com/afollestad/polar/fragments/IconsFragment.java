package com.afollestad.polar.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.afollestad.polar.R;
import com.afollestad.polar.adapters.IconAdapter;
import com.afollestad.polar.dialogs.IconDetailsDialog;
import com.afollestad.polar.fragments.base.BaseTabFragment;
import com.afollestad.polar.ui.IconPickerActivity;
import com.afollestad.polar.util.DrawableXmlParser;

import java.util.List;
import java.util.TimerTask;

import butterknife.ButterKnife;


public class IconsFragment extends BaseTabFragment implements
        SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    private IconAdapter mAdapter;

    public IconsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.icons, menu);
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem mSearchItem = menu.findItem(R.id.search);
        SearchView mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchItem);
        mSearchView.setQueryHint(getString(R.string.search_icons));
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnCloseListener(this);
        mSearchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_recyclerview, container, false);
        ((TextView) v.findViewById(android.R.id.empty)).setText(R.string.no_results);

        final int gridWidth = getResources().getInteger(R.integer.icon_grid_width);
        final RecyclerView mRecyclerView = ButterKnife.findById(v, android.R.id.list);
        mRecyclerView.setClipToPadding(false);

        final int gridSpacing = getResources().getDimensionPixelOffset(R.dimen.grid_spacing);
        mRecyclerView.setPadding(mRecyclerView.getPaddingLeft() + gridSpacing,
                mRecyclerView.getPaddingTop(),
                mRecyclerView.getPaddingRight() + gridSpacing,
                mRecyclerView.getPaddingBottom());

        mAdapter = new IconAdapter(getActivity(), gridWidth, new IconAdapter.ClickListener() {
            @Override
            public void onClick(View view, int section, int relative, int absolute) {
                selectItem(getActivity(), IconsFragment.this, mAdapter.getIcon(section, relative));
            }
        });

        final GridLayoutManager lm = new GridLayoutManager(getActivity(), gridWidth);
        mAdapter.setLayoutManager(lm);
        mRecyclerView.setLayoutManager(lm);
        mRecyclerView.setAdapter(mAdapter);
        return v;
    }

    public static void selectItem(FragmentActivity context, Fragment context2, DrawableXmlParser.Icon icon) {
        Bitmap bmp = null;
        if (icon.getId(context) != 0) {
            //noinspection ConstantConditions
            bmp = ((BitmapDrawable) ResourcesCompat.getDrawable(context.getResources(),
                    icon.getId(context), null)).getBitmap();
        }
        if (context instanceof IconPickerActivity) {
            context.setResult(Activity.RESULT_OK, new Intent().putExtra("icon", bmp));
            context.finish();
        } else {
            FragmentManager fm;
            if (context2 != null) fm = context2.getChildFragmentManager();
            else fm = context.getSupportFragmentManager();
            IconDetailsDialog.create(bmp, icon).show(fm, "ICON_DETAILS_DIALOG");
        }
    }

    private void setListShown(boolean shown) {
        final View v = getView();
        if (v != null) {
            v.findViewById(android.R.id.list).setVisibility(shown ?
                    View.VISIBLE : View.GONE);
            v.findViewById(android.R.id.progress).setVisibility(shown ?
                    View.GONE : View.VISIBLE);
            v.findViewById(android.R.id.empty).setVisibility(shown && mAdapter.getItemCount() == 0 ?
                    View.VISIBLE : View.GONE);
        }
    }

    @Override
    public int getTitle() {
        return R.string.icons;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) load();
    }

    private void load() {
        if (mAdapter.getItemCount() > 0) return;
        setListShown(false);
        final Handler mHandler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<DrawableXmlParser.Category> categories = DrawableXmlParser.parse(getActivity(), R.xml.drawable);
                mHandler.post(new TimerTask() {
                    @Override
                    public void run() {
                        mAdapter.set(categories);
                        setListShown(true);
                    }
                });
            }
        }).start();
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        mAdapter.filter(s);
        setListShown(true);
        return false;
    }

    @Override
    public boolean onClose() {
        mAdapter.filter(null);
        setListShown(true);
        return false;
    }
}