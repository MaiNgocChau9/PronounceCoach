package com.openpronounce.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Material-You style expandable action button, pinned bottom-right. Rounded-square
 * shapes for the whole family; tapping the main button reveals two options: type your
 * own text, or get a random word.
 */
@Composable
fun CreateFab(
    onCustomText: () -> Unit,
    onRandomWord: () -> Unit,
    onPickSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(250),
        label = "fabRotate"
    )

    Box(modifier = modifier) {
        Column(horizontalAlignment = Alignment.End) {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FabAction(
                        icon = Icons.Filled.Edit,
                        label = t("Custom text", "Tự nhập"),
                        shape = smallFabShape(),
                    ) {
                        expanded = false
                        onCustomText()
                    }
                    FabAction(
                        icon = Icons.Filled.Shuffle,
                        label = t("Random", "Ngẫu nhiên"),
                        shape = smallFabShape(),
                    ) {
                        expanded = false
                        onRandomWord()
                    }
                    FabAction(
                        icon = Icons.Filled.GraphicEq,
                        label = t("Practice a sound", "Luyện theo âm"),
                        shape = smallFabShape(),
                    ) {
                        expanded = false
                        onPickSound()
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            FloatingActionButton(
                onClick = { expanded = !expanded },
                shape = mainFabShape(),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(68.dp)
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = if (expanded) t("Close", "Đóng") else t("Create practice", "Tạo buổi luyện"),
                    modifier = Modifier
                        .size(30.dp)
                        .rotate(rotation)
                )
            }
        }
    }
}

@Composable
private fun FabAction(
    icon: ImageVector,
    label: String,
    shape: Shape,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        FloatingActionButton(
            onClick = onClick,
            shape = shape,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
            modifier = Modifier.size(50.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
        }
    }
}

/**
 * Rounded-square silhouettes, slightly tighter on the small buttons so the group
 * reads as one family.
 */
private fun mainFabShape(): Shape = RoundedCornerShape(20.dp)
private fun smallFabShape(): Shape = RoundedCornerShape(14.dp)
