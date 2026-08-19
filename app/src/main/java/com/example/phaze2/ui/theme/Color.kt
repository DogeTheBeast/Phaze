package com.example.phaze2.ui.theme

import androidx.compose.ui.graphics.Color

// i3-inspired palette from theme.yaml (base16)
// base01 #0B1014  base00 #0E1419  base02 #243340  base03 #323232
// base05 #E5E1CF  base06 #EEEEEE  base07 #FFFFFF
// base08 #FF3333  base09 #F19618  base0A #E6C446  base0B #B8CC52
// base0C #95E5CB  base0D #36A3D9  base0E #F07078  base0F #FF6565

val Primary = Color(0xFF36A3D9)              // base0D blue
val OnPrimary = Color(0xFF0B1014)             // base01
val PrimaryContainer = Color(0xFF1C4461)    // derived dark blue
val OnPrimaryContainer = Color(0xFFCFE8F7)   // derived light blue
val Secondary = Color(0xFFF07078)             // base0E pink
val SecondaryContainer = Color(0xFF59303A)    // derived dark magenta
val OnSecondaryContainer = Color(0xFFFFD9DE)  // derived light pink
val Tertiary = Color(0xFF95E5CB)              // base0C cyan
val TertiaryContainer = Color(0xFF1A4A3A)     // derived
val OnTertiaryContainer = Color(0xFFD5F5EA)   // derived
val Error = Color(0xFFFF3333)                 // base08 red
val ErrorContainer = Color(0xFF93000A)
val OnErrorContainer = Color(0xFFFFDAD6)
val Success = Color(0xFFB8CC52)              // base0B green — connected/downloaded state
val OnSuccess = Color(0xFF0B1014)            // base01
val Background = Color(0xFF0B1014)            // base01 page-bg
val OnBackground = Color(0xFFE5E1CF)          // base05 on-surface
val Surface = Color(0xFF0E1419)               // base00 surface
val OnSurface = Color(0xFFE5E1CF)             // base05
val SurfaceVariant = Color(0xFF243340)         // base02 container
val OnSurfaceVariant = Color(0xFF9B9B93)      // derived muted grey
val Outline = Color(0xFF5F7082)               // derived blue-grey
val OutlineVariant = Color(0xFF2B3B4C)        // derived
val Scrim = Color(0xFF000000)
val InverseSurface = Color(0xFFE5E1CF)
val InverseOnSurface = Color(0xFF0E1419)
val InversePrimary = Color(0xFF1C4461)
val SurfaceDim = Color(0xFF0B1014)
val SurfaceBright = Color(0xFF131E27)
val SurfaceContainerLowest = Color(0xFF0B1014)
val SurfaceContainerLow = Color(0xFF131E27)
val SurfaceContainer = Color(0xFF243340)
val SurfaceContainerHigh = Color(0xFF2D3D4D)
val SurfaceContainerHighest = Color(0xFF3B4C5F)
