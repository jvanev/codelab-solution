package com.sap.codelab.view.home

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.CompoundButton
import com.sap.codelab.databinding.RecyclerviewMemoBinding
import com.sap.codelab.model.Memo

/**
 * View holder for Memos.
 */
internal class MemoViewHolder(private val binding: RecyclerviewMemoBinding) : RecyclerView.ViewHolder(binding.root) {

    /**
     * Updates the memo view with the given memo.
     */
    fun update(memo: Memo, onClick: View.OnClickListener, onCheckboxChanged: CompoundButton.OnCheckedChangeListener) {
        binding.run {
            memoTitle.text = memo.title
            memoText.text = memo.description
        }
        updateCheckbox(memo, onCheckboxChanged)
        //This is needed if the user selects a given memo to show the detail screen
        itemView.tag = memo
        itemView.setOnClickListener(onClick)
    }

    /**
     * Updates the checkbox view.
     */
    private fun updateCheckbox(memo: Memo, onCheckboxChanged: CompoundButton.OnCheckedChangeListener) {
        // if the view is reused it will already have a listener already set on it. So in order this not to be called when the value is initialized
        // we remove the listener and set it back.
        binding.checkBox.apply {
            setOnCheckedChangeListener(null)
            isChecked = memo.isDone
            // We only let the user edit the checkbox if the item has not been marked as "done"
            isEnabled = !memo.isDone
            // We need the memo if the user ticks the checkbox, so we can update the memo
            tag = memo
            setOnCheckedChangeListener(onCheckboxChanged)
        }
    }
}