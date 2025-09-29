package com.jerryz.poems.ui.home

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 为诗词列表添加优雅间距和动画效果的ItemDecoration
 */
class PoemItemDecoration : RecyclerView.ItemDecoration() {
    
    companion object {
        private const val VERTICAL_SPACING = 16 // dp转换为px会在使用时处理
        private const val HORIZONTAL_SPACING = 16
        private const val TOP_SPACING = 8
        private const val BOTTOM_SPACING = 8
    }
    
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        
        val density = parent.context.resources.displayMetrics.density
        val verticalSpacing = (VERTICAL_SPACING * density).toInt()
        val horizontalSpacing = (HORIZONTAL_SPACING * density).toInt()
        val topSpacing = (TOP_SPACING * density).toInt()
        val bottomSpacing = (BOTTOM_SPACING * density).toInt()
        
        // 设置水平边距
        outRect.left = horizontalSpacing
        outRect.right = horizontalSpacing
        
        // 设置垂直边距
        if (position == 0) {
            outRect.top = topSpacing
        } else {
            outRect.top = verticalSpacing / 2
        }
        
        outRect.bottom = if (position == (state.itemCount - 1)) {
            bottomSpacing
        } else {
            verticalSpacing / 2
        }
    }
    
    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(canvas, parent, state)
        
        // 为可见的子视图添加轻微的透明度变化，创建深度感
        val childCount = parent.childCount
        val parentHeight = parent.height
        
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val childTop = child.top.toFloat()
            val childBottom = child.bottom.toFloat()
            val childCenter = (childTop + childBottom) / 2
            val parentCenter = parentHeight / 2f
            
            // 计算距离中心的比例，用于调整透明度
            val distanceRatio = kotlin.math.abs(childCenter - parentCenter) / parentCenter
            val alpha = 1.0f - (distanceRatio * 0.1f).coerceIn(0f, 0.3f)
            
            child.alpha = alpha.coerceIn(0.7f, 1.0f)
        }
    }
}