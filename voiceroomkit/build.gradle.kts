import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * Copyright (c) 2022 NetEase, Inc. All rights reserved.
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */

plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdk = 35
    namespace = "com.netease.yunxin.kit.voiceroomkit"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}
dependencies {
    // androidx
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("androidx.core:core-ktx:1.7.0")

    // xkit
    implementation("com.netease.yunxin.kit:alog:1.1.0")
//    implementation(
//        fileTree("../app/libs") {
//            include("common-ui-1.3.9.aar")
//        }
//    )

    implementation("com.netease.yunxin.kit.common:common:1.3.9")
    implementation("com.netease.yunxin.kit.common:common-network:1.1.8")
    api("com.netease.yunxin.kit.room:roomkit:1.34.0")

    implementation("com.google.code.gson:gson:2.10.1")
}
