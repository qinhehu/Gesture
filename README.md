# Gesture
use annotation extend gesture

## Demo

| sldeslip & autoclose | longpress & drag |
| ------------ | ------------- |
| ![Demo gif](https://github.com/Abeltongtong/Gesture/blob/master/sideslip.gif) | ![Demo gif](https://github.com/Abeltongtong/Gesture/blob/master/longpressdrag.gif)  |

## Usage
```java

    compile 'com.qinhe.gesture:gesture:1.0.1'
    
    @LongTouch
    @Drag
    @TurnRightSideslip(value = {R.id.image_read3, R.id.image_read4})
    @TurnLeftSideslip(autoClose = true, value = {R.id.image_read, R.id.image_read2})
    RecyclerView list;
```

License
======

```
   Copyright 2017 qinhe

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
