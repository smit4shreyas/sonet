package com.piusvelte.sonet.adapter;

import android.content.Context;
import android.support.annotation.AnimRes;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.piusvelte.sonet.R;
import com.piusvelte.sonet.Sonet;
import com.piusvelte.sonet.provider.Accounts;
import com.piusvelte.sonet.provider.Entity;
import com.piusvelte.sonet.social.Client;
import com.piusvelte.sonet.util.CircleTransformation;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by bemmanuel on 5/16/15.
 */
public class PostAccountsAdapter extends BaseAdapter {

    Context mContext;
    Picasso mPicasso;
    CircleTransformation mCircleTransformation;
    List<HashMap<String, String>> mAccounts;
    HashSet<Integer> mSelection = new HashSet<>();
    OnLocationClickListener mOnLocationClickListener;

    public PostAccountsAdapter(Context context,
            List<HashMap<String, String>> accounts,
            OnLocationClickListener onLocationClickListener) {
        mContext = context;
        mAccounts = accounts;
        mOnLocationClickListener = onLocationClickListener;
        mPicasso = Picasso.with(context);
        mCircleTransformation = new CircleTransformation();
    }

    @Override
    public int getCount() {
        if (mAccounts != null) {
            return mAccounts.size();
        }

        return 0;
    }

    @Override
    public HashMap<String, String> getItem(int position) {
        if (mAccounts != null && position < getCount()) {
            return mAccounts.get(position);
        }

        return null;
    }

    @Override
    public long getItemId(int position) {
        return getAccountId(getItem(position));
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.post_account_row, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.profile = (ImageView) convertView.findViewById(R.id.profile);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
            viewHolder.friend = (TextView) convertView.findViewById(R.id.friend);
            viewHolder.check = convertView.findViewById(R.id.check);
            viewHolder.location = convertView.findViewById(R.id.location);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (viewHolder != null) {
            HashMap<String, String> account = getItem(position);
            String url = getAccountProfileUrl(account);

            if (!TextUtils.isEmpty(url)) {
                mPicasso.load(url)
                        .transform(mCircleTransformation)
                        .into(viewHolder.profile);
            } else {
                mPicasso.load(R.drawable.ic_account_box_grey600_48dp)
                        .transform(mCircleTransformation)
                        .into(viewHolder.profile);
            }

            int service = getAccountService(account);
            Client.Network network = Client.Network.get(service);
            mPicasso.load(network.getIcon())
                    .into(viewHolder.icon);

            if (viewHolder.friend != null) {
                viewHolder.friend.setText(network + ": " + getAccountUsername(account));
            }

            if (mSelection.contains(position)) {
                if (viewHolder.profile.getVisibility() == View.VISIBLE) {
                    int offset = mContext.getResources().getInteger(android.R.integer.config_shortAnimTime);

                    animateVisibility(viewHolder.profile, R.anim.width_collapse, View.INVISIBLE, 0);
                    animateVisibility(viewHolder.check, R.anim.width_expand, View.VISIBLE, offset);

                    if (Client.Network.get(getAccountService(account)).isLocationSupported()) {
                        viewHolder.location.setOnClickListener(new LocationClickListener(mOnLocationClickListener, position));
                        animateVisibility(viewHolder.location, R.anim.slide_left_in, View.VISIBLE, offset);
                    } else {
                        viewHolder.location.setVisibility(View.GONE);
                        viewHolder.location.setOnClickListener(null);
                    }
                }
            } else if (viewHolder.profile.getVisibility() != View.VISIBLE) {
                int offset = mContext.getResources().getInteger(android.R.integer.config_shortAnimTime);

                animateVisibility(viewHolder.check, R.anim.width_collapse, View.GONE, 0);

                if (viewHolder.location.getVisibility() == View.VISIBLE) {
                    animateVisibility(viewHolder.location, R.anim.slide_right_out, View.GONE, 0);
                }

                animateVisibility(viewHolder.profile, R.anim.width_expand, View.VISIBLE, offset);
            }
        }

        return convertView;
    }

    private void animateVisibility(@NonNull final View view, @AnimRes int animResId, final int visibility, int offset) {
        Animation animation = AnimationUtils.loadAnimation(mContext, animResId);
        animation.setStartOffset(offset);

        switch (visibility) {
            case View.VISIBLE:
                view.setVisibility(visibility);
                break;

            case View.INVISIBLE:
                // intentional fall-through
            case View.GONE:
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        // NO-OP
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.setVisibility(visibility);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                        // NO-OP
                    }
                });
                break;
        }

        view.startAnimation(animation);
    }

    public void setSelection(int position, boolean isSelected) {
        if (isSelected) {
            mSelection.add(position);
        } else {
            mSelection.remove(position);
        }
    }

    public void clearSelection() {
        mSelection.clear();
    }

    public static long getAccountId(HashMap<String, String> account) {
        if (account != null) {
            return Long.parseLong(account.get(Accounts._ID));
        }

        return Sonet.INVALID_ACCOUNT_ID;
    }

    public static int getAccountService(HashMap<String, String> account) {
        if (account != null) {
            return Integer.parseInt(account.get(Accounts.SERVICE));
        }

        return 0;
    }

    public static String getAccountProfileUrl(HashMap<String, String> account) {
        if (account != null) {
            return account.get(Entity.PROFILE_URL);
        }

        return null;
    }

    public static String getAccountUsername(HashMap<String, String> account) {
        if (account != null) {
            return account.get(Accounts.USERNAME);
        }

        return null;
    }

    static class ViewHolder {
        ImageView profile;
        ImageView icon;
        TextView friend;
        View check;
        View location;
    }

    private class LocationClickListener implements View.OnClickListener {

        OnLocationClickListener listener;
        int position;

        LocationClickListener(OnLocationClickListener listener, int position) {
            this.listener = listener;
            this.position = position;
        }

        @Override
        public void onClick(View v) {
            listener.onLocationClick(position);
        }
    }

    public interface OnLocationClickListener {
        void onLocationClick(int position);
    }
}
