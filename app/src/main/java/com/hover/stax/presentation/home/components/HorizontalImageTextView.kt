/*
 * Copyright 2022 Stax
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hover.stax.presentation.home.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.hover.stax.R

@Composable
fun HorizontalImageTextView(
    @DrawableRes drawable: Int,
    @StringRes stringRes: Int,
    modifier: Modifier = Modifier,
    textStyle: TextStyle
) {
    Row(horizontalArrangement = Arrangement.Start, modifier = modifier) {
        Image(
            painter = painterResource(id = drawable),
            contentDescription = null,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        Text(
            text = stringResource(id = stringRes),
            style = textStyle,
            modifier = Modifier
                .padding(start = dimensionResource(id = R.dimen.margin_13))
                .align(Alignment.CenterVertically),
            color = colorResource(id = R.color.offWhite)
        )
    }
}