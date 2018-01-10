WatchFace
===========
Android WatchFace Sample 오픈소스를 이용하여 나만의 인터페이스를 가진 SmartWatchFace 생성을 목표.

자신의 SmartWatch에 자신만의 인터페이스를 형성하고 싶어하는 사람들에게 필요한 소스코드.

계획
=======
1. Android-WatchFace Fork.
2. Android studio에 source clone.
3. build 및 run 시도해보면서 소스 코드 이해하기.
4. 필자만의 인터페이스 형성하기.
5. 자신만의 인터페이스를 형성하고 싶어하는 사람들에게 이해하기 쉽게 인터페이스 관련 소스 주석 달기.

같이 협업 하시거나 작업에 도움을 주실분들은 언제든지 오세요.https://github.com/hth9021/android-WatchFace.
======================================================================================================


아래 내용은 Android WatchFace Sample 소스 설명, 관련사이트, 도움이 될 만한 사이트, 관련라이선스인 Apache2.0, 저작권에 관한 내용.
===========================================================================================================================

Android WatchFace Sample
===================================

A simple sample that demonstrates watch faces and complications for Wear 2.0.

Introduction
------------

This sample demonstrates how to create watch faces for Android Wear.

The analog watch face covers best practices for Wear 2.0 including complications and a new
configuration Activity for configuring both the watch face styles and the complications themselves.
We recommend you reuse the configuration code and follow the UX patterns in your watch face's
configuration activity. You can see a preview in the screenshots section of the README.

The analog watch face has three main classes:

- AnalogComplicationWatchFaceService.java (watchface folder) - Renders the watch face and active
complications.

- AnalogComplicationConfigActivity.java (config folder) - Allows user to adjust watch face settings
(arm and background colors) and watch face complications.

- AnalogComplicationConfigData.java (models folder) - Populates the configuration screen in the
previous step by returning a list of items. Each item represents something you can customize on the
watch face. Items include a preview of the watch face (where you can pick your complications),
marker color, background color, unread notifications, and background image complication. You can
extend this and add your own.


We include several older watch faces to show best practices for Wear 1.0. If you are targeting
older devices, you may want to review those watch faces (digital, opengl, and interactive).

The digital watch face includes a watch-side and phone-side configuration example.

This sample includes both a phone app and a wearable app.

Because watch face apps do not have a default Activity in their project, you will need to set your
Configurations to "Do not launch Activity" for both the Wear and Application modules. If you are
unsure how to do this, please review the "Run Starter project" section in the
[Google Watch Face Code Lab][1].

[1]: https://codelabs.developers.google.com/codelabs/watchface/index.html

Pre-requisites
--------------

- Android SDK 27
- Android Build Tools v26.0.2
- Android Support Repository

Screenshots
-------------

<img src="screenshots/analog-face.png" height="400" alt="Screenshot"/> <img src="screenshots/analog-watch-side-config-all.png" height="400" alt="Screenshot"/> <img src="screenshots/analog-watch-side-config-1.png" height="400" alt="Screenshot"/> <img src="screenshots/analog-watch-side-config-2.png" height="400" alt="Screenshot"/> <img src="screenshots/digital-face.png" height="400" alt="Screenshot"/> <img src="screenshots/digital-phone-side-config.png" height="400" alt="Screenshot"/> <img src="screenshots/digital-watch-side-config.png" height="400" alt="Screenshot"/> <img src="screenshots/interactive-face.png" height="400" alt="Screenshot"/> <img src="screenshots/tilt-face.png" height="400" alt="Screenshot"/> 

Getting Started
---------------

This sample uses the Gradle build system. To build this project, use the
"gradlew build" command or use "Import Project" in Android Studio.

Support
-------

- Google+ Community: https://plus.google.com/communities/105153134372062985968
- Stack Overflow: http://stackoverflow.com/questions/tagged/android

If you've found an error in this sample, please file an issue:
https://github.com/googlesamples/android-WatchFace

Patches are encouraged, and may be submitted by forking this project and
submitting a pull request through GitHub. Please see CONTRIBUTING.md for more details.

License
-------

Copyright 2017 The Android Open Source Project, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.
