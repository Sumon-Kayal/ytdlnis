package com.deniscerri.ytdl.ui.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.deniscerri.ytdl.databinding.ItemAccentBinding
import com.deniscerri.ytdl.ui.more.settings.SettingHost
import com.deniscerri.ytdl.util.ThemeUtil
import com.google.android.material.color.DynamicColors
import com.google.android.material.R as MaterialR

/**
 * Grid adapter for the "theme_accent" picker bottom sheet. Same shape as
 * [IconsSheetAdapter]: takes the [SettingHost], applies the pick directly on
 * click, then asks the host to refresh + recreate.
 *
 * Each card previews its accent by wrapping the option's style in a
 * [ContextThemeWrapper] and reading colorPrimary/colorOnPrimary back off it —
 * same technique as ytdlnis's non-Default themes already use for the app
 * itself, just applied to a preview swatch instead of the whole activity.
 * "Default" (Material You) has no fixed style to wrap, so it's previewed via
 * [DynamicColors.wrapContextIfAvailable] instead, which falls back to
 * BaseTheme's own colors on pre-Android-12 where dynamic color isn't available.
 */
class AccentAdapter(val host: SettingHost) : RecyclerView.Adapter<AccentAdapter.AccentViewHolder>() {

    class AccentViewHolder(
        val binding: ItemAccentBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccentViewHolder {
        val binding = ItemAccentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AccentViewHolder(binding)
    }

    override fun getItemCount() = ThemeUtil.availableAccents.size

    /**
     * Binds an accent preview and makes selecting it persist the accent, reapply themes to the
     * tracked activities, and refresh the settings host.
     */
    override fun onBindViewHolder(holder: AccentViewHolder, position: Int) {
        val accent = ThemeUtil.availableAccents[position]
        val context = holder.binding.root.context

        val themedContext = if (accent.value == "Default") {
            DynamicColors.wrapContextIfAvailable(context)
        } else {
            ContextThemeWrapper(context, accent.styleResource)
        }

        val primary = themedContext.colorFromAttr(MaterialR.attr.colorPrimary)
        val onPrimary = themedContext.colorFromAttr(MaterialR.attr.colorOnPrimary)

        holder.binding.apply {
            accentName.text = root.context.getString(accent.nameResource)
            accentCard.setCardBackgroundColor(primary)
            accentCard.strokeColor = onPrimary
            accentName.setTextColor(onPrimary)
            accentCard.rippleColor = ColorStateList.valueOf(onPrimary).withAlpha(40)

            root.setOnClickListener {
                val preferences = PreferenceManager.getDefaultSharedPreferences(host.getHostContext())
                preferences.edit { putString("theme_accent", accent.value) }
                ThemeUtil.updateThemes()
                host.refreshUI()
            }
        }
    }
}

/** Resolves a color-valued theme attribute from this context. */
private fun Context.colorFromAttr(attr: Int): Int {
    val value = TypedValue()
    theme.resolveAttribute(attr, value, true)
    return value.data
}
