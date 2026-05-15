package com.sixblock.app.ui.common

import androidx.fragment.app.Fragment
import com.sixblock.app.SixBlockApplication

val Fragment.sixBlockFactory: SixBlockViewModelFactory
    get() = SixBlockViewModelFactory((requireActivity().application as SixBlockApplication).container)
