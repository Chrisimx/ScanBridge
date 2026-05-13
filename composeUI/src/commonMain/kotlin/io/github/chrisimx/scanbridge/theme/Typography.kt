package io.github.chrisimx.scanbridge.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import scanbridge.composeui.generated.resources.*
import scanbridge.composeui.generated.resources.Res

@Composable
fun Poppins(): FontFamily = FontFamily(
    Font(Res.font.poppins_thin, weight = FontWeight.Thin),
    Font(Res.font.poppins_thinitalic, weight = FontWeight.Thin, style = FontStyle.Italic),

    Font(Res.font.poppins_extralight, weight = FontWeight.ExtraLight),
    Font(Res.font.poppins_extralightitalic, weight = FontWeight.ExtraLight, style = FontStyle.Italic),

    Font(Res.font.poppins_light, weight = FontWeight.Light),
    Font(Res.font.poppins_lightitalic, weight = FontWeight.Light, style = FontStyle.Italic),

    Font(Res.font.poppins_regular, weight = FontWeight.Normal),
    Font(Res.font.poppins_italic, weight = FontWeight.Normal, style = FontStyle.Italic),

    Font(Res.font.poppins_medium, weight = FontWeight.Medium),
    Font(Res.font.poppins_mediumitalic, weight = FontWeight.Medium, style = FontStyle.Italic),

    Font(Res.font.poppins_semibold, weight = FontWeight.SemiBold),
    Font(Res.font.poppins_semibolditalic, weight = FontWeight.SemiBold, style = FontStyle.Italic),

    Font(Res.font.poppins_bold, weight = FontWeight.Bold),
    Font(Res.font.poppins_bolditalic, weight = FontWeight.Bold, style = FontStyle.Italic),

    Font(Res.font.poppins_extrabold, weight = FontWeight.ExtraBold),
    Font(Res.font.poppins_extrabolditalic, weight = FontWeight.ExtraBold, style = FontStyle.Italic),

    Font(Res.font.poppins_black, weight = FontWeight.Black),
    Font(Res.font.poppins_blackitalic, weight = FontWeight.Black, style = FontStyle.Italic)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PoppinsTypography(): Typography {
    val poppinsFont = Poppins()

    return Typography(
        titleSmall = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        ),
        titleMediumEmphasized = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleLargeEmphasized = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        labelSmall = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            lineHeight = 14.sp,
            fontSize = 11.sp,
            letterSpacing = 0.sp
        ),
        labelMedium = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            lineHeight = 14.sp,
            fontSize = 14.sp,
            letterSpacing = 0.sp
        ),
        labelLarge = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp
        ),
        bodySmall = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp
        ),
        bodySmallEmphasized = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp
        ),
        bodyMediumEmphasized = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        bodyLargeEmphasized = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        displaySmall = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp
        ),
        displayMedium = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp
        ),
        displayLarge = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp
        ),
        displaySmallEmphasized = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        ),
        displayMediumEmphasized = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        ),
        displayLargeEmphasized = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        headlineMediumEmphasized = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = poppinsFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp
        )
    )
}
