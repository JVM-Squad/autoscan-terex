package com.gunnarro.android.terex.ui.adapter;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.gunnarro.android.terex.domain.entity.Invoice;
import com.gunnarro.android.terex.ui.view.InvoiceViewHolder;
import com.gunnarro.android.terex.utility.Utility;

public class InvoiceListAdapter extends ListAdapter<Invoice, InvoiceViewHolder> implements AdapterView.OnItemClickListener {

    public InvoiceListAdapter(@NonNull DiffUtil.ItemCallback<Invoice> diffCallback) {
        super(diffCallback);
        Log.d("InvoiceListAdapter", "init");
    }

    @NonNull
    @Override
    public InvoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return InvoiceViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(InvoiceViewHolder holder, int position) {
        holder.bindListLine(getItem(position));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(Utility.buildTag(getClass(), "onItemClick"), "position: " + position + ", id: " + id);
        notifyItemRangeRemoved(position, 1);
    }

    public static class InvoiceDiff extends DiffUtil.ItemCallback<Invoice> {

        @Override
        public boolean areItemsTheSame(@NonNull Invoice oldItem, @NonNull Invoice newItem) {
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Invoice oldItem, @NonNull Invoice newItem) {
            return oldItem.equals(newItem);
        }
    }
}
