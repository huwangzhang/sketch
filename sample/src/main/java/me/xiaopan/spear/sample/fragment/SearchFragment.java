package me.xiaopan.spear.sample.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.etsy.android.grid.StaggeredGridView;

import org.apache.http.HttpResponse;

import java.util.ArrayList;
import java.util.List;

import me.xiaopan.android.gohttp.GoHttp;
import me.xiaopan.android.gohttp.HttpRequest;
import me.xiaopan.android.gohttp.HttpRequestFuture;
import me.xiaopan.android.gohttp.JsonHttpResponseHandler;
import me.xiaopan.android.inject.InjectContentView;
import me.xiaopan.android.inject.InjectExtra;
import me.xiaopan.android.inject.InjectView;
import me.xiaopan.android.widget.PullRefreshLayout;
import me.xiaopan.spear.sample.MyFragment;
import me.xiaopan.spear.sample.R;
import me.xiaopan.spear.sample.activity.DetailActivity;
import me.xiaopan.spear.sample.activity.WindowBackgroundManager;
import me.xiaopan.spear.sample.adapter.SearchImageAdapter;
import me.xiaopan.spear.sample.net.request.SearchImageRequest;
import me.xiaopan.spear.sample.net.request.StarImageRequest;
import me.xiaopan.spear.sample.util.ScrollingPauseLoadManager;
import me.xiaopan.spear.sample.widget.HintView;
import me.xiaopan.spear.sample.widget.LoadMoreFooterView;

/**
 * 图片搜索Fragment
 */
@InjectContentView(R.layout.fragment_search)
public class SearchFragment extends MyFragment implements SearchImageAdapter.OnItemClickListener, PullRefreshLayout.OnRefreshListener, LoadMoreFooterView.OnLoadMoreListener {
    public static final String PARAM_OPTIONAL_STRING_SEARCH_KEYWORD = "PARAM_OPTIONAL_STRING_SEARCH_KEYWORD";

    @InjectView(R.id.refreshLayout_search) PullRefreshLayout pullRefreshLayout;
    @InjectView(R.id.list_search) private StaggeredGridView recyclerView;
    @InjectView(R.id.hintView_search) private HintView hintView;

    private SearchImageRequest searchImageRequest;
    private HttpRequestFuture refreshRequestFuture;
    private HttpRequestFuture loadMoreRequestFuture;
    private SearchImageAdapter searchImageListAdapter;
    private WindowBackgroundManager.WindowBackgroundLoader windowBackgroundLoader;
    private LoadMoreFooterView loadMoreFooterView;

    @InjectExtra(PARAM_OPTIONAL_STRING_SEARCH_KEYWORD) private String searchKeyword = "GIF";

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if(activity != null && activity instanceof WindowBackgroundManager.OnSetWindowBackgroundListener){
            windowBackgroundLoader = new WindowBackgroundManager.WindowBackgroundLoader(activity.getBaseContext(), (WindowBackgroundManager.OnSetWindowBackgroundListener) activity);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        searchImageRequest = new SearchImageRequest(searchKeyword);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setTitle(searchKeyword);
    }

    private void setTitle(String subtitle){
        if(getActivity() != null && getActivity() instanceof ActionBarActivity){
            ActionBar actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
            if(actionBar != null){
                actionBar.setTitle(subtitle);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.menu_search_view, menu);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.menu_searchView));
        searchView.setQueryHint(searchKeyword);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                s = s.trim();
                if ("".equals(s)) {
                    Toast.makeText(getActivity(), "搜索关键字不能为空", Toast.LENGTH_LONG).show();
                    return false;
                }

                setTitle(s);
                Bundle bundle = new Bundle();
                bundle.putString(SearchFragment.PARAM_OPTIONAL_STRING_SEARCH_KEYWORD, s);
                SearchFragment searchFragment = new SearchFragment();
                searchFragment.setArguments(bundle);
                getFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.window_push_enter, R.anim.window_push_exit)
                        .replace(R.id.frame_main_content, searchFragment)
                        .commit();

                ((InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(getActivity().getCurrentFocus()
                                        .getWindowToken(),
                                InputMethodManager.HIDE_NOT_ALWAYS);

                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pullRefreshLayout.setOnRefreshListener(this);

        recyclerView.setOnScrollListener(new ScrollingPauseLoadManager(view.getContext()));

        if (searchImageListAdapter == null) {
            pullRefreshLayout.startRefresh();
        } else {
            setAdapter(searchImageListAdapter);
            if(windowBackgroundLoader != null){
                windowBackgroundLoader.restore();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        loadMoreFooterView = null;
    }

    @Override
    public void onDetach() {
        if (refreshRequestFuture != null && !refreshRequestFuture.isFinished()) {
            refreshRequestFuture.cancel(true);
        }
        if(windowBackgroundLoader != null){
            windowBackgroundLoader.detach();
        }
        super.onDetach();
    }

    @Override
    protected void onUserVisibleChanged(boolean isVisibleToUser) {
        if(windowBackgroundLoader != null){
            windowBackgroundLoader.setUserVisible(isVisibleToUser);
        }
    }

    private void setAdapter(SearchImageAdapter adapter){
        if(loadMoreFooterView == null){
            loadMoreFooterView = new LoadMoreFooterView(getActivity(), recyclerView);
            loadMoreFooterView.setOnLoadMoreListener(this);
            recyclerView.addFooterView(loadMoreFooterView);
        }
        recyclerView.setAdapter(searchImageListAdapter = adapter);
        recyclerView.scheduleLayoutAnimation();
    }

    @Override
    public void onRefresh() {
        if (refreshRequestFuture != null && !refreshRequestFuture.isFinished()) {
            return;
        }

        if(loadMoreRequestFuture != null && !loadMoreRequestFuture.isFinished()){
            loadMoreRequestFuture.cancel(true);
        }

        searchImageRequest.setStart(0);
        refreshRequestFuture = GoHttp.with(getActivity()).newRequest(searchImageRequest, new JsonHttpResponseHandler(SearchImageRequest.Response.class), new HttpRequest.Listener<SearchImageRequest.Response>() {
            @Override
            public void onStarted(HttpRequest httpRequest) {
                hintView.hidden();
            }

            @Override
            public void onCompleted(HttpRequest httpRequest, HttpResponse httpResponse, SearchImageRequest.Response responseObject, boolean b, boolean b2) {
                if (getActivity() == null) {
                    return;
                }

                List<StarImageRequest.Image> imageList = new ArrayList<StarImageRequest.Image>();
                for (SearchImageRequest.Image image : responseObject.getImages()) {
                    imageList.add(image);
                }
                setAdapter(new SearchImageAdapter(getActivity(), recyclerView, imageList, SearchFragment.this));
                pullRefreshLayout.stopRefresh();

                if(loadMoreFooterView != null && loadMoreFooterView.isEnd()){
                    loadMoreFooterView.setEnd(false);
                }

                if(windowBackgroundLoader != null && imageList.size() > 0){
                    windowBackgroundLoader.load(imageList.get(0).getSourceUrl());
                }
            }

            @Override
            public void onFailed(HttpRequest httpRequest, HttpResponse httpResponse, HttpRequest.Failure failure, boolean b, boolean b2) {
                if (getActivity() == null) {
                    return;
                }

                pullRefreshLayout.stopRefresh();
                if (searchImageListAdapter == null) {
                    hintView.failure(failure, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            pullRefreshLayout.startRefresh();
                        }
                    });
                } else {
                    Toast.makeText(getActivity(), "刷新失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCanceled(HttpRequest httpRequest) {

            }
        }).responseHandleCompletedAfterListener(new SearchImageRequest.ResponseHandler()).go();
    }

    @Override
    public void onItemClick(int position, StarImageRequest.Image image) {
        DetailActivity.launch(getActivity(), (ArrayList<String>) searchImageListAdapter.getImageUrlList(), position);
    }

    @Override
    public void onLoadMore(final LoadMoreFooterView loadMoreFooterView) {
        searchImageRequest.setStart(searchImageListAdapter.getDataSize());
        loadMoreRequestFuture = GoHttp.with(getActivity()).newRequest(searchImageRequest, new JsonHttpResponseHandler(SearchImageRequest.Response.class), new HttpRequest.Listener<SearchImageRequest.Response>() {
            @Override
            public void onStarted(HttpRequest httpRequest) {

            }

            @Override
            public void onCompleted(HttpRequest httpRequest, HttpResponse httpResponse, SearchImageRequest.Response responseObject, boolean b, boolean b2) {
                if (getActivity() == null) {
                    return;
                }

                List<StarImageRequest.Image> newImageList = null;
                if (responseObject.getImages() != null) {
                    newImageList = new ArrayList<StarImageRequest.Image>();
                    for (SearchImageRequest.Image image : responseObject.getImages()) {
                        newImageList.add(image);
                    }
                }

                if (newImageList != null && newImageList.size() > 0) {
                    searchImageListAdapter.append(newImageList);
                    if (newImageList.size() < searchImageRequest.getSize()) {
                        loadMoreFooterView.setEnd(true);
                        Toast.makeText(getActivity(), "新送达" + newImageList.size() + "个包裹，已全部送完！", Toast.LENGTH_SHORT).show();
                    } else {
                        loadMoreFooterView.loadFinished(true);
                        Toast.makeText(getActivity(), "新送达" + newImageList.size() + "个包裹", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    loadMoreFooterView.setEnd(true);
                    Toast.makeText(getActivity(), "没有您的包裹了", Toast.LENGTH_SHORT).show();
                }
                searchImageListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailed(HttpRequest httpRequest, HttpResponse httpResponse, HttpRequest.Failure failure, boolean b, boolean b2) {
                if (getActivity() == null) {
                    return;
                }
                loadMoreFooterView.loadFinished(false);
                Toast.makeText(getActivity(), "快递投递失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCanceled(HttpRequest httpRequest) {
                loadMoreFooterView.loadFinished(false);
            }
        }).responseHandleCompletedAfterListener(new SearchImageRequest.ResponseHandler()).go();
    }
}
