package utils;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.netappeasy.fala.common.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import guide.util.ScreenUtils;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;


/**
 * @author wangyt
 * @date 2018/5/7
 * : adapter拓展适配器，兼容旧适配器，而不用修改adapter，并且拓展了空视图和没有更多视图；
 */

public class RvStateHelper extends RecyclerView.Adapter {

    public static final int STATE_NORMAL = 1;
    public static final int STATE_EMPTY = 2;
    public static final int STATE_ERROR = 3;
    public static final int STATE_NO_MORE = 4;
    public static final int STATE_DEFAULT = 5;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATE_NORMAL, STATE_EMPTY, STATE_ERROR, STATE_NO_MORE, STATE_DEFAULT})
    public @interface RecycleViewState {
    }

    /**
     * 错误视图点击
     */
    public interface OnErrorClickListener {
        /**
         * 错误视图点击回调
         *
         * @param view
         */
        void onErrorClick(View view);
    }

    /**
     * 空视图点击
     */
    public interface OnEmptyClickListener {
        /**
         * 空视图点击回调
         *
         * @param view
         */
        void onEmptyClick(View view);
    }

    /**
     * 没有更多点击
     */
    public interface OnNoMoreClickListener {
        /**
         * 没有更多点击回调
         *
         * @param view
         */
        void onNoMoreClick(View view);
    }

    /**
     * 空视图ViewType
     */
    private static final int VIEW_TYPE_EMPTY = 1000;
    /**
     * 没有更多ViewType
     */
    private static final int VIEW_TYPE_NO_MORE = 1001;
    /**
     * 错误页面ViewType
     */
    private static final int VIEW_TYPE_ERROR = 1002;

    /**
     * 未加载页面ViewType
     */
    private static final int VIEW_TYPE_DEFAULT = 1003;

    /**
     * 被代理的适配器引用
     */
    private RecyclerView.Adapter delegate;
    private RecyclerView mRecyclerView;
    private View mErrorView;
    private View mDefaultView;
    private View mEmptyView;
    private View mNoMoreView;
    private View mTvNoMoreView;
    private OnEmptyClickListener onEmptyClickListener;
    private OnErrorClickListener onErrorClickListener;
    private OnNoMoreClickListener onNoMoreClickListener;

    @RecycleViewState
    private int mState = STATE_EMPTY;

    public RvStateHelper(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;

        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        this.delegate = recyclerView.getAdapter();
        RecyclerView.AdapterDataObserver dataObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                notifyDataSetChanged();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                notifyItemRangeChanged(positionStart, itemCount);
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
                notifyItemRangeChanged(positionStart, itemCount, payload);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                notifyItemRangeInserted(positionStart, itemCount);
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                notifyItemRangeRemoved(positionStart, itemCount);
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                notifyItemMoved(fromPosition, toPosition);
            }
        };
        this.delegate.registerAdapterDataObserver(dataObserver);
        if (layoutManager instanceof GridLayoutManager) {
            final GridLayoutManager manager = (GridLayoutManager) layoutManager;
            final GridLayoutManager.SpanSizeLookup o = manager.getSpanSizeLookup();
            manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int type = getItemViewType(position);
                    if (type == VIEW_TYPE_NO_MORE || type == VIEW_TYPE_EMPTY || type == VIEW_TYPE_ERROR || type == VIEW_TYPE_DEFAULT) {
                        return manager.getSpanCount();
                    }
                    return o.getSpanSize(position);
                }
            });
        }
        mRecyclerView.setAdapter(this);
        createEmptyView();
        createErrorView();
        createDefaultView();
        createNoMoreView();
    }

    private void createErrorView() {
        if (mErrorView == null) {
            mErrorView = LayoutInflater.from(mRecyclerView.getContext()).inflate(R.layout.default_error, mRecyclerView, false);
        }
    }

    private void createEmptyView() {
        if (mEmptyView == null) {
            mEmptyView = LayoutInflater.from(mRecyclerView.getContext()).inflate(R.layout.default_empty, mRecyclerView, false);
        }
    }

    private void createDefaultView() {
        if (mDefaultView == null) {
            mDefaultView = LayoutInflater.from(mRecyclerView.getContext()).inflate(R.layout.default_loading, mRecyclerView, false);
        }
    }

    private void createNoMoreView() {
        if (mNoMoreView == null) {
            mNoMoreView = LayoutInflater.from(mRecyclerView.getContext()).inflate(R.layout.default_no_more, mRecyclerView, false);
            mTvNoMoreView = mNoMoreView.findViewById(R.id.tvNoMore);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_NO_MORE) {
            return new NoMoreHolder(mNoMoreView);
        } else if (viewType == VIEW_TYPE_EMPTY) {
            return new EmptyHolder(mEmptyView);
        } else if (viewType == VIEW_TYPE_ERROR) {
            return new ErrorHolder(mErrorView);
        } else if (viewType == VIEW_TYPE_DEFAULT) {
            return new DefaultHolder(mDefaultView);
        }
        return delegate.onCreateViewHolder(parent, viewType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (!(holder instanceof NoMoreHolder) && !(holder instanceof EmptyHolder) && !(holder instanceof ErrorHolder) && !(holder instanceof DefaultHolder)) {
            delegate.onBindViewHolder(holder, position);
        }
    }

    @Override
    public int getItemCount() {
        switch (mState) {
            case STATE_EMPTY:
            case STATE_ERROR:
            case STATE_DEFAULT:
                return 1;
            case STATE_NORMAL:
                return delegate.getItemCount();
            case STATE_NO_MORE:
                return delegate.getItemCount() + 1;
            default:
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        switch (mState) {
            case STATE_EMPTY:
                return VIEW_TYPE_EMPTY;
            case STATE_ERROR:
                return VIEW_TYPE_ERROR;
            case STATE_DEFAULT:
                return VIEW_TYPE_DEFAULT;
            case STATE_NORMAL:
                return delegate.getItemViewType(position);
            case STATE_NO_MORE:
                if (position >= delegate.getItemCount()) {
                    return VIEW_TYPE_NO_MORE;
                } else {
                    return delegate.getItemViewType(position);
                }
            default:
        }
        return -1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (!(holder instanceof DefaultHolder) && !(holder instanceof NoMoreHolder) && !(holder instanceof EmptyHolder) && !(holder instanceof ErrorHolder)) {
            delegate.onViewAttachedToWindow(holder);
        } else {
            ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
            if (lp != null && lp instanceof StaggeredGridLayoutManager.LayoutParams) {
                StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) lp;
                p.setFullSpan(true);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (!(holder instanceof DefaultHolder) && !(holder instanceof NoMoreHolder) && !(holder instanceof EmptyHolder) && !(holder instanceof ErrorHolder)) {
            delegate.onViewDetachedFromWindow(holder);
        }
    }

    /**
     * 设置rv状态
     *
     * @param state @see RecycleViewState
     */
    public void setState(@RecycleViewState int state) {
        if (mState == state) {
            return;
        }
        this.mState = state;
        notifyDataSetChanged();
    }

    /**
     * 设置错误视图图标
     *
     * @param res 资源id
     */
    @SuppressWarnings("ConstantConditions")
    public void setErrorViewIcon(@DrawableRes int res) {
        createErrorView();
        Context context = mErrorView.getContext();
        Drawable drawable = ContextCompat.getDrawable(context, res);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        TextView textView = mErrorView.findViewById(R.id.tvError);
        textView.setCompoundDrawables(null, drawable, null, null);
    }

    /**
     * 设置错误视图描述
     *
     * @param charSequence 描述
     */
    public void setErrorViewText(CharSequence charSequence) {
        createErrorView();
        TextView textView = mErrorView.findViewById(R.id.tvError);
        textView.setText(charSequence);
    }

    /**
     * 设置空视图图标
     *
     * @param res 资源id
     */
    @SuppressWarnings("ConstantConditions")
    public void setEmptyViewIcon(@DrawableRes int res) {
        createEmptyView();
        Context context = mEmptyView.getContext();
        Drawable drawable = ContextCompat.getDrawable(context, res);
        drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());
        TextView textView = mEmptyView.findViewById(R.id.tvEmpty);
        textView.setCompoundDrawables(null, drawable, null, null);
    }

    /**
     * 设置空视图描述
     *
     * @param charSequence 描述
     */
    public void setEmptyViewText(CharSequence charSequence) {
        createEmptyView();
        TextView textView = mEmptyView.findViewById(R.id.tvEmpty);
        textView.setText(charSequence);
    }

    /**
     * 设置加载视图
     *
     * @param res 资源id
     */
    public void setDefaultView(@DrawableRes int res) {
        createDefaultView();
        ImageView ivDefault = mDefaultView.findViewById(R.id.ivDefault);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(ivDefault.getResources(), res, options);
        float width = ScreenUtils.getScreenWidth(ivDefault.getContext());
        float imageWidth = 1.0f * options.outWidth * options.inTargetDensity / options.inDensity;
        float scale = width / imageWidth;
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        ivDefault.setImageMatrix(matrix);
        ivDefault.setImageResource(res);
    }

    /**
     * 空视图点击监听
     *
     * @param onEmptyClickListener 监听器
     */
    public void setOnEmptyClickListener(RvStateHelper.OnEmptyClickListener onEmptyClickListener) {
        this.onEmptyClickListener = onEmptyClickListener;
    }

    /**
     * 错误视图点击监听
     *
     * @param onErrorClickListener 监听器
     */
    public void setOnErrorClickListener(RvStateHelper.OnErrorClickListener onErrorClickListener) {
        this.onErrorClickListener = onErrorClickListener;
    }

    /**
     * 没有更多视图点击监听
     *
     * @param onNoMoreClickListener 监听器
     */
    public void setOnNoMoreClickListener(RvStateHelper.OnNoMoreClickListener onNoMoreClickListener) {
        this.onNoMoreClickListener = onNoMoreClickListener;
    }

    public View getEmptyView() {
        return mEmptyView;
    }

    public View getNoMoreView() {
        return mTvNoMoreView;
    }

    public View getErrorView() {
        return mErrorView;
    }

    public View getErrorTextView() {
        return mErrorView.findViewById(R.id.tvError);
    }

    public View getEmptyTextView() {
        return mEmptyView.findViewById(R.id.tvEmpty);
    }

    /**
     * 获取被代理的原adapter
     *
     * @return 被代理的原adapter
     */
    public RecyclerView.Adapter getDelegate() {
        return delegate;
    }

    private class NoMoreHolder extends RecyclerView.ViewHolder {
        TextView tvNoMore;

        NoMoreHolder(View itemView) {
            super(itemView);
            tvNoMore = itemView.findViewById(R.id.tvNoMore);
            tvNoMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onNoMoreClickListener != null) {
                        onNoMoreClickListener.onNoMoreClick(view);
                    }
                }
            });
        }
    }

    private class EmptyHolder extends RecyclerView.ViewHolder {
        TextView tvEmpty;

        EmptyHolder(View itemView) {
            super(itemView);
            tvEmpty = itemView.findViewById(R.id.tvEmpty);
            tvEmpty.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onEmptyClickListener != null) {
                        onEmptyClickListener.onEmptyClick(view);
                    }
                }
            });
        }
    }

    private class ErrorHolder extends RecyclerView.ViewHolder {
        TextView tvError;

        ErrorHolder(View itemView) {
            super(itemView);
            tvError = itemView.findViewById(R.id.tvError);
            tvError.setText(getContent(itemView.getContext()));
            tvError.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onErrorClickListener != null) {
                        onErrorClickListener.onErrorClick(view);
                    }
                }
            });
        }

        private CharSequence getContent(Context context) {
            String st1 = context.getString(R.string.title_net_error_1);
            String st2 = context.getString(R.string.title_net_error_2);
            SpannableString spannableString = new SpannableString(st1 + st2);
            spannableString.setSpan(new TextAppearanceSpan(itemView.getContext(), R.style.text_error), st1.length(), st1.length() + st2.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
            return spannableString;
        }
    }

    private class DefaultHolder extends RecyclerView.ViewHolder {

        DefaultHolder(View itemView) {
            super(itemView);
        }
    }
}
