package im.isolux.opencall.ui.components

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import im.isolux.opencall.ui.theme.Glass

/** Fond gris glace avec reflet miroir diagonal. */
@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val c = Glass.colors
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.backgroundTop, c.backgroundMiddle, c.backgroundBottom)))
            .background(
                Brush.linearGradient(
                    0f to Color.Transparent,
                    0.42f to c.highlight,
                    0.5f to Color.Transparent,
                    start = Offset(0f, 0f),
                    end = Offset(1400f, 2200f),
                ),
            ),
        content = content,
    )
}

fun Modifier.glass(shape: Shape, fillTop: Color, fillBottom: Color, strokeTop: Color, strokeBottom: Color, elevation: Dp = 0.dp): Modifier =
    this
        .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape, ambientColor = Color(0x33000000), spotColor = Color(0x22000000)) else Modifier)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(fillTop, fillBottom)))
        .border(1.dp, Brush.verticalGradient(listOf(strokeTop, strokeBottom)), shape)

@Composable
fun Modifier.glassSurface(shape: Shape = RoundedCornerShape(24.dp), elevation: Dp = 0.dp): Modifier {
    val c = Glass.colors
    return glass(shape, c.glassFillTop, c.glassFillBottom, c.glassStrokeTop, c.glassStrokeBottom, elevation)
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().glassSurface().padding(vertical = 6.dp), content = content)
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.headlineLarge,
        color = Glass.colors.textPrimary,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Glass.colors.textSecondary,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun GlassDivider() {
    HorizontalDivider(Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = Glass.colors.glassStrokeBottom)
}

/** Ligne de réglage : icône, titre, sous-titre, élément de fin optionnel. */
@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val c = Glass.colors
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background((iconTint ?: c.accent).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = iconTint ?: c.accent, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = c.textPrimary, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, color = c.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val c = Glass.colors
    SettingRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onClick = if (enabled) ({ onCheckedChange(!checked) }) else null,
    ) {
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = c.callGreen, checkedThumbColor = Color.White),
        )
    }
}

/** Pastille d'état (vert : actif, orange : limité, gris : inactif). */
@Composable
fun StatusPill(text: String, color: Color) {
    Row(
        Modifier.clip(CircleShape).background(color.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun GlassButton(text: String, modifier: Modifier = Modifier, tint: Color? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Glass.colors
    val color = tint ?: c.accent
    Box(
        modifier
            .glassSurface(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = if (enabled) 0.12f else 0.04f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) color else c.textSecondary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun InfoText(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text,
        color = color ?: Glass.colors.textSecondary,
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

/** Avatar circulaire avec initiales. */
@Composable
fun Avatar(name: String?, size: Dp = 40.dp) {
    val c = Glass.colors
    val initials = name?.split(' ')?.filter { it.isNotBlank() }?.take(2)?.joinToString("") { it.first().uppercase() }
        ?.takeIf { it.isNotEmpty() && it.first().isLetter() }
    Box(
        Modifier.size(size).glass(CircleShape, c.glassFillTop, c.glassFillBottom, c.glassStrokeTop, c.glassStrokeBottom),
        contentAlignment = Alignment.Center,
    ) {
        if (initials != null) {
            Text(initials, color = c.accent, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
        } else {
            Icon(Icons.Rounded.Person, null, tint = c.textSecondary)
        }
    }
}

/**
 * Liste en verre affichée paresseusement : chaque ligne est un élément de la LazyColumn,
 * les premier et dernier segments portent les coins arrondis de la carte.
 * Indispensable pour les longues listes (contacts, historique) : une carte unique
 * contenant toutes les lignes bloque l'interface et peut faire planter l'application.
 */
fun <T> LazyListScope.glassItems(
    items: List<T>,
    key: (T) -> Any,
    row: @Composable (T) -> Unit,
) {
    itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
        GlassSegment(first = index == 0, last = index == items.lastIndex) {
            row(item)
            if (index < items.lastIndex) GlassDivider()
        }
    }
}

@Composable
private fun GlassSegment(first: Boolean, last: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val c = Glass.colors
    val r = 24.dp
    val shape = RoundedCornerShape(
        topStart = if (first) r else 0.dp, topEnd = if (first) r else 0.dp,
        bottomStart = if (last) r else 0.dp, bottomEnd = if (last) r else 0.dp,
    )
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(c.glassFillTop, c.glassFillTop.copy(alpha = c.glassFillTop.alpha * 0.9f))))
            .padding(top = if (first) 6.dp else 0.dp, bottom = if (last) 6.dp else 0.dp),
        content = content,
    )
}
