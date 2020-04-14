/*
 * Copyright (c) 2020, Salesforce.com, inc.
 * All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause
 * For full license text, see the LICENSE file in the repo root or https://opensource.org/licenses/BSD-3-Clause
 *
 */

package com.salesforce.j2v8debugging.sample.di


import com.salesforce.j2v8debugging.sample.ExampleActivity

import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Binds sub-components within the app.
 */
@Module
abstract class BuildersModule {

    @ContributesAndroidInjector
    internal abstract fun bindExampleActivity(): ExampleActivity
}
