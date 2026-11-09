package com.sap.codelab.view.home

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import com.sap.codelab.databinding.RecyclerviewMemoBinding
import com.sap.codelab.model.Memo
import kotlin.math.max

/**
 * Adapter containing a set of memos.
 */
internal class MemoAdapter(private val items: MutableList<Memo>,
                           private val onClick: View.OnClickListener,
                           private val onCheckboxChanged: CompoundButton.OnCheckedChangeListener) : RecyclerView.Adapter<MemoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewTypee: Int): MemoViewHolder {
        return MemoViewHolder(newItemViewBinding(parent))
    }

    override fun onBindViewHolder(holder: MemoViewHolder, position: Int) {
        val memo = items[position]
        holder.update(memo, onClick, onCheckboxChanged)
    }

    override fun getItemCount(): Int = items.size

    /**
     * Updates the current list of items to the given list of items.
     */
    fun setItems(newItems: List<Memo>) {
        val count = itemCount
        items.clear()
        items.addAll(newItems)
        notifyItemRangeChanged(0, max(count, itemCount))
    }

    /**
     * Creates the view binding for a memo item displayed in the list.
     *
     * @param parent    - the parent view group of the item.
     * @return the view binding.
     */
    private fun newItemViewBinding(parent: ViewGroup): RecyclerviewMemoBinding {
        return RecyclerviewMemoBinding.inflate(LayoutInflater.from(parent.context))
    }
}