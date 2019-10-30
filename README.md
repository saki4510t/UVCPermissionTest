# UVCPermissionTest
===
Test app to confirm whether or not app can get USB permission for UVC device with verious target SDK version.

Copyright (c) 2019 saki t_saki@serenegiant.com

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

## Background
As documents on [Android Developers](https://developer.android.com/reference/android/hardware/usb/UsbManager.html#requestPermission(android.hardware.usb.UsbDevice,%20android.app.PendingIntent)) shows, app that use USB WebCam(UVC device) needs `CAMERA permission` if it runs on Android 9 and later.
But even if app has `CAMERA permission`, app can't get USB permission to access UVC device on Android 10 if target SDK version is 28 and later.

### The result of my tests  

OK in following tables means app could get USB permission NG means app could't get permission.

* targe SDK version <= 27

 |   | UVC, has CAMERA permission | UVC, has no CAMERA permission | other USB device | 
 --- | :---: | :---: | :---:
 | Android <= 8 | OK | OK | OK |
 | Android 9 | OK | NG(expected) | OK |
 | Android 10 | OK | NG(expected) | OK |

* targe SDK version >= 28

 |   | UVC, has CAMERA permission | UVC, has no CAMERA permission | other USB device | 
 --- | :---: | :---: | :---:
 | Android < 8 | OK | OK | OK |
 | Android 9 | OK | NG(expected) | OK |
 | Android 10 | **NG (unexpected)** | NG(expected) | OK |


* Test devices:
   * Android 10:
      * Pixel3
      * Essential Phone PH-1
   * Android 9:
      * Moto g(6) plus
   * Android 8:
      * Moto Z Play
      * Moto Z2 Play
      * ASUS ZenPhone 4 Pro    


### Note:  
   When app request permission, or when user give permission from system permission dialog, Android output error message on logCat.  
   ```
   UsbUserSettingsManager: Camera permission required for USB video class devices
   ```