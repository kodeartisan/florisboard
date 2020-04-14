package dev.patrickgold.florisboard.ime.keyboard

import android.content.Context
import android.graphics.Canvas
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.core.view.children
import com.google.android.flexbox.FlexboxLayout
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.ime.popup.KeyPopupManager
import dev.patrickgold.florisboard.ime.core.FlorisBoard
import dev.patrickgold.florisboard.ime.key.KeyView
import dev.patrickgold.florisboard.ime.key.KeyCode
import dev.patrickgold.florisboard.ime.key.KeyboardMode
import dev.patrickgold.florisboard.ime.layout.ComputedLayoutData

class KeyboardView : LinearLayout {

    private var hasCapsRecentlyChanged: Boolean = false
    private var shouldPrepareKeyPopups: Boolean = false
    private val osHandler = Handler()

    var caps: Boolean = false
    var capsLock: Boolean = false
    var florisboard: FlorisBoard? = null
    var computedLayout: ComputedLayoutData? = null
    var inputMethodService: InputMethodService? = null
    val popupManager =
        KeyPopupManager(this)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.customKeyboardStyle)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int) : super(context, attrs, defStyleAttrs)

    private fun buildLayout() {
        destroyLayout()
        val context = ContextThemeWrapper(context,
            R.style.KeyboardTheme_MaterialLight
        )
        this.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        val layout = computedLayout
        if (layout != null) {
            for (row in layout.arrangement) {
                val rowView =
                    KeyboardRowView(
                        context
                    )
                val rowViewLP = FlexboxLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
                rowView.layoutParams = rowViewLP
                rowView.setPadding(
                    resources.getDimension(R.dimen.keyboard_row_marginH).toInt(),
                    resources.getDimension(R.dimen.keyboard_row_marginV).toInt(),
                    resources.getDimension(R.dimen.keyboard_row_marginH).toInt(),
                    resources.getDimension(R.dimen.keyboard_row_marginV).toInt()
                )
                for (key in row) {
                    val keyView = KeyView(context, key, this)
                    /*val keyViewLP = FlexboxLayout.LayoutParams(
                        resources.getDimension(R.dimen.key_width).toInt(),
                        resources.getDimension(R.dimen.key_height).toInt()
                    )
                    keyViewLP.setMargins(
                        resources.getDimension(R.dimen.key_marginH).toInt(), 0,
                        resources.getDimension(R.dimen.key_marginH).toInt(), 0
                    )
                    keyView.layoutParams = keyViewLP*/
                    rowView.addView(keyView)
                }
                this.addView(rowView)
            }
        }
        shouldPrepareKeyPopups = true
    }

    private fun destroyLayout() {
        removeAllViews()
        //popupManager.reset()
        shouldPrepareKeyPopups = false
    }

    fun setKeyboardMode(keyboardMode: KeyboardMode) {
        computedLayout = florisboard?.layoutManager?.computeLayoutFor(keyboardMode)
        buildLayout()
    }

    fun onKeyClicked(code: Int) {
        val ic = inputMethodService?.currentInputConnection ?: return
        when (code) {
            KeyCode.DELETE -> ic.deleteSurroundingText(1, 0)
            KeyCode.ENTER -> {
                val action = inputMethodService?.currentInputEditorInfo?.imeOptions ?: EditorInfo.IME_NULL
                Log.d("imeOptions", action.toString())
                Log.d("imeOptions action only", (action and 0xFF).toString())
                if (action and EditorInfo.IME_FLAG_NO_ENTER_ACTION > 0) {
                    ic.sendKeyEvent(
                        KeyEvent(
                            KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_ENTER
                        )
                    )
                } else {
                    when (action and EditorInfo.IME_MASK_ACTION) {
                        EditorInfo.IME_ACTION_DONE,
                        EditorInfo.IME_ACTION_GO,
                        EditorInfo.IME_ACTION_NEXT,
                        EditorInfo.IME_ACTION_PREVIOUS,
                        EditorInfo.IME_ACTION_SEARCH,
                        EditorInfo.IME_ACTION_SEND -> {
                            ic.performEditorAction(action)
                        }
                        else -> {
                            ic.sendKeyEvent(
                                KeyEvent(
                                    KeyEvent.ACTION_DOWN,
                                    KeyEvent.KEYCODE_ENTER
                                )
                            )
                        }
                    }
                }
            }
            KeyCode.LANGUAGE_SWITCH -> {}
            KeyCode.SHIFT -> {
                if (hasCapsRecentlyChanged) {
                    osHandler.removeCallbacksAndMessages(null)
                    caps = true
                    capsLock = true
                    hasCapsRecentlyChanged = false
                } else {
                    caps = !caps
                    capsLock = false
                    hasCapsRecentlyChanged = true
                    osHandler.postDelayed({
                        hasCapsRecentlyChanged = false
                    }, 300)
                }
            }
            KeyCode.VIEW_SYMOBLS -> {}
            else -> {
                var text = code.toChar()
                if (caps) {
                    text = Character.toUpperCase(text)
                }
                ic.commitText(text.toString(), 1)
                if (!capsLock) {
                    caps = false
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        /*for (rowView in children) {
            for (keyView in (rowView as KeyboardRowView).children) {
                popupManager.updateLocation(keyView as KeyView)
            }
        }*/
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        /*if (shouldPrepareKeyPopups) {
            for (rowView in children) {
                for (keyView in (rowView as KeyboardRowView).children) {
                    popupManager.prepare(keyView as KeyView)
                }
            }
            shouldPrepareKeyPopups = false
        }*/
    }
}