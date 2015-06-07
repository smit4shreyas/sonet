package com.piusvelte.sonet.fragment;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.piusvelte.eidos.Eidos;
import com.piusvelte.sonet.OAuthLogin;
import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.SonetService;
import com.piusvelte.sonet.adapter.AccountAdapter;
import com.piusvelte.sonet.loader.AccountsProfilesLoaderCallback;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.service.AccountUpdateService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;

/**
 * Created by bemmanuel on 4/19/15.
 */
public class AccountsList extends ListFragment implements View.OnClickListener,
        AccountsProfilesLoaderCallback.OnAccountsLoadedListener {

    private static final int LOADER_ACCOUNT_PROFILES = 0;

    private static final int DELETE_ID = 0;

    private static final String DIALOG_ADD_ACCOUNT = "dialog:add_account";

    private static final String FRAGMENT_ACCOUNT_DETAIL = "fragment:account_detail";

    private static final int REQUEST_ADD_ACCOUNT = 1;
    private static final int REQUEST_REFRESH = 2;

    private List<HashMap<String, String>> mAccounts = new ArrayList<>();
    private AccountAdapter mAdapter;
    private FloatingActionButton mFloatingActionButton;

    public AccountsList() {
        setRetainInstance(true);
    }

    public static AccountsList newInstance() {
        return new AccountsList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.accounts, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        registerForContextMenu(getListView());

        mFloatingActionButton = (FloatingActionButton) view.findViewById(R.id.fab);
        mFloatingActionButton.setOnClickListener(this);

        mAdapter = new AccountAdapter(getActivity(), mAccounts);
        setListAdapter(mAdapter);
        getLoaderManager().initLoader(LOADER_ACCOUNT_PROFILES,
                null,
                new AccountsProfilesLoaderCallback(this, LOADER_ACCOUNT_PROFILES));
    }

    @Override
    public void onResume() {
        super.onResume();
        setFABVisibility();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isResumed() && !isRemoving()) {
            setFABVisibility();
        }
    }

    private void setFABVisibility() {
        if (getUserVisibleHint()) {
            if (mFloatingActionButton.getVisibility() != View.VISIBLE) {
                mFloatingActionButton.setTranslationY(getResources().getDimension(R.dimen.fab_animation_height));
                mFloatingActionButton.setVisibility(View.VISIBLE);
                mFloatingActionButton.animate()
                        .translationY(0)
                        .setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime))
                        .start();
            }
        } else if (mFloatingActionButton.getVisibility() == View.VISIBLE) {
            mFloatingActionButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, DELETE_ID, 0, R.string.delete_account);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DELETE_ID:
                getActivity().startService(AccountUpdateService.obtainIntent(getActivity(),
                        AccountUpdateService.ACTION_DELETE,
                        ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id,
                        AppWidgetManager.INVALID_APPWIDGET_ID));
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        HashMap<String, String> account = mAdapter.getItem(position);
        getChildFragmentManager().beginTransaction()
                .add(Settings.newInstance(AccountAdapter.getAccountId(account),
                                AccountAdapter.getAccountService(account),
                                AccountAdapter.getAccountProfileUrl(account),
                                AccountAdapter.getAccountUsername(account)),
                        FRAGMENT_ACCOUNT_DETAIL)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ADD_ACCOUNT:
                startActivityForResult(new Intent(getActivity(), OAuthLogin.class)
                        .putExtra(Accounts.SERVICE,
                                Integer.parseInt(getResources().getStringArray(R.array.service_values)[ItemsDialogFragment.getWhich(data, 0)]))
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, Sonet.INVALID_ACCOUNT_ID)
                        .putExtra(Sonet.EXTRA_ACCOUNT_ID, Sonet.INVALID_ACCOUNT_ID)
                        , REQUEST_REFRESH);
                break;

            case REQUEST_REFRESH:
                if (resultCode == Activity.RESULT_OK) {
                    Eidos.requestBackup(getActivity());
                    getActivity().startService(new Intent(getActivity(), SonetService.class)
                            .setAction(ACTION_REFRESH)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { AppWidgetManager.INVALID_APPWIDGET_ID }));
                    getLoaderManager().restartLoader(LOADER_ACCOUNT_PROFILES,
                            null,
                            new AccountsProfilesLoaderCallback(this, LOADER_ACCOUNT_PROFILES));
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mFloatingActionButton) {
            // add a new account
            String[] services = getResources().getStringArray(R.array.service_entries);
            ItemsDialogFragment.newInstance(services, REQUEST_ADD_ACCOUNT)
                    .show(getChildFragmentManager(), DIALOG_ADD_ACCOUNT);
        }
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void onAccountsLoaded(List<HashMap<String, String>> accounts) {
        mAccounts.clear();

        if (accounts != null) {
            mAccounts.addAll(accounts);
        }

        mAdapter.notifyDataSetChanged();
    }
}
